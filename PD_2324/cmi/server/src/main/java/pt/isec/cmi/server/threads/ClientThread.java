package pt.isec.cmi.server.threads;

import pt.isec.cmi.server.RequestHandler;
import pt.isec.cmi.server.session.SessionData;
import pt.isec.cmi.server.Server;
import pt.isec.cmi.server.Config;
import pt.isec.cmi.shared.Exceptions;
import pt.isec.cmi.shared.requests.Request;
import pt.isec.cmi.shared.requests.RequestType;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

public class ClientThread extends Thread
{
    private final Server server;
    private final Socket client;
    private RequestType type = RequestType.STOP;

    public ClientThread(Server server, Socket client)
    {
        this.server = server;
        this.client = client;
    }

    public Request getRequest() throws Exceptions.ClientClosedConnection
    {
        try
        {
            ObjectInputStream objectInput = new ObjectInputStream(client.getInputStream());
            return (Request) objectInput.readObject();
        }
        catch (EOFException | SocketException e)
        {
            throw new Exceptions.ClientClosedConnection();
        }
        catch (IOException | ClassNotFoundException e)
        {
            if (null != e.getMessage() && "Read timed out".equals(e.getMessage()))
                return null;

            handleException("[ ClientThread ] Error getting client input stream", e);
            return null;
        }
    }

    public boolean sendRequest(Request request) throws Exceptions.ClientClosedConnection
    {
        try
        {
            ObjectOutputStream objectOutput = new ObjectOutputStream(client.getOutputStream());
            objectOutput.writeObject(request);
            objectOutput.flush();
            return true;
        }
        catch (SocketException e)
        {
            throw new Exceptions.ClientClosedConnection();
        }
        catch (IOException e)
        {
            handleException("[ ClientThread ] Error getting client output stream", e);
            return false;
        }
    }

    private void handleException(String message, Exception e)
    {
        System.out.println(message + ": " + e.getClass() + " " + e.getMessage());

        type = null;
        interrupt();
    }

    public void kick()
    {
        type = RequestType.KICK;
        interrupt();
    }

    public void free()
    {
        type = null;
        interrupt();
    }

    public InetAddress getInetAddress()
    {
        return client.getInetAddress();
    }

    @Override
    @SuppressWarnings("BusyWait")
    public void run()
    {
        while (!isInterrupted())
        {
            try
            {
                Thread.sleep(Config.getInstance().updateInterval);
            }
            catch (InterruptedException e)
            {
                interrupt();
            }

            Request request = null;
            try
            {
                request = getRequest();
            }
            catch (Exceptions.ClientClosedConnection ignored)
            {
                System.out.println("[ ClientThread ] Client with ip: " + client.getInetAddress() + " closed the connection");

                type = null;
                interrupt();
            }
            if (null == request)
                continue;

            String sessionId = request.getSessionId();
            if (RequestType.START_SESSION == request.getType())
            {
                if (null != sessionId)
                {
                    System.out.println("[ ClientThread ] Malformed request from client with ip: " + client.getInetAddress() + ", session id should be null instead of: " + sessionId);
                    continue;
                }

                SessionData sessionData = server.createSession();
                if (null == sessionData)
                {
                    System.out.println("[ ClientThread ] Error creating session for client with ip: " + client.getInetAddress());
                    continue;
                }

                sessionId = sessionData.getId();
            }
            else if (null == sessionId)
            {
                System.out.println("[ ClientThread ] Malformed request from client with ip: " + client.getInetAddress() + ", session id should not be null");
                continue;
            }

            SessionData sessionData = server.getSessionById(sessionId);
            if (null == sessionData)
            {
                System.out.println("[ ClientThread ] Malformed request from client with ip: " + client.getInetAddress() + ", session with id: " + sessionId + " does not exist");
                continue;
            }

            Request.Response response = RequestHandler.processRequest(server, this, request, sessionData);
            if (null == response)
                continue;

            request.setResponse(response);

            try
            {
                if (!sendRequest(request))
                    System.out.println("[ ClientThread ] Error sending response to client with ip: " + client.getInetAddress());
            }
            catch (Exceptions.ClientClosedConnection ignored)
            {
                System.out.println("[ ClientThread ] Client with ip: " + client.getInetAddress() + " closed the connection");

                type = null;
                interrupt();
            }
        }
    }

    @Override
    public boolean isInterrupted()
    {
        return super.isInterrupted() || client.isClosed() || !server.isRunning();
    }

    @Override
    public void interrupt()
    {
        if (isInterrupted())
            return;

        super.interrupt();

        try
        {
            sendStopRequest();

            Thread.sleep(Config.getInstance().updateInterval);
        }
        catch (InterruptedException ignored) { }
        finally
        {
            try
            {
                client.close();
            }
            catch (IOException ignored)
            {
                System.out.println("[ ClientThread ] Error closing client socket with ip: " + client.getInetAddress());
            }
        }
    }

    private void sendStopRequest()
    {
        if (null == this.type)
            return;

        try
        {
            OutputStream output = client.getOutputStream();
            ObjectOutputStream objectOutput = new ObjectOutputStream(output);
            objectOutput.writeObject(new Request(type));
            objectOutput.flush();
        }
        catch (IOException e)
        {
            System.out.println("[ ClientThread ] Error sending stop request to client with ip: " + client.getInetAddress());
        }
    }
}
