package pt.isec.cmi.rest.client.shared.requests;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

public record RegisterEventData(String name, String location, LocalDate date, LocalTime startTime, int durationMinutes) implements Serializable
{
    @Serial
    private static final long serialVersionUID = 9L;

    @Override
    public String toString()
    {
        return String.format("RegisterEventData{name=%s, location=%s, date=%s, startTime=%s, durationMinutes=%d}", name, location, date, startTime, durationMinutes);
    }
}