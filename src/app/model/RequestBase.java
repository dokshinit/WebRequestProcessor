package app.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static util.StringTools.isEmptySafe;

/**
 * Базовая часть заявки (общая часть для использования в двух проектах: WebRequestProcessor + WebCabinet).
 *
 * @author Aleksey Dokshin <dant.it@gmail.com> (24.01.18).
 */
public abstract class RequestBase {

    public enum Type {
        REPORT(1, "Отчет"),
        CARDCHANGE(2, "Изменение данных"),
        CARDPAY(3, "Приобретение карт"),
        EXPORT(4, "Экспорт данных");

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

    interface SubType<T extends SubType<T>> {
        T en();

        int getId();

        String getCid();

        String getTitle();
    }

    public enum ReportType implements SubType<ReportType> {
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

        @Override
        public ReportType en() {
            return this;
        }

        @Override
        public int getId() {
            return id;
        }

        @Override
        public String getCid() {
            return cid;
        }

        @Override
        public String getTitle() {
            return title;
        }
    }

    public enum ExportType implements SubType<ExportType> {
        TRANSACTION(1, "Транзакции клиента");

        public int id;
        public String cid;
        public String title;

        ExportType(int id, String title) {
            this.id = id;
            this.cid = "" + id;
            this.title = title;
        }

        public static ExportType byId(int id) {
            for (ExportType item : values()) if (item.id == id) return item;
            return null;
        }

        @Override
        public ExportType en() {
            return this;
        }

        @Override
        public int getId() {
            return id;
        }

        @Override
        public String getCid() {
            return cid;
        }

        @Override
        public String getTitle() {
            return title;
        }
    }

    /** ID. */
    protected Integer id;
    /** Время создания в базе. */
    protected LocalDateTime dtCreate;
    /** Тип заявки - enum (по коду типа iType, определяется в конструкторе). */
    protected Type type;
    /** Код вида заявки. */
    protected Integer idSubType;
    /** Строка представления параметров для использования в тексте. */
    protected String paramsTitle;
    /** Карта параметров (определяется в конструкторе из строки). */
    protected ParamsMap paramsMap;
    /** Комментарий. */
    protected String comment;
    /** Состояние заявки - enum (по коду состояния iState, определяется в конструкторе). */
    protected State state;
    /** Имя файла ответа на заявка. */
    protected String fileName;
    /** Размера файла ответа на заявка. */
    protected Integer fileSize;
    /** Время обработки заявки. */
    protected LocalDateTime dtProcess;
    /** Кол-во попыток отправки ответа на заявка. */
    protected Integer sendTryRemain;
    /** Время последней отправки ответа на заявка. */
    protected LocalDateTime dtSend;
    /** Строка с результатом обработки заявки. */
    protected String result;

    /** Подвид заявки (обобщенный интерфейс). */
    protected final SubType subType; // add
    /** Вид отчёта (при типе заявки - отчёт). */
    protected final ReportType reportType; // add
    /** Вид экспорта (при типе заявки - экспорт). */
    protected final ExportType exportType; // add

    /**
     * Конструктор.
     *
     * @param params Строка параметров (в виде пар имя=значение разделенных символом(ами) перевода строки '\r\n').
     */
    protected RequestBase(Integer id, LocalDateTime dtCreate,
                          Integer iType, Integer iSubType, String paramsTitle, String params, String comment,
                          Integer istate, LocalDateTime dtProcess, String fileName, Integer fileSize,
                          Integer sendTryRemain, LocalDateTime dtSend, String result) {
        this.id = id;
        this.type = Type.byId(iType);
        this.idSubType = iSubType;
        this.paramsTitle = paramsTitle;
        this.paramsMap = new ParamsMap(params);
        this.comment = comment;
        this.state = State.byId(istate);
        this.dtCreate = dtCreate;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.dtProcess = dtProcess;
        this.sendTryRemain = sendTryRemain;
        this.dtSend = dtSend;
        this.result = result;

        switch (type) {
            case REPORT:
                reportType = ReportType.byId(iSubType);
                exportType = null;
                subType = reportType;
                break;
            case EXPORT:
                reportType = null;
                exportType = ExportType.byId(iSubType);
                subType = exportType;
                break;
            default:
                reportType = null;
                exportType = null;
                subType = null;
        }
    }

    public SubType getSubType() {
        return subType;
    }

    public ReportType getReportType() {
        return reportType;
    }

    public ExportType getExportType() {
        return exportType;
    }


