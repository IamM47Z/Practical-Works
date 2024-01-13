package pt.isec.cmi.server.threads;

import pt.isec.cmi.server.Config;
import pt.isec.cmi.server.Server;
import pt.isec.cmi.shared.data.Heartbeat;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ByteArrayOutputStream;

public class HeartbeatThread extends Thread
{
    private final Server server;

    public HeartbeatThread(Server server)
    {
        this.server = server;
    }

    @Override
    @SuppressWarnings("BusyWait")
    public void run()
    {
        while (!isInterrupted())
        {
            try
            {
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);

                Heartbeat heartbeat = server.generateHeartbeat();
                objectStream.writeObject(heartbeat);
                objectStream.flush();

                server.sendMulticastPacket(byteStream.toByteArray());

                objectStream.close();
                byteStream.close();
            }
            catch (IllegalStateException e)
            {
                if ("Server is not running".equals(e.getMessage()))
                    break;

                System.out.println("[ HeartbeatThread ] Error Sending Heartbeat: " + e.getClass() + " " + e.getMessage());
                break;
            }
            catch (IOException e)
            {
                System.out.println("[ HeartbeatThread ] Error Sending Heartbeat: " + e.getClass() + " " + e.getMessage());
                break;
            }

            try
            {
                Thread.sleep(Config.getInstance().heartbeatInterval);
            }
            catch (InterruptedException e)
            {
                break;
            }
        }
    }

    @Override
    public boolean isInterrupted()
    {
        return super.isInterrupted() || !server.isRunning();
    }
}
