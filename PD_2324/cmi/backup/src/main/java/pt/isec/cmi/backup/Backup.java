package pt.isec.cmi.backup;

import pt.isec.cmi.shared.data.Heartbeat;
import pt.isec.cmi.shared.connections.Multicast;
import pt.isec.cmi.shared.connections.rmi.RmiClient;

import pt.isec.cmi.backup.threads.HeartbeatThread;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;

import java.util.Objects;
import java.util.Scanner;

import java.net.DatagramPacket;

import java.rmi.RemoteException;
import java.rmi.NotBoundException;

public class Backup
{
    private final Multicast multicast;
    private RmiClient rmiClient = null;
    private HeartbeatThread heartbeatThread;

    private final String databasePath;
    private volatile boolean isRunning;

    private String checkStoragePath(String storagePath)
    {
        // Check if storage directory is empty
        //
        File storageDir = new File(storagePath);
        if (!storageDir.exists() && !storageDir.mkdirs())
        {
            System.out.println("[ Backup ] Error Creating Storage Directory");
            System.exit(1);
        }

        // Check if storage directory is a directory
        //
        if (!storageDir.exists() || !storageDir.isDirectory())
        {
            System.out.println("[ Backup ] Storage Directory is not a directory");
            System.exit(1);
        }

        // Check if storage directory is empty
        //
        if (storageDir.exists() && storageDir.isDirectory() && Objects.requireNonNull(storageDir.list()).length > 0)
        {
            System.out.println("[ Backup ] Storage Directory is not empty");
            System.exit(1);
        }

        // Remove trailing slash
        //
        if (storagePath.endsWith(File.separator))
            storagePath = storagePath.substring(0, storagePath.length() - 1);

        return storagePath;
    }

    public Backup(String storagePath)
    {
        if (null == storagePath || storagePath.isEmpty())
            throw new IllegalArgumentException("Storage Path cannot be null or empty");

        // check a storage path
        //
        storagePath = checkStoragePath(storagePath);

        // get database name from user
        //
        System.out.print("Enter Database Name: ");
        Scanner scanner = new Scanner(System.in);
        String databaseName = scanner.nextLine();

        System.out.println();

        // set database path
        //
        this.databasePath = storagePath + File.separator + databaseName + ".db";

        // Add shutdown hook
        //
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        // Set up Multicast
        //
        multicast = new Multicast(Config.getInstance().multicastPort);
        multicast.join(Config.getInstance().multicastAddress, Config.getInstance().multicastPort);
    }

    public boolean isRunning()
    {
        return isRunning;
    }

    public boolean start()
    {
        if (isRunning)
            return false;

        isRunning = true;

        // Start Heartbeat Thread
        //
        heartbeatThread = new HeartbeatThread(this);
        heartbeatThread.start();

        return true;
    }

    public void stop()
    {
        // Check if backup is running
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
        // Stop Heartbeat Thread
        //
        if (null != this.heartbeatThread)
        {
            heartbeatThread.interrupt();
            try
            {
                heartbeatThread.join();
            }
            catch (InterruptedException ignored) { }

            heartbeatThread = null;
        }

        // Free RMI Client
        //
        if (null != this.rmiClient)
        {
            rmiClient.free();
            rmiClient = null;
        }

        // Leave Multicast
        //
        if (null != this.multicast)
            multicast.leave();
    }

    public DatagramPacket receiveMulticastPacket(int data_size)
    {
        try
        {
            DatagramPacket packet = new DatagramPacket(new byte[data_size], data_size);
            multicast.receive(packet);
            return packet;
        }
        catch (InterruptedIOException ignored) { }
        catch (IOException e)
        {
            System.out.println("[ Backup ] Error Receiving Data: " + e.getClass() + " " + e.getMessage());
        }

        return null;
    }

    public void processHeartbeat(Heartbeat heartbeat)
    {
        if (null == heartbeat)
            throw new NullPointerException("Heartbeat cannot be null");

        // print heartbeat
        //
        System.out.println(heartbeat);

        // Check if rmi client is null
        //
        if (null == rmiClient)
        {
            // create rmi client
            //
            try
            {
                rmiClient = new RmiClient(heartbeat.getRmiName(), heartbeat.getRmiPort());
            }
            catch (RemoteException | NotBoundException e)
            {
                System.out.println("[ Backup ] Error Connecting to RMI Server: " + e.getClass() + " " + e.getMessage());
                return;
            }

            // copy database
            //
            rmiClient.copyDatabase(databasePath);

            System.out.println("[ Backup ] Database Copied");

            return;
        }

        // Check if a database version is the same
        //
        int version = rmiClient.getDatabaseVersion();
        if (version != heartbeat.getDatabaseVersion())
        {
            System.out.println("[ Backup ] Database Version Mismatch: " + version + " != " + heartbeat.getDatabaseVersion());
            System.exit(1);
        }
    }
}
