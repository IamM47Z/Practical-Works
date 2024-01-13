package pt.isec.cmi.shared.connections;

import java.io.IOException;
import java.net.*;

public class Multicast
{
    private MulticastSocket multicastSocket;
    private InetSocketAddress socketAddress;

    public Multicast(int port)
    {
        try
        {
            multicastSocket = new MulticastSocket(port);
        }
        catch (IOException e)
        {
            System.out.println("[ Multicast ] Error Creating Socket: " + e.getClass() + " " + e.getMessage());
            System.exit(1);
        }

        setTimeout(500);
    }

    public void join(String address, int port) throws NullPointerException
    {
        if (null == this.multicastSocket)
            throw new NullPointerException("Multicast Socket is null");

        try
        {
            socketAddress = new InetSocketAddress(address, port);
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());

            multicastSocket.joinGroup(socketAddress, networkInterface);
        } catch (IOException e)
        {
            System.out.println("[ Multicast ] Error Joining Group: " + e.getClass() + " " + e.getMessage());
        }
    }

    public void leave() throws NullPointerException
    {
        if (null == this.multicastSocket)
            throw new NullPointerException("Multicast Socket is null");

        try
        {
            multicastSocket.leaveGroup(socketAddress, NetworkInterface.getByInetAddress(InetAddress.getLocalHost()));
        }
        catch (Exception e)
        {
            System.out.println("[ Multicast ] Error Leaving Group: " + e.getClass() + " " + e.getMessage());
        }

        multicastSocket = null;
    }

    public void send(byte[] data) throws NullPointerException, IOException
    {
        if (null == this.multicastSocket)
            throw new NullPointerException("Multicast Socket is null");

        DatagramPacket packet = new DatagramPacket(data, data.length, socketAddress);
        multicastSocket.send(packet);
    }

    public void receive(DatagramPacket packet) throws NullPointerException, IOException
    {
        if (null == this.multicastSocket)
            throw new NullPointerException("Multicast Socket is null");

        multicastSocket.receive(packet);
    }

    public void setTimeout(int timeout)
    {
        if (null == this.multicastSocket)
            throw new NullPointerException("Multicast Socket is null");

        try
        {
            multicastSocket.setSoTimeout(timeout);
        }
        catch (SocketException e)
        {
            System.out.println("[ Multicast ] Error Setting Timeout: " + e.getClass() + " " + e.getMessage());
        }
    }
}
