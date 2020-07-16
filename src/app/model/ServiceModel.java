package app.model;

import app.App;
import app.ExError;
import app.ReportsMailer;
import app.export.Export;
import app.report.BaseReport;
import app.report.ClientCardReport;
import app.report.ClientTransactionReport;
import app.report.ClientTurnoverReport;
import net.sf.jasperreports.engine.JRException;
import util.StringTools;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static app.App.model;
import static app.model.Helper.fmtDT84;
import static util.DateTools.toMillis;
import static util.StringTools.isEmptySafe;

public class ServiceModel {

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
    public Request curItem;
    public int curItIndex, curItSize;

    public String errMessage;

    public int loadCount;
    public int processedCount, errorCount;
    public long loadMsec, processMsec;

    public int loadMaxSize, delayTime;


    public ServiceModel(Kind kind, int loadsize, int delay) {
        this.kind = kind;
        startTime = LocalDateTime.now();
        startTime = null;
        state = State.STARTING;
        curLoadTime = null;
        curLoadMsec = 0;
        curBlockProcessTime = null;
        curProcessTime = null;
        curItem = null;
        curItIndex = 0;
        loadCount = 0;
        processedCount = 0;
        errorCount = 0;
        loadMsec = 0;
        processMsec = 0;

        errMessage = null;

        loadMaxSize = loadsize;
        delayTime = delay;
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
        dst.curItem = curItem;
        dst.curItIndex = curItIndex;
        dst.curItSize = curItSize;
        dst.loadCount = loadCount;
        dst.processedCount = processedCount;
        dst.errorCount = errorCount;
        dst.loadMsec = loadMsec;
        dst.processMsec = processMsec;

        dst.errMessage = errMessage;

        dst.loadMaxSize = loadMaxSize;
        dst.delayTime = delayTime;
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
        errMessage = null;
    }

    public ArrayList<Request> loadRequests() throws ExError {
        switch (kind) {
            case PROCESSOR:
                return App.model.loadRequests(Request.State.PROCESSING, loadMaxSize);
            case SENDER:
                return App.model.loadRequests(Request.State.SENDING, loadMaxSize);
            default:
                return null;
        }
    }

    public synchronized void endDataLoad(ArrayList<Request> items, String errmsg) {
        curLoadMsec = System.currentTimeMillis() - toMillis(curLoadTime);
        curItem = null;
        curItIndex = 0;
        curItSize = items.size();
        loadCount++;
        state = errmsg == null && items.size() > 0 ? State.PROCESSING : State.SLEEPING;
        errMessage = errmsg;
        if (errmsg != null) errorCount++;
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
        curItem = req;
    }

    /** Обработка заявки. */
    public void processRequest(Request req) throws ExError {
        switch (kind) {
            case PROCESSOR:
                processRequestImpl(req);
                break;

            case SENDER:
                sendAnswerForRequest(req); // Ответы рассылаем единообразные.
                break;
        }
    }

