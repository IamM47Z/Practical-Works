package pt.isec.cmi.client.console.commands.console.commands.admin;

import pt.isec.cmi.client.console.commands.Client;
import pt.isec.cmi.client.console.commands.SessionManager;
import pt.isec.cmi.client.console.commands.console.Command;
import pt.isec.cmi.client.console.commands.console.CommandBase;
import pt.isec.cmi.shared.requests.Request;

import java.io.Serializable;
import java.util.List;

import static pt.isec.cmi.shared.requests.RequestType.GET_USER_PRESENCES;

@Command(name = "getUserPresences", usage = "getUserPresences <email>", description = "get presences for the specified user", requestType = GET_USER_PRESENCES)
public class GetUserPresences extends CommandBase
{
    @Override
    public boolean isAvailable(SessionManager sessionManager)
    {
        return sessionManager.isLoggedIn() && sessionManager.isAdmin();
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

        if (args.length != 2)
        {
            client.addToPrintQueue("Usage: " + getUsage());
            return;
        }

        if (!client.sendRequest(client.createRequest(GET_USER_PRESENCES).setData(args[1])))
            client.addToPrintQueue("Failed to send request");
    }
}
