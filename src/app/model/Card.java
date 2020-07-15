package app.model;

import java.time.LocalDate;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (28.12.17).
 */
public class Card {

    public enum WorkState {
        WORK(1, "Рабочая", "Рабочие"),
        LOCK(0, "Запрет", "На запрете"),
        ARREST(-1, "Арест", "Арестованные");
        //DESTROY(-2, "Изъята", "Изъятые");

        public int id;
        public String title, titleForMany;

        WorkState(int id, String title, String titleForMany) {
            this.id = id;
            this.title = title;
            this.titleForMany = titleForMany;
        }

        public static WorkState byId(Integer id) {
            if (id == null) return null;
            for (WorkState item : values()) if (item.id == id) return item;
            return null;
        }

        public int getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getTitleForMany() {
            return titleForMany;
        }
    }

    private LocalDate dtw, dtwEnd;
    private String idd, title; // idd-полный номер карты, title - сокращенный.
    private AccType accType;
    private WorkState workState;
    private LocalDate dtPay;
    private String driver, car, comment;
    private Long dbDayLimit; // 10*мл.

    public Card(LocalDate dtw, LocalDate dtwEnd, String iddCard, Integer iAccType, Integer iWork, LocalDate dtPay, String driver, String car, Long dbDayLimit, String comment) {
        this.dtw = dtw;
        this.dtwEnd = dtwEnd;
        this.idd = iddCard;
        this.title = iddCard.substring(3);
        this.accType = AccType.byId(iAccType);
        this.workState = WorkState.byId(iWork);
        this.dtPay = dtPay;
        this.driver = driver;
        this.car = car;
        this.dbDayLimit = dbDayLimit;
        this.comment = comment;
    }

    public LocalDate getDtw() {
        return dtw;
    }

    public LocalDate getDtwEnd() {
        return dtwEnd;
    }

    public String getIdd() {
        return idd;
    }

    public String getTitle() {
        return title;
    }

    public AccType getAccType() {
        return accType;
    }

    public WorkState getWorkState() {
        return workState;
    }

    public LocalDate getDtPay() {
        return dtPay;
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

    public Long getDbDayLimit() {
        return dbDayLimit;
    }
}
