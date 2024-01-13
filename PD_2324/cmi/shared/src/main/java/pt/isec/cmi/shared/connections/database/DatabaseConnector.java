package pt.isec.cmi.shared.connections.database;

import pt.isec.cmi.shared.StatementHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseConnector
{
    private final Object lock = new Object();
    private final String jdbcPath;
    private Connection connection;

    public DatabaseConnector(String databasePath)
    {
        this.jdbcPath = "jdbc:sqlite:" + databasePath;
    }

    private boolean fileExists()
    {
        File file = new File(jdbcPath.substring(12));
        return file.exists();
    }

    private void createDatabase() throws SQLException
    {
        if (null != this.connection)
            throw new RuntimeException("Database already connected");

        if (fileExists())
            throw new RuntimeException("Database file already exists");

        try
        {
            File file = new File(jdbcPath.substring(12));
            if (!file.getParentFile().exists())
                if (!file.getParentFile().mkdirs())
                    throw new RuntimeException("Failed to create database directory");

            if (!file.createNewFile())
                throw new RuntimeException("Failed to create database file");
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        this.connection = DriverManager.getConnection(jdbcPath);

        Statement statement = connection.createStatement();
        statement.closeOnCompletion();

        // Set database version
        //
        statement.executeUpdate("PRAGMA user_version = 0");

        // Create tables
        //
        statement.executeUpdate("CREATE TABLE users " +
                "(id INTEGER CONSTRAINT users_pk_id PRIMARY KEY AUTOINCREMENT, " +
                "email TEXT NOT NULL CONSTRAINT users_unique_email UNIQUE, " +
                "password TEXT NOT NULL, " +
                "username TEXT NOT NULL, " +
                "isAdmin BOOLEAN NOT NULL DEFAULT FALSE," +
                "nif INTEGER NOT NULL CONSTRAINT users_unique_nif UNIQUE)");

        statement.executeUpdate("CREATE TABLE events " +
                "(id INTEGER CONSTRAINT events_pk_id PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "date TEXT NOT NULL, " +
                "startTime TEXT NOT NULL, " +
                "durationMinutes INTEGER NOT NULL, " +
                "location TEXT NOT NULL, " +
                "owner INTEGER NOT NULL, " +
                "FOREIGN KEY (owner) REFERENCES users(id))");

        statement.executeUpdate("CREATE TABLE presences" +
                "(id INTEGER CONSTRAINT presences_pk_id PRIMARY KEY AUTOINCREMENT, " +
                "date TEXT NOT NULL, " +
                "user INTEGER NOT NULL, " +
                "event INTEGER NOT NULL, " +
                "FOREIGN KEY (user) REFERENCES users(id), " +
                "FOREIGN KEY (event) REFERENCES events(id))");

        statement.executeUpdate("CREATE TABLE codes " +
                "(id INTEGER CONSTRAINT codes_pk_id PRIMARY KEY AUTOINCREMENT, " +
                "code TEXT NOT NULL CONSTRAINT codes_unique_code UNIQUE, " +
                "date TEXT NOT NULL, " +
                "durationMinutes INTEGER NOT NULL, " +
                "event INTEGER NOT NULL, " +
                "owner INTEGER NOT NULL, " +
                "FOREIGN KEY (event) REFERENCES events(id), " +
                "FOREIGN KEY (owner) REFERENCES users(id))");

        // Create root user
        //
        PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO users (email, password, username, nif, isAdmin) VALUES (?, ?, ?, ?, ?)");
        preparedStatement.setString(1, "root@cmi.pt");
        preparedStatement.setString(2, "root");
        preparedStatement.setString(3, "root");
        preparedStatement.setInt(4, 123456789);
        preparedStatement.setBoolean(5, true);
        preparedStatement.executeUpdate();
    }

    public int getVersion() throws SQLException
    {
        synchronized (lock)
        {
            if (null == this.connection)
                throw new RuntimeException("Database not connected");

            Statement statement = connection.createStatement();
            statement.closeOnCompletion();
            ResultSet result = statement.executeQuery("PRAGMA user_version");

            if (result.next())
                return result.getInt("user_version");

            return 0;
        }
    }

    void incrementVersion() throws SQLException
    {
        synchronized (lock)
        {
            if (null == this.connection)
                throw new RuntimeException("Database not connected");

            int version = getVersion();

            Statement statement = connection.createStatement();
            statement.closeOnCompletion();
            statement.executeUpdate("PRAGMA user_version = " + (version + 1));
        }
    }

    public boolean start()
    {
        synchronized (lock)
        {
            if (null != this.connection)
                throw new RuntimeException("Database already connected");

            if (!fileExists())
            {
                try
                {
                    createDatabase();
                } catch (SQLException e)
                {
                    System.out.println("[ DatabaseConnector ] Error creating database: " + e.getClass() + ": " + e.getMessage());
                    return false;
                }
            }

            try
            {
                this.connection = DriverManager.getConnection(jdbcPath);
                return true;
            } catch (SQLException e)
            {
                System.out.println("[ DatabaseConnector ] Error connecting to database: " + e.getClass() + ": " + e.getMessage());
            }
            return false;
        }
    }

    public void executeStatementClone(String statement, List<Object> fieldValues) throws SQLException
    {
        synchronized (lock)
        {
            if (null == this.connection)
                throw new RuntimeException("Database not connected");

            PreparedStatement preparedStatement = connection.prepareStatement(statement);
            for (int i = 0; i < fieldValues.size(); i++)
                preparedStatement.setObject(i + 1, fieldValues.get(i));
            preparedStatement.executeUpdate();

            incrementVersion();
        }
    }

    public ResultSet getTableEntryByFields(String tableName, List<String> fieldsName, List<Object> fieldsValue) throws SQLException
    {
        synchronized (lock)
        {
            if (null == this.connection)
                throw new RuntimeException("Database not connected");
        }

        // not synchronized because query is not an operation that changes the database
        //
        String query = StatementHelper.buildSelectQuery(tableName, fieldsName);
        HookedStatement statement = prepareStatement(query, fieldsValue);
        return statement.executeQuery();
    }

    public ResultSet getTableEntryByField(String tableName, String fieldName, Object fieldValue) throws SQLException
    {
        return getTableEntryByFields(tableName, List.of(fieldName), List.of(fieldValue));
    }

    public void createTableEntry(String tableName, List<String> fieldsName, List<Object> fieldsValue) throws SQLException
    {
        synchronized (lock)
        {
            if (null == this.connection)
                throw new RuntimeException("Database not connected");

            String query = StatementHelper.buildInsertQuery(tableName, fieldsName);
            prepareStatement(query, fieldsValue).executeUpdate();
        }
    }

    public void updateTableEntry(String tableName, int id, List<String> fieldsName, List<Object> fieldsValue) throws SQLException
    {
        synchronized (lock)
        {
            if (null == this.connection)
                throw new RuntimeException("Database not connected");

            String query = StatementHelper.buildUpdateQuery("id", tableName, fieldsName);
            HookedStatement statement = prepareStatement(query, fieldsValue);
            statement.setObject(fieldsValue.size() + 1, id);
            statement.executeUpdate();
        }
    }

    public void deleteTableEntryByFields(String tableName, List<String> fieldsName, List<Object> fieldsValue) throws SQLException
    {
        synchronized (lock)
        {
            if (null == this.connection)
                throw new RuntimeException("Database not connected");

            String query = StatementHelper.buildDeleteQuery(tableName, fieldsName);
            HookedStatement statement = prepareStatement(query, fieldsValue);
            statement.executeUpdate();
        }
    }

    public void deleteTableEntryByField(String tableName, String fieldName, Object fieldValue) throws SQLException
    {
        deleteTableEntryByFields(tableName, List.of(fieldName), List.of(fieldValue));
    }

    public void deleteTableEntry(String tableName, int id) throws SQLException
    {
        deleteTableEntryByField(tableName, "id", id);
    }

    public void close()
    {
        synchronized (lock)
        {
            if (null == this.connection)
                return;

            try
            {
                connection.close();
            } catch (SQLException e)
            {
                System.out.println("[ DatabaseConnector ] Error closing database connection: " + e.getClass() + ": " + e.getMessage());
            }
            connection = null;
        }
    }

    public byte[] getDatabase()
    {
        synchronized (lock)
        {
            if (null == this.connection)
            {
                System.out.println("[ DatabaseConnector ] Database not connected");
                return null;
            }

            try
            {
                File file = new File(jdbcPath.substring(12));
                if (!file.exists())
                {
                    System.out.println("[ DatabaseConnector ] Database file does not exist");
                    return null;
                }

                return Files.readAllBytes(file.toPath());
            } catch (IOException e)
            {
                System.out.println("[ DatabaseConnector ] Error reading database file: " + e.getClass() + ": " + e.getMessage());
                return null;
            }
        }
    }

    public static void saveDatabase(String databasePath, byte[] dbData)
    {
        try
        {
            File file = new File(databasePath);
            if (!file.exists())
            {
                if (!file.createNewFile())
                    throw new RuntimeException("Failed to create database file");
            }

            Files.write(file.toPath(), dbData);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private final List<UpdateCallback> updateCallbacks = new ArrayList<>();

    public void addOnDatabaseUpdate(UpdateCallback function)
    {
        synchronized (lock)
        {
            updateCallbacks.add(function);
        }
    }

    public void removeOnDatabaseUpdate(UpdateCallback function)
    {
        synchronized (lock)
        {
            updateCallbacks.remove(function);
        }
    }

    void processUpdateCallbacks(String query, List<Object> fieldValues)
    {
        synchronized (lock)
        {
            for (UpdateCallback function : updateCallbacks)
                function.apply(query, fieldValues);
        }
    }

    private HookedStatement prepareStatement(String query, List<Object> fieldsValue) throws SQLException
    {
        HookedStatement statement = new HookedStatement(this, connection.prepareStatement(query));

        for (int i = 0; i < fieldsValue.size(); i++)
            statement.setObject(i + 1, fieldsValue.get(i));

        return statement;
    }
}