package app.model;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (22.12.17).
 */
public enum ContractType {

    NORMAL(1, "Договор", "Д"),
    MUNICIPAL(2, "Муниципальный контракт", "М/К"),
    GOVERNMENT(3, "Государственный контракт", "Г/К");

    public final int id;
    public final String title, abbreviation;

    ContractType(int idd, String title, String abbreviation) {
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

    public static ContractType byId(Integer idd) {
        if (idd == null) return null;
        for (ContractType item : values()) if (item.id == idd) return item;
        return null;
    }
}
