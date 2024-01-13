package pt.isec.cmi.server.data;

import pt.isec.cmi.shared.data.CodeData;

import java.time.LocalDateTime;

public class Code implements CsvConvertible
{
    private final User owner;
    private final Event event;
    private final String code;
    private final LocalDateTime startTime;
    private final int id, durationMinutes;

    public Code(int id, String code, LocalDateTime startTime, Event event, User owner, int durationMinutes)
    {
        assert null != owner;
        assert null != event;

        this.id = id;
        this.code = code;
        this.event = event;
        this.owner = owner;
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
    }

    public int getId()
    {
        return id;
    }

    public String getCode()
    {
        return code;
    }

    public Event getEvent()
    {
        return event;
    }

    public User getOwner()
    {
        return owner;
    }

    public LocalDateTime getStartTime()
    {
        return startTime;
    }

    public int getDurationMinutes()
    {
        return durationMinutes;
    }

    public CodeData toCodeData()
    {
        return new CodeData(id, code, durationMinutes, event.toEventData(), owner.toUserData(), startTime);
    }

    @Override
    public String toCsv()
    {
        return String.format("\"%s\";\"%d\";\"%s\";\"%s\";\"%s\"", code, durationMinutes, event.getName(), owner.getEmail(), startTime);
    }

    @Override
    public String getCsvHeader()
    {
        return "\"Code\";\"Lifespan\";\"Event Name\";\"Owner Email\";\"Start Time\"";
    }
}
