package pt.isec.cmi.shared.data;

import java.io.Serial;
import java.io.Serializable;

public record UserData(int id, String email, String username, Integer nif, boolean isAdmin) implements Serializable
{
    @Serial
    private static final long serialVersionUID = 6L;

    @Override
    public String toString()
    {
        return String.format("UserData{id=%d, email=%s, email=%s, nif=%d, isAdmin=%b}", id, email, username, nif, isAdmin);
    }
}
