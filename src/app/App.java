/*
 * Copyright (c) 2015, Aleksey Nikolaevich Dokshin. All right reserved.
 * Contacts: dant.it@gmail.com, dokshin@list.ru.
 */
package app;

import app.model.*;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import app.report.*;
import net.sf.jasperreports.engine.JRException;
import util.CommonTools;
import util.StringTools;

import static app.model.Helper.*;
import static util.DateTools.formatHHMMSS;
import static util.DateTools.toMillis;
import static util.StringTools.*;

/**
 * Приложение для автоматической обработки заявок, формируемых в Личном Кабинете "Системы Топливных Карт", а так же
 * (если заказано) - отправка результатов исполнения заявки на адреса электронной почты клиента.
 * <p>
 * Состоит из двух потоков - сервис обработки заявок и сервис рассылки результатов.
 * <p>
 * Состояние сервисов пишется в файлы: state/processor.state, state/sender.state.
 * <p>
 * Вывод в лог: logs/app_[дата].log - общий лог приложения; logs/proc_[дата].log - лог всех событий обработки;
 * log/send_[дата].log - лог всех событий отправки.
 *
 * @author Докшин Алексей Николаевич <dant.it@gmail.com>
 */
public class App {

    /** Логгер для вывода отладочной информации. */
    public final static LoggerExt logger = LoggerExt.getCommonLogger();
    public final static LoggerExt procLogger = LoggerExt.getNewLogger("proc");
    public final static LoggerExt sendLogger = LoggerExt.getNewLogger("send");

    /** Модель приложения. */
    public final static AppModel model = new AppModel();
    /** Версия. */
    public static final String version = "v2018.02.13";
    /** Информация о авторских правах. */
    public static final String copyright = "© Докшин Алексей Николаевич, <u>dokshin@gmail.com</u>";

    public static boolean isWindowsOS = false;
    public static boolean isLinuxOS = false;

    public static String homeDir = "";

    /**
     * Точка старта приложения.
     */
    public static void main(String[] args) {

        try {
            initLog(); // Инициализация логгера.

            initDB(); // Настройка параметров БД.

            startApp(); // Старт приложения.

        } catch (Exception ex) {
            logger.error("Ошибка запуска приложения!", ex);
        }
    }

    /**
     * Инициализация лога.
     */
    private static void initLog() {
        String s = System.getProperty("os.name", "").toLowerCase().substring(0, 3);
        if (s.equals("win")) isWindowsOS = true;
        if (s.equals("lin")) isLinuxOS = true;

        // Отключаем вывод лога в консоль.
        LoggerExt.removeConsoleOutput();
        // Логгируем все сообщения.
        logger.setLevel(Level.ALL);
        // Настраиваем логгер для файлового вывода.
        logger.enable(true).toFile();

        procLogger.setLevel(Level.ALL);
        procLogger.enable(true).toFile();
        sendLogger.setLevel(Level.ALL);
        sendLogger.enable(true).toFile();

        logger.config("Инициализация приложения");
    }

    /**
     * Настройка подключения к БД.
     */
    private static void initDB() throws Exception {
        logger.config("Настройка параметров БД");
        try {
            model.configureDB();

        } catch (Exception ex) {
            logger.error("Ошибка настройки параметров БД!", ex);
            throw ex;
        }
    }

    private static boolean isTerminated = false;

    private static boolean safeTermSleep(int time) {
        if (!CommonTools.safeInterruptedSleep(time)) {
            isTerminated = true;
            return true;
        }
        return false;
    }

    private static ServiceModel procModel, sendModel;

    /**
     * Запуск приложения.
     */
    private static void startApp() {
        procModel = new ServiceModel(ServiceModel.Kind.PROCESSOR);
        sendModel = new ServiceModel(ServiceModel.Kind.SENDER);

        final Thread p1 = new Thread(() -> serviceThreadBody(procModel));
        final Thread p2 = new Thread(() -> serviceThreadBody(sendModel));

        p1.start();
        p2.start();

        while (!isTerminated) {
            updateState();
            safeTermSleep(200);
        }
    }

    private static void serviceThreadBody(ServiceModel mod) {
        logger.infof("Старт сервиса: %s", mod.getName()); // обработки заявок.
        ServiceModel.Kind kind = mod.startService();

        while (!isTerminated) {
            try {
                mod.startDataLoad();
                ArrayList<Request> its = mod.loadRequests();
                mod.endDataLoad(its);

                if (!its.isEmpty()) {
                    mod.startBlockProcess();
                    for (Request request : its) {
                        mod.startProcess(request);
                        mod.processRequest(request);
                        mod.endProcess();
                        procLogger.infof("Обработка: №%d от %s", request.getId(), fmtDT86(request.getDtCreate()));
                        if (safeTermSleep(100)) return;
                    }
                }
                mod.setState(ServiceModel.State.SLEEPING);
                if (safeTermSleep(2000)) return;

            } catch (Exception ex) {
                mod.setState(ServiceModel.State.SLEEPING);
                procLogger.error("Ошибка при обработке заявок!", ex);
                if (safeTermSleep(2000)) return;
            }
        }
        mod.endService();
    }

