package pt.isec.cmi.server.threads;

import pt.isec.cmi.server.Server;
import pt.isec.cmi.shared.Exceptions;
import pt.isec.cmi.shared.requests.Request;
import pt.isec.cmi.shared.requests.RequestType;

import java.net.Socket;

import java.util.List;
import java.util.ArrayList;

public class ListenerThread extends Thread
{
    private final Server server;
    private final List<ClientThread> clientThreads = new ArrayList<>();

    public ListenerThread(Server server)
    {
        this.server = server;
    }

    @Override
    public void run()
    {
        while (!isInterrupted())
        {
            Socket socket = server.acceptClient();
            if (null == socket)
                continue;

            ClientThread thread = new ClientThread(server, socket);
            thread.start();

            clientThreads.add(thread);
        }
    }

    @Override
    public boolean isInterrupted()
    {
        return super.isInterrupted() || !server.isRunning();
    }

    @Override
    public void interrupt()
    {
        if (isInterrupted())
            return;

        super.interrupt();

        for (ClientThread client : clientThreads)
            if (!client.isInterrupted())
                client.interrupt();
    }

    public void propagateDatabaseUpdate()
    {
        for (ClientThread clientThread : clientThreads)
        {
            try
            {
                clientThread.sendRequest(new Request(RequestType.DATABASE_UPDATE));
            }
            catch (Exceptions.ClientClosedConnection e)
            {
                System.out.println("[ ListenerThread ] Error sending DATABASE_UPDATE request to client with ip: " + clientThread.getInetAddress() + ", client closed connection");
            }
        }
    }
}
