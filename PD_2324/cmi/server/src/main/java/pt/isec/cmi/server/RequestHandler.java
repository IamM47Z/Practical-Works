package pt.isec.cmi.server;

import pt.isec.cmi.server.data.Code;
import pt.isec.cmi.server.data.Event;
import pt.isec.cmi.server.data.Presence;
import pt.isec.cmi.server.data.User;
import pt.isec.cmi.server.session.SessionData;
import pt.isec.cmi.server.threads.ClientThread;
import pt.isec.cmi.server.data.CsvConvertible;
import pt.isec.cmi.shared.Exceptions;
import pt.isec.cmi.shared.data.EventData;
import pt.isec.cmi.shared.requests.*;
import pt.isec.cmi.shared.requests.filters.FilterData;

import java.io.Serializable;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.sqlite.SQLiteErrorCode.SQLITE_CONSTRAINT;
import static org.sqlite.SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE;

public class RequestHandler
{
    public static Request.Response processRequest(Server server, ClientThread clientThread, Request request, SessionData sessionData)
    {
        return switch (request.getType())
        {
            // Regular requests
            //
            case LOGIN -> handleLogin(server, clientThread, request, sessionData);
            case FREE_SESSION -> handleFreeSession(server, clientThread, sessionData);
            case REGISTER -> handleRegister(server, clientThread, request, sessionData);
            case START_SESSION -> handleStartSession(server, clientThread, sessionData);

            // Requests for logged-in users
            //
            case LOGOUT -> handleLogout(server, clientThread, request, sessionData);
            case ADD_PRESENCE -> handleAddPresence(server, clientThread, request, sessionData);
            case EDIT_PROFILE -> handleEditProfile(server, clientThread, request, sessionData);
            case GET_PRESENCES -> handleGetPresences(server, clientThread, request, sessionData);
            case GET_QUERY_CSV -> handleQueryCsv(server, clientThread, request, sessionData);

            // Requests for admins
            //
            case CREATE_EVENT -> handleCreateEvent(server, clientThread, request, sessionData);
            case GENERATE_CODE -> handleGenerateCode(server, clientThread, request, sessionData);
            case EDIT_EVENT -> handleEditEvent(server, clientThread, request, sessionData);
            case REMOVE_EVENT -> handleRemoveEvent(server, clientThread, request, sessionData);
            case GET_EVENTS -> handleGetEvents(server, clientThread, request, sessionData);
            case GET_EVENT_PRESENCES -> handleGetEventPresences(server, clientThread, request, sessionData);
            case GET_USER_PRESENCES -> handleGetUserPresences(server, clientThread, request, sessionData);
            case ADD_PRESENCES -> handleAddPresences(server, clientThread, request, sessionData);
            case REMOVE_PRESENCES -> handleRemovePresences(server, clientThread, request, sessionData);

            // Default
            //
            default ->
            {
                if (null == sessionData.getId())
                    System.out.println("[ RequestHandler ] Malformed request from client with ip: " + clientThread.getInetAddress() + ", request type: " + request.getType() + " is not available");
                else
                    System.out.println("[ RequestHandler ] Malformed request from client with session id: " + sessionData.getId() + ", request type: " + request.getType() + " is not available");
                yield null;
            }
        };
    }

    private static Request.Response handleRemovePresences(Server server, ClientThread clientThread, Request request, SessionData sessionData)
    {
        if (!sessionData.isLoggedIn())
        {
            System.out.println("[ RequestHandler ] Error removing presences with session id: " + sessionData.getId() + ", client is not logged in");
            return new Request.Response("You are not logged in");
        }

        if (!sessionData.isAdmin())
        {
            System.out.println("[ RequestHandler ] Error removing presences with session id: " + sessionData.getId() + ", client is not admin");
            return new Request.Response("You are not admin");
        }

        List<Serializable> dataList = request.getDataList();
        if (null == dataList)
        {
            System.out.println("[ RequestHandler ] Error removing presences with session id: " + sessionData.getId() + ", data is null");
            return new Request.Response("Data is null");
        }

        List<RegisterPresenceData> presenceDataList = new ArrayList<>();
        for (Serializable data : dataList)
        {
            if (!(data instanceof RegisterPresenceData presenceData))
            {
                System.out.println("[ RequestHandler ] Error removing presences with session id: " + sessionData.getId() + ", data is not of type RegisterPresenceData");
                return new Request.Response("Data is not of type RegisterPresenceData");
            }

            presenceDataList.add(presenceData);
        }

        for (RegisterPresenceData presenceData : presenceDataList)
        {
            try
            {
                User user = server.getUserByEmail(presenceData.email());
                if (null == user)
                {
                    System.out.println("[ RequestHandler ] Error removing presences with session id: " + sessionData.getId() + ", user with email: " + presenceData.email() + " does not exist");
                    return new Request.Response("User does not exist");
                }

                Event event = server.getEventById(presenceData.eventId());
                if (null == event)
                {
                    System.out.println("[ RequestHandler ] Error removing presences with session id: " + sessionData.getId() + ", event with id: " + presenceData.eventId() + " does not exist");
                    return new Request.Response("Event does not exist");
                }

                Presence presence = server.getPresence(user.getId(), event.getId());
                if (null == presence)
                {
                    System.out.println("[ RequestHandler ] Error removing presences with session id: " + sessionData.getId() + ", user with email: " + presenceData.email() + " does not have a Presence");
                    return new Request.Response("User does not have a presence");
                }

                server.deletePresence(presenceData.eventId(), user.getId());
            }
            catch (SQLException e)
            {
                System.out.println("[ RequestHandler ] Error removing presences with session id: " + sessionData.getId() + ", database error: " + e.getClass() + ": " + e.getMessage());
                return new Request.Response("Database error");
            }
        }

        System.out.println("[ RequestHandler ] Removed " + presenceDataList.size() + " presences with session id: " + sessionData.getId());

        return new Request.Response();
    }

