package pt.isec.cmi.rest.server.database;

import com.google.gson.Gson;
import org.springframework.security.crypto.bcrypt.BCrypt;
import pt.isec.cmi.rest.server.shared.StatementHelper;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.sqlite.SQLiteErrorCode.SQLITE_CONSTRAINT;
import static org.sqlite.SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE;

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
        preparedStatement.setString(2, BCrypt.hashpw("root", BCrypt.gensalt()));
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
            {
                if (fieldValues.get(i) instanceof LocalDateTime)
                    preparedStatement.setString(i + 1, new Gson().toJson(fieldValues.get(i), LocalDateTime.class));
                else
                    preparedStatement.setObject(i + 1, fieldValues.get(i));
            }
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
        PreparedStatement statement = prepareStatement(query, fieldsValue);
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
            PreparedStatement statement = prepareStatement(query, fieldsValue);
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
            PreparedStatement statement = prepareStatement(query, fieldsValue);
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

    private PreparedStatement prepareStatement(String query, List<Object> fieldsValue) throws SQLException
    {
        PreparedStatement statement = connection.prepareStatement(query);

        for (int i = 0; i < fieldsValue.size(); i++)
        {
            if (fieldsValue.get(i) instanceof LocalDateTime)
                statement.setString(i + 1, new Gson().toJson(fieldsValue.get(i), LocalDateTime.class));
            else
                statement.setObject(i + 1, fieldsValue.get(i));
        }

        return statement;
    }

    public static String handleSqlConstraintException(SQLException e, String tableName)
    {

        if (e.getErrorCode() == SQLITE_CONSTRAINT_UNIQUE.code || e.getErrorCode() == SQLITE_CONSTRAINT.code)
        {
            String constraintName = e.getMessage().split(tableName + ".")[1];
            return constraintName.substring(0, constraintName.length() - 1);
        }

        return null;
    }
}