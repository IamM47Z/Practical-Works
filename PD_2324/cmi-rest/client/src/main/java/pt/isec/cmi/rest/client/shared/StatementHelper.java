package pt.isec.cmi.rest.client.shared;

import java.util.List;

public class StatementHelper
{
    public static String buildSelectQuery(String tableName, List<String> fieldsName)
    {
        StringBuilder query = new StringBuilder("SELECT * FROM ").append(tableName).append(" WHERE ");
        appendFieldConditions(query, fieldsName);
        return query.toString();
    }

    public static String buildInsertQuery(String tableName, List<String> fieldsName)
    {
        StringBuilder query = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
        appendFieldNames(query, fieldsName);
        query.append(") VALUES (");
        appendValuePlaceholders(query, fieldsName.size());
        query.append(")");
        return query.toString();
    }

    public static String buildUpdateQuery(String filterName, String tableName, List<String> fieldsName)
    {
        StringBuilder query = new StringBuilder("UPDATE ").append(tableName).append(" SET ");
        appendFieldUpdateConditions(query, fieldsName);
        query.append(" WHERE ").append(filterName).append(" = ?");
        return query.toString();
    }

    public static String buildDeleteQuery(String tableName, List<String> fieldsName)
    {
        StringBuilder query = new StringBuilder("DELETE FROM ").append(tableName).append(" WHERE ");
        appendFieldConditions(query, fieldsName);
        return query.toString();
    }

    public static String removeParametersFromQuery(String queryString)
    {
        // get all the string until the line break
        //
        return queryString.substring(0, queryString.indexOf("\n"));
    }

    private static void appendFieldNames(StringBuilder query, List<String> fieldsName)
    {
        for (int i = 0; i < fieldsName.size(); i++)
        {
            String fieldName = fieldsName.get(i);

            if (fieldName == null || fieldName.isEmpty())
                throw new RuntimeException("Invalid field name");

            query.append(fieldName);

            if (i != fieldsName.size() - 1)
                query.append(", ");
        }
    }

    private static void appendFieldConditions(StringBuilder query, List<String> fieldsName)
    {
        for (int i = 0; i < fieldsName.size(); i++)
        {
            String fieldName = fieldsName.get(i);
            if (fieldName == null || fieldName.isEmpty())
                throw new RuntimeException("Invalid field name");

            query.append(fieldName).append(" = ?");

            if (i != fieldsName.size() - 1)
                query.append(" AND ");
        }
    }

    private static void appendFieldUpdateConditions(StringBuilder query, List<String> fieldsName)
    {
        for (int i = 0; i < fieldsName.size(); i++)
        {
            String fieldName = fieldsName.get(i);
            if (fieldName == null || fieldName.isEmpty())
                throw new RuntimeException("Invalid field name");

            query.append(fieldName).append(" = ?");

            if (i != fieldsName.size() - 1)
                query.append(", ");
        }
    }

    private static void appendValuePlaceholders(StringBuilder query, int count)
    {
        for (int i = 0; i < count; i++)
        {
            query.append("?");

            if (i != count - 1)
                query.append(", ");
        }
    }
}
