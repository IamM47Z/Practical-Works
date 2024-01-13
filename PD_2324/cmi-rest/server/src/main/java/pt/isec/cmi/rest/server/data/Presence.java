package pt.isec.cmi.rest.server.data;

import pt.isec.cmi.rest.server.shared.data.PresenceData;

import java.time.LocalDateTime;
public class Presence
{
    private final int id;
    private final User user;
    private final Event event;
    private final LocalDateTime date;

    public Presence(int id, User user, Event event, LocalDateTime date)
    {
        assert null != user;
        assert null != event;

        this.id = id;
        this.user = user;
        this.event = event;
        this.date = date;
    }

    public int getId()
    {
        return id;
    }

    public User getUser()
    {
        return user;
    }

    public Event getEvent()
    {
        return event;
    }

    public LocalDateTime getDate()
    {
        return date;
    }

    public PresenceData toPresenceData()
    {
        return new PresenceData(id, user.getId(), event.getId(), date);
    }
}
