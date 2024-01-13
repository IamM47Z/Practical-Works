package pt.isec.cmi.client.console.commands.console.commands;

import pt.isec.cmi.client.console.commands.Client;
import pt.isec.cmi.client.console.commands.SessionManager;
import pt.isec.cmi.client.console.commands.console.Command;
import pt.isec.cmi.client.console.commands.console.CommandBase;
import pt.isec.cmi.shared.requests.Request;

import static pt.isec.cmi.shared.requests.RequestType.LOGOUT;

@Command(name = "logout", description = "logout from the server", usage = "logout", requestType = LOGOUT)
public class Logout extends CommandBase
{
    @Override
    public boolean isAvailable(SessionManager sessionManager)
    {
        return sessionManager.isLoggedIn();
    }

    @Override
    public void handleSuccess(SessionManager sessionManager, Request.Response response)
    {
        Client client = (Client) sessionManager;

        client.onLogout();
        client.addToPrintQueue("Logged out successfully");
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

        if (!client.sendRequest(client.createRequest(LOGOUT)))
            client.addToPrintQueue("Error sending request");
    }
}