    private static Request.Response handleAddPresences(Server server, ClientThread clientThread, Request request, SessionData sessionData)
    {
        if (!sessionData.isLoggedIn())
        {
            System.out.println("[ RequestHandler ] Error adding presences with session id: " + sessionData.getId() + ", client is not logged in");
            return new Request.Response("You are not logged in");
        }

        if (!sessionData.isAdmin())
        {
            System.out.println("[ RequestHandler ] Error adding presences with session id: " + sessionData.getId() + ", client is not admin");
            return new Request.Response("You are not admin");
        }

        List<Serializable> dataList = request.getDataList();
        if (null == dataList)
        {
            System.out.println("[ RequestHandler ] Error adding presences with session id: " + sessionData.getId() + ", data is null");
            return new Request.Response("Data is null");
        }

        List<RegisterPresenceData> presenceDataList = new ArrayList<>();
        for (Serializable data : dataList)
        {
            if (!(data instanceof RegisterPresenceData presenceData))
            {
                System.out.println("[ RequestHandler ] Error adding presences with session id: " + sessionData.getId() + ", data is not of type RegisterPresenceData");
                return new Request.Response("Data is not of type RegisterPresenceData");
            }

            presenceDataList.add(presenceData);
        }

        LocalDateTime registrationTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        for (RegisterPresenceData presenceData : presenceDataList)
        {
            try
            {
                User user = server.getUserByEmail(presenceData.email());
                if (null == user)
                {
                    System.out.println("[ RequestHandler ] Error adding presences with session id: " + sessionData.getId() + ", user with email: " + presenceData.email() + " does not exist");
                    return new Request.Response("User does not exist");
                }

                Event event = server.getEventById(presenceData.eventId());
                if (null == event)
                {
                    System.out.println("[ RequestHandler ] Error adding presences with session id: " + sessionData.getId() + ", event with id: " + presenceData.eventId() + " does not exist");
                    return new Request.Response("Event does not exist");
                }

                // Check if user is already registered in any event at the same time
                //
                List<Presence> presenceList = server.getPresencesByUserId(user.getId());
                if (null != presenceList)
                {
                    for (Presence presence : presenceList)
                    {
                        Event presenceEvent = server.getEventById(presence.getEvent().getId());
                        if (null == presenceEvent)
                        {
                            System.out.println("[ RequestHandler ] Error registering presence with session id: " + sessionData.getId() + ", event with id: " + presence.getEvent() + " does not exist");
                            return new Request.Response("Event does not exist");
                        }

                        LocalDateTime startTime = event.getDate().atTime(event.getStartTime());
                        LocalDateTime endTime = startTime.plusMinutes(event.getDurationMinutes());

                        if (startTime.isBefore(registrationTime) && endTime.isAfter(registrationTime))
                        {
                            System.out.println("[ RequestHandler ] Error registering presence with session id: " + sessionData.getId() + ", user is already registered in event with id: " + presence.getEvent());
                            return new Request.Response("User is already registered in another event at the same time");
                        }
                    }
                }

                server.registerPresence(user.getId(), presenceData.eventId(), registrationTime);
            }
            catch (SQLException e)
            {
                String constraintName = handleSqlConstraintException(e, "presences");
                if (null != constraintName)
                {
                    System.out.println("[ RequestHandler ] Error adding presences with session id: " + sessionData.getId() + ", " + constraintName + " already registered");
                    return new Request.Response(constraintName + " already registered");
                }

                System.out.println("[ RequestHandler ] Error adding presences with session id: " + sessionData.getId() + ", database error: " + e.getClass() + ": " + e.getMessage());
                return new Request.Response("Database error");
            }
        }

        System.out.println("[ RequestHandler ] Added " + presenceDataList.size() + " presences with session id: " + sessionData.getId());

        return new Request.Response();
    }