    private static void processReportRequest(Request req) {

        BaseReport report;
        Client client;
        LocalDate dtstart, dtend, dtw;

        try {
            switch (req.getReportType()) {
                case TURNOVER:
                    dtstart = req.getParamAsLocalDate("dtStart");
                    dtend = req.getParamAsLocalDate("dtEnd");
                    client = model.loadClient(req.getIddClient(), req.getIddSub(), dtend);
                    report = new ClientTurnoverReport(client, dtstart, dtend);
                    break;

                case TRANSACTION:
                    dtstart = req.getParamAsLocalDate("dtStart");
                    dtend = req.getParamAsLocalDate("dtEnd");
                    client = model.loadClient(req.getIddClient(), req.getIddSub(), dtend);
                    Integer iddAzs = req.getParamAsInteger("iddAzs");
                    ClientTransactionReport.Mode mode = ClientTransactionReport.Mode.byId(req.getParamAsInteger("idReportMode"));
                    report = new ClientTransactionReport(client, dtstart, dtend, iddAzs, mode);
                    break;

                case CARD:
                    dtw = req.getParamAsLocalDate("dtw");
                    client = model.loadClient(req.getIddClient(), req.getIddSub(), dtw);
                    Card.WorkState workState = Card.WorkState.byId(req.getParamAsInteger("idWorkState"));
                    ClientCardReport.Mode mode2 = ClientCardReport.Mode.byId(req.getParamAsInteger("idReportMode"));
                    report = new ClientCardReport(client, dtw, workState, mode2);
                    break;

                default:
                    throw new ExError("Неизвестный вид отчёта!");
            }

            String path = req.getAnswerPath();
            createDirectoryIfNotExist(path);

            try {
                // Генерируем отчёт.
                report.build();
            } catch (ExError ex) {
                // Облом. завершаем запрос или ввести счётчик попыток?
                throw new ExError("Ошибка построения отчёта!");
            }


            String name = req.getAutoFileName();
            String filenamePDF = name + ".pdf";
            String filenameZIP = name + ".zip";
            String fullfilenameZIP = path + File.separator + filenameZIP;
            try {
                File file = new File(fullfilenameZIP);
                FileOutputStream fos = new FileOutputStream(file);
                ZipOutputStream outs = new ZipOutputStream(fos);
                ZipEntry ze = new ZipEntry(filenamePDF);
                outs.putNextEntry(ze);
                report.exportToPDF(outs);
                outs.closeEntry();
                outs.close();

                req.setFile(filenameZIP, (int) file.length());

            } catch (JRException ex) {
                deleteFileSafe(fullfilenameZIP);
                throw new ExError("Ошибка механизма экспорта отчёта!");
            } catch (Exception ex) {
                deleteFileSafe(fullfilenameZIP);
                throw new ExError("Ошибка экспорта отчёта!");
            }

            // Меняем статус заявки на завершенный или ожидающий отправки.
            try {
                if (req.getSendTryRemain() != null && req.getSendTryRemain() > 0 && !isEmptySafe(client.getEmail())) {
                    req.setState(Request.State.SENDING, null);
                } else {
                    req.setState(Request.State.FINISHED, null);
                }
                model.updateRequestProcess(req);
            } catch (Exception ex) {
                throw new ExError("Ошибка изменения состояния заявки в БД!");
            }

        } catch (Exception ex) {
            // Облом. завершаем запрос или ввести счётчик попыток?
            if (req.getState() != Request.State.ERROR) {
                req.setState(Request.State.ERROR, ex.getMessage());
            } else {
                req.setState(Request.State.ERROR, req.getResult() + " (" + ex.getMessage() + ")");
            }
            try {
                model.updateRequestProcess(req);
            } catch (Exception ignore) {
            }
        }
    }

