package pt.isec.cmi.server.session;

import pt.isec.cmi.server.data.User;
import pt.isec.cmi.server.data.CsvConvertible;

import java.util.List;

public class SessionData
{
    private final String id;

    public SessionData(String id)
    {
        this.id = id;
    }

    private boolean loggedIn;
    private User me;

    public boolean isLoggedIn()
    {
        return loggedIn;
    }

    public User getUser()
    {
        return me;
    }

    public String getId()
    {
        return id;
    }

    public void onLogin(User user)
    {
        this.loggedIn = true;
        this.me = user;
    }

    public void onLogout()
    {
        this.loggedIn = false;
        this.me = null;
    }

    public boolean isAdmin()
    {
        return me.isAdmin();
    }

    private List<CsvConvertible> lastQueryResults;

    public List<CsvConvertible> getLastQueryResults()
    {
        return lastQueryResults;
    }

    public void setLastQueryResults(List<CsvConvertible> lastQueryResults)
    {
        this.lastQueryResults = lastQueryResults;
    }
}