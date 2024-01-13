package pt.isec.cmi.rest.client.console.commands.admin;

import pt.isec.cmi.rest.client.Client;
import pt.isec.cmi.rest.client.SessionManager;
import pt.isec.cmi.rest.client.console.Command;
import pt.isec.cmi.rest.client.console.CommandBase;
import pt.isec.cmi.rest.client.requests.Request;
import pt.isec.cmi.rest.client.requests.RequestType;
import pt.isec.cmi.rest.client.shared.Exceptions;

import java.net.http.HttpResponse;

@Command(name = "removeEvent", usage = "removeEvent <eventId>", description = "removes an event")
public class RemoveEvent extends CommandBase
{
    @Override
    public boolean isAvailable(SessionManager sessionManager)
    {
        return sessionManager.isLoggedIn() && sessionManager.isAdmin();
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

        try
        {
            HttpResponse<String> response = client.request(new Request(RequestType.REMOVE_EVENT), eventId);
            if (200 != response.statusCode())
            {
                client.addToPrintQueue("Error removing event: " + response.body());
                return;
            }

            client.addToPrintQueue(response.body());
        }
        catch (Exceptions.InvalidToken e)
        {
            client.addToPrintQueue("Invalid token");
        }
    }
}
