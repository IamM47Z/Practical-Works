package pt.isec.cmi.shared.connections.tcp;

import pt.isec.cmi.shared.connections.rmi.IRmiClient;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.util.List;

public class TcpServer
{
    private ServerSocket tcpSocket;

    public TcpServer(int tcpPort) throws IOException
    {
        tcpSocket = new ServerSocket(tcpPort);
    }

    public void setTimeout(int timeout) throws IOException
    {
        if (null == this.tcpSocket)
            throw new IllegalStateException("TCP Socket is null");

        tcpSocket.setSoTimeout(timeout);
    }

    public int getTcpPort()
    {
        if (null == this.tcpSocket)
            throw new IllegalStateException("TCP Socket is null");

        return tcpSocket.getLocalPort();
    }

    public Socket acceptClient() throws IOException
    {
        if (null == this.tcpSocket)
            throw new IllegalStateException("TCP Socket is null");

        return tcpSocket.accept();
    }

    public void close()
    {
        if (null == this.tcpSocket)
            throw new IllegalStateException("TCP Socket is null");

        try
        {
            tcpSocket.close();
        }
        catch (IOException e)
        {
            System.out.println("[ TcpServer ] Error Closing TCP Socket: " + e.getClass() + " " + e.getMessage());
        }

        tcpSocket = null;
    }
}