    private static Request.Response handleGetUserPresences(Server server, ClientThread clientThread, Request request, SessionData sessionData)
    {
        if (!sessionData.isLoggedIn())
        {
            System.out.println("[ RequestHandler ] Error getting user presences with session id: " + sessionData.getId() + ", client is not logged in");
            return new Request.Response("You are not logged in");
        }

        if (!sessionData.isAdmin())
        {
            System.out.println("[ RequestHandler ] Error getting user presences with session id: " + sessionData.getId() + ", client is not admin");
            return new Request.Response("You are not admin");
        }

        String userEmail = (String) request.getData();

        User user;
        try
        {
            user = server.getUserByEmail(userEmail);
            if (null == user)
            {
                System.out.println("[ RequestHandler ] Error getting user presences with session id: " + sessionData.getId() + ", user with email: " + userEmail + " does not exist");
                return new Request.Response("User does not exist");
            }
        }
        catch (SQLException e)
        {
            System.out.println("[ RequestHandler ] Error getting user presences with session id: " + sessionData.getId() + ", database error: " + e.getClass() + ": " + e.getMessage());
            return new Request.Response("Database error");
        }

        try
        {
            List<Presence> presences = server.getPresencesByUserId(user.getId());
            if (null == presences)
            {
                System.out.println("[ RequestHandler ] Error getting user presences with session id: " + sessionData.getId() + ", database error");
                return new Request.Response("Database error");
            }

            List<Serializable> presenceDataList = new ArrayList<>();
            for (Presence presence : presences)
                presenceDataList.add(presence.toPresenceData());

            System.out.println("[ RequestHandler ] Found " + presenceDataList.size() + " presences for user with id: " + user.getId());

            sessionData.setLastQueryResults(new ArrayList<>(presences));

            return new Request.Response().setDataList(presenceDataList);
        }
        catch (SQLException e)
        {
            System.out.println("[ RequestHandler ] Error getting user presences with session id: " + sessionData.getId() + ", database error: " + e.getClass() + ": " + e.getMessage());
            return new Request.Response("Database error");
        }
    }

    private static Request.Response handleGetEventPresences(Server server, ClientThread clientThread, Request request, SessionData sessionData)
    {
        if (!sessionData.isLoggedIn())
        {
            System.out.println("[ RequestHandler ] Error getting event presences with session id: " + sessionData.getId() + ", client is not logged in");
            return new Request.Response("You are not logged in");
        }

        if (!sessionData.isAdmin())
        {
            System.out.println("[ RequestHandler ] Error getting event presences with session id: " + sessionData.getId() + ", client is not admin");
            return new Request.Response("You are not admin");
        }

        int eventId = (int) request.getData();

        try
        {
            Event event = server.getEventById(eventId);
            if (null == event)
            {
                System.out.println("[ RequestHandler ] Error getting event presences with session id: " + sessionData.getId() + ", event with id: " + eventId + " does not exist");
                return new Request.Response("Event does not exist");
            }
        } catch (SQLException e)
        {
            System.out.println("[ RequestHandler ] Error getting event presences with session id: " + sessionData.getId() + ", database error: " + e.getClass() + ": " + e.getMessage());
            return new Request.Response("Database error");
        }

        try
        {
            List<Presence> presences = server.getPresencesByEventId(eventId);
            if (null == presences)
            {
                System.out.println("[ RequestHandler ] Error getting event presences with session id: " + sessionData.getId() + ", database error");
                return new Request.Response("Database error");
            }

            List<Serializable> presenceDataList = new ArrayList<>();
            for (Presence presence : presences)
                presenceDataList.add(presence.toPresenceData());

            System.out.println("[ RequestHandler ] Found " + presenceDataList.size() + " presences for event with id: " + eventId);

            sessionData.setLastQueryResults(new ArrayList<>(presences));

            return new Request.Response().setDataList(presenceDataList);
        }
        catch (SQLException e)
        {
            System.out.println("[ RequestHandler ] Error getting event presences with session id: " + sessionData.getId() + ", database error: " + e.getClass() + ": " + e.getMessage());
            return new Request.Response("Database error");
        }
    }

    private static Request.Response handleGetEvents(Server server, ClientThread clientThread, Request request, SessionData sessionData)
    {
        if (!sessionData.isLoggedIn())
        {
            System.out.println("[ RequestHandler ] Error getting events with session id: " + sessionData.getId() + ", client is not logged in");
            return new Request.Response("You are not logged in");
        }

        if (!sessionData.isAdmin())
        {
            System.out.println("[ RequestHandler ] Error getting events with session id: " + sessionData.getId() + ", client is not admin");
            return new Request.Response("You are not admin");
        }

        try
        {
            List<Event> events = server.getEventsByUserId(sessionData.getUser().getId());
            if (events.isEmpty())
            {
                System.out.println("[ RequestHandler ] No events found for user with session id: " + sessionData.getId());
                return new Request.Response().setDataList(new ArrayList<>());
            }

            List<Serializable> filters = request.getDataList();

            List<CsvConvertible> queryDataList = new ArrayList<>();
            List<Serializable> eventDataList = new ArrayList<>();
            for (Event event : events)
            {
                // Filter event
                //
                if (null != filters)
                {
                    EventData data = event.toEventData();

                    boolean match = true;
                    for (Serializable filter : filters)
                    {
                        if (((FilterData) filter).matches(data))
                            continue;

                        match = false;
                        break;
                    }

                    if (!match)
                        continue;
                }

                // Add event to list
                //
                eventDataList.add(event.toEventData());
                queryDataList.add(event);
            }

            System.out.println("[ RequestHandler ] Found " + eventDataList.size() + " events for user with session id: " + sessionData.getId());

            sessionData.setLastQueryResults(queryDataList);

            return new Request.Response().setDataList(eventDataList);
        }
        catch (SQLException e)
        {
            System.out.println("[ RequestHandler ] Error getting events with session id: " + sessionData.getId() + ", database error: " + e.getClass() + ": " + e.getMessage());
            return new Request.Response("Database error");
        }
    }

