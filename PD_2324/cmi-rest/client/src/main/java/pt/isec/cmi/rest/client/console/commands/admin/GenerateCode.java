package pt.isec.cmi.rest.client.console.commands.admin;

import pt.isec.cmi.rest.client.Client;
import pt.isec.cmi.rest.client.SessionManager;
import pt.isec.cmi.rest.client.console.Command;
import pt.isec.cmi.rest.client.console.CommandBase;
import pt.isec.cmi.rest.client.requests.Request;
import pt.isec.cmi.rest.client.requests.RequestType;
import pt.isec.cmi.rest.client.shared.Exceptions;

import java.net.http.HttpResponse;

@Command(name = "generateCode", description = "generates a presence code for a given event and user", usage = "generateCode <eventId> <codeLifespan>")
public class GenerateCode extends CommandBase
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

            HttpResponse<String> response = client.request(new Request(RequestType.GENERATE_CODE), eventId, codeLifespan);
            if (200 != response.statusCode())
            {
                sessionManager.addToPrintQueue("Error generating code: " + response.body());
                return;
            }

            sessionManager.addToPrintQueue("Code generated successfully: " + response.body());
        }
        catch (NumberFormatException e)
        {
            sessionManager.addToPrintQueue("Invalid argument, expected integer");
        }
        catch (Exceptions.InvalidToken e)
        {
            sessionManager.addToPrintQueue("Invalid token");
        }
    }
}
