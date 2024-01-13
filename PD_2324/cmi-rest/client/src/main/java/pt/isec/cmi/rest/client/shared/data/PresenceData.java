package pt.isec.cmi.rest.client.shared.data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

public record PresenceData(int id, int userId, int eventId, LocalDateTime time) implements Serializable
{
    @Serial
    private static final long serialVersionUID = 5L;

    @Override
    public String toString()
    {
        return String.format("PresenceData{id=%d, userId=%d, eventId=%d, time=%s}", id, userId, eventId, time);
    }
}
