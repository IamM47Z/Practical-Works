package pt.isec.cmi.shared.requests;

import java.io.Serial;
import java.io.Serializable;

public record RegisterPresenceData(int eventId, String email) implements Serializable
{
    @Serial
    private static final long serialVersionUID = 10L;

    @Override
    public String toString()
    {
        return String.format("RegisterPresenceData{eventId=%d, email=%s}", eventId, email);
    }
}
