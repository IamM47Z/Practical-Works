package pt.isec.cmi.rest.client.shared.requests.filters;

import pt.isec.cmi.rest.client.shared.TimeUtils;
import pt.isec.cmi.rest.client.shared.data.EventData;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

public class FilterData implements Serializable
{
    @Serial
    private static final long serialVersionUID = 13L;

    private FilterType type;
    private String valueString = null;
    private LocalDate valueDate = null;
    private LocalTime valueTime = null;

    public FilterData(FilterType type, String value)
    {
        this.type = type;
        this.valueString = value;
    }

    public FilterData(FilterType type, LocalDate value)
    {
        this.type = type;
        this.valueDate = value;
    }

    public FilterData(FilterType type, LocalTime value)
    {
        this.type = type;
        this.valueTime = value;
    }

    public boolean matches(EventData eventData)
    {
        return switch (type)
        {
            case EVENT_NAME -> eventData.name().equals(valueString);
            case SEARCH_START_DATE -> TimeUtils.isEqualOrAfter(eventData.date(), valueDate);
            case SEARCH_END_DATE -> TimeUtils.isEqualOrBefore(eventData.date(), valueDate);
            case SEARCH_START_TIME -> TimeUtils.isEqualOrAfter(eventData.startTime(), valueTime);
            case SEARCH_END_TIME -> TimeUtils.isEqualOrBefore(eventData.startTime().plusMinutes(eventData.durationMinutes()), valueTime);
        };
    }

    FilterType type()
    {
        return type;
    }

    LocalDate date()
    {
        return valueDate;
    }

    LocalTime time()
    {
        return valueTime;
    }

    String string()
    {
        return valueString;
    }
}