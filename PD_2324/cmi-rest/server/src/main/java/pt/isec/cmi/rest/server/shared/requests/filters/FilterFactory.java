package pt.isec.cmi.rest.server.shared.requests.filters;

import pt.isec.cmi.rest.server.shared.TimeUtils;

import java.time.LocalDate;
import java.time.LocalTime;

public class FilterFactory
{
    public static FilterData createFilter(String type, String value)
    {
        return switch (type)
        {
            case "name" -> new FilterData(FilterType.EVENT_NAME, value);
            case "start-date" ->
            {
                try
                {
                    LocalDate time = LocalDate.parse(value, TimeUtils.DATE_FORMATTER);
                    yield new FilterData(FilterType.SEARCH_START_DATE, time);
                }
                catch (Exception e)
                {
                    throw new IllegalArgumentException("Invalid date format");
                }
            }
            case "end-date" ->
            {
                try
                {
                    LocalDate time = LocalDate.parse(value, TimeUtils.DATE_FORMATTER);
                    yield new FilterData(FilterType.SEARCH_END_DATE, time);
                }
                catch (Exception e)
                {
                    throw new IllegalArgumentException("Invalid date format");
                }
            }
            case "start-time" ->
            {
                try
                {
                    LocalTime time = LocalTime.parse(value, TimeUtils.TIME_FORMATTER);
                    yield new FilterData(FilterType.SEARCH_START_TIME, time);
                }
                catch (Exception e)
                {
                    throw new IllegalArgumentException("Invalid time format");
                }
            }
            case "end-time" ->
            {
                try
                {
                    LocalTime time = LocalTime.parse(value, TimeUtils.TIME_FORMATTER);
                    yield new FilterData(FilterType.SEARCH_END_TIME, time);
                }
                catch (Exception e)
                {
                    throw new IllegalArgumentException("Invalid time format");
                }
            }
            default -> throw new IllegalArgumentException("Invalid filter type");
        };
    }
}
