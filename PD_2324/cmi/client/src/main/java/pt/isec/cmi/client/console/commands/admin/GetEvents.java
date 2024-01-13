package pt.isec.cmi.client.console.commands.admin;

import pt.isec.cmi.client.Client;
import pt.isec.cmi.client.SessionManager;
import pt.isec.cmi.client.console.Command;
import pt.isec.cmi.client.console.CommandBase;
import pt.isec.cmi.shared.requests.Request;
import pt.isec.cmi.shared.requests.filters.FilterFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static pt.isec.cmi.shared.requests.RequestType.GET_EVENTS;

@Command(name = "getEvents", usage = "getEvents [--name [eventName]] [--start-date [(dd/MM/yyyy)]] [--end-date [(dd/MM/yyyy)]] [--start-time [(HH:mm)]] [--end-time [(HH:mm)]]", description = "get user events that match the given filters", requestType = GET_EVENTS)
public class GetEvents extends CommandBase
{
    @Override
    public boolean isAvailable(SessionManager sessionManager)
    {
        return sessionManager.isLoggedIn() && sessionManager.isAdmin();
    }

    @Override
    public void handleSuccess(SessionManager sessionManager, Request.Response response)
    {
        List<Serializable> events = response.getDataList();
        if (events.isEmpty())
        {
            sessionManager.addToPrintQueue("No events found");
            return;
        }

        sessionManager.addToPrintQueue("Found " + events.size() + " events:");

        for (Serializable eventObj : events)
            sessionManager.addToPrintQueue(String.valueOf(eventObj));
    }

    @Override
    public void execute(SessionManager sessionManager, String[] args)
    {
        Client client = (Client) sessionManager;

        List<Serializable> filters = new ArrayList<>();
        for (int i = 1; i < args.length; i += 2)
        {
            if (i + 1 >= args.length)
            {
                client.addToPrintQueue("Usage: " + getUsage());
                return;
            }

            if (!args[i].startsWith("--"))
            {
                client.addToPrintQueue("Invalid argument: " + args[i]);
                return;
            }

            String type = args[i].substring(2);

            try
            {
                filters.add(FilterFactory.createFilter(type, args[i + 1]));
            }
            catch (Exception e)
            {
                client.addToPrintQueue(e.getMessage());
                return;
            }
        }

        if (!client.sendRequest(client.createRequest(GET_EVENTS).setDataList(filters)))
            client.addToPrintQueue("Failed to send request");
    }
}
