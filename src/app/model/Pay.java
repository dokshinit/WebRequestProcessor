package app.model;

import java.time.LocalDate;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (22.01.18).
 */
public class Pay {

    private LocalDate dtw;
    private String doc;
    private AccType accType;
    private Oil oil;
    private Long volume;
    private Long summa;

    public Pay(LocalDate dtw, String doc, Integer iAccType, Integer iddOil, Long volume, Long summa) {
        this.doc = doc;
        this.dtw = dtw;
        this.accType = AccType.byId(iAccType);
        this.oil = Oil.byId(iddOil);
        this.volume = volume;
        this.summa = summa;
    }

    public String getDoc() {
        return doc;
    }

    public LocalDate getDtw() {
        return dtw;
    }

    public AccType getAccType() {
        return accType;
    }

    public Oil getOil() {
        return oil;
    }

    public Long getVolume() {
        return volume;
    }

    public Long getSumma() {
        return summa;
    }
}
