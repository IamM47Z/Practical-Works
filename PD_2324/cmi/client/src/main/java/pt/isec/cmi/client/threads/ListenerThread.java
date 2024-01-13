package pt.isec.cmi.client.threads;

import pt.isec.cmi.client.Client;
import pt.isec.cmi.shared.requests.Request;
import pt.isec.cmi.shared.requests.RequestType;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import static pt.isec.cmi.shared.requests.RequestType.NONE;

public class ListenerThread extends Thread
{
    private final Client client;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public ListenerThread(Client client)
    {
        this.client = client;
    }

    // Handles requests that have a response
    //
    private void processResponse(RequestType type, Request.Response response)
    {
        if (NONE == type)
            return;

        client.getCommandList().forEach(command ->
        {
            if (command.getAnnotation().requestType().equals(type))
                command.handleSuccess(client, response);
        });
    }

    // Handles requests that don't have a response
    //
    private void processRequest(RequestType type, Request request)
    {
        if (NONE == type)
            return;

        switch (type)
        {
            case KICK ->
            {
                client.addToPrintQueue("You have been kicked from the server");
                exit();
            }
            case STOP ->
            {
                client.addToPrintQueue("Server has stopped");
                exit();
            }
        }
    }

    @Override
    public void run()
    {
        while (!isInterrupted())
        {
            Request request = client.getRequest();
            if (null == request)
                continue;

            Request.Response response = request.getResponse();
            if (null == response)
            {
                processRequest(request.getType(), request);
                continue;
            }

            boolean success = response.isSuccess();
            if (!success)
            {
                client.addToPrintQueue(response.getErrorMessage());
                continue;
            }

            processResponse(request.getType(), response);
        }

        executorService.shutdown();
    }

    private void exit()
    {
        try
        {
            executorService.submit(() -> System.exit(0)).get();
        }
        catch (InterruptedException | ExecutionException ignored)
        {
            interrupt();
        }
    }

    @Override
    public void interrupt()
    {
        if (isInterrupted())
            return;

        super.interrupt();

        executorService.shutdown();
    }
}
