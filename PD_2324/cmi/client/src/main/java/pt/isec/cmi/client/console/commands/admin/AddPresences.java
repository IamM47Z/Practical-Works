package pt.isec.cmi.client.console.commands.admin;

import pt.isec.cmi.client.Client;
import pt.isec.cmi.client.SessionManager;
import pt.isec.cmi.client.console.Command;
import pt.isec.cmi.client.console.CommandBase;

import pt.isec.cmi.shared.requests.RegisterPresenceData;
import pt.isec.cmi.shared.requests.Request;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static pt.isec.cmi.shared.requests.RequestType.ADD_PRESENCES;

@Command(name = "addPresences", description = "adds presences to an event given the event id and the emails of the users to add", usage = "addPresences <eventId> <email> <email> ...", requestType = ADD_PRESENCES)
public class AddPresences extends CommandBase
{
    @Override
    public boolean isAvailable(SessionManager sessionManager)
    {
        return sessionManager.isLoggedIn() && sessionManager.isAdmin();
    }

    @Override
    public void handleSuccess(SessionManager sessionManager, Request.Response response)
    {
        Client client = (Client) sessionManager;

        client.addToPrintQueue("Presences added successfully");
    }

    @Override
    public void execute(SessionManager sessionManager, String[] args)
    {
        Client client = (Client) sessionManager;

        if (3 > args.length)
        {
            client.addToPrintQueue("Usage: " + getAnnotation().usage());
            return;
        }

        int eventId;
        try 
        {
            eventId = Integer.parseInt(args[1]);
        }
        catch (NumberFormatException ignored)
        {
            client.addToPrintQueue("Invalid event ID, must be a number");
            return;
        }

        List<Serializable> data = new ArrayList<>();

        for(int i = 2; i < args.length; i++)
            data.add(new RegisterPresenceData(eventId, args[i]));

        if (!client.sendRequest(client.createRequest(ADD_PRESENCES).setDataList(data)))
            client.addToPrintQueue("Error sending ADD_PRESENCES request");
    }
}
