package pt.isec.cmi.client.console.commands.console.commands;

import pt.isec.cmi.client.console.commands.Client;
import pt.isec.cmi.client.console.commands.SessionManager;
import pt.isec.cmi.client.console.commands.console.Command;
import pt.isec.cmi.client.console.commands.console.CommandBase;
import pt.isec.cmi.shared.requests.Request;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static pt.isec.cmi.shared.requests.RequestType.GET_QUERY_CSV;

@Command(name = "getQueryCsv", usage = "getQueryCsv <filePath>", description = "gets a csv file with the last query results", requestType = GET_QUERY_CSV)
public class GetQueryCsv extends CommandBase
{
    private String filePath;

    @Override
    public boolean isAvailable(SessionManager sessionManager)
    {
        return sessionManager.isLoggedIn();
    }

    @Override
    public void handleSuccess(SessionManager sessionManager, Request.Response response)
    {
        String csvContent = (String) response.getData();
        if (csvContent == null)
        {
            sessionManager.addToPrintQueue("Failed to get query csv");
            return;
        }

        // save csv to file
        File file = new File(filePath);
        if (!file.exists())
        {
            if (file.getParentFile() != null && !file.getParentFile().exists())
                file.getParentFile().mkdirs();

            try
            {
                file.createNewFile();
            }
            catch (Exception e)
            {
                sessionManager.addToPrintQueue("Failed to create file");
                return;
            }
        }

        try
        {
            Files.writeString(file.toPath(), csvContent);
        }
        catch (IOException e)
        {
            sessionManager.addToPrintQueue("Failed to write to file");
            return;
        }

        sessionManager.addToPrintQueue("Query csv saved to " + filePath);
    }

    @Override
    public void execute(SessionManager sessionManager, String[] args)
    {
        Client client = (Client) sessionManager;

        if (args.length != 2)
        {
            sessionManager.addToPrintQueue("Usage: " + getUsage());
            return;
        }

        filePath = args[1] + ".csv";

        File file = new File(filePath);
        if (file.exists())
        {
            sessionManager.addToPrintQueue("File already exists");
            return;
        }

        if (!client.sendRequest(client.createRequest(GET_QUERY_CSV)))
            sessionManager.addToPrintQueue("Failed to get query csv");
    }
}
