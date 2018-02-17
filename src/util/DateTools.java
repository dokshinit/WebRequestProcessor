/*
 * Copyright (c) 2013, Aleksey Nikolaevich Dokshin. All right reserved.
 * Contacts: dant.it@gmail.com, dokshin@list.ru.
 */
package util;

import java.text.SimpleDateFormat;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Расширение функционала операций с датами.
 *
 * @author Докшин Алексей Николаевич <dant.it@gmail.com>
 */
public class DateTools {

    /**
     * Предопределенный формат для вывода даты с номером года 2 знака.
     */
    public static SimpleDateFormat formatShort = new SimpleDateFormat("dd.MM.yy");
    /**
     * Предопределенный формат для вывода даты с номером года 4 знака.
     */
    public static SimpleDateFormat formatFull = new SimpleDateFormat("dd.MM.yyyy");

    /**
     * Возвращает формат для преобразования даты в строку в соответствии с флагом.
     *
     * @param isShort Флаг - короткий формат (true) или длинный (false).
     * @return Форматер.
     */
    public static SimpleDateFormat getFormat(boolean isShort) {
        if (isShort) {
            return formatShort;
        } else {
            return formatFull;
        }
    }

    /**
     * Комбинирует дату из двух дат. Из первого параметра берёт дату, из второго - время.
     *
     * @param date Дата.
     * @param time Время.
     * @return Комбинированная дата.
     */
    public static Date getDT(Date date, Date time) {
        // ВНИМАНИЕ!!!
        // Переносим именно поля даты из первого аргумента во второй!
        // Т.к. при операциях с полями времени заморочки с UTS и поясами,
        // что может вызвать ошибки с неправильным преобразованием времени! (гемор)
        Calendar d = Calendar.getInstance();
        d.setTime(date);
        Calendar t = Calendar.getInstance();
        t.setTime(time);
        t.set(Calendar.ERA, d.get(Calendar.ERA));
        t.set(Calendar.YEAR, d.get(Calendar.YEAR));
        t.set(Calendar.MONTH, d.get(Calendar.MONTH));
        t.set(Calendar.DAY_OF_MONTH, d.get(Calendar.DAY_OF_MONTH));
        return t.getTime();
    }

