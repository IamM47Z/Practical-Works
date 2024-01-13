package pt.isec.cmi.rest.client.console.commands;

import pt.isec.cmi.rest.client.Client;
import pt.isec.cmi.rest.client.SessionManager;
import pt.isec.cmi.rest.client.console.Command;
import pt.isec.cmi.rest.client.console.CommandBase;

@Command(name = "logout", description = "logout from the server", usage = "logout")
public class Logout extends CommandBase
{
    @Override
    public boolean isAvailable(SessionManager sessionManager)
    {
        return sessionManager.isLoggedIn();
    }

    @Override
    public void execute(SessionManager sessionManager, String[] args)
    {
        Client client = (Client) sessionManager;

        if (1 != args.length)
        {
            client.addToPrintQueue("Usage: " + this.getClass().getAnnotation(Command.class).usage());
            return;
        }

        client.logout();
        client.addToPrintQueue("Logged out successfully");
    }
}
