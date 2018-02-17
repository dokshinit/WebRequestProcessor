package app.model;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (20.12.17).
 */
public enum Firm {

    TP(1, "ТП", "Транзит", "ООО \"Транзит-Плюс\"", "1101028043",
            "Юр.адрес: 197022, г.Санкт-Петербург, Песочная набережная, д.42, корпус 2, литер «А»",
            "Тел./факс: (8212) 20-19-07, 21-15-98 (нефтебаза) email: tk@tp-rk.ru сайт: www.tp-rk.ru",
            "р/с 40702810328000102750 в Коми отделении №8617 ПАО Сбербанк г.Сыктывкар, БИК 048702640, к/c 30101810400000000640"),

    K2000(2, "К2", "Компания", "ООО \"Компания 2000\"", "1101027258",
            "Юр.адрес: 167982 г.Сыктывкар, ул.Маркова, д.28",
            "Тел./факс: (8212) 20-19-17, 20-33-17 email: buh@k2000-rk.ru сайт: www.k2000-rk.ru",
            "р/с 40702810128000102400 в Коми отделении №8617 ПАО Сбербанк г.Сыктывкар, БИК 048702640, к/c 30101810400000000640"),

    SNOW(103, "СН", "Снежинка", "ООО \"Снежинка\"", "1102066588",
            "Юр.адрес: 169300 г.Ухта, ул.Вокзальная, д.11",
            "Тел./факс: (8216) 74-91-33  email: iap2000@yandex.ru",
            "р/с 40702810028190001786 в Коми отделении №8617 ПАО Сбербанк г.Сыктывкар, БИК 048702640, к/с 30101810400000000640, ОГРН 1101102002242"),
    //
    HOLDING(0, "HO", "Холдинг", "Холдинг", "",
            "Адрес: 167000 г.Сыктывкар, ул.Советская, д.3", "Тел./факс: (8212) 20-19-17, 20-19-07", "");

    public final int id;
    public final String abbreviation;
    public final String name;
    public final String title;
    public final String inn, address, contacts, bank;

    Firm(int idd, String abbreviation, String name, String title, String inn, String address, String contacts, String bank) {
        this.id = idd;
        this.abbreviation = abbreviation;
        this.name = name;
        this.title = title;
        this.inn = inn;
        this.address = address;
        this.contacts = contacts;
        this.bank = bank;
    }

    public int getId() {
        return id;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public String getInn() {
        return inn;
    }

    public String getAddress() {
        return address;
    }

    public String getContacts() {
        return contacts;
    }

    public String getBank() {
        return bank;
    }

    public static Firm byId(Integer idd) {
        if (idd == null) return null;
        for (Firm item : values()) if (item.id == idd) return item;
        return null;
    }
}
