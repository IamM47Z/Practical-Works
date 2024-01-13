package pt.isec.cmi.client.console.commands.console.commands.admin;

import pt.isec.cmi.client.console.commands.Client;
import pt.isec.cmi.client.console.commands.SessionManager;
import pt.isec.cmi.client.console.commands.console.Command;
import pt.isec.cmi.client.console.commands.console.CommandBase;
import pt.isec.cmi.shared.requests.Request;

import static pt.isec.cmi.shared.requests.RequestType.REMOVE_EVENT;

@Command(name = "removeEvent", usage = "removeEvent <eventId>", description = "removes an event", requestType = REMOVE_EVENT)
public class RemoveEvent extends CommandBase
{
    @Override
    public boolean isAvailable(SessionManager sessionManager)
    {
        return sessionManager.isLoggedIn() && sessionManager.isAdmin();
    }

    @Override
    public void handleSuccess(SessionManager sessionManager, Request.Response response)
    {
        sessionManager.addToPrintQueue("Event removed successfully");
    }

    @Override
    public void execute(SessionManager sessionManager, String[] args)
    {
        Client client = (Client) sessionManager;

        if (2 > args.length)
        {
            client.addToPrintQueue("Usage: " + getAnnotation().usage());
            return;
        }

        int eventId;
        try
        {
            eventId = Integer.parseInt(args[1]);
        }
        catch (NumberFormatException e)
        {
            client.addToPrintQueue("Invalid event id");
            return;
        }

        if (!client.sendRequest(client.createRequest(REMOVE_EVENT, eventId)))
            client.addToPrintQueue("Error sending REMOVE_EVENT request");
    }
}
