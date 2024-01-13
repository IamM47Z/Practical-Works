package pt.isec.cmi.client.console.commands;

import pt.isec.cmi.client.console.commands.console.ConsoleApplication;
import pt.isec.cmi.shared.data.UserData;

public class SessionManager extends ConsoleApplication
{
    private boolean loggedIn;
    private UserData data;

    public boolean isLoggedIn()
    {
        return loggedIn;
    }

    public UserData getData()
    {
        return data;
    }

    public void onLogin(UserData data)
    {
        this.loggedIn = true;
        this.data = data;
    }

    public void onLogout()
    {
        this.loggedIn = false;
        this.data = null;
    }

    public boolean isAdmin()
    {
        return data.isAdmin();
    }
}