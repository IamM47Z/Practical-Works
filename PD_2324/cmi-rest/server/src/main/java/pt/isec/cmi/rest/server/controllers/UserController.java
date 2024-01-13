package pt.isec.cmi.rest.server.controllers;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.isec.cmi.rest.server.components.Server;
import pt.isec.cmi.rest.server.data.User;
import pt.isec.cmi.rest.server.security.TokenService;

@RestController
@RequestMapping("/user")
public class UserController
{
    private final Server server;
    private final TokenService tokenService;

    @Autowired
    public UserController(TokenService tokenService, Server server)
    {
        this.server = server;
        this.tokenService = tokenService;
    }

    @GetMapping("/details")
    public ResponseEntity<String> getPrivilege(@RequestHeader("Authorization") String authorization)
    {
        String userEmail = tokenService.getUserEmailFromAuthorization(authorization);
        if (userEmail == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid authorization token");

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

        return ResponseEntity.ok(new Gson().toJson(user.toUserData()));
    }
}
