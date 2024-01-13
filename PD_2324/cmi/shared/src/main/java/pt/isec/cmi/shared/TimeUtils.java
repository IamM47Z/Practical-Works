package pt.isec.cmi.shared;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class TimeUtils
{
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static boolean isEqualOrAfter(LocalDate value1, LocalDate value2)
    {
        if (null == value1 || null == value2)
            return false;

        return value1.isEqual(value2) || value1.isAfter(value2);
    }

    public static boolean isEqualOrAfter(LocalTime value1, LocalTime value2)
    {
        if (null == value1 || null == value2)
            return false;

        return value1.equals(value2) || value1.isAfter(value2);
    }


    public static boolean isEqualOrBefore(LocalDate value1, LocalDate value2)
    {
        if (null == value1 || null == value2)
            return false;

        return value1.isEqual(value2) || value1.isBefore(value2);
    }

    public static boolean isEqualOrBefore(LocalTime value1, LocalTime value2)
    {
        if (null == value1 || null == value2)
            return false;

        return value1.equals(value2) || value1.isBefore(value2);
    }

    public static boolean isInRange(LocalTime start, LocalTime end, LocalTime value, boolean excluded)
    {
        if (excluded)
            return value.isAfter(start) && value.isBefore(end);

        return isEqualOrAfter(value, start) && isEqualOrBefore(value, end);
    }

    public static boolean isInRange(LocalTime start, LocalTime end, LocalTime value)
    {
        return isInRange(start, end, value, false);
    }

    public static boolean isInRange(LocalDate start, LocalDate end, LocalDate value, boolean excluded)
    {
        if (excluded)
            return value.isAfter(start) && value.isBefore(end);

        return isEqualOrAfter(value, start) && isEqualOrBefore(value, end);
    }

    public static boolean isInRange(LocalDate start, LocalDate end, LocalDate value)
    {
        return isInRange(start, end, value, false);
    }
}
