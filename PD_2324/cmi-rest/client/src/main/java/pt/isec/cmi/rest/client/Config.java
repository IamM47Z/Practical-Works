package pt.isec.cmi.rest.client;

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
}
