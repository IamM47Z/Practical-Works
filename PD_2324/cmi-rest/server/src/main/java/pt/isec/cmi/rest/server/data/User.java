package pt.isec.cmi.rest.server.data;

import org.springframework.security.crypto.bcrypt.BCrypt;
import pt.isec.cmi.rest.server.shared.Exceptions;
import pt.isec.cmi.rest.server.shared.data.UserData;
import pt.isec.cmi.rest.server.shared.requests.RegisterData;

public class User
{
    private int nif;
    private final int id;
    private final boolean isAdmin;
    private String email, username;
    private transient final String passwordHash;

    public User(int id, String email, String passwordHash, String username, Integer nif, boolean isAdmin) throws Exceptions.InvalidFieldValue
    {
        if (0 > id)
            throw new Exceptions.InvalidFieldValue("ID cannot be negative");

        this.id = id;
        this.isAdmin = isAdmin;
        this.passwordHash = passwordHash;

        setNif(nif);
        setEmail(email);
        setUsername(username);
    }

    public int getId()
    {
        return id;
    }

    public Integer getNif()
    {
        return nif;
    }

    public void setNif(Integer nif) throws Exceptions.InvalidFieldValue
    {
        if (null == nif)
            throw new Exceptions.InvalidFieldValue("NIF cannot be null");

        if (9 != nif.toString().length())
            throw new Exceptions.InvalidFieldValue("NIF must be 9 digits long");

        this.nif = nif;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email) throws Exceptions.InvalidFieldValue
    {
        if (null == email || email.isEmpty())
            throw new Exceptions.InvalidFieldValue("Email cannot be null or empty");

        if (!email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$"))
            throw new Exceptions.InvalidFieldValue("Invalid email format");

        this.email = email;
    }

    public boolean checkPassword(String password)
    {
        return BCrypt.checkpw(password, this.passwordHash);
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username) throws Exceptions.InvalidFieldValue
    {
        if (null == username || username.isEmpty())
            throw new Exceptions.InvalidFieldValue("Username cannot be null or empty");

        if (3 > username.length())
            throw new Exceptions.InvalidFieldValue("Username must be at least 3 characters long");

        this.username = username;
    }

    public boolean isAdmin()
    {
        return isAdmin;
    }

    public UserData toUserData()
    {
        return new UserData(id, email, username, nif, isAdmin);
    }

    public static void isValidInput(RegisterData data) throws Exceptions.InvalidFieldValue
    {
        String password = data.password();

        if (null == password || password.isEmpty())
            throw new Exceptions.InvalidFieldValue("Password cannot be null or empty");

        if (3 > password.length())
            throw new Exceptions.InvalidFieldValue("Password must be at least 3 characters long");

        new User(0, data.email(), BCrypt.hashpw(password, BCrypt.gensalt()), data.username(), data.nif(), false);
    }
}
