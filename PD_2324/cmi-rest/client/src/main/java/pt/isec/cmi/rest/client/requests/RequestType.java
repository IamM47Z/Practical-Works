package pt.isec.cmi.rest.client.requests;

public enum RequestType
{
    LOGIN,
    REGISTER,
    USER_DETAILS,
    GET_PRESENCES,
    ADD_PRESENCE,

    CREATE_EVENT,
    GENERATE_CODE,
    GET_EVENT_PRESENCES,
    GET_EVENTS,
    REMOVE_EVENT;

    public String getEndpoint(Object... args)
    {
        return switch (this)
        {
            case LOGIN -> "/authenticate";
            case REGISTER -> "/register";
            case USER_DETAILS -> "/user/details";
            case GET_PRESENCES -> "/presence/list";
            case ADD_PRESENCE -> "/presence/register/%s".formatted(args[0]);

            case CREATE_EVENT -> "/event/create";
            case GENERATE_CODE -> "/event/%s/code/generate/%s".formatted(args[0], args[1]);
            case GET_EVENT_PRESENCES -> "/event/%s/presence/list".formatted(args[0]);
            case GET_EVENTS -> "/event/list";
            case REMOVE_EVENT -> "/event/%s/delete".formatted(args[0]);
        };
    }

    public String getMethod()
    {
        return switch (this)
        {
            case LOGIN, REGISTER, ADD_PRESENCE, CREATE_EVENT, GENERATE_CODE -> "POST";
            case USER_DETAILS, GET_PRESENCES, GET_EVENT_PRESENCES, GET_EVENTS -> "GET";
            case REMOVE_EVENT -> "DELETE";
        };
    }

    public boolean requiresAuth()
    {
        return switch (this)
        {
            case LOGIN, REGISTER -> false;
            case USER_DETAILS, GET_PRESENCES, ADD_PRESENCE, CREATE_EVENT, GENERATE_CODE, GET_EVENT_PRESENCES, GET_EVENTS, REMOVE_EVENT -> true;
        };
    }
}
