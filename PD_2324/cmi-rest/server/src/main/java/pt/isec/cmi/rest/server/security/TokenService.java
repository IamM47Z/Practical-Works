package pt.isec.cmi.rest.server.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import pt.isec.cmi.rest.server.data.User;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class TokenService
{
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;

    public TokenService(JwtEncoder encoder, JwtDecoder decoder)
    {
        this.encoder = encoder;
        this.decoder = decoder;
    }

    public String generateToken(User user)
    {
        Instant now = Instant.now();

        JwtClaimsSet claimsSet = JwtClaimsSet.builder()
                .issuer("self")
                .issuedAt(now)
                .expiresAt(now.plus(5, ChronoUnit.MINUTES))
                .subject(user.getEmail())
                .build();

        return this.encoder.encode(JwtEncoderParameters.from(claimsSet)).getTokenValue();
    }

    public String getUserEmailFromAuthorization(String authorization)
    {
        if (authorization == null || !authorization.startsWith("Bearer "))
            return null;

        Jwt decodedToken = decoder.decode(authorization.substring(7));
        if (decodedToken == null)
            return null;

        Instant expiresAt = decodedToken.getExpiresAt();
        if (expiresAt == null)
            return null;

        if (expiresAt.isBefore(Instant.now()))
            return null;

        return decodedToken.getSubject();
    }
}
