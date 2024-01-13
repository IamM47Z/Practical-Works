package pt.isec.cmi.shared.connections.database;

import java.util.List;

@FunctionalInterface
public interface UpdateCallback
{
    public void apply(String query, List<Object> fieldValues);
}