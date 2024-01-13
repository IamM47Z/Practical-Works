package pt.isec.cmi.shared.requests.filters;

import pt.isec.cmi.shared.TimeUtils;
import pt.isec.cmi.shared.data.EventData;

import java.io.Serial;
import java.io.Serializable;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;

public record FilterData(FilterType type, Object value) implements Serializable
{
    @Serial
    private static final long serialVersionUID = 13L;

    public boolean matches(EventData eventData)
    {
        return switch (type)
        {
            case EVENT_NAME -> eventData.name().equals(value);
            case SEARCH_START_DATE -> TimeUtils.isEqualOrAfter(eventData.date(), (LocalDate) value);
            case SEARCH_END_DATE -> TimeUtils.isEqualOrBefore(eventData.date(), (LocalDate) value);
            case SEARCH_START_TIME -> TimeUtils.isEqualOrAfter(eventData.startTime(), (LocalTime) value);
            case SEARCH_END_TIME -> TimeUtils.isEqualOrBefore(eventData.startTime().plusMinutes(eventData.durationMinutes()), (LocalTime) value);
        };
    }
}