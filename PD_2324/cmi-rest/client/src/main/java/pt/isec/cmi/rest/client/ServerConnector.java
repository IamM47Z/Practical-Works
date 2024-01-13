package pt.isec.cmi.rest.client;

import com.google.gson.Gson;
import pt.isec.cmi.rest.client.shared.Exceptions;
import pt.isec.cmi.rest.client.shared.requests.RegisterData;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

public class ServerConnector
{
    private final static String SERVER_URL = "http://localhost:8080";
    private String token;

    public ServerConnector()
    {
        token = null;
    }

    protected void logout()
    {
        token = null;
    }

    protected HttpResponse<String> register(String method, String endpoint, RegisterData data)
    {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SERVER_URL + endpoint))
                .header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(new Gson().toJson(data)))
                .build();

        try
        {
            return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        catch (InterruptedException e)
        {
            return null;
        }
    }

    protected boolean login(String method, String endpoint, String email, String password)
    {
        String token = Base64.getEncoder().encodeToString((email + ":" + password).getBytes());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SERVER_URL + endpoint))
                .header("Content-Type", "application/json")
                .header("Authorization", "Basic " + token)
                .method(method, HttpRequest.BodyPublishers.noBody())
                .build();

        try
        {
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200)
            {
                this.token = response.body();
                return true;
            }
            else
            {
                this.token = null;
                return false;
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        catch (InterruptedException e)
        {
            return false;
        }
    }

    protected HttpResponse<String> request(boolean useToken, String method, String endpoint, String data) throws Exceptions.InvalidToken
    {
        if (token == null)
            throw new Exceptions.InvalidToken();

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(SERVER_URL + endpoint))
                .header("Content-Type", "application/json")
                .method(method, data != null ? HttpRequest.BodyPublishers.ofString(data) : HttpRequest.BodyPublishers.noBody());

        if (useToken)
            builder.header("Authorization", "Bearer " + token);

        try
        {
            return HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        catch (InterruptedException e)
        {
            return null;
        }
    }
}