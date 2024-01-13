package pt.isec.cmi.client.console.commands.console.commands.admin;

import pt.isec.cmi.client.console.commands.Client;
import pt.isec.cmi.client.console.commands.SessionManager;
import pt.isec.cmi.client.console.commands.console.Command;
import pt.isec.cmi.client.console.commands.console.CommandBase;
import pt.isec.cmi.shared.requests.GenerateCodeData;
import pt.isec.cmi.shared.requests.Request;

import static pt.isec.cmi.shared.requests.RequestType.GENERATE_CODE;

@Command(name = "generateCode", description = "generates a presence code for a given event and user", usage = "generateCode <eventId> <codeLifespan>", requestType = GENERATE_CODE)
public class GenerateCode extends CommandBase
{
    @Override
    public boolean isAvailable(SessionManager sessionManager)
    {
        return sessionManager.isLoggedIn() && sessionManager.isAdmin();
    }

    @Override
    public void handleSuccess(SessionManager sessionManager, Request.Response response)
    {
        sessionManager.addToPrintQueue("Generated code: " + response.getData());
    }

    @Override
    public void execute(SessionManager sessionManager, String[] args)
    {
        Client client = (Client) sessionManager;

        if (args.length != 3)
        {
            sessionManager.addToPrintQueue("Usage: " + getUsage());
            return;
        }

        try
        {
            int eventId = Integer.parseInt(args[1]);
            int codeLifespan = Integer.parseInt(args[2]);

            if(codeLifespan < 0)
                sessionManager.addToPrintQueue("Invalid codeLifespan");

            GenerateCodeData data = new GenerateCodeData(eventId, codeLifespan);

            if (!client.sendRequest(client.createRequest(GENERATE_CODE, data)))
                sessionManager.addToPrintQueue("Failed to generate code");
        }
        catch (NumberFormatException e)
        {
            sessionManager.addToPrintQueue("Invalid argument, expected integer");
        }
    }
}
