package pt.isec.cmi.client.console.commands;

import pt.isec.cmi.client.SessionManager;
import pt.isec.cmi.client.Client;
import pt.isec.cmi.client.console.Command;
import pt.isec.cmi.client.console.CommandBase;
import pt.isec.cmi.shared.requests.RegisterData;
import pt.isec.cmi.shared.requests.Request;

import static pt.isec.cmi.shared.requests.RequestType.REGISTER;

@Command(name = "register", description = "register to the server", usage = "register <email> <password> <name> <nif>", requestType = REGISTER)
public class Register extends CommandBase
{
    @Override
    public boolean isAvailable(SessionManager sessionManager)
    {
        return !sessionManager.isLoggedIn();
    }

    @Override
    public void handleSuccess(SessionManager sessionManager, Request.Response response)
    {
        sessionManager.addToPrintQueue("Registered successfully");
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

        if (!client.sendRequest(client.createRequest(REGISTER, data)))
            client.addToPrintQueue("Error sending request");
    }
}