    private static Request.Response handleRemoveEvent(Server server, ClientThread clientThread, Request request, SessionData sessionData)
    {
        if (!sessionData.isLoggedIn())
        {
            System.out.println("[ RequestHandler ] Error removing event with session id: " + sessionData.getId() + ", client is not logged in");
            return new Request.Response("You are not logged in");
        }

        if (!sessionData.isAdmin())
        {
            System.out.println("[ RequestHandler ] Error removing event with session id: " + sessionData.getId() + ", client is not admin");
            return new Request.Response("You are not admin");
        }

        int eventId = (int) request.getData();
        if (1 > eventId)
        {
            System.out.println("[ RequestHandler ] Error removing event with session id: " + sessionData.getId() + ", data is null");
            return new Request.Response("Invalid eventId value");
        }

        try
        {
            Event event = server.getEventById(eventId);
            if (null == event)
            {
                System.out.println("[ RequestHandler ] Error removing event with session id: " + sessionData.getId() + ", event with id: " + eventId + " does not exist");
                return new Request.Response("Event with id: " + eventId + " does not exist");
            }

            List<Presence> presenceList = server.getPresencesByEventId(eventId);
            if (!presenceList.isEmpty())
            {
                System.out.println("[ RequestHandler ] Error removing event with session id: " + sessionData.getId() + ", event with id: " + eventId + " has presences");
                return new Request.Response("Event has presences");
            }
        }
        catch (SQLException e)
        {
            System.out.println("[ RequestHandler ] Error removing event with session id: " + sessionData.getId() + ", database error: " + e.getClass() + ": " + e.getMessage());
            return new Request.Response("Database error");
        }

        try {
            List<Code> codeList = server.getCodesByEventId(eventId);
            if (!codeList.isEmpty())
                for(Code code : codeList)
                    server.deleteCode(code.getId());
        }
        catch (SQLException e) {
            System.out.println("[ RequestHandler ] Error removing event with session id: " + sessionData.getId() + ", database error while removing codes: " + e.getClass() + ": " + e.getMessage());
            return new Request.Response("Error removing event associated codes");
        }

        try
        {
            server.deleteEvent(eventId);
        }
        catch (SQLException e)
        {
            System.out.println("[ RequestHandler ] Error removing event with session id: " + sessionData.getId() + ", database error: " + e.getClass() + ": " + e.getMessage());
            return new Request.Response("Database error");
        }

        System.out.println("[ RequestHandler ] Removed event with id: " + eventId + " , requested by client with session id " + sessionData.getId());

        return new Request.Response();
    }

    private static Request.Response handleEditEvent(Server server, ClientThread clientThread, Request request, SessionData sessionData)
    {
        if (!sessionData.isLoggedIn())
        {
            System.out.println("[ RequestHandler ] Error editing event with session id: " + sessionData.getId() + ", client is not logged in");
            return new Request.Response("You are not logged in");
        }

        if (!sessionData.isAdmin())
        {
            System.out.println("[ RequestHandler ] Error editing event with session id: " + sessionData.getId() + ", client is not admin");
            return new Request.Response("You are not admin");
        }

        EventData data = (EventData) request.getData();
        if (null == data)
        {
            System.out.println("[ RequestHandler ] Error editing event with session id: " + sessionData.getId() + ", data is null");
            return new Request.Response("Data is null");
        }

        Event event;
        try
        {
            event = server.getEventById(data.id());
            if (null == event)
            {
                System.out.println("[ RequestHandler ] Error editing event with session id: " + sessionData.getId() + ", event does not exist");
                return new Request.Response("Event does not exist");
            }

            List<Presence> presenceList = server.getPresencesByEventId(data.id());
            if (!presenceList.isEmpty() && (null != data.startTime() || 0 != data.durationMinutes() || null != data.date()))
            {
                System.out.println("[ RequestHandler ] Error editing event with session id: " + sessionData.getId() + ", event with id: " + data.id() + " has presences");
                return new Request.Response("Event has presences");
            }
        }
        catch (SQLException e)
        {
            System.out.println("[ RequestHandler ] Error editing event with session id: " + sessionData.getId() + ", database error: " + e.getClass() + ": " + e.getMessage());
            return new Request.Response("Database error");
        }

        LocalDate date = data.date();
        if (null == date)
            date = event.getDate();

        LocalTime startTime = data.startTime();
        if (null == startTime)
            startTime = event.getStartTime();

        int durationMinutes = data.durationMinutes();
        if (0 == durationMinutes)
            durationMinutes = event.getDurationMinutes();

        // Check if data is valid
        //
        try
        {
            event.setName(data.name());
            event.setLocation(data.location());
            event.setDate(data.date());
            event.setStartTime(data.startTime());
            event.setDurationMinutes(data.durationMinutes());
        }
        catch (Exceptions.InvalidFieldValue e)
        {
            System.out.println("[ RequestHandler ] Error editing profile with session id: " + sessionData.getId() + ", " + e.getMessage());
            return new Request.Response(e.getMessage());
        }

        try
        {
            server.updateEventFields(data.id(), List.of("name", "location", "date", "startTime", "durationMinutes"), List.of(data.name(), data.location(), date, startTime, durationMinutes));
        }
        catch (SQLException e)
        {
            String constraintName = handleSqlConstraintException(e, "events");
            if (null != constraintName)
            {
                System.out.println("[ RequestHandler ] Error editing event with session id: " + sessionData.getId() + ", " + constraintName + " already registered");
                return new Request.Response(constraintName + " already registered");
            }

            System.out.println("[ RequestHandler ] Error editing event with session id: " + sessionData.getId() + ", database error: " + e.getClass() + ": " + e.getMessage());
            return new Request.Response("Database error");
        }

        System.out.println("[ RequestHandler ] Edited event with session id: " + sessionData.getId() + ", name: " + data.name() + ", start time: " + data.startTime() + ", duration: " + data.durationMinutes() + " minutes");

        return new Request.Response();
    }

