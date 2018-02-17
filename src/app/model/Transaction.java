package app.model;

import java.time.LocalDateTime;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (15.12.17).
 */
public class Transaction {

    private LocalDateTime dtStart, dtEnd;
    private String iddCard, cardInfo;
    private Integer idd, iddAzs, iddTrk;
    private Long dbPrice, dbVolReq, dbVolume, dbSumma; // коп. \ 10*мл.
    //
    private Oil oil;
    private AccType accType;

    public Transaction(LocalDateTime dtStart, LocalDateTime dtEnd, String iddCard, String cardInfo,
                       Integer idd, Integer iddAzs, Integer iddTrk, Integer iddOil, Integer iAccType,
                       Long dbPrice, Long dbVolReq, Long dbVolume, Long dbSumma) {
        this.idd = idd;
        this.dtStart = dtStart;
        this.dtEnd = dtEnd;
        this.iddCard = iddCard.substring(3); // Для вывода, номера карт усекам до последних 6 знаков.
        this.cardInfo = cardInfo;
        this.iddAzs = iddAzs;
        this.iddTrk = iddTrk;
        this.oil = Oil.byId(iddOil);
        this.accType = AccType.byId(iAccType);
        this.dbPrice = dbPrice;
        this.dbVolReq = dbVolReq;
        this.dbVolume = dbVolume;
        this.dbSumma = dbSumma;
    }

    public Integer getIdd() {
        return idd;
    }

    public LocalDateTime getStart() {
        return dtStart;
    }

    public LocalDateTime getEnd() {
        return dtEnd;
    }

    public String getCard() {
        return iddCard;
    }

    public String getCardInfo() {
        return cardInfo;
    }

    public Integer getIddAzs() {
        return iddAzs;
    }

    public Integer getIddTrk() {
        return iddTrk;
    }

    public Oil getOil() {
        return oil;
    }

    public AccType getAccType() {
        return accType;
    }

    public Long getPrice() {
        return dbPrice;
    }

    public Long getVolReq() {
        return dbVolReq;
    }

    public Long getVolume() {
        return dbVolume;
    }

    public Long getSumma() {
        return dbSumma;
    }
}
