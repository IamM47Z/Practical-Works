package pt.isec.cmi.client.console.commands.console.commands;

import pt.isec.cmi.client.console.commands.Client;
import pt.isec.cmi.client.console.commands.SessionManager;
import pt.isec.cmi.client.console.commands.console.Command;
import pt.isec.cmi.client.console.commands.console.CommandBase;
import pt.isec.cmi.shared.data.UserData;
import pt.isec.cmi.shared.requests.LoginData;
import pt.isec.cmi.shared.requests.Request;

import static pt.isec.cmi.shared.requests.RequestType.LOGIN;

@Command(name = "login", description = "login to the server", usage = "login <email> <password>", requestType = LOGIN)
public class Login extends CommandBase
{
    @Override
    public boolean isAvailable(SessionManager sessionManager)
    {
        return !sessionManager.isLoggedIn();
    }

    @Override
    public void handleSuccess(SessionManager sessionManager, Request.Response response)
    {
        Client client = (Client) sessionManager;

        UserData data = (UserData) response.getData();
        if (null == data)
        {
            client.addToPrintQueue("Malformed response, data is null");
            return;
        }

        client.onLogin(data);
        client.addToPrintQueue("Logged in successfully, welcome " + data.username() + "!");
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

        LoginData data = new LoginData(email, password);

        if (!client.sendRequest(client.createRequest(LOGIN, data)))
            client.addToPrintQueue("Error sending LOGIN request");
    }
}
