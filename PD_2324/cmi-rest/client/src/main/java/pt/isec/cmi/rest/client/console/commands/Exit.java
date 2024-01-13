package pt.isec.cmi.rest.client.console.commands;

import pt.isec.cmi.rest.client.SessionManager;
import pt.isec.cmi.rest.client.console.Command;
import pt.isec.cmi.rest.client.console.CommandBase;

@Command(name = "exit", description = "terminates the application", usage = "exit")
public class Exit extends CommandBase
{
    @Override
    public boolean isAvailable(SessionManager sessionManager)
    {
        return true;
    }

    @Override
    public void execute(SessionManager sessionManager, String[] args)
    {
        sessionManager.addToPrintQueue("Exiting...");

        System.exit(0);
    }
}
