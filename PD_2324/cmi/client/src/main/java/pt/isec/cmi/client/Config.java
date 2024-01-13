package pt.isec.cmi.client;

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

    public final int updateInterval = 500;
    public final int loginTimeout = 10;
}
