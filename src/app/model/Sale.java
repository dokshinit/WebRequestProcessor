package app.model;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (22.01.18).
 */
public class Sale {

    private AccType accType;
    private Oil oil;
    private Long price, volume, summa;

    public Sale(Integer iAccType, Integer iddOil, Long price, Long volume, Long summa) {
        this.accType = AccType.byId(iAccType);
        this.oil = Oil.byId(iddOil);
        this.price = price;
        this.volume = volume;
        this.summa = summa;
    }

    public AccType getAccType() {
        return accType;
    }

    public Oil getOil() {
        return oil;
    }

    public Long getPrice() {
        return price;
    }

    public Long getVolume() {
        return volume;
    }

    public Long getSumma() {
        return summa;
    }
}
