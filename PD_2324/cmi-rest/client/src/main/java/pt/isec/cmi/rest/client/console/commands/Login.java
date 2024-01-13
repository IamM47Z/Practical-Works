package pt.isec.cmi.rest.client.console.commands;

import com.google.gson.Gson;
import pt.isec.cmi.rest.client.Client;
import pt.isec.cmi.rest.client.SessionManager;
import pt.isec.cmi.rest.client.console.Command;
import pt.isec.cmi.rest.client.console.CommandBase;
import pt.isec.cmi.rest.client.requests.Request;
import pt.isec.cmi.rest.client.requests.RequestType;
import pt.isec.cmi.rest.client.shared.Exceptions;
import pt.isec.cmi.rest.client.shared.data.UserData;

import java.net.http.HttpResponse;

@Command(name = "login", description = "login to the server", usage = "login <email> <password>")
public class Login extends CommandBase
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

        if (3 != args.length)
        {
            client.addToPrintQueue("Usage: " + this.getClass().getAnnotation(Command.class).usage());
            return;
        }

        String email = args[1];
        String password = args[2];

        try
        {
            if (client.login(email, password))
            {
                HttpResponse<String> response = client.request(new Request(RequestType.USER_DETAILS));
                if (200 != response.statusCode())
                {
                    client.addToPrintQueue("Error getting user details");
                    return;
                }

                client.onLogin(new Gson().fromJson(response.body(), UserData.class));
                client.addToPrintQueue("Logged in successfully, welcome " + email + "!");
            }
            else
            {
                client.addToPrintQueue("Error logging in");
            }
        }
        catch (Exceptions.InvalidToken e)
        {
            client.addToPrintQueue("Invalid token");
        }
    }
}
