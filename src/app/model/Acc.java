package app.model;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (27.12.17).
 */
public class Acc {

    private Integer idd;
    private Firm firm;
    private Integer iddClient, iddSub;
    private Oil oil;
    private AccType type;
    private Long dbStart, dbSale, dbPay, dbEnd; // коп. \ 10*мл.

    private String title;

    public Acc(Integer idd, Integer iddFirm, Integer iddClient, Integer iddSub, Integer iAccType, Integer iddOil,
               Long dbStart, Long dbSale, Long dbPay, Long dbEnd) {
        this.idd = idd;
        this.firm = Firm.byId(iddFirm);
        this.iddClient = iddClient;
        this.iddSub = iddSub;
        this.oil = Oil.byId(iddOil);
        this.type = AccType.byId(iAccType);
        this.dbStart = dbStart;
        this.dbSale = dbSale;
        this.dbPay = dbPay;
        this.dbEnd = dbEnd;

        this.title = "№" + idd + " (" + type.title + (type == AccType.KEEP ? (": " + oil.title) : "") + ")";
    }

    public Integer getIdd() {
        return idd;
    }

    public Firm getFirm() {
        return firm;
    }

    public Integer getIddClient() {
        return iddClient;
    }

    public Integer getIddSub() {
        return iddSub;
    }

    public Oil getOil() {
        return oil;
    }

    public AccType getType() {
        return type;
    }

    public Long getDbStart() {
        return dbStart;
    }

    public Long getDbSale() {
        return dbSale;
    }

    public Long getDbPay() {
        return dbPay;
    }

    public Long getDbEnd() {
        return dbEnd;
    }

    public String getTitle() {
        return title;
    }
}
