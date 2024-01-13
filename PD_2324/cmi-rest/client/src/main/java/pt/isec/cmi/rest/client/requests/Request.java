package pt.isec.cmi.rest.client.requests;

public class Request
{
    private final Object data;
    private final RequestType type;

    public Request(RequestType type)
    {
        this.type = type;
        this.data = null;
    }

    public Request(RequestType type, Object data)
    {
        this.type = type;
        this.data = data;
    }

    public RequestType getType()
    {
        return type;
    }

    public Object getData()
    {
        return data;
    }
}
