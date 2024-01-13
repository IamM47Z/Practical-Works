package pt.isec.cmi.shared.connections.rmi;

import java.rmi.Remote;

public interface IRmiService extends Remote
{
    void unregisterClient(IRmiClient client) throws java.rmi.RemoteException;
    void registerClient(IRmiClient client) throws java.rmi.RemoteException;

    byte[] getDatabase() throws java.rmi.RemoteException;
}
