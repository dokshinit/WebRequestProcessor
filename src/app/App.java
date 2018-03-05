/*
 * Copyright (c) 2015, Aleksey Nikolaevich Dokshin. All right reserved.
 * Contacts: dant.it@gmail.com, dokshin@list.ru.
 */
package app;

import app.model.AppModel;
import app.model.Request;
import app.model.ServiceModel;
import util.CommonTools;
import util.StringTools;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.logging.Level;

import static app.model.Helper.fmtDT84;
import static app.model.Helper.fmtDT86;
import static util.DateTools.formatHHMMSS;
import static util.DateTools.toMillis;

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

    /**
     * Логгер для вывода отладочной информации.
     */
    public final static LoggerExt logger = LoggerExt.getCommonLogger();
    public final static LoggerExt procLogger = LoggerExt.getNewLogger("proc");
    public final static LoggerExt sendLogger = LoggerExt.getNewLogger("send");

    /**
     * Модель приложения.
     */
    public final static AppModel model = new AppModel();

    public static boolean isUI = false;

    /**
     * Точка старта приложения.
     */
    public static void main(String[] args) {

        Runtime.getRuntime().addShutdownHook(new Thread(App::stopApp, "Shutdown-thread"));

        for (String s : args) {
            if ("showui".equals(s)) isUI = true;
        }

        try {
            initLog(); // Инициализация логгера.

            initModel(); // Настройка модели.

            startApp(); // Старт приложения.

        } catch (Exception ex) {
            logger.error("Ошибка запуска приложения!", ex);
        }
    }

    static LoggerExt.FileUpdateCheck byMinutes = (log) -> {
        LocalTime now = LocalTime.now();
        LocalTime tm = log.getFileDT().toLocalTime();
        return now.getHour() != tm.getHour() || now.getMinute() != tm.getMinute();
    };

    /**
     * Инициализация лога.
     */
    private static void initLog() {
        // Отключаем вывод лога в консоль.
        LoggerExt.removeConsoleOutput();
        // Логгируем все сообщения.
        logger.setLevel(Level.ALL);
        // Настраиваем логгер для файлового вывода.
        logger.enable(true).toFile(); //logger.getFilePath(), "%1$s_%3$tH%3$tM.logx", byMinutes

        procLogger.setLevel(Level.ALL);
        procLogger.enable(true).toFile();
        sendLogger.setLevel(Level.ALL);
        sendLogger.enable(true).toFile();

        logger.configf("Инициализация приложения (%s)", isUI ? "c UI" : "без UI");
    }

    /**
     * Настройка модели.
     */
    private static void initModel() throws Exception {
        logger.config("Создание модели...");
        try {
            model.init();

        } catch (Exception ex) {
            logger.error("Ошибка создания модели!", ex);
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

    /**
     * Запуск приложения.
     */
    private static void startApp() {
        if (!isUI) {
            try {
                model.createDirectoryIfNotExist(model.statePath);
            } catch (Exception ex) {
                logger.errorf(ex, "Не удалось создать каталог для файла состояния! (%s)", model.statePath);
            }
        }

        if (isUI) out.cursorOff().attr(0).clear().at(1, 1);

        final Thread p1 = new Thread(() -> serviceThreadBody(model.procModel));
        final Thread p2 = new Thread(() -> serviceThreadBody(model.sendModel));

        p1.start();
        p2.start();

        while (!isTerminated) {
            updateState();
            safeTermSleep(model.redrawInterval);
        }

        if (isUI) out.cursorOn();
    }

    private static void serviceThreadBody(ServiceModel mod) {
        logger.infof("Старт сервиса: %s", mod.getName()); // обработки заявок.
        ServiceModel.Kind kind = mod.startService();
        LoggerExt log = kind == ServiceModel.Kind.PROCESSOR ? procLogger : sendLogger;

        ArrayList<Request> emptylist = new ArrayList<>();

        while (!isTerminated) {
            String errmsg = null;
            mod.startDataLoad();
            ArrayList<Request> its = emptylist;
            try {
                its = mod.loadRequests();
            } catch (Exception ex) {
                errmsg = ex.getMessage();
                log.error("Ошибка запроса заявок!", ex);
            }
            mod.endDataLoad(its, errmsg);

            if (errmsg == null && !its.isEmpty()) {
                mod.startBlockProcess();
                for (Request request : its) {
                    log.infof("Обработка: №%d от %s", request.getId(), fmtDT86(request.getDtCreate()));
                    mod.startProcess(request);
                    errmsg = null;
                    try {
                        mod.processRequest(request);
                    } catch (Exception ex) {
                        errmsg = ex.getMessage();
                        log.error("Ошибка обработки заявки!", ex);
                    }
                    mod.endProcess(errmsg);
                    if (safeTermSleep(100)) return;
                }
            }
            mod.setState(ServiceModel.State.SLEEPING);
            if (safeTermSleep(mod.delayTime)) return;
        }
        mod.endService();
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

    private static ServiceModel PM = new ServiceModel(ServiceModel.Kind.PROCESSOR, 0, 0);
    private static ServiceModel SM = new ServiceModel(ServiceModel.Kind.SENDER, 0, 0);

    static String trunc(String s, int len) {
        int n = s.length();
        if (n <= len) return s;
        return s.substring(0, len);
    }

    private static void outServiceSection(ServiceModel mod, StringTools.TextBuilder b) {
        String name = " " + mod.getName() + " ";
        int wrest = w - name.length() - 1;
        String h1 = "---", h2 = "---", h3 = "---", h4 = "---", h5 = "---";
        String s1 = "---", s2 = "---", s3 = "---", s4 = "---";
        long time = System.currentTimeMillis();
        if (mod.startTime != null) {
            h1 = fmtDT86(mod.startTime);
            h2 = formatHHMMSS(System.currentTimeMillis() - toMillis(mod.startTime));
            h3 = String.format("%d, время %s", mod.loadCount, formatHHMMSS(mod.loadMsec));
            h4 = String.format("%d, время %s", mod.processedCount, formatHHMMSS(mod.processMsec));
            h5 = String.format("%d", mod.errorCount);

            if (mod.state == ServiceModel.State.PROCESSING && mod.curItem != null) {
                Request r = mod.curItem;
                int n = mod.curItSize;
                int i = mod.curItIndex;
                int p = n == 0 ? 100 : (int) (i * 100L / n);
                s1 = String.format("%d, время %s", n, formatHHMMSS(mod.curLoadMsec));
                s2 = String.format("%d / %d (%d%%), время %s", i, n, p, formatHHMMSS(time - toMillis(mod.curBlockProcessTime)));
                s3 = String.format("№%d от %s, время %s", r.getId(), fmtDT84(r.getDtCreate()), formatHHMMSS(time - toMillis(mod.curProcessTime)));
                s4 = String.format("%s '%s'", r.getType().title, r.getTitle());
            }
        }

        if (isUI) {
            int c1 = 18, c2 = 19;
            out.bgcolor(bgbase).print(w, "");
            out.bold().color(117, c2).println(name).boldOff().color(bgbase, c1); // 27
            out.println(delim3_4).color(7, bgbase);
            out.println(w, "   Время начала работы : %s", h1);
            out.println(w, "   Общее время работы  : %s", h2);
            out.println(w, "   Запросов заявок     : %s", h3);
            out.println(w, "   Обработанных заявок : %s", h4);
            out.println(w, "   Ошибок              : %s", h5);
            out.println(w, "   Состояние           : %s", mod.state);
            out.print(w, "   Текущий запрос ").color(c1).println(delim111).color(7);
            out.println(w, "      Загружено заявок : %s", s1);
            out.println(w, "     Обработано заявок : %s", s2);
            out.println(w, "        Обрабатывается : %s", s3);
            if (mod.errMessage == null) {
                out.println(w, "                       : %s", s4);
            } else {
                out.color(7, 88).print(w, "                Ошибка : ")
                        .color(228).println(trunc(mod.errMessage, w - 25)).color(7, bgbase);
            }
        }

        //
        if (!isUI) {
            b.println("[%s]", name);
            b.println("   Время начала работы : %s", h1);
            b.println("   Общее время работы  : %s", h2);
            b.println("   Запросов заявок     : %s", h3);
            b.println("   Обработанных заявок : %s", h4);
            b.println("   Ошибок              : %s", h5);
            b.println("   Состояние           : %s", mod.state);
            b.println("   Текущий запрос --------------------------------");
            b.println("      Загружено заявок : %s", s1);
            b.println("     Обработано заявок : %s", s2);
            b.println("        Обрабатывается : %s", s3);
            if (mod.errMessage == null) {
                b.println("                       : %s", s4);
            } else {
                b.println("                Ошибка : %s", mod.errMessage);
            }
        }
    }

    private static int bgtitle = 19, bgbase = 17;

    public static void updateState() {
        stateUpdateTime = LocalDateTime.now();
        model.procModel.copyTo(PM);
        model.sendModel.copyTo(SM);
        //
        if (isUI) {
            out.reset().at(1, 1);
            out.color(15, bgtitle).bold().print(w, " «Сервис автоматизированной обработки заявок»").boldOff()
                    .atX(w - 19).bold().color(230).println(fmtDT86(stateUpdateTime)).boldOff();
            out.color(45).print(w, " v2018.02.15").atX(w - 46).color(123).print("© Докшин Алексей Николаевич, ")
                    .color(49).underline().println("dokshin@gmail.com").underlineOff();
            out.color(bgbase, 18).println(delim3_4);
            outServiceSection(PM, null);
            out.println(w);
            outServiceSection(SM, null);
            out.color(18, bgbase).println(delim1_4).reset();
        }

        // В файл...
        if (!isUI) {
            try {
                //model.createDirectoryIfNotExist("./state/replicator");
                StringTools.TextBuilder b = new StringTools.TextBuilder();
                b.println(" «Сервис автоматизированной обработки заявок»               %s", fmtDT86(stateUpdateTime));
                b.println(" v2018.02.15                     © Докшин Алексей Николаевич, dokshin@gmail.com");
                b.println("--------------------------------------------------------------------------------");
                outServiceSection(PM, b);
                b.println("--------------------------------------------------------------------------------");
                outServiceSection(SM, b);
                b.println("--------------------------------------------------------------------------------");

                FileWriter fw = new FileWriter(model.statePath + File.separator + "app.state");
                fw.append(b.toString());
                fw.flush();
                fw.close();

            } catch (Exception ignore) {
            }
        }
    }

    private static void stopApp() {
        if (isUI) out.reset().color(7, 0).clear().cursorOn();
        logger.infof("Приложение завершено!");
    }

}
