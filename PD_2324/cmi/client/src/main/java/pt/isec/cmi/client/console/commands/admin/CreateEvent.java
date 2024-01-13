package pt.isec.cmi.client.console.commands.admin;

import pt.isec.cmi.client.Client;
import pt.isec.cmi.client.SessionManager;
import pt.isec.cmi.client.console.Command;
import pt.isec.cmi.client.console.CommandBase;

import pt.isec.cmi.shared.TimeUtils;
import pt.isec.cmi.shared.requests.RegisterEventData;
import pt.isec.cmi.shared.requests.Request;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static pt.isec.cmi.shared.requests.RequestType.CREATE_EVENT;

@Command(name = "createEvent", description = "creates a new event", usage = "createEvent <name> <location> <date> <startTime> <duration>", requestType = CREATE_EVENT)
public class CreateEvent extends CommandBase
{
    @Override
    public boolean isAvailable(SessionManager sessionManager)
    {
        return sessionManager.isLoggedIn() && sessionManager.isAdmin();
    }

    @Override
    public void handleSuccess(SessionManager sessionManager, Request.Response response)
    {
        sessionManager.addToPrintQueue("Event created successfully");
    }

    @Override
    public void execute(SessionManager sessionManager, String[] args)
    {
        Client client = (Client) sessionManager;

        if (6 != args.length)
        {
            client.addToPrintQueue("Usage: " + getAnnotation().usage());
            return;
        }

        LocalDate newDate;
        try
        {
            newDate = LocalDate.parse(args[3], TimeUtils.DATE_FORMATTER);
        }
        catch (Exception e)
        {
            client.addToPrintQueue("Invalid date format");
            return;
        }

        LocalTime newTime;
        try
        {
            newTime = LocalTime.parse(args[4], TimeUtils.TIME_FORMATTER);
        }
        catch (Exception e)
        {
            client.addToPrintQueue("Invalid time format");
            return;
        }

        int duration;
        try
        {
            duration = Integer.parseInt(args[5]);
        }
        catch (NumberFormatException ignored)
        {
            client.addToPrintQueue("Invalid event duration, must be a number");
            return;
        }

        RegisterEventData data = new RegisterEventData(args[1], args[2], newDate, newTime, duration);

        if (!client.sendRequest(client.createRequest(CREATE_EVENT, data)))
            client.addToPrintQueue("Error sending CREATE_EVENT request");
    }
}
