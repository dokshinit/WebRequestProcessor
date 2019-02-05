package app.model;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Request extends RequestBase {

    protected Integer idUser, iddClient, iddSub;
    protected LocalDateTime dtSendTry;

    public Request(Integer id, Integer idUser, Integer iddClient, Integer iddSub, LocalDateTime dtCreate,
                   Integer iType, Integer iSubType, String paramsTitle, String params, String comment,
                   Integer istate, LocalDateTime dtProcess, String fileName, Integer fileSize,
                   Integer sendTryRemain, LocalDateTime dtSendTry, LocalDateTime dtSend, String result) {
        super(id, dtCreate, iType, iSubType, paramsTitle, params, comment, istate, dtProcess, fileName,
                fileSize, sendTryRemain, dtSend, result);
        this.idUser = idUser;
        this.iddClient = iddClient;
        this.iddSub = iddSub;
        this.dtSendTry = dtSendTry;
    }

    public Integer getIdUser() {
        return idUser;
    }

    public Integer getIddClient() {
        return iddClient;
    }

    public Integer getIddSub() {
        return iddSub;
    }

    public LocalDateTime getDtSendTry() {
        return dtSendTry;
    }

    /** Путь для файла ответа на заявку. */
    public String getAnswerPath() {
        return "answers" + File.separator + iddClient + (iddSub != 0 ? "-" + iddSub : "");
    }

    protected final DateTimeFormatter FMT_DT_FILE = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    protected String filenameByTemplate(String stype, Enum en) {
        return String.format("%s_n%03d_%s_%s", FMT_DT_FILE.format(LocalDateTime.now()), getId(), stype, en.name().toLowerCase());
    }

    /** Автогенерация имени для файла ответа на заявку. */
    public String getAutoFileName() {
        switch (type) {
            case REPORT:
                return filenameByTemplate("report", reportType);
            case EXPORT:
                return filenameByTemplate("export", exportType);
            default:
                return "";
        }
    }

    public void setFile(String fileName, Integer fileSize) {
        this.fileName = fileName;
        this.fileSize = fileSize;
    }

    public void setState(State state, String result) {
        this.state = state;
        this.result = result;
    }

    public void updateByProcess(LocalDateTime dtprocess) {
        this.dtProcess = dtprocess;
    }

    public void updateBySend(Integer isendtryremain, LocalDateTime dtsendtry, LocalDateTime dtsend) {
        this.sendTryRemain = isendtryremain;
        this.dtSendTry = dtsendtry;
        this.dtSend = dtsend;
    }
}
