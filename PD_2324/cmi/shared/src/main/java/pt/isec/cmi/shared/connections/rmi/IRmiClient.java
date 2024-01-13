package pt.isec.cmi.shared.connections.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface IRmiClient extends Remote
{
    boolean updateDatabase(String statement, List<Object> fieldValues) throws RemoteException;
}
