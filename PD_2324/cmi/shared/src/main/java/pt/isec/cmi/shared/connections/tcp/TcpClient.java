package pt.isec.cmi.shared.connections.tcp;

import java.io.*;
import java.net.Socket;

public class TcpClient
{
    private Socket tcpSocket;

    public TcpClient(String address, int port) throws IOException
    {
        tcpSocket = new Socket(address, port);
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

    public OutputStream getOutputStream() throws IOException
    {
        if (null == this.tcpSocket)
            throw new IllegalStateException("TCP Socket is null");

        return tcpSocket.getOutputStream();
    }

    public InputStream getInputStream() throws IOException
    {
        if (null == this.tcpSocket)
            throw new IllegalStateException("TCP Socket is null");

        return tcpSocket.getInputStream();
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
            System.out.println("[ TcpClient ] Error Closing TCP Socket: " + e.getClass() + " " + e.getMessage());
        }

        tcpSocket = null;
    }
}
