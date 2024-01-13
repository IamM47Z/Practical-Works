package pt.isec.cmi.server.session;

import pt.isec.cmi.server.data.User;

import java.util.List;
import java.util.ArrayList;

public class SessionManager
{
    private final List<SessionData> sessionData = new ArrayList<>();

    public SessionData getSessionById(String id)
    {
        synchronized (this.sessionData)
        {
            for (SessionData sessionData : this.sessionData)
                if (sessionData.getId().equals(id))
                    return sessionData;

            return null;
        }
    }

    public SessionData getSessionByUsername(String username)
    {
        synchronized (this.sessionData)
        {
            for (SessionData sessionData : this.sessionData)
            {
                if (!sessionData.isLoggedIn())
                    continue;

                User user = sessionData.getUser();
                if (user.getEmail().equals(username))
                    return sessionData;
            }

            return null;
        }
    }

    private String generateId()
    {
        String id;
        do
        {
            id = String.valueOf((int)(Math.random() * 1000000));
        }
        while (null != this.getSessionById(id));

        return id;
    }

    public boolean addSession(SessionData sessionData)
    {
        synchronized (this.sessionData)
        {
            if (null != this.getSessionById(sessionData.getId()))
                throw new RuntimeException("Session already exists");

            this.sessionData.add(sessionData);
            return true;
        }
    }

    public SessionData createSession()
    {
        String id = generateId();
        SessionData sessionData = new SessionData(id);
        return addSession(sessionData) ? sessionData : null;
    }

    public boolean removeSession(SessionData sessionData)
    {
        synchronized (this.sessionData)
        {
            return this.sessionData.remove(sessionData);
        }
    }


    public boolean isUserOnline(String username)
    {
        return null != this.getSessionByUsername(username);
    }
}
