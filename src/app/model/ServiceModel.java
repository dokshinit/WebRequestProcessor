package app.model;

import app.App;
import app.ExError;
import app.ReportsMailer;
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

import static app.App.logger;
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

    private void processReportRequest(Request req) {

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
            model.createDirectoryIfNotExist(path);

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
                model.deleteFileSafe(fullfilenameZIP);
                throw new ExError("Ошибка механизма экспорта отчёта!");
            } catch (Exception ex) {
                model.deleteFileSafe(fullfilenameZIP);
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

    private void sendAnswerForReportRequest(Request req) {
        try {
            Client client = model.loadClient(req.getIddClient(), req.getIddSub(), LocalDate.now());

            ReportsMailer mailer = new ReportsMailer("smtp.tp-rk.ru", "reports@tp-rk.ru", "XuQ9eb9hqZ");

            StringTools.TextBuilder sb = new StringTools.TextBuilder();
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
}