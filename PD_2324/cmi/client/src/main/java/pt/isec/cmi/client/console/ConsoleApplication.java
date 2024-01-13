package pt.isec.cmi.client.console;

import pt.isec.cmi.client.SessionManager;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public abstract class ConsoleApplication
{
    public static class ConsoleThread extends Thread
    {
        private final ConsoleApplication consoleApplication;
        private final ExecutorService executorService = Executors.newSingleThreadExecutor();

        public ConsoleThread(ConsoleApplication consoleApplication)
        {
            this.consoleApplication = consoleApplication;
        }

        @Override
        public void run()
        {
            Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);

            while (!isInterrupted())
            {
                if (consoleApplication.hasPrintQueue())
                {
                    consoleApplication.printQueue();
                    System.out.println();
                }

                System.out.print("Command: ");

                try
                {
                    Future<String> commandFuture = executorService.submit(scanner::nextLine);
                    while (!commandFuture.isDone())
                    {
                        if (consoleApplication.hasPrintQueue())
                        {
                            System.out.print("\r");
                            consoleApplication.printQueue();
                            System.out.print("\nCommand: ");
                        }

                        try
                        {
                            String command = commandFuture.get(100, TimeUnit.MILLISECONDS);
                            processCommand(command);
                        } catch (TimeoutException ignored) { }
                    }
                }
                catch (ExecutionException e)
                {
                    e.printStackTrace();
                }
                catch (InterruptedException e)
                {
                    break;
                }
            }

            executorService.shutdown();
        }

        private void processCommand(String command)
        {
            String[] args = command.split(" ");
            if (0 == args.length)
                return;

            boolean found = false;
            Set<CommandBase> commandList = consoleApplication.getCommandList();
            for (CommandBase cmd : commandList)
            {
                Command commandInfo = cmd.getAnnotation();
                if (null == commandInfo)
                    continue;

                if (!Objects.equals(commandInfo.name(), args[0]))
                    continue;

                found = true;

                if (!cmd.isAvailable((SessionManager) consoleApplication))
                    continue;

                System.out.println();

                try
                {
                    executorService.submit(() -> cmd.execute((SessionManager) consoleApplication, args)).get();
                } catch (ExecutionException e)
                {
                    e.printStackTrace();
                } catch (InterruptedException e)
                {
                    interrupt();
                }

                return;
            }

            if (!found)
                System.out.println("\nCommand not found\n");
            else
                System.out.println("\nCommand not available\n");
        }

        @Override
        public void interrupt()
        {
            if (isInterrupted())
                return;

            super.interrupt();

            System.out.print("\r");
            consoleApplication.printQueue();

            executorService.shutdown();
        }
    }

    private final Set<CommandBase> commandList = new TreeSet<>();

    public void registerCommand(CommandBase command)
    {
        if (null == command || null == command.getAnnotation() || null == command.getAnnotation().name()
                || command.getAnnotation().name().isEmpty() || null == command.getAnnotation().description() || command.getAnnotation().description().isEmpty())
            throw new IllegalArgumentException("Invalid command");

        commandList.add(command);
    }

    public void unregisterCommand(CommandBase command)
    {
        if (null == command)
        {
            System.out.println("Invalid command");
            return;
        }

        commandList.remove(command);
    }

    public Set<CommandBase> getCommandList()
    {
        return new TreeSet<>(commandList);
    }

    private ConsoleThread consoleThread;

    protected void startConsoleThread()
    {
        if (null != this.consoleThread)
            return;

        consoleThread = new ConsoleThread(this);
        consoleThread.start();
    }

    protected void stopConsoleThread()
    {
        if (null == this.consoleThread)
            return;

        consoleThread.interrupt();
        try
        {
            consoleThread.join();
        }
        catch (InterruptedException ignored)
        {
        }

        consoleThread = null;
    }

    public void printHelp()
    {
        System.out.println("Available commands:");

        for (CommandBase command : commandList)
        {
            if (!command.isAvailable((SessionManager) this))
                continue;

            System.out.printf("\t%s\n", command);
        }

        System.out.println();
    }

    private final List<String> printQueue = new ArrayList<>();

    public void addToPrintQueue(String s)
    {
        synchronized (printQueue)
        {
            printQueue.add(s);
        }
    }

    public boolean hasPrintQueue()
    {
        synchronized (printQueue)
        {
            return !printQueue.isEmpty();
        }
    }

    public void printQueue()
    {
        synchronized (printQueue)
        {
            for (String s : printQueue)
                System.out.println(s);

            printQueue.clear();
        }
    }
}
