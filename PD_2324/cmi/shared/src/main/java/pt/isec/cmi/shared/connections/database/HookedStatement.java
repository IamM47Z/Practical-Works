package pt.isec.cmi.shared.connections.database;

import pt.isec.cmi.shared.StatementHelper;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class HookedStatement
{
    private final String query;
    private final PreparedStatement statement;
    private final DatabaseConnector databaseConnector;
    private final List<Object> fieldValues = new ArrayList<>();

    public HookedStatement(DatabaseConnector databaseConnector, PreparedStatement statement)
    {
        this.statement = statement;
        this.databaseConnector = databaseConnector;
        this.query = StatementHelper.removeParametersFromQuery(statement.toString());

        try
        {
            this.statement.closeOnCompletion();
        } catch (SQLException ignored) { }
    }

    public void setObject(int parameterIndex, Object x) throws SQLException
    {
        statement.setObject(parameterIndex, x);
        fieldValues.add(x);
    }

    public int executeUpdate() throws SQLException
    {
        // process update callbacks
        //
        databaseConnector.processUpdateCallbacks(query, fieldValues);

        // execute update
        //
        int ret = statement.executeUpdate();

        // increment database version
        //
        databaseConnector.incrementVersion();

        return ret;
    }

    public ResultSet executeQuery() throws SQLException
    {
        return statement.executeQuery();
    }
}