    private static Request.Response handleCreateEvent(Server server, ClientThread clientThread, Request request, SessionData sessionData)
    {
        if (!sessionData.isLoggedIn())
        {
            System.out.println("[ RequestHandler ] Error creating event with session id: " + sessionData.getId() + ", client is not logged in");
            return new Request.Response("You are not logged in");
        }

        if (!sessionData.isAdmin())
        {
            System.out.println("[ RequestHandler ] Error generating code with session id: " + sessionData.getId() + ", client is not admin");
            return new Request.Response("You are not admin");
        }

        RegisterEventData data = (RegisterEventData) request.getData();
        if (null == data)
        {
            System.out.println("[ RequestHandler ] Error creating event with session id: " + sessionData.getId() + ", data is null");
            return new Request.Response("Data is null");
        }

        try
        {
            server.registerEvent(sessionData.getUser().getId(), data);
        } catch (SQLException e)
        {
            String constraintName = handleSqlConstraintException(e, "events");
            if (null != constraintName)
            {
                System.out.println("[ RequestHandler ] Error creating event with session id: " + sessionData.getId() + ", " + constraintName + " already registered");
                return new Request.Response(constraintName + " already registered");
            }

            System.out.println("[ RequestHandler ] Error creating event with session id: " + sessionData.getId() + ", database error: " + e.getClass() + ": " + e.getMessage());
            return new Request.Response("Database error");
        }

        System.out.println("[ RequestHandler ] Created event with session id: " + sessionData.getId() + ", name: " + data.name() + ", start time: " + data.startTime() + ", duration: " + data.durationMinutes() + " minutes");

        return new Request.Response();
    }

    private static Request.Response handleGenerateCode(Server server, ClientThread clientThread, Request request, SessionData sessionData)
    {
        if (!sessionData.isLoggedIn())
        {
            System.out.println("[ RequestHandler ] Error generating code with session id: " + sessionData.getId() + ", client is not logged in");
            return new Request.Response("You are not logged in");
        }

        if (!sessionData.isAdmin())
        {
            System.out.println("[ RequestHandler ] Error generating code with session id: " + sessionData.getId() + ", client is not admin");
            return new Request.Response("You are not admin");
        }

        GenerateCodeData data = (GenerateCodeData) request.getData();
        if (null == data)
        {
            System.out.println("[ RequestHandler ] Error generating code with session id: " + sessionData.getId() + ", data is null");
            return new Request.Response("Data is null");
        }

        if (0 >= data.lifespanMinutes())
        {
            System.out.println("[ RequestHandler ] Error generating code with session id: " + sessionData.getId() + ", lifespanMinutes is invalid");
            return new Request.Response("Invalid lifespanMinutes value");
        }

        try
        {
            Event event = server.getEventById(data.eventId());
            if (null == event)
            {
                System.out.println("[ RequestHandler ] Error generating code with session id: " + sessionData.getId() + ", event with id: " + data.eventId() + " does not exist");
                return new Request.Response("Event does not exist");
            }

            if (!event.isHappeningAt(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)))
            {
                System.out.println("[ RequestHandler ] Error generating code with session id: " + sessionData.getId() + ", event with id: " + data.eventId() + " is not happening now");
                return new Request.Response("Event is not happening now");
            }
        }
        catch (SQLException e)
        {
            System.out.println("[ RequestHandler ] Error generating code with session id: " + sessionData.getId() + ", database error: " + e.getClass() + ": " + e.getMessage());
            return new Request.Response("Database error");
        }

        // Delete old codes
        try
        {
            List<Code> oldCodes = server.getCodesByEventId(data.eventId());
            if (!oldCodes.isEmpty())
                for (Code oldCode : oldCodes)
                    server.deleteCode(oldCode.getId());
        }
        catch (SQLException e)
        {
            System.out.println("[ RequestHandler ] Error generating code with session id: " + sessionData.getId() + ", database error: " + e.getClass() + ": " + e.getMessage());
            return new Request.Response("Database error");
        }

        String code;
        try
        {
            code = server.generateCode(data.eventId(), sessionData.getUser().getId(), data.lifespanMinutes());
        }
        catch (SQLException e)
        {
            String constraintName = handleSqlConstraintException(e, "codes");
            if (null != constraintName)
            {
                System.out.println("[ RequestHandler ] Error generating code with session id: " + sessionData.getId() + ", " + constraintName + " already registered");
                return new Request.Response(constraintName + " already registered");
            }

            System.out.println("[ RequestHandler ] Error generating code with session id: " + sessionData.getId() + ", database error: " + e.getClass() + ": " + e.getMessage());
            return new Request.Response("Database error");
        }