    private static void sendAnswerForReportRequest(Request req) {
        try {
            Client client = model.loadClient(req.getIddClient(), req.getIddSub(), LocalDate.now());

            ReportsMailer mailer = new ReportsMailer("smtp.tp-rk.ru", "reports@tp-rk.ru", "XuQ9eb9hqZ");

            TextBuilder sb = new TextBuilder();
            sb.println("Ответ на заявку: №%d от %s", req.getId(), fmtDT84(req.getDtCreate()));
            sb.println("---");
            sb.println("Данное сообщение сформировано службой автоматической рассылки ответов на заявки,");
            sb.println("созданные в Личном Кабинете (ЛК) клиента Системы Топливных Карт (СТК) %s.", client.getFirm().getTitle());
            sb.println("Не отвечайте на это письмо, входящие сообщения на данный адрес заблокрованы.");

            //if (true) throw new ExError("123");
            mailer.sendMail("PC.FCService", client.getEmail(), null, "Автоматический ответ за заявку ЛК СТК!", sb.toString(),
                    req.getAnswerPath() + File.separator + req.getFileName());
            logger.infof("SEND ANSWER: %d of %s", req.getId(), fmtDT84(req.getDtCreate()));

            req.setState(Request.State.FINISHED, null);

        } catch (Exception mex) {
            logger.error("Ошибка отправки ответа на заявку!", mex);
            if (req.getSendTryRemain() != null && req.getSendTryRemain() <= 1) {
                req.setState(Request.State.ERROR, "Ошибка отправки ответа на заявку!");
            }
        }
        try {
            model.updateRequestSend(req);
        } catch (Exception ignore) {
        }
    }


    public static class ServiceModel {

        public enum Kind {
            PROCESSOR, SENDER
        }

        public enum State {
            STARTING, DATALOADING, PROCESSING, SLEEPING, STOPPED
        }

        public Kind kind;
        public LocalDateTime startTime, endTime;
        public State state;
        public LocalDateTime curLoadTime;
        public long curLoadMsec;
        public LocalDateTime curBlockProcessTime;
        public LocalDateTime curProcessTime;
        public ArrayList<Request> curItems;
        public int curIndex;

        public int loadCount;
        public int processedCount;
        public long loadMsec, processMsec;

        public ServiceModel(Kind kind) {
            this.kind = kind;
            startTime = LocalDateTime.now();
            startTime = null;
            state = State.STARTING;
            curLoadTime = null;
            curLoadMsec = 0;
            curBlockProcessTime = null;
            curProcessTime = null;
            curItems = null;
            curIndex = 0;
            loadCount = 0;
            processedCount = 0;
            loadMsec = 0;
            processMsec = 0;
        }

        public synchronized void copyTo(ServiceModel dst) {
            dst.kind = kind;
            dst.startTime = startTime;
            dst.endTime = endTime;
            dst.state = state;
            dst.curLoadTime = curLoadTime;
            dst.curLoadMsec = curLoadMsec;
            dst.curBlockProcessTime = curBlockProcessTime;
            dst.curProcessTime = curProcessTime;
            dst.curItems = curItems;
            dst.curIndex = curIndex;
            dst.loadCount = loadCount;
            dst.processedCount = processedCount;
            dst.loadMsec = loadMsec;
            dst.processMsec = processMsec;
        }

        public synchronized Kind startService() {
            startTime = LocalDateTime.now();
            state = State.STARTING;
            return kind;
        }

        public synchronized void endService() {
            endTime = LocalDateTime.now();
            state = State.STOPPED;
        }

        public synchronized void startDataLoad() {
            state = State.DATALOADING;
            curLoadTime = LocalDateTime.now();
            curLoadMsec = 0;
        }

        public ArrayList<Request> loadRequests() throws ExError {
            switch (kind) {
                case PROCESSOR:
                    return model.loadRequests(Request.State.PROCESSING, 100);
                case SENDER:
                    return model.loadRequests(Request.State.SENDING, 100);
                default:
                    return null;
            }
        }

        public synchronized void endDataLoad(ArrayList<Request> items) {
            curLoadMsec = System.currentTimeMillis() - toMillis(curLoadTime);
            curItems = items;
            curIndex = 0;
            loadCount++;
            state = items.size() > 0 ? State.PROCESSING : State.SLEEPING;
            //
            loadMsec += curLoadMsec;
        }

        public synchronized void startBlockProcess() {
            state = State.PROCESSING;
            curBlockProcessTime = LocalDateTime.now();
            curProcessTime = LocalDateTime.now();
        }

        public synchronized void startProcess(Request req) {
            state = State.PROCESSING;
            curProcessTime = LocalDateTime.now();
        }

        public void processRequest(Request req) throws ExError {
            switch (kind) {
                case PROCESSOR:
                    processReportRequest(req);
                    break;
                case SENDER:
                    sendAnswerForReportRequest(req);
                    break;
            }
        }

        public synchronized void endProcess() {
            curIndex++;
            processedCount++;
            //
            processMsec += System.currentTimeMillis() - toMillis(curProcessTime);
        }

        public String getName() {
            switch (kind) {
                case PROCESSOR:
                    return "Обработка заявок";
                case SENDER:
                    return "Рассылка ответов на заявки";
                default:
                    return "";
            }
        }

        public synchronized void setState(State state) {
            this.state = state;
        }
    }


