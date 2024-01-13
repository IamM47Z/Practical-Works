package pt.isec.cmi.rest.client.console.commands;

import pt.isec.cmi.rest.client.Client;
import pt.isec.cmi.rest.client.SessionManager;
import pt.isec.cmi.rest.client.console.Command;
import pt.isec.cmi.rest.client.console.CommandBase;

import pt.isec.cmi.rest.client.requests.Request;
import pt.isec.cmi.rest.client.requests.RequestType;
import pt.isec.cmi.rest.client.shared.Exceptions;
import pt.isec.cmi.rest.client.shared.data.PresenceData;
import pt.isec.cmi.rest.client.shared.requests.filters.FilterFactory;

import java.io.Serializable;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Command(name = "getPresences", usage = "getPresences [--name [eventName]] [--start-date [(dd/MM/yyyy)]] [--end-date [(dd/MM/yyyy)]] [--start-time [(HH:mm)]] [--end-time [(HH:mm)]]", description = "get user registered presences in events that match the given filters")
public class GetPresences extends CommandBase
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

        Request request;
        if (filters.isEmpty())
            request = new Request(RequestType.GET_PRESENCES);
        else
            request = new Request(RequestType.GET_PRESENCES, filters);

        try
        {
            HttpResponse<String> response = client.request(request);
            if (200 != response.statusCode())
            {
                client.addToPrintQueue("Error getting presences: " + response.body());
                return;
            }

            client.addToPrintQueue(response.body());
        } catch (Exceptions.InvalidToken e)
        {
            client.addToPrintQueue("Invalid token");
        }
    }
}
