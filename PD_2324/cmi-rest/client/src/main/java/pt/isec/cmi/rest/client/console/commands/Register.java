package pt.isec.cmi.rest.client.console.commands;

import pt.isec.cmi.rest.client.SessionManager;
import pt.isec.cmi.rest.client.Client;
import pt.isec.cmi.rest.client.console.Command;
import pt.isec.cmi.rest.client.console.CommandBase;
import pt.isec.cmi.rest.client.requests.Request;
import pt.isec.cmi.rest.client.requests.RequestType;
import pt.isec.cmi.rest.client.shared.Exceptions;
import pt.isec.cmi.rest.client.shared.requests.RegisterData;

import java.net.http.HttpResponse;

@Command(name = "register", description = "register to the server", usage = "register <email> <password> <name> <nif>")
public class Register extends CommandBase
{
    @Override
    public boolean isAvailable(SessionManager sessionManager)
    {
        return !sessionManager.isLoggedIn();
    }

    @Override
    public void execute(SessionManager sessionManager, String[] args)
    {
        Client client = (Client) sessionManager;

        if (5 != args.length)
        {
            client.addToPrintQueue("Usage: " + this.getClass().getAnnotation(Command.class).usage());
            return;
        }

        String username = args[1];
        String password = args[2];
        String name = args[3];

        int nif;
        try
        {
            nif = Integer.parseInt(args[4]);

            if (9 != Integer.toString(nif).length())
            {
                client.addToPrintQueue("Invalid NIF, must be 9 digits");
                return;
            }
        }
        catch (NumberFormatException e)
        {
            client.addToPrintQueue("Invalid NIF, must be 9 digits");
            return;
        }

        RegisterData data = new RegisterData(username, password, name, nif);

        HttpResponse<String> response = client.register(data);
        if (201 != response.statusCode())
        {
            client.addToPrintQueue("Error registering: " + response.body());
            return;
        }

        client.addToPrintQueue(response.body());
    }
}