    /**
     * Добавляет к дате заданное количество дней.
     *
     * @param date  Дата.
     * @param count Количество дней. Может быть отрицательным для уменьшения.
     * @return Инкрементированная дата.
     */
    public static Date addDay(Date date, int count) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DAY_OF_MONTH, count);
        return c.getTime();
    }

    /**
     * Добавляет к дате заданное количество месяцев.
     *
     * @param date  Дата.
     * @param count Количество месяцев. Может быть отрицательным для уменьшения.
     * @return Инкрементированная дата.
     */
    public static Date addMonth(Date date, int count) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.MONTH, count);
        return c.getTime();
    }

    /**
     * Возвращает дату, переданную в значении.
     *
     * @param value Значение.
     * @return Если значение - дата, то возвращается дата, если нет - null.
     */
    public static Date getDateFromValue(Object value) {
        if ((value != null) && (value instanceof Date)) {
            return (Date) value;
        } else {
            return null;
        }
    }

    /**
     * Возвращает значение из переданной даты.
     *
     * @param date Дата.
     * @return Если дата не null, то возвращается дата, если нет - целое = 0.
     */
    public static Object getValueFromDate(Date date) {
        if (date != null) {
            return date;
        } else {
            return (Integer) 0;
        }
    }

    /**
     * Возвращает дату начала месяца.
     *
     * @param date Исходная дата.
     * @return Дата начала месяца.
     */
    public static Date getStartMonth(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.DAY_OF_MONTH, 1);
        return c.getTime();
    }

    /**
     * Возвращает дату конца месяца.
     *
     * @param date Исходная дата.
     * @return Дата конца месяца.
     */
    public static Date getEndMonth(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.roll(Calendar.DAY_OF_MONTH, -1);
        return c.getTime();
    }

    /**
     * Возвращает дату по указанным числовым данным (с нулевым временем!).
     *
     * @param year  Год.
     * @param month Месяц (1..12)!
     * @param day   День.
     * @return Дата.
     */
    public static Date getDate(int year, int month, int day) {
        Calendar c = Calendar.getInstance();
        c.set(year, month - 1, day, 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    /**
     * Возвращает указанную дату без времени.
     *
     * @param date Дата.
     * @return Дата с обнуленным временем.
     */
    public static Date getDateWOTime(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    /**
     * Возвращает время из указанной даты.
     *
     * @param date Дата.
     * @return Дата с обнуленной датой (только время).
     */
    public static Date getTimeWODate(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.ERA, 0);
        c.set(Calendar.YEAR, 0);
        c.set(Calendar.MONTH, 0);
        c.set(Calendar.DAY_OF_MONTH, 0);
        return c.getTime();
    }

    /**
     * Возвращает дату с добавлением в неё текущего времени.
     *
     * @param date Дата.
     * @return Дата с установленным текущим временем.
     */
    public static Date getDateWithCurrentTime(Date date) {
        return getDT(date, new Date());
    }

    /**
     * Проверка на попадание даты в диапазон дат.
     *
     * @param value Проверяемая дата.
     * @param d1    Начало периода (включительно!).
     * @param d2    Конец периода (НЕ включительно!).
     * @return Если хоть один из аргументов равен null или проверяемая дата выходит за диапазон, то возвращает false,
     * иначе true.
     */
    public static boolean isInPeriodSafe(Date value, Date d1, Date d2) {
        if (value == null || d1 == null || d2 == null) {
            return false;
        } else {
            return !(value.before(d1) || !value.before(d2));
        }
    }

    /**
     * Проверка на попадание даты в диапазон дат (конечная дата включается!).
     *
     * @param value Проверяемая дата.
     * @param d1    Начало периода (включительно!).
     * @param d2    Конец периода (НЕ включительно!).
     * @return Если хоть один из аргументов равен null или проверяемая дата выходит за диапазон, то возвращает false,
     * иначе true.
     */
    public static boolean isInPeriodIncSafe(Date value, Date d1, Date d2) {
        if (value == null || d1 == null || d2 == null) {
            return false;
        } else {
            return !(value.before(d1) || value.after(d2));
        }
    }

    /**
     * Извлечение года из даты.
     *
     * @param date Дата.
     * @return Год (полное значение - четырехзначное число).
     */
    public static int extractYear(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return c.get(Calendar.YEAR);
    }

    /**
     * Извлечение времени из таймштампа в виде кол-ва миллисекунд с начала суток.
     *
     * @param dt Таймштамп.
     * @return Кол-во миллисекунд с начала суток.
     */
    public static long extractDayTimeAsMillis(Date dt) {
        Calendar c = Calendar.getInstance();
        c.setTime(dt);
        int h = c.get(Calendar.HOUR_OF_DAY);
        int m = c.get(Calendar.MINUTE);
        int s = c.get(Calendar.SECOND);
        int ms = c.get(Calendar.MILLISECOND);
        return (long) ms + (s * 1000) + (m * 60000) + (h * 3600000);
    }

    public static long extractDayTimeAsMillis(long dt) {
        return extractDayTimeAsMillis(new Date(dt));
    }

    public static int extractDayTimeAsSeconds(Date dt) {
        return (int) (DateTools.extractDayTimeAsMillis(dt) / 1000);
    }

    public static int extractDayTimeAsMinutes(Date dt) {
        return (int) (DateTools.extractDayTimeAsMillis(dt) / 60000);
    }


    public static Date getDayTimeFromMillis(long millis) {
        int val = (int) (millis < 0 ? -millis : millis) % (24 * 3600 * 1000);
        Calendar c = Calendar.getInstance();
        //c.setTimeZone(TimeZone.getDefault());
        //c.setTime(new Date());
        c.set(Calendar.YEAR, 0);
        c.set(Calendar.MONTH, 0);
        c.set(Calendar.DAY_OF_MONTH, 0);
        c.set(Calendar.HOUR_OF_DAY, val / 3600000);
        c.set(Calendar.MINUTE, (val % 3600000) / 60000);
        c.set(Calendar.SECOND, (val % 60000) / 1000);
        c.set(Calendar.MILLISECOND, val % 1000);
        return c.getTime();
    }

    /**
     * Представление кол-ва миллисекунд в виде строки (HH:MM:SS) миллисекунды отбрасываются.
     *
     * @param mseconds Кол-во миллисекунд.
     * @return Строка.
     */
    public static String formatHHMMSS(long mseconds) {
        long val = mseconds < 0 ? -mseconds : mseconds;
        long h = val / 3600000;
        long m = (val % 3600000) / 60000;
        long s = (val % 60000) / 1000;
        return String.format("%s%d:%02d:%02d", mseconds < 0 ? "-" : "", h, m, s);
    }

    public static final String daysOfWeekShort[] = {"пн", "вт", "ср", "чт", "пт", "сб", "вс"};
    public static final String daysOfWeek[] = {"понедельник", "вторник", "среда", "четверг", "пятница", "суббота", "воскресение"};

    /** Возвращает день недели начиная с понедельника (!): пн=1 ... вс=7, 0-ошибка. */
    public static int getDayOfWeekNumber(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        switch (c.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY:
                return 1;
            case Calendar.TUESDAY:
                return 2;
            case Calendar.WEDNESDAY:
                return 3;
            case Calendar.THURSDAY:
                return 4;
            case Calendar.FRIDAY:
                return 5;
            case Calendar.SATURDAY:
                return 6;
            case Calendar.SUNDAY:
                return 7;
        }
        return 0;
    }

    public static String getDayOfWeekShortTitle(Date date) {
        if (date == null) return "";
        return daysOfWeekShort[getDayOfWeekNumber(date) - 1];
    }

    public static String getDayOfWeekTitle(Date date) {
        if (date == null) return "";
        return daysOfWeek[getDayOfWeekNumber(date) - 1];
    }

    public static final String monthsOfYear[] = {"январь", "февраль", "март", "апрель", "май", "июнь",
            "июль", "август", "сентябрь", "октябрь", "ноябрь", "девабрь"};
    public static final String monthsOfYearTh[] = {"января", "февраля", "марта", "апреля", "мая", "июня",
            "июля", "августа", "сентября", "октября", "ноября", "девабря"};

    /** Возвращает номер месяца из даты. */
    public static int getMonthOfYear(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return c.get(Calendar.MONTH) + 1;
    }

    /** Возвращает номер дня месяца из даты. */
    public static int getDayOfMonth(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return c.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * Возвращает название меяца для его номера.
     *
     * @param month Номер месяца 1-12.
     * @param th    true - в родительском падеже, false - в именительном.
     * @return Наименование месяца.
     */
    public static String getMonthOfYearTitle(int month, boolean th) {
        return th ? monthsOfYearTh[month - 1] : monthsOfYear[month - 1];
    }

    /**
     * Возвращает название меяца для даты.
     *
     * @param date Дата.
     * @param th   true - в родительском падеже, false - в именительном.
     * @return Наименование месяца.
     */
    public static String getMonthOfYearTitle(Date date, boolean th) {
        if (date == null) return "";
        return getMonthOfYearTitle(getMonthOfYear(date), th);
    }
    public static Date asDate(LocalTime localTime) {
        if (localTime == null) return null;
        Date dt = Date.from(localTime.atDate(LocalDate.of(1, 1, 2000)).atZone(ZoneId.systemDefault()).toInstant());
        return DateTools.getTimeWODate(dt);
    }

    public static Date asDate(LocalDate localDate) {
        return localDate == null ? null : Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
    }

    public static Date asDate(LocalDateTime localDateTime) {
        return localDateTime == null ? null : Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static long toMillis(LocalDateTime localDateTime) {
        return localDateTime == null ? 0 : localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public static ZonedDateTime asZonedDataTime(Date date) {
        return  date == null ? null : Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault());
    }

    public static LocalTime asLocalTime(Date date) {
        return date == null ? null : asZonedDataTime(date).toLocalTime();
    }

    public static LocalDate asLocalDate(Date date) {
        return date == null ? null : asZonedDataTime(date).toLocalDate();
    }

    public static LocalDateTime asLocalDateTime(Date date) {
        return date == null ? null : asZonedDataTime(date).toLocalDateTime();
    }
}
