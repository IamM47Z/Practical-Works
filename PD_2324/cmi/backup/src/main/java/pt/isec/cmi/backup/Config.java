package pt.isec.cmi.backup;

public class Config
{
    private static Config instance;

    private Config() { }

    public static Config getInstance()
    {
        if (null == Config.instance)
            instance = new Config();

        return instance;
    }

    public final int maxTimeWithoutHeartbeat = 30;  // seconds
    public final int multicastPort = 4444;
    public final String multicastAddress = "230.44.44.44";
}
