package pt.isec.cmi.shared.requests;

import java.io.Serial;

public record RegisterData(String email, String password, String username, Integer nif) implements java.io.Serializable
{
    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public String toString()
    {
        return String.format("RegisterData{email=%s, password=%s, email=%s, nif=%d}", email, password, username, nif);
    }
}
