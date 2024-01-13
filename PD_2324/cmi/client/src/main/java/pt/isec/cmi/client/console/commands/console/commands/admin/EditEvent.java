package pt.isec.cmi.client.console.commands.console.commands.admin;

import pt.isec.cmi.client.console.commands.Client;
import pt.isec.cmi.client.console.commands.SessionManager;
import pt.isec.cmi.client.console.commands.console.Command;
import pt.isec.cmi.client.console.commands.console.CommandBase;
import pt.isec.cmi.shared.TimeUtils;
import pt.isec.cmi.shared.data.EventData;
import pt.isec.cmi.shared.requests.Request;

import java.time.LocalDate;
import java.time.LocalTime;

import static pt.isec.cmi.shared.requests.RequestType.EDIT_EVENT;

@Command(name = "editEvent", description = "edits an event with the given id (time arguments are optional and can only be edited if the event has no registered presences)", usage = "editEvent <eventId> <newName> <newLocation> [newDate] [newStartTime] [duration]", requestType = EDIT_EVENT)
public class EditEvent extends CommandBase
{
    @Override
    public boolean isAvailable(SessionManager sessionManager)
    {
        return sessionManager.isLoggedIn() && sessionManager.isAdmin();
    }

    @Override
    public void handleSuccess(SessionManager sessionManager, Request.Response response)
    {
        sessionManager.addToPrintQueue("Event edited successfully");
    }

    @Override
    public void execute(SessionManager sessionManager, String[] args)
    {
        Client client = (Client) sessionManager;

        if (4 > args.length || 7 < args.length)
        {
            client.addToPrintQueue("Usage: " + getAnnotation().usage());
            return;
        }

        int eventId;
        try
        {
            eventId = Integer.parseInt(args[1]);
        }
        catch (NumberFormatException ignored)
        {
            client.addToPrintQueue("Invalid event id, must be a number");
            return;
        }

        LocalDate newDate = null;
        if (5 <= args.length)
        {
            try
            {
                newDate = LocalDate.parse(args[4], TimeUtils.DATE_FORMATTER);
            }
            catch (Exception e)
            {
                client.addToPrintQueue("Invalid date format");
                return;
            }
        }

        LocalTime newTime = null;
        if (6 <= args.length)
        {
            try
            {
                newTime = LocalTime.parse(args[5], TimeUtils.TIME_FORMATTER);
            }
            catch (Exception e)
            {
                client.addToPrintQueue("Invalid time format");
                return;
            }
        }

        int duration = 0;
        if (7 == args.length)
        {
            try
            {
                duration = Integer.parseInt(args[6]);
            }
            catch (NumberFormatException ignored)
            {
                client.addToPrintQueue("Invalid event duration, must be a number");
                return;
            }
        }

        EventData data = new EventData(eventId, args[2], args[3], newDate, newTime, duration );

        if (!client.sendRequest(client.createRequest(EDIT_EVENT, data)))
            client.addToPrintQueue("Error sending EDIT_EVENT request");
    }
}
