package pt.isec.cmi.server;

import pt.isec.cmi.server.data.User;
import pt.isec.cmi.server.session.SessionManager;
import pt.isec.cmi.server.threads.HeartbeatThread;
import pt.isec.cmi.server.threads.ListenerThread;
import pt.isec.cmi.shared.connections.database.DatabaseConnector;
import pt.isec.cmi.server.data.Code;
import pt.isec.cmi.server.data.Event;
import pt.isec.cmi.server.data.Presence;
import pt.isec.cmi.shared.Exceptions;
import pt.isec.cmi.shared.connections.rmi.RmiService;
import pt.isec.cmi.shared.data.CodeData;
import pt.isec.cmi.shared.data.Heartbeat;
import pt.isec.cmi.shared.requests.*;

import pt.isec.cmi.shared.connections.Multicast;
import pt.isec.cmi.shared.connections.tcp.TcpServer;

import java.io.IOException;
import java.net.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Server extends SessionManager
{
    private final TcpServer tcpServer;
    private final Multicast multicast;
    private final RmiService rmiService;
    private final DatabaseConnector databaseConnector;

    private volatile boolean isRunning;
    private Thread listenerThread, heartbeatThread;

    public Server(int tcpPort, String sqlDatabasePath, String rmiName, int rmiPort) throws IOException
    {
        // Add shutdown hook
        //
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        // Set up database
        //
        databaseConnector = new DatabaseConnector(sqlDatabasePath);

        // Set up Multicast
        //
        multicast = new Multicast(Config.getInstance().multicastPort);
        multicast.join(Config.getInstance().multicastAddress, Config.getInstance().multicastPort);

        // Set up TCP server
        //
        tcpServer = new TcpServer(tcpPort);

        // Set up RMI server
        //
        rmiService = new RmiService(databaseConnector, rmiName, rmiPort);
    }

    public void stop()
    {
        // Check if server is running
        //
        if (!isRunning)
            return;

        // Free resources
        //
        free();

        // Set running flag to false
        //
        isRunning = false;

        // Remove shutdown hook
        //
        try
        {
            Runtime.getRuntime().removeShutdownHook(new Thread(this::stop));
        }
        catch (IllegalStateException ignored) { }
    }

    private void free()
    {
        // Stop listener thread
        //
        if (null != this.listenerThread)
        {
            listenerThread.interrupt();
            try
            {
                listenerThread.join();
            }
            catch (InterruptedException ignored) { }

            listenerThread = null;
        }

        // Stop heartbeat thread
        //
        if (null != this.heartbeatThread)
        {
            heartbeatThread.interrupt();
            try
            {
                heartbeatThread.join();
            }
            catch (InterruptedException ignored) { }

            heartbeatThread = null;
        }

        // Close TCP server
        //
        if (null != this.tcpServer)
            tcpServer.close();

        // Leave multicast group
        //
        if (null != this.multicast)
            multicast.leave();

        // Free RMI service
        //
        if (null != this.rmiService)
            rmiService.free();

        // Close database
        //
        if (null != this.databaseConnector)
            databaseConnector.close();
    }

    public boolean isRunning()
    {
        return isRunning;
    }

    public boolean start()
    {
        if (isRunning)
            return false;

        isRunning = true;

        // Start database
        //
        if (!databaseConnector.start())
            return false;

        // Add database update listener
        //
        databaseConnector.addOnDatabaseUpdate(this::onDatabaseUpdate);

        // Set TCP timeout
        //
        try
        {
            tcpServer.setTimeout(Config.getInstance().updateInterval);
        }
        catch (IOException e)
        {
            System.out.println("[ Server ] Error setting TCP timeout: " + e.getClass() + ": " + e.getMessage());
            return false;
        }

        // Start listener thread
        //
        listenerThread = new ListenerThread(this);
        listenerThread.start();

        // Start heartbeat thread
        //
        heartbeatThread = new HeartbeatThread(this);
        heartbeatThread.start();

        return true;
    }

    // Multicast Related
    //
    public Socket acceptClient()
    {
        try
        {
            return tcpServer.acceptClient();
        }
        catch (IOException e)
        {
            if ("Accept timed out".equals(e.getMessage()))
                return null;

            if (isRunning)
                System.out.println("[ Server ] Error Accepting Client: " + e.getMessage());

            return null;
        }
    }

    public void sendMulticastPacket(byte[] data)
    {
        try
        {
            multicast.send(data);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    // Database Related
    //
    public Heartbeat generateHeartbeat()
    {
        // synchronized because we don't want to generate a heartbeat while the database is being updated
        //
        synchronized (databaseConnector)
        {
            try
            {
                return new Heartbeat(rmiService.getPort(), rmiService.getName(), databaseConnector.getVersion());
            }
            catch (SQLException e)
            {
                throw new RuntimeException(e);
            }
        }
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
        } catch (Exceptions.InvalidFieldValue e)
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
            databaseConnector.createTableEntry("users", List.of("email", "password", "username", "nif"), List.of(data.email(), data.password(), data.username(), data.nif()));
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
                codes.add(new Code(result.getInt("id"), result.getString("code"), result.getObject("date", LocalDateTime.class), getEventById(result.getInt("event")), getUserById(result.getInt("owner")), result.getInt("durationMinutes")));

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

            return new Code(result.getInt("id"), result.getString("code"), result.getObject("date", LocalDateTime.class), getEventById(result.getInt("event")), getUserById(result.getInt("owner")), result.getInt("durationMinutes"));
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
                presences.add(new Presence(result.getInt("id"), getUserById(result.getInt("user")), getEventById(result.getInt("event")), result.getObject("date", LocalDateTime.class)));

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
                presences.add(new Presence(result.getInt("id"), getUserById(result.getInt("user")), getEventById(result.getInt("event")), result.getObject("date", LocalDateTime.class)));

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

            return new Presence(result.getInt("id"), getUserById(result.getInt("user")), getEventById(result.getInt("event")), result.getObject("date", LocalDateTime.class));
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

    public void onDatabaseUpdate(String query, List<Object> fieldValues)
    {
        // Propagate database update to all backup servers
        //
        rmiService.propagateDatabaseUpdate(query, fieldValues);

        // Propagate database update to all clients
        //
        ((ListenerThread)listenerThread).propagateDatabaseUpdate();
    }
}