package pt.isec.cmi.shared.connections.rmi;

import pt.isec.cmi.shared.connections.database.DatabaseConnector;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class RmiService extends UnicastRemoteObject implements IRmiService
{
    private final int rmiPort;
    private final String rmiName;
    private final DatabaseConnector databaseConnector;
    private final List<IRmiClient> clients = new ArrayList<>();

    public RmiService(DatabaseConnector databaseConnector, String rmiName, int rmiPort) throws RemoteException
    {
        if (null == databaseConnector)
            throw new NullPointerException("DatabaseConnector cannot be null");

        this.rmiName = rmiName;
        this.rmiPort = rmiPort;

        this.databaseConnector = databaseConnector;

        try
        {
            Registry r = LocateRegistry.getRegistry(null, rmiPort);
            r.unbind(rmiName);
        }
        catch (Exception ignored) { }

        try
        {
            Registry r = LocateRegistry.createRegistry(rmiPort);
            r.rebind(rmiName, this);
        }
        catch (Exception e)
        {
            System.out.println("[ RmiService ] Error creating RMI registry: " + e.getClass() + ": " + e.getMessage());
            System.exit(1);
        }
    }

    public void free()
    {
        try
        {
            UnicastRemoteObject.unexportObject(this, true);
        }
        catch (Exception e)
        {
            System.out.println("[ RmiService ] Error freeing RMI service: " + e.getClass() + ": " + e.getMessage());
        }

        try
        {
            Registry r = LocateRegistry.getRegistry(null, rmiPort);
            r.unbind(rmiName);
        }
        catch (Exception e)
        {
            System.out.println("[ RmiService ] Error unbinding RMI service: " + e.getClass() + ": " + e.getMessage());
        }
    }

    public String getName()
    {
        return rmiName;
    }

    public int getPort()
    {
        return rmiPort;
    }

    @Override
    public void unregisterClient(IRmiClient client) throws RemoteException
    {
        synchronized (clients)
        {
            if (!clients.contains(client))
                return;

            System.out.println("[ RmiService ] Client unregistered");
            clients.remove(client);
        }
    }

    @Override
    public void registerClient(IRmiClient client) throws RemoteException
    {
        synchronized (clients)
        {
            if (clients.contains(client))
                return;

            System.out.println("[ RmiService ] Client registered");
            clients.add(client);
        }
    }

    @Override
    public byte[] getDatabase() throws RemoteException
    {
        return databaseConnector.getDatabase();
    }

    public void propagateDatabaseUpdate(String query, List<Object> fieldValues)
    {
        synchronized (clients)
        {
            for (IRmiClient client : clients)
            {
                try
                {
                    client.updateDatabase(query, fieldValues);
                }
                catch (RemoteException e)
                {
                    System.out.println("[ RmiService ] Error updating client database: " + e.getClass() + ": " + e.getMessage());
                }
            }
        }
    }
}
