package pt.isec.cmi.server;

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

    public final int codeLength = 6;
    public final int updateInterval = 500;
    public final int heartbeatInterval = 10 * 1000;
    public final int multicastPort = 4444;
    public final String multicastAddress = "230.44.44.44";
}
