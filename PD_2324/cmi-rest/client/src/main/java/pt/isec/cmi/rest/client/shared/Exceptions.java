package pt.isec.cmi.rest.client.shared;

public class Exceptions
{
    public static class InvalidToken extends Exception
    {
        public InvalidToken()
        {
            super("Invalid token");
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
