package com.animeverse.security;

import com.animeverse.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private static final String ISSUER = "animeverse";
    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_USER_ID = "uid";

    private final SecretKey signingKey;
    private final long expirationMillis;

    public JwtService(JwtProperties properties) {
        if (properties.secret() == null || properties.secret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must contain at least 32 UTF-8 bytes");
        }
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = properties.expirationMinutes() * 60_000L;
    }

    public String generateAccessToken(AuthenticatedUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(user.username())
                .claim(CLAIM_USER_ID, user.id())
                .claim(CLAIM_TYPE, "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMillis)))
                .signWith(signingKey)
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        Object value = parseClaims(token).get(CLAIM_USER_ID);
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("Token is missing a numeric user id");
        }
        return number.longValue();
    }

    public Claims parseClaims(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (!"access".equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new IllegalArgumentException("Not an access token");
        }
        return claims;
    }
}
