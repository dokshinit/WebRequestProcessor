package app.model;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (22.12.17).
 */
public enum AccType {

    MONEY(0, "Деньги", "Д"),
    BANK(1, "Банк", "П/К"),
    KEEP(7, "Хранение", "О/Х");

    public final int id;
    public final String title, abbreviation;

    AccType(int idd, String title, String abbreviation) {
        this.id = idd;
        this.title = title;
        this.abbreviation = abbreviation;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public static AccType byId(Integer idd) {
        if (idd == null) return null;
        for (AccType item : values()) if (item.id == idd) return item;
        return null;
    }
}
