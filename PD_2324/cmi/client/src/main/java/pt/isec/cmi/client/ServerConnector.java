package pt.isec.cmi.client;

import pt.isec.cmi.shared.Exceptions;
import pt.isec.cmi.shared.requests.LoginData;
import pt.isec.cmi.shared.requests.Request;
import pt.isec.cmi.shared.connections.tcp.TcpClient;
import pt.isec.cmi.shared.requests.RequestType;

import java.io.*;
import java.net.SocketException;
import java.util.Scanner;

public class ServerConnector
{
    private final TcpClient tcpClient;
    private String sessionId;

    public ServerConnector(String address, int port) throws IOException
    {
        tcpClient = new TcpClient(address, port);
    }

    // quick fix to comply with the statement
    //
    public boolean login()
    {
        // Get Credentials
        //
        Scanner scanner = new Scanner(System.in);
        System.out.print("Email: ");
        String email = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        System.out.println();

        // Set TCP timeout
        //
        try
        {
            tcpClient.setTimeout(Config.getInstance().loginTimeout * 1000);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        // Send LOGIN request
        //
        try
        {
            LoginData data = new LoginData(email, password);

            Request request = new Request(RequestType.LOGIN, sessionId);
            request.setData(data);

            if (!sendRequest(request))
            {
                System.out.println("[ ServerConnector ] Error sending LOGIN request");
                return false;
            }
        }
        catch (Exceptions.ServerClosedConnection e)
        {
            System.out.println("[ ServerConnector ] Error sending LOGIN request, server closed connection");
            return false;
        }

        // Get request response
        //
        boolean success = false;
        try
        {
            Request request = getRequest();
            if (null == request)
            {
                System.out.println("[ ServerConnector ] Error getting LOGIN response, request is null");
                return false;
            }

            Request.Response response = request.getResponse();
            if (null == response)
            {
                System.out.println("[ ServerConnector ] Error getting LOGIN response, response is null");
                return false;
            }

            success = response.isSuccess();
        }
        catch (Exceptions.ServerClosedConnection ignored)
        {
            System.out.println("[ ServerConnector ] Error getting LOGIN response, server closed connection");
            return false;
        }

        // Restore TCP timeout
        //
        try
        {
            tcpClient.setTimeout(Config.getInstance().updateInterval);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        return success;
    }

    public boolean start()
    {
        // Send START_SESSION request
        //
        try
        {
            if (!sendRequest(new Request(RequestType.START_SESSION)))
            {
                System.out.println("[ ServerConnector ] Error sending START_SESSION request");
                return false;
            }
        }
        catch (Exceptions.ServerClosedConnection e)
        {
            System.out.println("[ ServerConnector ] Error sending START_SESSION request, server closed connection");
            return false;
        }

        // Get session id
        //
        Request request = null;
        try
        {
            request = getRequest();
        }
        catch (Exceptions.ServerClosedConnection ignored)
        {
            System.out.println("[ ServerConnector ] Error getting START_SESSION response, server closed connection");
            return false;
        }
        if (null == request)
        {
            System.out.println("[ ServerConnector ] Error getting START_SESSION response, request is null");
            return false;
        }

        // Get request response
        //
        Request.Response response = request.getResponse();
        if (null == response)
        {
            System.out.println("[ ServerConnector ] Error getting START_SESSION response, response is null");
            return false;
        }

        // Get response data
        //
        Object data = response.getData();
        if (null == data)
        {
            System.out.println("[ ServerConnector ] Error getting START_SESSION response, data is null");
            return false;
        }

        sessionId = data.toString();
        if (null == this.sessionId)
        {
            System.out.println("[ ServerConnector ] Error getting START_SESSION response, session id is null");
            return false;
        }

        System.out.println("[ ServerConnector ] Session ID: " + sessionId + "\n");

        try
        {
            tcpClient.setTimeout(Config.getInstance().updateInterval);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        // return login();

        return true;
    }

    public void free()
    {
        // Stop session
        //
        try
        {
            sendRequest(new Request(RequestType.FREE_SESSION, sessionId));
        }
        catch (Exceptions.ServerClosedConnection ignored) { }

        // Close TCP client
        //
        tcpClient.close();
    }

    public Request getRequest() throws Exceptions.ServerClosedConnection
    {
        try
        {
            ObjectInputStream objectInput = new ObjectInputStream(tcpClient.getInputStream());
            return (Request) objectInput.readObject();
        }
        catch (EOFException | SocketException ignored)
        {
            throw new Exceptions.ServerClosedConnection();
        }
        catch (IOException | ClassNotFoundException e)
        {
            if (null != e.getMessage() && "Read timed out".equals(e.getMessage()))
                return null;

            System.out.println("[ ServerConnector ] Error getting client input stream: " + e.getClass() + " " + e.getMessage());
            return null;
        }
    }

    public boolean sendRequest(Request request) throws Exceptions.ServerClosedConnection
    {
        try
        {
            ObjectOutputStream objectOutput = new ObjectOutputStream(tcpClient.getOutputStream());
            objectOutput.writeObject(request);
            objectOutput.flush();
            return true;
        }
        catch (SocketException ignored)
        {
            throw new Exceptions.ServerClosedConnection();
        }
        catch (IOException e)
        {
            System.out.println("[ ServerConnector ] Error getting client output stream: " + e.getMessage());
            return false;
        }
    }

    public Request createRequest(RequestType type)
    {
        return new Request(type, sessionId);
    }
}
