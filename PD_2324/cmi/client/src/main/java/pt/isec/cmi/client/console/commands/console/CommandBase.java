package pt.isec.cmi.client.console.commands.console;

import pt.isec.cmi.client.console.commands.SessionManager;
import pt.isec.cmi.shared.requests.Request;

public abstract class CommandBase implements Comparable<CommandBase>
{
    public abstract boolean isAvailable(SessionManager sessionManager);

    public abstract void handleSuccess(SessionManager sessionManager, Request.Response response);

    public abstract void execute(SessionManager sessionManager, String[] args);

    public Command getAnnotation()
    {
        return this.getClass().getAnnotation(Command.class);
    }

    @Override
    public String toString()
    {
        return getAnnotation().name() + " - " + getAnnotation().description();
    }

    public String getUsage()
    {
        return getAnnotation().usage();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (null == obj)
            return false;

        if (obj.getClass() != this.getClass())
            return false;

        return ((CommandBase) obj).getAnnotation().name().equals(this.getAnnotation().name());
    }
    @Override
    public int compareTo(CommandBase commandBase)
    {
        return this.getAnnotation().name().compareTo(commandBase.getAnnotation().name());
    }
}
