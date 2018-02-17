package app.model;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (20.12.17).
 */
public class Azs {

    private int idd;
    private String title;
    private String address;
    //
    private int iddFirm, iddFirmMC;
    private int ibWork;

    public Azs(int idd, String title, String address, int iddFirm, int iddFirmMC, int ibWork) {
        this.idd = idd;
        this.title = title;
        this.address = address;
        this.iddFirm = iddFirm;
        this.iddFirmMC = iddFirmMC;
        this.ibWork = ibWork;
    }

    public int getIdd() {
        return idd;
    }

    public String getTitle() {
        return title;
    }

    public String getAddress() {
        return address;
    }

    public int getIddFirm() {
        return iddFirm;
    }

    public int getIddFirmMC() {
        return iddFirmMC;
    }

    public int getIbWork() {
        return ibWork;
    }
}