        System.out.println("[ RequestHandler ] Generated code with session id: " + sessionData.getId() + ", code: " + code + ", lifespanMinutes: " + data.lifespanMinutes() + " minutes");

        return new Request.Response().setData(code);
    }

    private static Request.Response handleGetPresences(Server server, ClientThread clientThread, Request request, SessionData sessionData)
    {
        if (!sessionData.isLoggedIn())
        {
            System.out.println("[ RequestHandler ] Error getting presences with session id: " + sessionData.getId() + ", client is not logged in");
            return new Request.Response("You are not logged in");
        }

        User user = sessionData.getUser();
        if (null == user)
        {
            System.out.println("[ RequestHandler ] Error getting presences with session id: " + sessionData.getId() + ", user field in session data is null");
            return new Request.Response("User field in session data is null");
        }

        try
        {
            List<Presence> presences = new ArrayList<>(server.getPresencesByUserId(user.getId()));
            if (presences.isEmpty())
            {
                System.out.println("[ RequestHandler ] No presences found for user with session id: " + sessionData.getId());
                return new Request.Response().setDataList(new ArrayList<>());
            }

            List<Serializable> filters = request.getDataList();

            List<CsvConvertible> queryDataList = new ArrayList<>();
            List<Serializable> presenceDataList = new ArrayList<>();
            for (Presence presence : presences)
            {
                // Get event
                //
                Event event = server.getEventById(presence.getEvent().getId());
                if (null == event)
                {
                    System.out.println("[ RequestHandler ] Error getting presences with session id: " + sessionData.getId() + ", event with id: " + presence.getEvent() + " does not exist");
                    return new Request.Response("Event does not exist");
                }

                // Filter event
                //
                if (null != filters)
                {
                    EventData data = event.toEventData();

                    boolean match = true;
                    for (Serializable filter : filters)
                    {
                        if (((FilterData) filter).matches(data))
                            continue;

                        match = false;
                        break;
                    }

                    if (!match)
                        continue;
                }

                // Add presence to lists
                //
                queryDataList.add(presence);
                presenceDataList.add(presence.toPresenceData());
            }

            System.out.println("[ RequestHandler ] Found " + presenceDataList.size() + " presences for user with session id: " + sessionData.getId());

            sessionData.setLastQueryResults(queryDataList);

            return new Request.Response().setDataList(presenceDataList);
        } catch (SQLException e)
        {
            System.out.println("[ RequestHandler ] Error getting presences with session id: " + sessionData.getId() + ", database error: " + e.getClass() + ": " + e.getMessage());
            return new Request.Response("Database error");
        }
    }

    private static Request.Response handleQueryCsv(Server server, ClientThread clientThread, Request request, SessionData sessionData)
    {
        if (!sessionData.isLoggedIn())
        {
            System.out.println("[ RequestHandler ] Error querying csv with session id: " + sessionData.getId() + ", client is not logged in");
            return new Request.Response("You are not logged in");
        }

        User user = sessionData.getUser();
        if (null == user)
        {
            System.out.println("[ RequestHandler ] Error querying csv with session id: " + sessionData.getId() + ", user field in session data is null");
            return new Request.Response("User field in session data is null");
        }

        List<CsvConvertible> lastQuery = sessionData.getLastQueryResults();
        if (null == lastQuery)
        {
            System.out.println("[ RequestHandler ] Error querying csv with session id: " + sessionData.getId() + ", last query results are null");
            return new Request.Response("Last query results are null");
        }

        StringBuilder csv = new StringBuilder();
        for (CsvConvertible query: lastQuery)
        {
            if (csv.isEmpty())
                csv.append(query.getCsvHeader()).append("\n");

            csv.append(query.toCsv());

            if (query != lastQuery.get(lastQuery.size() - 1))
                csv.append("\n");
        }

        System.out.println("[ RequestHandler ] Queried csv with session id: " + sessionData.getId() + ", found " + lastQuery.size() + " results");

        return new Request.Response().setData(csv.toString());
    }

    private static Request.Response handleAddPresence(Server server, ClientThread clientThread, Request request, SessionData sessionData)
    {
        if (!sessionData.isLoggedIn())
        {
            System.out.println("[ RequestHandler ] Error registering presence with session id: " + sessionData.getId() + ", client is not logged in");
            return new Request.Response("You are not logged in");
        }

        User user = sessionData.getUser();
        if (null == user)
        {
            System.out.println("[ RequestHandler ] Error registering presence with session id: " + sessionData.getId() + ", user field in session data is null");
            return new Request.Response("User field in session data is null");
        }

        try
        {
            LocalDateTime registrationTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

            // Get event code
            //
            String eventCode = (String) request.getData();
            if (null == eventCode)
            {
                System.out.println("[ RequestHandler ] Error registering presence with session id: " + sessionData.getId() + ", event code is null");
                return new Request.Response("Event code is null");
            }

            // Get code from a database
            //
            Code code = server.getCode(eventCode);
            if (null == code)
            {
                System.out.println("[ RequestHandler ] Error registering presence with session id: " + sessionData.getId() + ", event code: " + eventCode + " does not exist");
                return new Request.Response("Event code does not exist");
            }

            // Check if code is active
            //
            LocalDateTime codeStartTime = code.getStartTime();
            LocalDateTime codeEndTime = codeStartTime.plusMinutes(code.getDurationMinutes());

            if (registrationTime.isBefore(codeStartTime) || registrationTime.isAfter(codeEndTime))
            {
                System.out.println("[ RequestHandler ] Error registering presence with session id: " + sessionData.getId() + ", event with id: " + code.getEvent().getId() + " is not active");
                return new Request.Response("Code is not active");
            }

            // Check if event exists
            //
            Event event = server.getEventById(code.getEvent().getId());
            if (null == event)
            {
                System.out.println("[ RequestHandler ] Error registering presence with session id: " + sessionData.getId() + ", event with id: " + code.getEvent().getId() + " does not exist");
                return new Request.Response("Event does not exist");
            }

            // Check if user is already registered in any event at the same time
            //
            List<Presence> presenceList = server.getPresencesByUserId(user.getId());
            if (null != presenceList)
            {
                for (Presence presence : presenceList)
                {
                    Event presenceEvent = server.getEventById(presence.getEvent().getId());
                    if (null == presenceEvent)
                    {
                        System.out.println("[ RequestHandler ] Error registering presence with session id: " + sessionData.getId() + ", event with id: " + presence.getEvent() + " does not exist");
                        return new Request.Response("Event does not exist");
                    }

                    LocalDateTime startTime = event.getDate().atTime(event.getStartTime());
                    LocalDateTime endTime = startTime.plusMinutes(event.getDurationMinutes());

                    if (startTime.isBefore(codeStartTime) && endTime.isAfter(codeStartTime))
                    {
                        System.out.println("[ RequestHandler ] Error registering presence with session id: " + sessionData.getId() + ", user is already registered in event with id: " + presence.getEvent());
                        return new Request.Response("User is already registered in another event at the same time");
                    }
                }
            }

            System.out.println("[ RequestHandler ] Registering presence for user with session id: " + sessionData.getId() + " on event with code: " + eventCode);

            // Register presence
            //
            server.registerPresence(user.getId(), event.getId(), registrationTime);
        }
        catch (SQLException e)
        {
            String constraintName = handleSqlConstraintException(e, "presences");
            if (null != constraintName)
            {
                System.out.println("[ RequestHandler ] Error registering presence with session id: " + sessionData.getId() + ", " + constraintName + " already registered");
                return new Request.Response(constraintName + " already registered");
            }

            System.out.println("[ RequestHandler ] Error registering presence with session id: " + sessionData.getId() + ", database error: " + e.getClass() + ": " + e.getMessage());
            return new Request.Response("Database error");
        }

        return new Request.Response();
    }

    private static Request.Response handleEditProfile(Server server, ClientThread clientThread, Request request, SessionData sessionData)
    {
        if (!sessionData.isLoggedIn())
        {
            System.out.println("[ RequestHandler ] Error editing profile with session id: " + sessionData.getId() + ", client is not logged in");
            return new Request.Response("You are not logged in");
        }

        User user = sessionData.getUser();
        if (null == user)
        {
            System.out.println("[ RequestHandler ] Error editing profile with session id: " + sessionData.getId() + ", user field in session data is null");
            return new Request.Response("User field in session data is null");
        }

        RegisterData data = (RegisterData) request.getData();
        if (null == data)
        {
            System.out.println("[ RequestHandler ] Error editing profile with session id: " + sessionData.getId() + ", data is null");
            return new Request.Response("Data is null");
        }

        // Check if data is valid
        //
        try
        {
            user.setNif(data.nif());
            user.setEmail(data.email());
            user.setPassword(data.password());
            user.setUsername(data.username());
        }
        catch (Exceptions.InvalidFieldValue e)
        {
            System.out.println("[ RequestHandler ] Error editing profile with session id: " + sessionData.getId() + ", " + e.getMessage());
            return new Request.Response(e.getMessage());
        }

        System.out.println("[ RequestHandler ] Editing profile for user with session id: " + sessionData.getId());

        try
        {
            server.updateUserFields(user.getId(), List.of("email", "password", "username", "nif"), List.of(data.email(), data.password(), data.username(), data.nif()));
        }
        catch (SQLException e)
        {
            String constraintName = handleSqlConstraintException(e, "users");
            if (null != constraintName)
            {
                System.out.println("[ RequestHandler ] Error editing profile with session id: " + sessionData.getId() + ", " + constraintName + " already registered");
                return new Request.Response(constraintName + " already registered");
            }

            System.out.println("[ RequestHandler ] Error editing profile with session id: " + sessionData.getId() + ", database error: " + e.getClass() + ": " + e.getMessage());
            return new Request.Response("Database error");
        }

        return new Request.Response();
    }

    private static Request.Response handleRegister(Server server, ClientThread clientThread, Request request, SessionData sessionData)
    {
        if (sessionData.isLoggedIn())
        {
            System.out.println("[ RequestHandler ] Error registering user with session id: " + sessionData.getId() + ", client is already logged in");
            return new Request.Response("You are already logged in");
        }

        RegisterData data = (RegisterData) request.getData();
        if (null == data)
        {
            System.out.println("[ RequestHandler ] Error registering user with session id: " + sessionData.getId() + ", data is null");
            return new Request.Response("Data is null");
        }

        // Check if data is valid
        //
        try
        {
            User.isValidInput(data);
        }
        catch (Exceptions.InvalidFieldValue e)
        {
            System.out.println("[ RequestHandler ] Error registering user with session id: " + sessionData.getId() + ", " + e.getMessage());
            return new Request.Response(e.getMessage());
        }

        System.out.println("[ RequestHandler ] Registering user with session id: " + sessionData.getId() + ", email: " + data.email() + ", password: " + data.password());

        try
        {
            server.registerUser(data);
        }
        catch (SQLException e)
        {
            String constraintName = handleSqlConstraintException(e, "users");
            if (null != constraintName)
            {
                System.out.println("[ RequestHandler ] Error registering user with session id: " + sessionData.getId() + ", " + constraintName + " already registered");
                return new Request.Response(constraintName + " already registered");
            }

            System.out.println("[ RequestHandler ] Error registering user with session id: " + sessionData.getId() + ", database error: " + e.getClass() + ": " + e.getMessage());
            return new Request.Response("Database error");
        }

        return new Request.Response();
    }

    @SuppressWarnings("unused")
    private static Request.Response handleLogout(Server server, ClientThread clientThread, Request request, SessionData sessionData)
    {
        if (!sessionData.isLoggedIn())
        {
            System.out.println("[ RequestHandler ] Error logging out user with session id: " + sessionData.getId() + ", client is not logged in");
            return new Request.Response("You are not logged in");
        }

        User user = sessionData.getUser();
        if (null == user)
        {
            System.out.println("[ RequestHandler ] Error logging out user with session id: " + sessionData.getId() + ", user field in session data is null");
            return new Request.Response("User field in session data is null");
        }

        System.out.println("[ RequestHandler ] Logging out user with session id: " + sessionData.getId() + ", email: " + user.getEmail());

        sessionData.onLogout();

        return new Request.Response();
    }

    private static Request.Response handleLogin(Server server, ClientThread clientThread, Request request, SessionData sessionData)
    {
        if (sessionData.isLoggedIn())
        {
            System.out.println("[ RequestHandler ] Error logging in user with session id: " + sessionData.getId() + ", client is already logged in");
            return new Request.Response("You are already logged in");
        }

        LoginData data = (LoginData) request.getData();
        if (null == data)
        {
            System.out.println("[ RequestHandler ] Error logging in user with session id: " + sessionData.getId() + ", data is null");
            return new Request.Response("Data is null");
        }

        System.out.println("[ RequestHandler ] Logging in user with session id: " + sessionData.getId() + ", email: " + data.email() + ", password: " + data.password());

        User user;
        try
        {
            user = server.getUserByEmail(data.email());
        }
        catch (SQLException e)
        {
            String constraintName = handleSqlConstraintException(e, "users");
            if (null != constraintName)
            {
                System.out.println("[ RequestHandler ] Error logging in user with session id: " + sessionData.getId() + ", " + constraintName + " already registered");
                return new Request.Response(constraintName + " already registered");
            }

            System.out.println("[ RequestHandler ] Error logging in user with session id: " + sessionData.getId() + ", database error: " + e.getClass() + ": " + e.getMessage());
            return new Request.Response("Database error");
        }

        if (null == user)
        {
            System.out.println("[ RequestHandler ] Error logging in user with session id: " + sessionData.getId() + ", user does not exist");
            return new Request.Response("User does not exist");
        }

        if (!user.checkPassword(data.password()))
        {
            System.out.println("[ RequestHandler ] Error logging in user with session id: " + sessionData.getId() + ", wrong password");
            return new Request.Response("Wrong password");
        }

        if (server.isUserOnline(user.getEmail()))
        {
            System.out.println("[ RequestHandler ] Error logging in user with session id: " + sessionData.getId() + ", user is already logged in");
            return new Request.Response("User is already logged in");
        }

        sessionData.onLogin(user);

        return new Request.Response().setData(user.toUserData());
    }

    private static Request.Response handleStartSession(Server server, ClientThread clientThread, SessionData sessionData)
    {
        System.out.println("[ RequestHandler ] Starting session with id: " + sessionData.getId());
        return new Request.Response().setData(sessionData.getId());
    }

    private static Request.Response handleFreeSession(Server server, ClientThread clientThread, SessionData sessionData)
    {
        System.out.println("[ RequestHandler ] Freeing session with id: " + sessionData.getId());

        if (!server.removeSession(sessionData))
            System.out.println("[ RequestHandler ] Error freeing session with id: " + sessionData.getId());

        clientThread.free();
        return null;
    }

    private static String handleSqlConstraintException(SQLException e, String tableName)
    {

        if (e.getErrorCode() == SQLITE_CONSTRAINT_UNIQUE.code || e.getErrorCode() == SQLITE_CONSTRAINT.code)
        {
            String constraintName = e.getMessage().split(tableName + ".")[1];
            return constraintName.substring(0, constraintName.length() - 1);
        }

        return null;
    }
}