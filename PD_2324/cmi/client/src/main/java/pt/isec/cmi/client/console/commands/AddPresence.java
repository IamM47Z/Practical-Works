package pt.isec.cmi.client.console.commands;

import pt.isec.cmi.client.Client;
import pt.isec.cmi.client.SessionManager;
import pt.isec.cmi.client.console.Command;
import pt.isec.cmi.client.console.CommandBase;

import pt.isec.cmi.shared.requests.Request;

import static pt.isec.cmi.shared.requests.RequestType.ADD_PRESENCE;

@Command(name = "addPresence", description = "adds presence to an event given the event code", usage = "addPresence <eventCode>", requestType = ADD_PRESENCE)
public class AddPresence extends CommandBase
{
    @Override
    public boolean isAvailable(SessionManager sessionManager)
    {
        return sessionManager.isLoggedIn();
    }

    @Override
    public void handleSuccess(SessionManager sessionManager, Request.Response response)
    {
        sessionManager.addToPrintQueue("Presence added successfully");
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

        if (!client.sendRequest(client.createRequest(ADD_PRESENCE, args[1])))
            client.addToPrintQueue("Error adding presence request");
    }
}
