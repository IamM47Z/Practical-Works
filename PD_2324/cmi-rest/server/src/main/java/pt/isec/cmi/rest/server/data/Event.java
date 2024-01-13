package pt.isec.cmi.rest.server.data;

import pt.isec.cmi.rest.server.shared.Exceptions;
import pt.isec.cmi.rest.server.shared.TimeUtils;
import pt.isec.cmi.rest.server.shared.data.EventData;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
public class Event
{
    private final int id;
    private LocalDate date;
    private LocalTime startTime;
    private String name, location;
    private int durationMinutes;
    private final User owner;

    public Event(int id, String name, User owner, String location, LocalDate date, LocalTime startTime, int durationMinutes) throws Exceptions.InvalidFieldValue {
        assert null != owner;

        if (0 > id)
            throw new Exceptions.InvalidFieldValue("ID cannot be negative");

        this.id = id;
        this.owner = owner;

        setName(name);
        setLocation(location);
        setDate(date);
        setStartTime(startTime);
        setDurationMinutes(durationMinutes);
    }

    public int getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public LocalDate getDate()
    {
        return date;
    }

    public LocalTime getStartTime()
    {
        return startTime;
    }

    public String getLocation()
    {
        return location;
    }

    public User getOwner()
    {
        return owner;
    }

    public int getDurationMinutes()
    {
        return durationMinutes;
    }

    public void setStartTime(LocalTime startTime) throws Exceptions.InvalidFieldValue
    {
        if (null == startTime)
            throw new Exceptions.InvalidFieldValue("Start time cannot be null");

        this.startTime = startTime;
    }

    public void setDate(LocalDate date) throws Exceptions.InvalidFieldValue
    {
        if (null == date)
            throw new Exceptions.InvalidFieldValue("Date cannot be null");

        this.date = date;
    }

    public void setName(String name) throws Exceptions.InvalidFieldValue
    {
        if (null == name || name.isEmpty())
            throw new Exceptions.InvalidFieldValue("Name cannot be null or empty");

        this.name = name;
    }

    public void setLocation(String location) throws Exceptions.InvalidFieldValue
    {
        if (null == location || location.isEmpty())
            throw new Exceptions.InvalidFieldValue("Location cannot be null or empty");

        this.location = location;
    }

    public void setDurationMinutes(int durationMinutes) throws Exceptions.InvalidFieldValue
    {
        if (0 > durationMinutes)
            throw new Exceptions.InvalidFieldValue("Duration cannot be negative");

        this.durationMinutes = durationMinutes;
    }

    public boolean isHappeningAt(LocalDateTime dateTime)
    {
        LocalDate date = dateTime.toLocalDate();
        LocalTime time = dateTime.toLocalTime();

        if (!date.equals(this.date))
            return false;

        LocalTime endTime = this.startTime.plusMinutes(this.durationMinutes);
        if (this.startTime.isAfter(endTime))
            return !TimeUtils.isInRange(endTime, this.startTime, time, true);

        return TimeUtils.isInRange(this.startTime, endTime, time);
    }

    public EventData toEventData()
    {
        return new EventData(id, name, location, date, startTime, durationMinutes);
    }
}