    public String getParams() {
        return paramsMap.toString();
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

    public Integer getReportModeParam() {
        return getParamAsInteger("idReportMode");
    }

    public void setReportModeParam(Integer id) {
        paramsMap.put("idReportMode", id == null ? "" : ("" + id));
    }

    public Integer getExportModeParam() {
        return getParamAsInteger("idExportMode");
    }

    public void setExportModeParam(Integer id) {
        paramsMap.put("idExportMode", id == null ? "" : ("" + id));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public Integer getId() {
        return id;
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

    /** Расшифровка заявки. Можно сделать разной для разных типов, если надо! */
    public String getTitle() {
        return (subType != null) ? subType.getTitle() : type.getTitle();
    }

    public String getFullTitle() {
        return getTitle() + (isEmptySafe(paramsTitle) ? "" : " " + paramsTitle);
    }

    public String getParamsTitle() {
        return paramsTitle;
    }

    public String getComment() {
        return comment;
    }

    public String getFullComment() {
        return comment + (result != null && !result.isEmpty() ? "[" + result + "]" : "");
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

    public LocalDateTime getDtSend() {
        return dtSend;
    }

    public String getResult() {
        return result;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Карта параметров заявки. Вместо использования связанно карты, выделен в отдельный класс для удобства операций с
     * параметрами (в него вынесены и методы некоторых операций).
     */
    public static class ParamsMap {

        /** Шаблон имени. */
        private static final String keynameRegex = "[a-zA-Z0-9_\\.]{1,}";
        /** Шаблон для проверки имени ключа. */
        private static final Pattern keysPattern = Pattern.compile("^(" + keynameRegex + ")$");
        /** Шаблон для разбора строки параметров. */
        private static final Pattern paramsPattern = Pattern.compile("(" + keynameRegex + ")(=)(.*)(\r\n|\n\r|\r|\n|$)");

        private final LinkedHashMap<String, String> map;

        /** Конструктор. Пустая карта. */
        public ParamsMap() {
            map = new LinkedHashMap<>();
        }

        /**
         * Конструктор из строки параметров. Формат строки: имя+'='+значение+разделитель, где разделитель - перевод
         * строки, имя - только латинские символы, цифры, '_', '.', значение - любые символы кроме '\\', '\n' и '\r'.
         * Символы '\\', '\n' и '\r' в значениях маскируются/демаскируются автоматически при преобразовании строки в
         * карту и обратно!
         * <p>
         * Пример: "idReportType=1\ndtStart=01.01.2010\ndtEnd=31.01.2010\nsTitle=123\\n234\\n345".
         */
        public ParamsMap(String params) {
            this();
            fromString(params);
        }

        /** Конструктор из списка строк, где каждая пара строк задаёт ключ и его значение. Кол-во строк - только четное! */
        public ParamsMap(String... pairs) {
            this();
            fromPairs(pairs);
        }

        /** Заполнение карты из строки с параметрами (формат строки см. в {@link #ParamsMap(String)}). */
        public void fromString(String params) {
            map.clear();
            Matcher m = paramsPattern.matcher(params);
            int i = 0; // позиция конца найденного куска (следующий найденный должен начинаться с этой позиции.
            while (m.find()) {
                if (i != m.start()) throw new RuntimeException("Нарушена целостность параметров!");
                i = m.end();
                map.put(m.group(1), unmaskSysChars(m.group(3)));
            }
        }

        public boolean isKeynameValid(String keyname) {
            return keysPattern.matcher(keyname).matches();
        }

        /** Заполнение карты из массива строк, которые идут парами: ключ, значение. Кол-во строк всегда чётное! */
        public void fromPairs(String... pairs) {
            map.clear();
            if ((pairs.length & 1) != 0)
                throw new RuntimeException("Кол-во аргументов должно быть четным (пары значений)!");
            for (int i = 0; i < pairs.length; i += 2) put(pairs[i], pairs[i + 1]);
        }

        /** Представление карты в виде строки параметров (формат строки см. в {@link #ParamsMap(String)}). */
        public String toString() {
            StringBuilder sb = new StringBuilder();
            forEach((i, li, e) ->
                    sb.append(i == 0 ? "" : "\r\n").append(e.getKey()).append("=").append(maskSysChars(e.getValue())));
            return sb.toString();
        }

        /** Добавление/задание ключа и его значения в карту. */
        public ParamsMap put(String key, String value) {
            if (!isKeynameValid(key)) throw new RuntimeException("Неверное имя ключа: " + key + "!");
            map.put(key, value);
            return this;
        }

        /** Получение значения ключа из карты. */
        public String get(String key) {
            return map.get(key);
        }

        /** Функциональный интерфейс для удобной обработки всех элементов карты. */
        @FunctionalInterface
        interface Processor {
            void process(int curindex, int lastindex, Map.Entry<String, String> entry);
        }

        /** Метод для удобной обработки всех элементов карты. Для каждого элемента выполняет заданную функцию. */
        public void forEach(Processor func) {
            int i = 0, n = map.size() - 1;
            for (Map.Entry<String, String> e : map.entrySet()) {
                func.process(i, n, e);
                i++;
            }
        }

        /** Замаскировать служебные символы. Маскируются только сам слэш и перевод строки/возврат каретки. */
        public String maskSysChars(String src) {
            if (src == null) return null;
            return src.replace("\\", "\\\\").
                    replace("\n", "\\n").
                    replace("\r", "\\r");
        }

        /** Размаскировать служебные символы. Демаскируются только сам слэш и перевод строки/возврат каретки. */
        public String unmaskSysChars(String src) {
            if (src == null) return null;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < src.length(); i++) {
                char ch = src.charAt(i);
                if (ch == '\\') {
                    i++;
                    if (i >= src.length()) break; // Ошибка по идее...
                    switch (src.charAt(i)) {
                        case '\\':
                            sb.append("\\");
                            break;
                        case 'n':
                            sb.append("\n");
                            break;
                        case 'r':
                            sb.append("\r");
                            break;
                        default:
                            i--; // Не спецсимвол - не обрабатываем.
                    }
                } else {
                    sb.append(ch);
                }
            }
            return sb.toString();
        }

//        public static void main(String[] args) {
//            ParamsMap pm = new ParamsMap();
//            pm.put("idType", "123\n234");
//            pm.put("idType2", "222");
//
//            String s1 = pm.toString();
//            System.out.println(s1);
//
//            ParamsMap pm2 = new ParamsMap(s1);
//
//            String s2 = pm2.toString();
//            System.out.println(s2);
//        }
    }
}
