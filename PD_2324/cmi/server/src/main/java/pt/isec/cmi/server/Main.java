package pt.isec.cmi.server;

import java.io.IOException;

public class Main
{
    private static String getUsage()
    {
        return "java pt.isec.cmi.server.Main <tcp_port> <sql_database_path> <rmi_name> <rmi_port>";
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

    public static void main(String[] args)
    {
        printLogo();

        if (4 != args.length)
        {
            System.out.println("Correct Usage: " + getUsage());
            return;
        }

        int tcpPort;
        int rmiPort;
        try
        {
            tcpPort = Integer.parseInt(args[0]);
            rmiPort = Integer.parseInt(args[3]);
        }
        catch (NumberFormatException e)
        {
            System.out.println("Correct Usage: " + getUsage());
            return;
        }

        Server server;
        try
        {
            server = new Server(tcpPort, args[1], args[2], rmiPort);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        if (!server.start())
        {
            System.out.println("Error starting server");
            return;
        }

        System.out.print("Waiting for clients...\r");
    }
}