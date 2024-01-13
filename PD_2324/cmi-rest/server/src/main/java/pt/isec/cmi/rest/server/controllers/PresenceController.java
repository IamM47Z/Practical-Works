package pt.isec.cmi.rest.server.controllers;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.isec.cmi.rest.server.components.Server;
import pt.isec.cmi.rest.server.data.Code;
import pt.isec.cmi.rest.server.data.Event;
import pt.isec.cmi.rest.server.data.Presence;
import pt.isec.cmi.rest.server.data.User;
import pt.isec.cmi.rest.server.security.TokenService;
import pt.isec.cmi.rest.server.shared.data.EventData;
import pt.isec.cmi.rest.server.shared.requests.filters.FilterData;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static pt.isec.cmi.rest.server.database.DatabaseConnector.handleSqlConstraintException;

@RestController
@RequestMapping("/presence")
public class PresenceController
{
    private final Server server;
    private final TokenService tokenService;

    @Autowired
    public PresenceController(TokenService tokenService, Server server)
    {
        this.server = server;
        this.tokenService = tokenService;
    }

    @PostMapping("/register/{eventCode}")
    public ResponseEntity<String> addCode(@RequestHeader("Authorization") String authorization, @PathVariable String eventCode)
    {
        String userEmail = tokenService.getUserEmailFromAuthorization(authorization);
        if (userEmail == null)
            return ResponseEntity.badRequest().body("Invalid authorization token");

        User user;
        try
        {
            user = server.getUserByEmail(userEmail);
        }
        catch (Exception e)
        {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }

        if (user == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid authorization token");

        try
        {
            LocalDateTime registrationTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

            // Get code from a database
            //
            Code code = server.getCode(eventCode);
            if (null == code)
                return ResponseEntity.badRequest().body("Invalid event code");

            // Check if code is active
            //
            LocalDateTime codeStartTime = code.getStartTime();
            LocalDateTime codeEndTime = codeStartTime.plusMinutes(code.getDurationMinutes());

            if (registrationTime.isBefore(codeStartTime) || registrationTime.isAfter(codeEndTime))
                return ResponseEntity.badRequest().body("Event code is not active");

            // Check if event exists
            //
            Event event = server.getEventById(code.getEvent().getId());
            if (null == event)
                return ResponseEntity.badRequest().body("Event does not exist");

            // Check if user is already registered in any event at the same time
            //
            List<Presence> presenceList = server.getPresencesByUserId(user.getId());
            if (null != presenceList)
            {
                for (Presence presence : presenceList)
                {
                    Event presenceEvent = server.getEventById(presence.getEvent().getId());
                    if (null == presenceEvent)
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");

                    LocalDateTime startTime = event.getDate().atTime(event.getStartTime());
                    LocalDateTime endTime = startTime.plusMinutes(event.getDurationMinutes());

                    if (startTime.isBefore(codeStartTime) && endTime.isAfter(codeStartTime))
                        return ResponseEntity.badRequest().body("User is already registered in another event at the same time");
                }
            }

            // Register presence
            //
            server.registerPresence(user.getId(), event.getId(), registrationTime);
        }
        catch (SQLException e)
        {
            String constraintName = handleSqlConstraintException(e, "presences");
            if (null != constraintName)
                return ResponseEntity.badRequest().body("User is already registered in this event");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }

        return ResponseEntity.ok("Presence registered");
    }

    @GetMapping("/list")
    public ResponseEntity<String> getList(@RequestHeader("Authorization") String authorization, @RequestBody(required = false) List<FilterData> filtersList)
    {
        String userEmail = tokenService.getUserEmailFromAuthorization(authorization);
        if (userEmail == null)
            return ResponseEntity.badRequest().body(null);

        User user;
        try
        {
            user = server.getUserByEmail(userEmail);
        } catch (Exception e)
        {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        if (user == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);

        List<Presence> presenceList;
        try
        {
            presenceList = server.getPresencesByUserId(user.getId());
        } catch (SQLException e)
        {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        if (null == filtersList)
            return ResponseEntity.ok(new Gson().toJson(presenceList));

        filtersList.forEach((filter) -> presenceList.removeIf(presence ->
        {
            EventData eventData = presence.getEvent().toEventData();
            return !filter.matches(eventData);
        }));

        return ResponseEntity.ok(new Gson().toJson(presenceList.stream().map(Presence::toPresenceData)));
    }
}