    public synchronized void endProcess(String errmsg) {
        curItIndex++;
        processedCount++;
        errMessage = errmsg;
        if (errmsg != null) errorCount++;
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

    public synchronized void setError(String err) {
        this.errMessage = err;
    }

    /** Обработчик заявок, ожидающих обработки (вся общая часть вынесена в этот метод). */
    private void processRequestImpl(Request req) throws ExError {
        try {
            switch (req.getType()) {
                case REPORT:
                    processReportRequest(req);
                    break;
                case EXPORT:
                    processExportRequest(req);
                    break;
                default:
                    throw new ExError("Неизвестный тип заявки!");
            }

            // Меняем статус заявки на завершенный или ожидающий отправки.
            try {
                Client client = model.loadClient(req, req.dtCreate.toLocalDate()); // По умолчанию читаем на дату создания заявки.
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
            // Облом. завершаем заявку или ввести счётчик попыток?
            if (req.getState() != Request.State.ERROR) {
                req.setState(Request.State.ERROR, ex.getMessage());
            } else {
                req.setState(Request.State.ERROR, req.getResult() + " (" + ex.getMessage() + ")");
            }
            try {
                model.updateRequestProcess(req);
            } catch (Exception ignore) {
            }
            throw ex;
        }
    }

    /** Обработчик заявки на генерацию отчёта. */
    private void processReportRequest(Request req) throws ExError {

        BaseReport report;
        Client client;
        Integer iddazs;
        String iddcard;
        LocalDate dtstart, dtend, dtw;

        switch (req.getReportType()) {
            case TURNOVER:
                dtstart = req.getParamAsLocalDate("dtStart");
                dtend = req.getParamAsLocalDate("dtEnd");
                client = model.loadClient(req, dtend);
                report = new ClientTurnoverReport(client, dtstart, dtend);
                break;

            case TRANSACTION:
                dtstart = req.getParamAsLocalDate("dtStart");
                dtend = req.getParamAsLocalDate("dtEnd");
                client = model.loadClient(req, dtend);
                iddazs = req.getParamAsInteger("iddAzs");
                iddcard = req.getParamAsString("iddCard");
                if (isEmptySafe(iddcard)) iddcard = null;
                ClientTransactionReport.Mode mode = ClientTransactionReport.Mode.byId(req.getReportModeParam());
                report = new ClientTransactionReport(client, dtstart, dtend, iddazs, iddcard, mode);
                break;

            case CARD:
                dtw = req.getParamAsLocalDate("dtw");
                client = model.loadClient(req, dtw);
                Card.WorkState workState = Card.WorkState.byId(req.getParamAsInteger("idWorkState"));
                ClientCardReport.Mode mode2 = ClientCardReport.Mode.byId(req.getReportModeParam());
                report = new ClientCardReport(client, dtw, workState, mode2);
                break;

            default:
                throw new ExError("Неизвестный вид отчёта!");
        }

        String path = req.getAnswerPath();
        model.createDirectoryIfNotExist(path);

        try {
            // Генерируем отчёт.
            report.build();
        } catch (ExError ex) {
            // Облом. завершаем заявка или ввести счётчик попыток?
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
            model.deleteFileSafe(fullfilenameZIP);
            throw new ExError("Ошибка механизма экспорта отчёта!");
        } catch (Exception ex) {
            model.deleteFileSafe(fullfilenameZIP);
            throw new ExError("Ошибка экспорта отчёта!");
        }
    }

    /** Обработчик заявки на генерацию экспортных данных. */
    private void processExportRequest(Request req) throws ExError {

        Export export;
        Client client;
        Integer iddazs;
        String iddcard;
        LocalDate dtstart, dtend, dtw;

        switch (req.getExportType()) {
            case TRANSACTION:
                dtstart = req.getParamAsLocalDate("dtStart");
                dtend = req.getParamAsLocalDate("dtEnd");
                client = model.loadClient(req, dtend);
                iddazs = req.getParamAsInteger("iddAzs");
                iddcard = req.getParamAsString("iddCard");
                if (isEmptySafe(iddcard)) iddcard = null;
                Integer mode = req.getExportModeParam();
                export = new Export(client, dtstart, dtend, iddazs, iddcard, mode);
                break;

            default:
                throw new ExError("Неизвестный вид экспорта!");
        }

        String path = req.getAnswerPath();
        model.createDirectoryIfNotExist(path);

        String name = req.getAutoFileName();
        String filenamePDF = name + ".xls";
        String filenameZIP = name + ".zip";
        String fullfilenameZIP = path + File.separator + filenameZIP;
        try {
            File file = new File(fullfilenameZIP);
            FileOutputStream fos = new FileOutputStream(file);
            ZipOutputStream outs = new ZipOutputStream(fos);
            ZipEntry ze = new ZipEntry(filenamePDF);
            outs.putNextEntry(ze);
            export.exportTransactions(outs);
            outs.closeEntry();
            outs.close();

            req.setFile(filenameZIP, (int) file.length());

        } catch (Exception ex) {
            model.deleteFileSafe(fullfilenameZIP);
            throw new ExError("Ошибка экспорта!");
        }
    }

    /** Обработчик заявок, ожидающих отправки ответа на заявку. */
    private void sendAnswerForRequest(Request req) throws ExError {
        try {
            Client client = model.loadClient(req, LocalDate.now());

            ReportsMailer mailer = new ReportsMailer("smtp.tp-rk.ru", "reports@tp-rk.ru", "XuQ9eb9hqZ");

            StringTools.TextBuilder sb = new StringTools.TextBuilder();
            sb.println("Ответ на заявку: №%d от %s", req.getId(), fmtDT84(req.getDtCreate()));
            sb.println("---");
            sb.println("Данное сообщение сформировано службой автоматической рассылки ответов на заявки,");
            sb.println("созданные в Личном Кабинете (ЛК) клиента Системы Топливных Карт (СТК) %s.", client.getFirm().getTitle());
            sb.println("Не отвечайте на это письмо, входящие сообщения на данный адрес заблокированы.");

            //if (true) throw new ExError("123");
            mailer.sendMail("PC.FCService", client.getEmail(), null, "Автоматический ответ за заявку ЛК СТК!", sb.toString(),
                    req.getAnswerPath() + File.separator + req.getFileName());

            // Меняем статус заявки на завершенный.
            try {
                req.setState(Request.State.FINISHED, null);
                model.updateRequestSend(req);
            } catch (Exception ex) {
                throw new ExError("Ошибка изменения состояния заявки в БД!");
            }

        } catch (Exception ex) {
            if (req.getSendTryRemain() != null && req.getSendTryRemain() <= 1) {
                req.setState(Request.State.ERROR, "Ошибка отправки ответа на заявку!");
            }
            try {
                model.updateRequestSend(req);
            } catch (Exception ignore) {
            }

            if (ex instanceof ExError) throw (ExError) ex;
            throw new ExError("Ошибка отправки ответа на заявку! (%s)", ex.getMessage());
        }
    }
}
