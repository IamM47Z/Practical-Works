package pt.isec.cmi.rest.client.console.commands.admin;

import pt.isec.cmi.rest.client.Client;
import pt.isec.cmi.rest.client.SessionManager;
import pt.isec.cmi.rest.client.console.Command;
import pt.isec.cmi.rest.client.console.CommandBase;

import pt.isec.cmi.rest.client.requests.Request;
import pt.isec.cmi.rest.client.requests.RequestType;
import pt.isec.cmi.rest.client.shared.Exceptions;
import pt.isec.cmi.rest.client.shared.TimeUtils;
import pt.isec.cmi.rest.client.shared.requests.RegisterEventData;

import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalTime;

@Command(name = "createEvent", description = "creates a new event", usage = "createEvent <name> <location> <date> <startTime> <duration>")
public class CreateEvent extends CommandBase
{
    @Override
    public boolean isAvailable(SessionManager sessionManager)
    {
        return sessionManager.isLoggedIn() && sessionManager.isAdmin();
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

        try
        {
            HttpResponse<String> response = client.request(new Request(RequestType.CREATE_EVENT, data));
            if (200 != response.statusCode())
            {
                client.addToPrintQueue("Error creating event: " + response.body());
                return;
            }

            client.addToPrintQueue(response.body());
        }
        catch (Exceptions.InvalidToken e)
        {
            client.addToPrintQueue("Invalid token");
        }
    }
}
