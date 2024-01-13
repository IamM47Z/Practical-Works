package pt.isec.cmi.rest.client.console.commands;

import pt.isec.cmi.rest.client.Client;
import pt.isec.cmi.rest.client.SessionManager;
import pt.isec.cmi.rest.client.console.Command;
import pt.isec.cmi.rest.client.console.CommandBase;
import pt.isec.cmi.rest.client.requests.Request;
import pt.isec.cmi.rest.client.requests.RequestType;
import pt.isec.cmi.rest.client.shared.Exceptions;

import java.net.http.HttpResponse;

@Command(name = "addPresence", description = "adds presence to an event given the event code", usage = "addPresence <eventCode>")
public class AddPresence extends CommandBase
{
    @Override
    public boolean isAvailable(SessionManager sessionManager)
    {
        return sessionManager.isLoggedIn() && !sessionManager.isAdmin();
    }

    @Override
    public void execute(SessionManager sessionManager, String[] args)
    {
        Client client = (Client) sessionManager;

        if (2 != args.length)
        {
            client.addToPrintQueue("Usage: " + getAnnotation().usage());
            return;
        }

        try
        {
            HttpResponse<String> response = client.request(new Request(RequestType.ADD_PRESENCE), args[1]);
            if (200 != response.statusCode())
            {
                client.addToPrintQueue("Error adding presence request: " + response.body());
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
