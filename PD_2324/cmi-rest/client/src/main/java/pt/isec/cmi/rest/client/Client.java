package pt.isec.cmi.rest.client;

import com.google.gson.Gson;
import pt.isec.cmi.rest.client.requests.Request;
import pt.isec.cmi.rest.client.requests.RequestType;
import pt.isec.cmi.rest.client.shared.Exceptions;
import pt.isec.cmi.rest.client.shared.requests.RegisterData;

import java.net.http.HttpResponse;

public class Client extends SessionManager
{
    private final ServerConnector serverConnector;

    private volatile boolean isRunning;

    public Client()
    {
        // Add shutdown hook
        //
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        // Set up server
        //
        serverConnector = new ServerConnector();
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
        // Stop console thread
        //
        stopConsoleThread();
    }

    public boolean isRunning()
    {
        return isRunning;
    }

    public HttpResponse<String> request(Request request, Object ... args) throws Exceptions.InvalidToken
    {
        if (null == request)
            throw new IllegalArgumentException("Invalid request");

        String body = null;
        if (null != request.getData())
        {
            Gson gson = new Gson();
            body = gson.toJson(request.getData());
        }

        RequestType type = request.getType();
        return serverConnector.request(type.requiresAuth(), type.getMethod(), type.getEndpoint(args), body);
    }

    public HttpResponse<String> register(RegisterData data)
    {
        if (isLoggedIn())
            throw new IllegalStateException("Already logged in");

        RequestType type = RequestType.REGISTER;
        return serverConnector.register(type.getMethod(), type.getEndpoint(), data);
    }

    public boolean login(String email, String password)
    {
        if (isLoggedIn())
            return false;

        RequestType type = RequestType.LOGIN;
        return serverConnector.login(type.getMethod(), type.getEndpoint(), email, password);
    }

    public void logout()
    {
        if (!isLoggedIn())
            return;

        serverConnector.logout();
        onLogout();
    }
}
