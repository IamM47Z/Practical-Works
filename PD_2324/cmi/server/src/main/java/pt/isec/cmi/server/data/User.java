package pt.isec.cmi.server.data;

import pt.isec.cmi.shared.Exceptions;
import pt.isec.cmi.shared.data.UserData;
import pt.isec.cmi.shared.requests.RegisterData;

public class User implements CsvConvertible
{
    private int nif;
    private final int id;
    private final boolean isAdmin;
    private String email, password, username;

    public User(int id, String email, String password, String username, Integer nif, boolean isAdmin) throws Exceptions.InvalidFieldValue
    {
        if (0 > id)
            throw new Exceptions.InvalidFieldValue("ID cannot be negative");

        this.id = id;
        this.isAdmin = isAdmin;

        setNif(nif);
        setEmail(email);
        setPassword(password);
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

    public void setPassword(String password) throws Exceptions.InvalidFieldValue
    {
        if (null == password || password.isEmpty())
            throw new Exceptions.InvalidFieldValue("Password cannot be null or empty");

        if (3 > password.length())
            throw new Exceptions.InvalidFieldValue("Password must be at least 3 characters long");

        this.password = password;
    }

    public boolean checkPassword(String password)
    {
        return this.password.equals(password);
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

    @Override
    public String toCsv()
    {
        return String.format("\"%s\";\"%s\";\"%d\";\"%b\"", email, username, nif, isAdmin);
    }

    @Override
    public String getCsvHeader()
    {
        return "\"Email\";\"Username\";\"NIF\";\"Is Admin\"";
    }

    public static void isValidInput(RegisterData data) throws Exceptions.InvalidFieldValue
    {
        new User(0, data.email(), data.password(), data.username(), data.nif(), false);
    }
}
