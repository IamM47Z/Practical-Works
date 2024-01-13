package pt.isec.cmi.client.console.commands;

import pt.isec.cmi.client.Client;
import pt.isec.cmi.shared.requests.filters.FilterFactory;
import pt.isec.cmi.client.SessionManager;
import pt.isec.cmi.client.console.Command;
import pt.isec.cmi.client.console.CommandBase;

import pt.isec.cmi.shared.requests.Request;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static pt.isec.cmi.shared.requests.RequestType.GET_PRESENCES;

@Command(name = "getPresences", usage = "getPresences [--name [eventName]] [--start-date [(dd/MM/yyyy)]] [--end-date [(dd/MM/yyyy)]] [--start-time [(HH:mm)]] [--end-time [(HH:mm)]]", description = "get user registered presences in events that match the given filters", requestType = GET_PRESENCES)
public class GetPresences extends CommandBase
{
    @Override
    public boolean isAvailable(SessionManager sessionManager)
    {
        return sessionManager.isLoggedIn();
    }

    @Override
    public void handleSuccess(SessionManager sessionManager, Request.Response response)
    {
        List<Serializable> presences = response.getDataList();
        if (presences.isEmpty())
        {
            sessionManager.addToPrintQueue("No presences found");
            return;
        }

        sessionManager.addToPrintQueue("Found " + presences.size() + " presences:");

        for (Serializable presenceObj : presences)
            sessionManager.addToPrintQueue(String.valueOf(presenceObj));
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

        if (!client.sendRequest(client.createRequest(GET_PRESENCES).setDataList(filters)))
            client.addToPrintQueue("Failed to send request");
    }
}
