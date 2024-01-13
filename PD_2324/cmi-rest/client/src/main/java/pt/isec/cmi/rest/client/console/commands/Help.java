package pt.isec.cmi.rest.client.console.commands;

import pt.isec.cmi.rest.client.SessionManager;
import pt.isec.cmi.rest.client.console.Command;
import pt.isec.cmi.rest.client.console.CommandBase;

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
