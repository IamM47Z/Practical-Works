package pt.isec.cmi.shared.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.time.ZonedDateTime;

public class Heartbeat implements java.io.Serializable
{
    @Serial
    private static final long serialVersionUID = 4L;

    private final int rmiPort;
    private final String rmiName;
    private final int databaseVersion;
    private final ZonedDateTime timestamp = ZonedDateTime.now();

    public Heartbeat(int rmiPort, String rmiName, int databaseVersion)
    {
        this.rmiPort = rmiPort;
        this.rmiName = rmiName;
        this.databaseVersion = databaseVersion;
    }

    public int getRmiPort()
    {
        return rmiPort;
    }

    public String getRmiName()
    {
        return rmiName;
    }

    public int getDatabaseVersion()
    {
        return databaseVersion;
    }

    public ZonedDateTime getTimestamp()
    {
        return timestamp;
    }

    @Override
    public String toString()
    {
        return "[ Heartbeat ] RMI port: " + rmiPort + " RMI name: " + rmiName + " Database version: " + databaseVersion + " Timestamp: " + timestamp;
    }

    public static int getMaxSerializedSize()
    {
        int maxSize = 0;
        try
        {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);

            Heartbeat heartbeat = new Heartbeat(999999, "rmi_name_test", 999999);
            objectStream.writeObject(heartbeat);
            objectStream.flush();

            maxSize = byteStream.toByteArray().length;

            objectStream.close();
            byteStream.close();
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error getting maximum serialized size of heartbeat class: " + e.getClass() + " " + e.getMessage());
        }

        return maxSize;
    }
}
