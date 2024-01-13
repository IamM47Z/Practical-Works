package pt.isec.cmi.shared.connections.rmi;

import pt.isec.cmi.shared.connections.database.DatabaseConnector;

import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.util.List;

public class RmiClient extends UnicastRemoteObject implements IRmiClient
{
    private IRmiService server;
    private DatabaseConnector databaseConnector = null;

    public RmiClient(String rmiName, int rmiPort) throws RemoteException, NotBoundException
    {
        Registry r = LocateRegistry.getRegistry(null, rmiPort);
        this.server = (IRmiService) r.lookup(rmiName);
    }

    public void copyDatabase(String databasePath)
    {
        try
        {
            // get database from server
            //
            byte[] dbData = server.getDatabase();

            // save database to disk
            //
            DatabaseConnector.saveDatabase(databasePath, dbData);

            // create database connector
            //
            databaseConnector = new DatabaseConnector(databasePath);
            if (!databaseConnector.start())
            {
                System.out.println("[ RmiClient ] Error Starting Database");
                System.exit(1);
            }

            // register rmi server client
            //
            server.registerClient(this);
        }
        catch (RemoteException e)
        {
            System.out.println("[ RmiClient ] Error Getting Database: " + e.getClass() + " " + e.getMessage());
            System.exit(1);
        }
    }

    public int getDatabaseVersion()
    {
        if (null == databaseConnector)
        {
            System.out.println("[ RmiClient ] Database not initialized");
            return -1;
        }

        try
        {
            return databaseConnector.getVersion();
        }
        catch (SQLException e)
        {
            System.out.println("[ RmiClient ] Error Getting Database Version: " + e.getClass() + " " + e.getMessage());
            return -1;
        }
    }

    public void free()
    {
        if (null != server)
        {
            try
            {
                server.unregisterClient(this);
            }
            catch (ConnectException ignored)
            {
                System.out.println("[ RmiClient ] Server already closed connection");
            }
            catch (RemoteException e)
            {
                System.out.println("[ RmiClient ] Error Unregistering Client: " + e.getClass() + " " + e.getMessage());
            }

            server = null;
        }

        if (null != databaseConnector)
        {
            databaseConnector.close();
            databaseConnector = null;
        }
    }

    @Override
    public boolean updateDatabase(String query, List<Object> fieldValues) throws RemoteException
    {
        if (null == databaseConnector)
        {
            System.out.println("[ RmiClient ] Database not initialized");
            return false;
        }

        try
        {
            databaseConnector.executeStatementClone(query, fieldValues);
        }
        catch (SQLException e)
        {
            System.out.println("[ RmiClient ] Error Updating Database: " + e.getClass() + " " + e.getMessage());
        }

        System.out.println("[ RmiClient ] Updated Database: " + query + " " + fieldValues);
        return true;
    }
}
