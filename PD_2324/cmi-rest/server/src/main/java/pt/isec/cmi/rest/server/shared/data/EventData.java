package pt.isec.cmi.rest.server.shared.data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

public record EventData(int id, String name, String location, LocalDate date, LocalTime startTime, int durationMinutes) implements Serializable
{
    @Serial
    private static final long serialVersionUID = 3L;

    @Override
    public String toString()
    {
        return String.format("EventData{id=%d, name=%s, location=%s, date=%s, startTime=%s, durationMinutes=%d}", id, name, location, date, startTime, durationMinutes);
    }
}
