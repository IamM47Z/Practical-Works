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
import pt.isec.cmi.rest.server.shared.requests.RegisterEventData;
import pt.isec.cmi.rest.server.shared.requests.filters.FilterData;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static pt.isec.cmi.rest.server.database.DatabaseConnector.handleSqlConstraintException;

@RestController
@RequestMapping("/event")
public class EventController
{
    private final Server server;
    private final TokenService tokenService;

    @Autowired
    public EventController(TokenService tokenService, Server server)
    {
        this.server = server;
        this.tokenService = tokenService;
    }

    @PostMapping("/create")
    public ResponseEntity<String> createEvent(@RequestHeader("Authorization") String authorization, @RequestBody RegisterEventData data)
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

        if (!user.isAdmin())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Insufficient privileges");

        try
        {
            server.registerEvent(user.getId(), data);
        }
        catch (SQLException e)
        {
            String constraintName = handleSqlConstraintException(e, "events");
            if (constraintName != null)
                return ResponseEntity.badRequest().body(constraintName + " already registered");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }

        return ResponseEntity.ok("Event created");
    }

    @PostMapping("/{eventId}/code/generate/{lifespanMinutes}")
    public ResponseEntity<String> generateCode(@RequestHeader("Authorization") String authorization, @PathVariable int eventId, @PathVariable int lifespanMinutes)
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

        if (!user.isAdmin())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Insufficient privileges");

        if (0 >= lifespanMinutes)
            return ResponseEntity.badRequest().body("Invalid lifespan value");

        try
        {
            Event event = server.getEventById(eventId);
            if (null == event)
                return ResponseEntity.badRequest().body("Invalid event id");

            if (!event.isHappeningAt(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)))
                return ResponseEntity.badRequest().body("Event is not happening now");
        }
        catch (SQLException e)
        {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }

        // Delete old codes
        try
        {
            List<Code> oldCodes = server.getCodesByEventId(eventId);
            if (!oldCodes.isEmpty())
                for (Code oldCode : oldCodes)
                    server.deleteCode(oldCode.getId());
        }
        catch (SQLException e)
        {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }

        String code;
        try
        {
            code = server.generateCode(eventId, user.getId(), lifespanMinutes);
        }
        catch (SQLException e)
        {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }

        return ResponseEntity.ok(code);
    }

    @GetMapping("/{eventId}/presence/list")
    public ResponseEntity<String> getPresences(@RequestHeader("Authorization") String authorization, @PathVariable int eventId)
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

        if (!user.isAdmin())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Insufficient privileges");

        List<Presence> presenceList;
        try
        {
            presenceList = server.getPresencesByEventId(eventId);
        }
        catch (SQLException e)
        {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        return ResponseEntity.ok(new Gson().toJson(presenceList.stream().map(Presence::toPresenceData)));
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

        if (!user.isAdmin())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Insufficient privileges");

        List<Event> eventlist;
        try
        {
            eventlist = server.getEventsByUserId(user.getId());
        }
        catch (SQLException e)
        {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        if (null == filtersList)
            return ResponseEntity.ok(new Gson().toJson(eventlist));

        filtersList.forEach(filter -> eventlist.removeIf(event -> !filter.matches(event.toEventData())));
        return ResponseEntity.ok(new Gson().toJson(eventlist.stream().map(Event::toEventData)));
    }

    @DeleteMapping("/{eventId}/delete")
    public ResponseEntity<String> deleteEvent(@RequestHeader("Authorization") String authorization, @PathVariable int eventId)
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

        if (!user.isAdmin())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Insufficient privileges");

        try
        {
            Event event = server.getEventById(eventId);
            if (null == event)
                return ResponseEntity.badRequest().body("Invalid event id");

            List<Presence> presences = server.getPresencesByEventId(eventId);
            if (!presences.isEmpty())
                return ResponseEntity.badRequest().body("Event has presences registered");
        }
        catch (SQLException e)
        {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }

        try
        {
            List<Code> codes = server.getCodesByEventId(eventId);
            if (!codes.isEmpty())
                for (Code code : codes)
                    server.deleteCode(code.getId());
        }
        catch (SQLException e)
        {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }

        try
        {
            server.deleteEvent(eventId);
        }
        catch (SQLException e)
        {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }

        return ResponseEntity.ok("Event deleted");
    }
}