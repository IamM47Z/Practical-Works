package pt.isec.cmi.shared.requests;

import java.io.Serial;

public record LoginData(String email, String password) implements java.io.Serializable
{
    @Serial
    private static final long serialVersionUID = 8L;

    @Override
    public String toString()
    {
        return String.format("LoginData{email=%s, password=%s}", email, password);
    }
}
