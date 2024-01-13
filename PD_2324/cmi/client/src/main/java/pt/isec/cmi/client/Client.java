package pt.isec.cmi.client;

import pt.isec.cmi.shared.Exceptions;
import pt.isec.cmi.shared.requests.Request;

import pt.isec.cmi.client.threads.ListenerThread;
import pt.isec.cmi.shared.requests.RequestType;

import java.io.IOException;
import java.io.Serializable;

public class Client extends SessionManager
{
    private final ServerConnector serverConnector;
    private ListenerThread listenerThread;

    private volatile boolean isRunning;

    public Client(String address, int port) throws IOException
    {
        // Add shutdown hook
        //
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        // Set up server
        //
        serverConnector = new ServerConnector(address, port);
    }

    public boolean start()
    {
        // Check if client is running
        //
        if (isRunning)
            return false;

        // Set running to true
        //
        isRunning = true;

        // Start server connection
        //
        if (!serverConnector.start())
        {
            isRunning = false;
            return false;
        }

        // Start listener thread
        //
        listenerThread = new ListenerThread(this);
        listenerThread.start();

        // Start console thread
        //
        startConsoleThread();

        return true;
    }

    public void stop()
    {
        // Check if client is running
        //
        if (!isRunning)
            return;

        // Set running to false
        //
        isRunning = false;

        // Remove shutdown hook
        //
        try
        {
            Runtime.getRuntime().removeShutdownHook(new Thread(this::stop));
        }
        catch (IllegalStateException ignored) { }

        // Free resources
        //
        free();
    }

    private void free()
    {
        // Stop listener thread
        //
        if (null != this.listenerThread)
        {
            listenerThread.interrupt();
            try
            {
                listenerThread.join();
            }
            catch (InterruptedException ignored)
            {
            }

            listenerThread = null;
        }

        // Stop console thread
        //
        stopConsoleThread();

        // Free server connection
        //
        serverConnector.free();
    }

    public boolean isRunning()
    {
        return isRunning;
    }

    public boolean sendRequest(Request request)
    {
        try
        {
            return serverConnector.sendRequest(request);
        }
        catch (Exceptions.ServerClosedConnection ignored)
        {
            addToPrintQueue("Connection closed by server");
            System.exit(0);
        }

        return false;
    }

    public Request getRequest()
    {
        try
        {
            return serverConnector.getRequest();
        }
        catch (Exceptions.ServerClosedConnection ignored)
        {
            addToPrintQueue("Connection closed by server");
            System.exit(0);
        }

        return null;
    }

    public Request createRequest(RequestType type)
    {
        return serverConnector.createRequest(type);
    }

    public Request createRequest(RequestType type, Serializable data)
    {
        return serverConnector.createRequest(type).setData(data);
    }
}
