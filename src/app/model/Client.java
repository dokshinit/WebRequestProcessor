package app.model;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (02.02.18).
 */
public class Client {

    private Firm firm;
    private int idd, iddSub;
    private String inn, name, title, subTitle, address, email, phone;
    private boolean isFond;
    private int iWork;
    private String oilLimit, azsLimit;
    private String comment;
    private Long credit;

    public Client(int iddfirm, int idd, int iddSub, String inn, String name, String title, String subTitle,
                  String address, String email, String phone, boolean isFond, int iWork,
                  String oilLimit, String azsLimit, String comment, Long credit) {
        this.firm = Firm.byId(iddfirm);
        this.idd = idd;
        this.iddSub = iddSub;
        this.inn = inn;
        this.name = name;
        this.title = title;
        this.subTitle = subTitle;
        this.address = address;
        this.email = email;
        this.phone = phone;
        this.isFond = isFond;
        this.iWork = iWork;
        this.oilLimit = oilLimit;
        this.azsLimit = azsLimit;
        this.comment = comment;
        this.credit = credit;
    }

    public Firm getFirm() {
        return firm;
    }

    public int getIdd() {
        return idd;
    }

    public int getIddSub() {
        return iddSub;
    }

    public String getInn() {
        return inn;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public String getSubTitle() {
        return subTitle;
    }

    public String getAddress() {
        return address;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public boolean isFond() {
        return isFond;
    }

    public int getWork() {
        return iWork;
    }

    public String getOilLimit() {
        return oilLimit;
    }

    public String getAzsLimit() {
        return azsLimit;
    }

    public String getComment() {
        return comment;
    }

    public Long getCredit() {
        return credit;
    }
}
