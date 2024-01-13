package pt.isec.cmi.rest.client;

import pt.isec.cmi.rest.client.shared.data.UserData;

import pt.isec.cmi.rest.client.console.ConsoleApplication;

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