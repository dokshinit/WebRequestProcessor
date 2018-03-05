package app.model;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;

import static util.StringTools.isEmptySafe;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (24.01.18).
 */
public class Request {

    public enum Type {
        REPORT(1, "Отчет"),
        CARDCHANGE(2, "Изменение данных"),
        CARDPAY(3, "Приобретение карт");

        public int id;
        public String title;

        Type(int id, String title) {
            this.id = id;
            this.title = title;
        }

        public static Type byId(Integer id) {
            if (id == null) return null;
            for (Type item : values()) if (item.id == id) return item;
            return null;
        }

        public int getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }
    }

    public enum State {

        // Незавершенные (>0).
        PROCESSING(1, "Обработка"),
        SENDING(2, "Отправка"),
        // Завершенные (<=0).
        FINISHED(0, "Завершено"),
        ERROR(-1, "Ошибка");

        public int id;
        public String title;

        State(int id, String title) {
            this.id = id;
            this.title = title;
        }

        public static State byId(Integer id) {
            if (id == null) return null;
            for (State item : values()) if (item.id == id) return item;
            return null;
        }

        public int getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }
    }

    public enum Mode {

        INCOMPLETE(1, "Исполняемые"),
        FINISHED(2, "Завершённые");

        public int id;
        public String title;

        Mode(int id, String title) {
            this.id = id;
            this.title = title;
        }

        public static Mode byId(Integer id) {
            if (id == null) return null;
            for (Mode item : values()) if (item.id == id) return item;
            return null;
        }

        public int getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }
    }

    public enum ReportType {
        TURNOVER(1, "Обороты клиента"),
        TRANSACTION(2, "Транзакции клиента"),
        CARD(3, "Карты клиента");

        public int id;
        public String cid;
        public String title;

        ReportType(int id, String title) {
            this.id = id;
            this.cid = "" + id;
            this.title = title;
        }

        public static ReportType byId(Integer id) {
            if (id == null) return null;
            for (ReportType item : values()) if (item.id == id) return item;
            return null;
        }

        public int getId() {
            return id;
        }

        public String getCid() {
            return cid;
        }

        public String getTitle() {
            return title;
        }
    }

    private Integer id;
    private Integer idUser, iddClient, iddSub;
    private LocalDateTime dtCreate;
    private Type type;
    private Integer idSubType;
    private ReportType reportType;
    private String paramsTitle;
    private String params;
    private String comment;
    private State state;
    private LocalDateTime dtProcess;
    private String fileName;
    private Integer fileSize;
    private Integer sendTryRemain;
    private LocalDateTime dtSendTry;
    private LocalDateTime dtSend;
    private String result;

    private HashMap<String, String> paramsMap;

    private ArrayList<CardItem> cardItems;

    public Request(Integer id, Integer idUser, Integer iddClient, Integer iddSub, LocalDateTime dtCreate, Integer iType, Integer iSubType,
                   String paramsTitle, String params, String comment, Integer istate, LocalDateTime dtProcess,
                   String fileName, Integer fileSize, Integer sendTryRemain, LocalDateTime dtSendTry, LocalDateTime dtSend, String result) {
        this.id = id;
        this.idUser = idUser;
        this.iddClient = iddClient;
        this.iddSub = iddSub;
        this.type = Type.byId(iType);
        this.idSubType = iSubType;
        this.reportType = type == Type.REPORT ? ReportType.byId(iSubType) : null;
        this.paramsTitle = paramsTitle;
        this.params = params;
        this.paramsMap = paramsToMap(params);
        this.comment = comment;
        this.state = State.byId(istate);
        this.dtCreate = dtCreate;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.dtProcess = dtProcess;
        this.sendTryRemain = sendTryRemain;
        this.dtSendTry = dtSendTry;
        this.dtSend = dtSend;
        this.result = result;


        this.cardItems = new ArrayList<>();
    }

    public String getParams() {
        return params;
    }

    public String getParamAsString(String key) {
        return paramsMap.get(key);
    }

    public Integer getParamAsInteger(String key) {
        String s = paramsMap.get(key);
        return s != null && !s.trim().isEmpty() ? new Integer(s.trim()) : null;
    }

    public int getParamAsInt(String key, int whenNull) {
        String s = paramsMap.get(key);
        return s != null && !s.trim().isEmpty() ? Integer.parseInt(s.trim()) : whenNull;
    }

    public Long getParamAsLong(String key) {
        String s = paramsMap.get(key);
        return s != null && !s.trim().isEmpty() ? new Long(s.trim()) : null;
    }

    public LocalDate getParamAsLocalDate(String key) {
        String s = paramsMap.get(key);
        return s != null && !s.trim().isEmpty() ? LocalDate.from(Helper.FMT_DATE_DDMMYYYY.parse(s.trim())) : null;
    }

    public LocalDateTime getParamAsLocalDateTime(String key) {
        String s = paramsMap.get(key);
        return s != null && !s.trim().isEmpty() ? LocalDateTime.from(Helper.FMT_DT_DDMMYYYYHHMMSS.parse(s.trim())) : null;
    }

    public static HashMap<String, String> paramsToMap(String params) {
        HashMap<String, String> map = new HashMap<>();
        String[] sa = params.split("\r\n|\n\r|\n|\r");
        for (String s : sa) {
            int n = s.indexOf('=');
            if (n == -1) continue;
            String id = s.substring(0, n), val = (n + 1 < s.length()) ? s.substring(n + 1) : "";
            map.put(id, val);
        }
        return map;
    }

    public static String mapToParams(HashMap<String, String> map) {
        StringBuilder sb = new StringBuilder();
        String[] keys = map.keySet().toArray(new String[1]);
        boolean isfirst = true;
        for (String key : keys) {
            String value = map.get(key);
            if (!isfirst) sb.append("\r\n"); //win
            sb.append(key).append('=').append(value);
            isfirst = false;
        }
        return sb.toString();
    }

    public Integer getId() {
        return id;
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

    public LocalDateTime getDtCreate() {
        return dtCreate;
    }

    public Type getType() {
        return type;
    }

    public Integer getIdSubType() {
        return idSubType;
    }

    public ReportType getReportType() {
        return reportType;
    }

    public String getParamsTitle() {
        return paramsTitle;
    }

    public String getTitle() {
        if (type == Type.REPORT) {
            return reportType.getTitle();
        }
        // TODO: Здесь добавится расшифровка для других типов заявок (для подтипов, если надо).
        return type.getTitle();
    }

    public String getFullTitle() {
        return getTitle() + (isEmptySafe(paramsTitle) ? "" : " " + paramsTitle);
    }

    public String getComment() {
        return comment;
    }

    public State getState() {
        return state;
    }

    public LocalDateTime getDtProcess() {
        return dtProcess;
    }

    public String getFileName() {
        return fileName;
    }

    public Integer getFileSize() {
        return fileSize;
    }

    public Integer getSendTryRemain() {
        return sendTryRemain;
    }

    public LocalDateTime getDtSendTry() {
        return dtSendTry;
    }

    public LocalDateTime getDtSend() {
        return dtSend;
    }

    public String getResult() {
        return result;
    }

    public ArrayList<CardItem> getCardItems() {
        return cardItems;
    }

    final DateTimeFormatter FMT_DT_FILE = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public String getAutoFileName() {
        if (type == Type.REPORT)
            return "n" + id + "_report_" + reportType.name().toLowerCase() + "_" + FMT_DT_FILE.format(LocalDateTime.now());
        return "";
    }

    public String getAnswerPath() {
        return "answers" + File.separator + iddClient + (iddSub != 0 ? "-" + iddSub : "");
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

    public static class CardItem {
        private String idd;
        private String driver, car, comment;
        private Long dayLimit;
        private String oilLimit, azsLimit;
        private Card.WorkState work;

        public CardItem(String idd, String driver, String car, String comment, Long dayLimit, String oilLimit, String azsLimit, Card.WorkState work) {
            this.idd = idd;
            this.driver = driver;
            this.car = car;
            this.comment = comment;
            this.dayLimit = dayLimit;
            this.oilLimit = oilLimit;
            this.azsLimit = azsLimit;
            this.work = work;
        }

        public String getIdd() {
            return idd;
        }

        public String getDriver() {
            return driver;
        }

        public String getCar() {
            return car;
        }

        public String getComment() {
            return comment;
        }

        public Long getDayLimit() {
            return dayLimit;
        }

        public String getOilLimit() {
            return oilLimit;
        }

        public String getAzsLimit() {
            return azsLimit;
        }

        public Card.WorkState getWork() {
            return work;
        }
    }
}
