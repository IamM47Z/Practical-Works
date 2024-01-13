package pt.isec.cmi.client;

import org.reflections.Reflections;
import pt.isec.cmi.client.console.Command;
import pt.isec.cmi.client.console.CommandBase;
import pt.isec.cmi.shared.data.UserData;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;

public class Main
{
    private static String getUsage()
    {
        return "java pt.isec.cmi.client.Main <server_ip> <server_port>";
    }

    private static void printLogo()
    {
        System.out.println("  _____                  _   __  __      _____       ");
        System.out.println(" / ____|                | | |  \\/  |    |_   _|      ");
        System.out.println("| |     ___  _   _ _ __ | |_| \\  / | ___  | |  _ __  ");
        System.out.println("| |    / _ \\| | | | '_ \\| __| |\\/| |/ _ \\ | | | '_ \\ ");
        System.out.println("| |___| (_) | |_| | | | | |_| |  | |  __/_| |_| | | |");
        System.out.println(" \\_____\\___/ \\__,_|_| |_|\\__|_|  |_|\\___|_____|_| |_|\n");
    }

    private static Set<Class<?>> getCommandClasses()
    {
        Reflections reflections = new Reflections("pt.isec.cmi.client.console.commands");
        return reflections.getTypesAnnotatedWith(Command.class);
    }

    private static void registerCommands(Client client)
    {
        Set<Class<?>> commands = getCommandClasses();
        for (Class<?> commandClass : commands)
        {
            try
            {
                CommandBase command = (CommandBase) commandClass.getConstructor().newInstance();
                client.registerCommand(command);
            }
            catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e)
            {
                System.out.println("Error registering command " + commandClass.getSimpleName() + ": " + e.getMessage());
                System.exit(1);
            }
        }
    }

    public static void main(String[] args)
    {
        printLogo();

        if (2 != args.length)
        {
            System.out.println("Correct Usage: " + getUsage());
            return;
        }

        int serverPort;
        try
        {
            serverPort = Integer.parseInt(args[1]);
        }
        catch (NumberFormatException e)
        {
            System.out.println("Correct Usage: " + getUsage());
            return;
        }

        Client client;
        try
        {
            client = new Client(args[0], serverPort);
        }
        catch (IOException ignored)
        {
            System.out.println("Error creating client, check if server is running");
            return;
        }

        // Start client
        //
        if (!client.start())
        {
            System.out.println("Error starting client, check if server is running");
            return;
        }

        // Register commands
        //
        registerCommands(client);
    }
}