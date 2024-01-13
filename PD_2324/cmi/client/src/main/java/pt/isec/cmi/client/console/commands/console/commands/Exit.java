package pt.isec.cmi.client.console.commands.console.commands;

import pt.isec.cmi.client.console.commands.SessionManager;
import pt.isec.cmi.client.console.commands.console.Command;
import pt.isec.cmi.client.console.commands.console.CommandBase;
import pt.isec.cmi.shared.requests.Request;

import static pt.isec.cmi.shared.requests.RequestType.NONE;

@Command(name = "exit", description = "terminates the application", usage = "exit", requestType = NONE)
public class Exit extends CommandBase
{
    @Override
    public boolean isAvailable(SessionManager sessionManager)
    {
        return true;
    }

    @Override
    public void handleSuccess(SessionManager sessionManager, Request.Response response)
    {
        sessionManager.addToPrintQueue("Malformed request, exit command should not return a response");
    }

    @Override
    public void execute(SessionManager sessionManager, String[] args)
    {
        sessionManager.addToPrintQueue("Exiting...");

        System.exit(0);
    }
}