    private static ANSIOut out = new ANSIOut();
    // Время последнего обновления инфы на экране.
    private static LocalDateTime stateUpdateTime = null;
    private static int w = 80;
    private static final String delimS = StringTools.fill('─', w);
    private static final String delimSB = StringTools.fill('━', w);
    private static final String delimD = StringTools.fill('═', w);
    private static final String delim1_4 = StringTools.fill('▂', w);
    private static final String delim1_2 = StringTools.fill('▄', w);
    private static final String delim3_4 = StringTools.fill('▆', w);

    private static final String delim111 = StringTools.fill('─', w - 20);

    private static ServiceModel PM = new ServiceModel(ServiceModel.Kind.PROCESSOR);
    private static ServiceModel SM = new ServiceModel(ServiceModel.Kind.SENDER);

    private static void outServiceSection(ServiceModel mod) {
        String name = " " + mod.getName() + " ";
        int wrest = w - name.length() - 1;
        String h1 = "---", h2 = "---", h3 = "---", h4 = "---";
        String s1 = "---", s2 = "---", s3 = "---", s4 = "---";
        long time = System.currentTimeMillis();
        if (mod.startTime != null) {
            h1 = fmtDT86(mod.startTime);
            h2 = formatHHMMSS(System.currentTimeMillis() - toMillis(mod.startTime));
            h3 = String.format("%d, время %s", mod.loadCount, formatHHMMSS(mod.loadMsec));
            h4 = String.format("%d, время %s", mod.processedCount, formatHHMMSS(mod.processMsec));

            if (mod.state == ServiceModel.State.PROCESSING) {
                int n = mod.curItems.size();
                int i = mod.curIndex;
                int p = n == 0 ? 100 : (int) (i * 100L / n);
                Request r = mod.curItems.get(i);
                s1 = String.format("%d, время %s", n, formatHHMMSS(mod.curLoadMsec));
                s2 = String.format("%d / %d (%d%%), время %s", i, n, p, formatHHMMSS(time - toMillis(mod.curBlockProcessTime)));
                s3 = String.format("№%d от %s, время %s", r.getId(), fmtDT84(r.getDtCreate()), formatHHMMSS(time - toMillis(mod.curProcessTime)));
                s4 = String.format("%s '%s'", r.getType().title, r.getTitle());
            }
        }
        int c1 = 18, c2 = 19;
        out.bgcolor(bgbase).print(w, "");
        out.bold().color(117, c2).println(name).boldOff().color(bgbase, c1); // 27
        out.println(delim3_4).color(7, bgbase);
        out.println(w, "   Время начала работы : %s", h1);
        out.println(w, "   Общее время работы  : %s", h2);
        out.println(w, "   Запросов заявок     : %s", h3);
        out.println(w, "   Обработанных заявок : %s", h4);
        out.println(w, "   Состояние           : %s", mod.state);
        out.print(w, "   Текущий запрос ").color(c1).println(delim111).color(7);
        out.println(w, "      Загружено заявок : %s", s1);
        out.println(w, "     Обработано заявок : %s", s2);
        out.println(w, "        Обрабатывается : %s", s3);
        out.println(w, "                       : %s", s4);
    }

    private static int bgtitle = 19, bgbase = 17;

    public static void updateState() {
        stateUpdateTime = LocalDateTime.now();
        procModel.copyTo(PM);
        sendModel.copyTo(SM);
        //
        out.reset().at(1, 1);
        out.color(15, bgtitle).bold().print(w, " «Сервис автоматизированной обработки заявок»").boldOff()
                .atX(w - 19).bold().color(230).println(fmtDT86(stateUpdateTime)).boldOff();
        out.color(45).print(w, " v2018.02.15").atX(w - 46).color(123).print("© Докшин Алексей Николаевич, ")
                .color(49).underline().println("dokshin@gmail.com").underlineOff();
        out.color(bgbase, 18).println(delim3_4);
        outServiceSection(PM);
        out.println(w);
        outServiceSection(SM);
        out.color(18, bgbase).println(delim1_4).reset();
    }

    /** Создание каталога, если не существует. */
    public static File createDirectoryIfNotExist(String path) throws ExError {
        try {
            File dir = new File(path);
            if (!dir.exists()) {
                if (!dir.mkdir()) {
                    throw new ExError("Ошибка создания каталога!");
                }
            }
            return dir;
        } catch (ExError ex) {
            throw ex;
        } catch (Exception e) {
            throw new ExError(e, "Ошибка создания каталога!");
        }
    }

    /** Безопасное удаление файла. */
    public static void deleteFileSafe(String path) {
        try {
            File file = new File(path);
            if (file.exists()) {
                // Если не удалился сразу - пробуем удалить при выходе на случай, если залочен.
                if (!file.delete()) file.deleteOnExit();
            }
        } catch (Exception ignore) {
        }
    }
}
