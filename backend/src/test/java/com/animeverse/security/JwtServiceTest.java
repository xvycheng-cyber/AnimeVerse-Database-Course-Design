package com.animeverse.security;

import com.animeverse.config.JwtProperties;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            new JwtProperties("test-secret-that-is-longer-than-thirty-two-bytes", 60)
    );

    @Test
    void shouldIssueAccessTokenWithCurrentPrincipalIdentity() {
        AuthenticatedUser principal = new AuthenticatedUser(
                42L,
                "sakura",
                "hash",
                "Sakura",
                true,
                Set.of("USER")
        );

        String token = jwtService.generateAccessToken(principal);

        assertThat(jwtService.extractUsername(token)).isEqualTo("sakura");
        assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
        assertThat(jwtService.parseClaims(token).get("type")).isEqualTo("access");
    }

    @Test
    void shouldRejectTamperedToken() {
        AuthenticatedUser principal = new AuthenticatedUser(
                1L,
                "admin",
                "hash",
                "Admin",
                true,
                Set.of("ADMIN")
        );
        String token = jwtService.generateAccessToken(principal);

        assertThatThrownBy(() -> jwtService.extractUsername(token + "tampered"))
                .isInstanceOf(JwtException.class);
    }
}
