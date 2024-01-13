package pt.isec.cmi.client.console.commands;

import pt.isec.cmi.client.SessionManager;
import pt.isec.cmi.client.console.Command;
import pt.isec.cmi.client.console.CommandBase;
import pt.isec.cmi.shared.requests.Request;

import java.util.List;
import java.util.Set;

@Command(name = "help", description = "displays the list of available commands or provides usage information for a specified command", usage = "help <command>")
public class Help extends CommandBase
{
    @Override
    public boolean isAvailable(SessionManager sessionManager)
    {
        return true;
    }

    @Override
    public void handleSuccess(SessionManager sessionManager, Request.Response response)
    {
        sessionManager.addToPrintQueue("Malformed request, help command should not return a response");
    }

    @Override
    public void execute(SessionManager sessionManager, String[] args)
    {
        if (2 != args.length && 1 != args.length)
        {
            sessionManager.addToPrintQueue("Usage: " + this.getClass().getAnnotation(Command.class).usage());
            return;
        }

        if (2 == args.length)
        {
            Set<CommandBase> commandList = sessionManager.getCommandList();
            for (CommandBase command: commandList)
            {
                if (!command.getAnnotation().name().equals(args[1]))
                    continue;

                sessionManager.addToPrintQueue(command.getUsage());
                return;
            }

            sessionManager.addToPrintQueue("Command not found");
            return;
        }

        sessionManager.printHelp();
    }
}
