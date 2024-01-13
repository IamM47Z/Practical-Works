package pt.isec.cmi.client.console.commands.console.commands.admin;

import pt.isec.cmi.client.console.commands.Client;
import pt.isec.cmi.client.console.commands.SessionManager;
import pt.isec.cmi.client.console.commands.console.Command;
import pt.isec.cmi.client.console.commands.console.CommandBase;
import pt.isec.cmi.shared.requests.RegisterPresenceData;
import pt.isec.cmi.shared.requests.Request;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static pt.isec.cmi.shared.requests.RequestType.REMOVE_PRESENCES;

@Command(name = "removePresences", description = "removes presences from an event given the event id and the emails of the users to be removed", usage = "removePresences <eventId> <email> ... <email>", requestType = REMOVE_PRESENCES)
public class RemovePresences extends CommandBase
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

        client.addToPrintQueue("Presences removed successfully");
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

        for (int i = 2; i < args.length; i++)
        {
            RegisterPresenceData presenceData = new RegisterPresenceData(eventId, args[i]);
            if (data.contains(presenceData))
                continue;

            data.add(new RegisterPresenceData(eventId, args[i]));
        }

        if (!client.sendRequest(client.createRequest(REMOVE_PRESENCES).setDataList(data)))
            client.addToPrintQueue("Error sending REMOVE_PRESENCES request");
    }
}
