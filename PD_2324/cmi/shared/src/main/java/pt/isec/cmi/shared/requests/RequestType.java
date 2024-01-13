package pt.isec.cmi.shared.requests;

public enum RequestType
{
    NONE,

    // reverse requests
    //
    STOP,
    KICK,
    DATABASE_UPDATE,

    // regular requests
    //
    LOGIN,
    REGISTER,
    FREE_SESSION,
    START_SESSION,
    GET_QUERY_CSV,

    // requests for logged-in users
    //
    LOGOUT,
    ADD_PRESENCE,
    EDIT_PROFILE,
    GET_PRESENCES,

    // requests for admins
    //
    EDIT_EVENT,
    CREATE_EVENT,
    REMOVE_EVENT,
    ADD_PRESENCES,
    GENERATE_CODE,
    GET_EVENTS,
    GET_EVENT_PRESENCES,
    GET_USER_PRESENCES,
    REMOVE_PRESENCES
}
