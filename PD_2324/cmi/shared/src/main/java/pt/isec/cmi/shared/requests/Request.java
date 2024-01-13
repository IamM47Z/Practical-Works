package pt.isec.cmi.shared.requests;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public class Request implements java.io.Serializable
{
    @Serial
    private static final long serialVersionUID = 11L;

    public static class Response implements java.io.Serializable
    {
        @Serial
        private static final long serialVersionUID = 12L;

        private Serializable data = null;
        private List<Serializable> dataList = null;
        private final boolean isSuccess;
        private final String errorMessage;

        public Response(String errorMessage)
        {
            this.isSuccess = false;
            this.errorMessage = errorMessage;
        }

        public Response()
        {
            this.isSuccess = true;
            this.errorMessage = null;
        }

        public boolean isSuccess()
        {
            return isSuccess;
        }

        public String getErrorMessage()
        {
            return errorMessage;
        }

        public Serializable getData()
        {
            return data;
        }

        public Response setData(Serializable data)
        {
            this.data = data;
            return this;
        }

        public List<Serializable> getDataList()
        {
            return dataList;
        }

        public Response setDataList(List<Serializable> dataList)
        {
            this.dataList = dataList;
            return this;
        }
    }

    private final RequestType type;
    private final String sessionId;

    private Serializable data = null;
    private List<Serializable> dataList = null;
    private Response response = null;

    public Request(RequestType type)
    {
        this.type = type;
        this.sessionId = null;
    }

    public Request(RequestType type, String sessionId)
    {
        this.type = type;
        this.sessionId = sessionId;
    }

    public RequestType getType()
    {
        return type;
    }

    public String getSessionId()
    {
        return sessionId;
    }

    public Response getResponse()
    {
        return response;
    }

    public Request setResponse(Response response)
    {
        this.response = response;
        return this;
    }

    public Serializable getData()
    {
        return data;
    }

    public Request setData(Serializable data)
    {
        this.data = data;
        return this;
    }

    public List<Serializable> getDataList()
    {
        return dataList;
    }

    public Request setDataList(List<Serializable> dataList)
    {
        this.dataList = dataList;
        return this;
    }
}
