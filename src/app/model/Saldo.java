package app.model;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (22.01.18).
 */
public class Saldo {

    private Integer iddAcc;
    private AccType accType;
    private Oil oil;
    private Long saldo;

    public Saldo(Integer iddAcc, Integer iAccType, Integer iddOil, Long saldo) {
        this.iddAcc = iddAcc;
        this.accType = AccType.byId(iAccType);
        this.oil = Oil.byId(iddOil);
        this.saldo = saldo;
    }

    public Integer getIddAcc() {
        return iddAcc;
    }

    public AccType getAccType() {
        return accType;
    }

    public Oil getOil() {
        return oil;
    }

    public Long getSaldo() {
        return saldo;
    }
}
