package pt.isec.cmi.backup;

public class Main
{
    private static String getUsage()
    {
        return "java pt.isec.cmi.backup.Main <storage_path>";
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

        if (1 != args.length)
        {
            System.out.println("Correct Usage: " + getUsage());
            return;
        }

        Backup backup = new Backup(args[0]);
        if (!backup.start())
            System.out.println("Failed to start backup");
    }
}
