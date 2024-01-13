package pt.isec.cmi.rest.server.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import pt.isec.cmi.rest.server.shared.Exceptions;
import pt.isec.cmi.rest.server.components.Server;
import pt.isec.cmi.rest.server.data.User;
import pt.isec.cmi.rest.server.shared.requests.RegisterData;
import pt.isec.cmi.rest.server.security.TokenService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import static pt.isec.cmi.rest.server.database.DatabaseConnector.handleSqlConstraintException;

@RestController
public class AuthController
{
    private final Server server;
    private final TokenService tokenService;

    @Autowired
    public AuthController(TokenService tokenService, Server server)
    {
        this.server = server;
        this.tokenService = tokenService;
    }

    @PostMapping("/authenticate")
    public ResponseEntity<String> authenticate(@RequestHeader("Authorization") String authorization)
    {
        if (authorization == null || !authorization.startsWith("Basic "))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid authorization header");

        String[] credentials = new String(java.util.Base64.getDecoder().decode(authorization.substring(6))).split(":");

        if (credentials.length != 2)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid authorization header");

        String email = credentials[0];
        String password = credentials[1];

        try
        {
            User user = server.getUserByEmail(email);

            if (user == null || !user.checkPassword(password))
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");

            String token = tokenService.generateToken(user);
            return ResponseEntity.status(HttpStatus.OK).header("Authorization", "Bearer " + token).body(token);
        }
        catch (SQLException e)
        {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody() RegisterData data)
    {
        if (data == null)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid request body");

        try
        {
            User user = server.getUserByEmail(data.email());

            if (user != null)
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already registered");

            User.isValidInput(data);

            server.registerUser(data);
        }
        catch (SQLException e)
        {
            String constraintName = handleSqlConstraintException(e, "users");
            if (null != constraintName)
                return ResponseEntity.status(HttpStatus.CONFLICT).body(constraintName + " already registered");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
        catch (Exceptions.InvalidFieldValue e)
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
    }
}
