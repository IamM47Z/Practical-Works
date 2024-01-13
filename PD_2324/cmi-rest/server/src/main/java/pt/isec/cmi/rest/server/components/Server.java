package pt.isec.cmi.rest.server.components;

import com.google.gson.Gson;
import jakarta.annotation.PreDestroy;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;
import pt.isec.cmi.rest.server.Config;
import pt.isec.cmi.rest.server.shared.Exceptions;
import pt.isec.cmi.rest.server.data.Code;
import pt.isec.cmi.rest.server.data.Event;
import pt.isec.cmi.rest.server.data.Presence;
import pt.isec.cmi.rest.server.data.User;
import pt.isec.cmi.rest.server.database.DatabaseConnector;
import pt.isec.cmi.rest.server.shared.requests.RegisterData;
import pt.isec.cmi.rest.server.shared.requests.RegisterEventData;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class Server
{
    private volatile boolean isRunning = true;
    private final DatabaseConnector databaseConnector;

    public Server()
    {
        this.databaseConnector = new DatabaseConnector("databases/rest-cmi.db");

        if (!this.databaseConnector.start())
            throw new RuntimeException("Failed to start database connector");
    }

    @PreDestroy
    public void stop()
    {
        if (!isRunning)
            return;

        isRunning = false;

        databaseConnector.close();
    }

    public User getUserByField(String fieldName, Object fieldValue) throws SQLException
    {
        // not synchronized because a query doesn't change the database
        //
        ResultSet result = databaseConnector.getTableEntryByField("users", fieldName, fieldValue);
        if (!result.next())
            return null;

        try
        {
            return new User(result.getInt("id"), result.getString("email"), result.getString("password"), result.getString("username"), result.getInt("nif"), result.getBoolean("isAdmin"));
        }
        catch (Exceptions.InvalidFieldValue e)
        {
            System.out.println("[ Server ] Found entry with an invalid field value: " + e.getMessage());
            return null;
        }
    }

    public User getUserByEmail(String email) throws SQLException
    {
        return getUserByField("email", email);
    }

    public User getUserById(int userId) throws SQLException
    {
        return getUserByField("id", userId);
    }

    public void registerUser(RegisterData data) throws SQLException
    {
        synchronized (databaseConnector)
        {
            databaseConnector.createTableEntry("users", List.of("email", "password", "username", "nif"), List.of(data.email(), BCrypt.hashpw(data.password(), BCrypt.gensalt()), data.username(), data.nif()));
        }
    }

    public void updateUserFields(int userId, List<String> fieldsName, List<Object> fieldsValue) throws SQLException
    {
        synchronized (databaseConnector)
        {
            ResultSet result = databaseConnector.getTableEntryByField("users", "id", userId);
            if (!result.next())
                System.out.println("[ Server ] User with id " + userId + " not found");

            databaseConnector.updateTableEntry("users", userId, fieldsName, fieldsValue);
        }
    }

    public void deleteUser(int userId) throws SQLException
    {
        synchronized (databaseConnector)
        {
            databaseConnector.deleteTableEntry("users", userId);
        }
    }

    public List<Event> getEventsByUserId(int userId) throws SQLException
    {
        synchronized (databaseConnector)
        {
            ResultSet result = databaseConnector.getTableEntryByField("events", "owner", userId);
            List<Event> events = new ArrayList<>();
            while (result.next())
            {
                try
                {
                    events.add(new Event(result.getInt("id"), result.getString("name"), getUserById(result.getInt("owner")), result.getString("location"), result.getObject("date", LocalDate.class), result.getObject("startTime", LocalTime.class), result.getInt("durationMinutes")));
                }
                catch (Exceptions.InvalidFieldValue e)
                {
                    System.out.println("[ Server ] Found entry with an invalid field value: " + e.getMessage());
                    return null;
                }
            }
            return events;
        }
    }

    public Event getEventByField(String fieldName, Object fieldValue) throws SQLException
    {
        synchronized (databaseConnector)
        {
            ResultSet result = databaseConnector.getTableEntryByField("events", fieldName, fieldValue);
            if (!result.next())
                return null;

            try
            {
                return new Event(result.getInt("id"), result.getString("name"), getUserById(result.getInt("owner")), result.getString("location"), result.getObject("date", LocalDate.class), result.getObject("startTime", LocalTime.class), result.getInt("durationMinutes"));
            }
            catch (Exceptions.InvalidFieldValue e)
            {
                System.out.println("[ Server ] Found entry with an invalid field value: " + e.getMessage());
                return null;
            }
        }
    }

    public Event getEventById(int eventId) throws SQLException
    {
        return getEventByField("id", eventId);
    }

    public void registerEvent(int ownerId, RegisterEventData data) throws SQLException
    {
        synchronized (databaseConnector)
        {
            databaseConnector.createTableEntry("events", List.of("name", "owner", "location", "date", "startTime", "durationMinutes"), List.of(data.name(), ownerId, data.location(), data.date(), data.startTime(), data.durationMinutes()));
        }
    }

    public void updateEventFields(int eventId, List<String> fieldsName, List<Object> fieldsValue) throws SQLException
    {
        synchronized (databaseConnector)
        {
            ResultSet result = databaseConnector.getTableEntryByField("events", "id", eventId);
            if (!result.next())
                System.out.println("Event not found");

            databaseConnector.updateTableEntry("events", eventId, fieldsName, fieldsValue);
        }
    }

    public void deleteEvent(int eventId) throws SQLException
    {
        synchronized (databaseConnector)
        {
            databaseConnector.deleteTableEntry("events", eventId);
        }
    }

    public List<Code> getCodesByEventId(int eventId) throws SQLException
    {
        synchronized (databaseConnector)
        {
            ResultSet result = databaseConnector.getTableEntryByField("codes", "event", eventId);
            List<Code> codes = new ArrayList<>();
            while (result.next())
                codes.add(new Code(result.getInt("id"), result.getString("code"), new Gson().fromJson(result.getString("date"), LocalDateTime.class), getEventById(result.getInt("event")), getUserById(result.getInt("owner")), result.getInt("durationMinutes")));

            return codes;
        }
    }

    public Code getCodeByField(String fieldName, Object fieldValue) throws SQLException
    {
        synchronized (databaseConnector)
        {
            ResultSet result = databaseConnector.getTableEntryByField("codes", fieldName, fieldValue);
            if (!result.next())
                return null;

            return new Code(result.getInt("id"), result.getString("code"), new Gson().fromJson(result.getString("date"), LocalDateTime.class), getEventById(result.getInt("event")), getUserById(result.getInt("owner")), result.getInt("durationMinutes"));
        }
    }

    public Code getCode(String code) throws SQLException
    {
        synchronized (databaseConnector)
        {
            return getCodeByField("code", code);
        }
    }

    private static String generateCode()
    {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder randomStringBuilder = new StringBuilder(Config.getInstance().codeLength);

        Random random = new Random();

        for (int i = 0; i < 6; i++)
        {
            int randomIndex = random.nextInt(characters.length());
            char randomChar = characters.charAt(randomIndex);
            randomStringBuilder.append(randomChar);
        }

        // Convert StringBuilder to String
        return randomStringBuilder.toString();
    }

    public String generateCode(int eventId, int ownerId, int lifespanMinutes) throws SQLException
    {
        synchronized (databaseConnector)
        {
            String code;
            do
            {
                code = generateCode();
            } while (getCode(code) != null);

            registerCode(code, LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES), eventId, ownerId, lifespanMinutes);
            return code;
        }
    }

    public void registerCode(String code, LocalDateTime date, int eventId, int ownerId, int durationMinutes) throws SQLException
    {
        synchronized (databaseConnector)
        {
            databaseConnector.createTableEntry("codes", List.of("code", "date", "event", "owner", "durationMinutes"), List.of(code, date, eventId, ownerId, durationMinutes));
        }
    }

    public void deleteCode(int codeId) throws SQLException
    {
        synchronized (databaseConnector)
        {
            databaseConnector.deleteTableEntry("codes", codeId);
        }
    }

    public List<Presence> getPresencesByEventId(int eventId) throws SQLException
    {
        synchronized (databaseConnector)
        {
            ResultSet result = databaseConnector.getTableEntryByField("presences", "event", eventId);
            List<Presence> presences = new ArrayList<>();
            while (result.next())
                presences.add(new Presence(result.getInt("id"), getUserById(result.getInt("user")), getEventById(result.getInt("event")), new Gson().fromJson(result.getString("date"), LocalDateTime.class)));

            return presences;
        }
    }

    public List<Presence> getPresencesByUserId(int userId) throws SQLException
    {
        synchronized (databaseConnector)
        {
            ResultSet result = databaseConnector.getTableEntryByField("presences", "user", userId);
            List<Presence> presences = new ArrayList<>();
            while (result.next())
                presences.add(new Presence(result.getInt("id"), getUserById(result.getInt("user")), getEventById(result.getInt("event")), new Gson().fromJson(result.getString("date"), LocalDateTime.class)));

            return presences;
        }
    }

    public Presence getPresence(int userId, int eventId) throws SQLException
    {
        synchronized (databaseConnector)
        {
            ResultSet result = databaseConnector.getTableEntryByFields("presences", List.of("user", "event"), List.of(userId, eventId));
            if (!result.next())
                return null;

            return new Presence(result.getInt("id"), getUserById(result.getInt("user")), getEventById(result.getInt("event")), new Gson().fromJson(result.getString("date"), LocalDateTime.class));
        }
    }

    public void registerPresence(int userId, int eventId, LocalDateTime registrationTime) throws SQLException
    {
        synchronized (databaseConnector)
        {
            databaseConnector.createTableEntry("presences", List.of("user", "event", "date"), List.of(userId, eventId, registrationTime));
        }
    }

    public void deletePresence(int eventId, int userId) throws SQLException
    {
        synchronized (databaseConnector)
        {
            databaseConnector.deleteTableEntryByFields("presences", List.of("event", "user"), List.of(eventId, userId));
        }
    }
}
