package app.model;

import java.time.LocalDate;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (26.12.17).
 */
public class Contract {

    private Integer idd;
    private String number;
    private ContractType type;
    private LocalDate dtSign;
    private LocalDate dtStartFact;
    private LocalDate dtEndFact;
    private Integer iExpired;

    public Contract(Integer idd, String number, Integer iType, LocalDate dtSign, LocalDate dtStartFact, LocalDate dtEndFact, Integer iExpired) {
        this.idd = idd;
        this.number = number;
        this.dtSign = dtSign;
        this.type = ContractType.byId(iType);
        this.dtStartFact = dtStartFact;
        this.dtEndFact = dtEndFact;
        this.iExpired = iExpired;
    }

    public Integer getIdd() {
        return idd;
    }

    public String getNumber() {
        return number;
    }

    public LocalDate getDtSign() {
        return dtSign;
    }

    public LocalDate getDtStartFact() {
        return dtStartFact;
    }

    public LocalDate getDtEndFact() {
        return dtEndFact;
    }

    public int getExpiredState() {
        return iExpired;
    }

    public String getExpiredStyle() {
        switch (iExpired) {
            case -1: return "expired-future";
            case 0: return "expired-no";
            case 1: return "expired-yes";
        }
        return "";
    }

    public static String getExpiredTitle(int id) {
        switch (id) {
            case -1: return "Не стартовал";
            case 0: return "Действует";
            case 1: return "Истёк";
        }
        return "";
    }

    public ContractType getType() {
        return type;
    }
}
