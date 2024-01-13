package pt.isec.cmi.shared.requests;

import java.io.Serial;
import java.io.Serializable;

public record GenerateCodeData(int eventId, int lifespanMinutes) implements Serializable
{
    @Serial
    private static final long serialVersionUID = 7L;
}
