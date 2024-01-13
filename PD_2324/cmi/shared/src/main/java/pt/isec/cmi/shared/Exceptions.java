package pt.isec.cmi.shared;

public class Exceptions
{
    public static class ClientClosedConnection extends Exception
    {
        public ClientClosedConnection()
        {
            super("Client closed connection");
        }
    }

    public static class ServerClosedConnection extends Exception
    {
        public ServerClosedConnection()
        {
            super("Server closed connection");
        }
    }

    public static class InvalidFieldValue extends Exception
    {
        public InvalidFieldValue(String reason)
        {
            super("Invalid field value: " + reason);
        }
    }
}
