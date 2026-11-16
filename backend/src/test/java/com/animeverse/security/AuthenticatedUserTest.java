package com.animeverse.security;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticatedUserTest {

    @Test
    void disabledAccountIsDisabledRatherThanLocked() {
        AuthenticatedUser user = new AuthenticatedUser(
                7L,
                "disabled-user",
                "hash",
                "Disabled",
                false,
                Set.of("USER")
        );

        assertThat(user.isEnabled()).isFalse();
        assertThat(user.isAccountNonLocked()).isTrue();
    }
}
