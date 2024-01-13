package pt.isec.cmi.backup.threads;

import pt.isec.cmi.backup.Backup;
import pt.isec.cmi.backup.Config;

import pt.isec.cmi.shared.data.Heartbeat;

import java.net.DatagramPacket;

import java.time.ZonedDateTime;

import java.io.ObjectInputStream;
import java.io.ByteArrayInputStream;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;

public class HeartbeatThread extends Thread
{
    private final Backup backup;
    private ZonedDateTime lastTimestamp = ZonedDateTime.now();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public HeartbeatThread(Backup backup)
    {
        this.backup = backup;
    }

    @Override
    public void run()
    {
        final int maxSize = Heartbeat.getMaxSerializedSize();

        // Receive heartbeat
        //
        while (!isInterrupted())
        {
            if (ZonedDateTime.now().isAfter(lastTimestamp.plusSeconds(Config.getInstance().maxTimeWithoutHeartbeat)))
            {
                System.out.println("[ HeartbeatThread ] No Heartbeat Received in " + Config.getInstance().maxTimeWithoutHeartbeat + " seconds.");
                exit();
            }

            DatagramPacket packet = backup.receiveMulticastPacket(maxSize);
            if (null == packet)
                continue;

            Heartbeat heartbeat;
            try
            {
                ByteArrayInputStream byteStream = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                ObjectInputStream objectStream = new ObjectInputStream(byteStream);

                heartbeat = (Heartbeat) objectStream.readObject();

                objectStream.close();
                byteStream.close();
            }
            catch (Exception e)
            {
                System.out.println("[ HeartbeatThread ] Error Deserializing Heartbeat: " + e.getClass() + " " + e.getMessage());
                continue;
            }

            if (heartbeat.getTimestamp().isAfter(lastTimestamp))
                lastTimestamp = heartbeat.getTimestamp();

            executorService.submit(() -> backup.processHeartbeat(heartbeat));
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
