package com.animeverse.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Size(min = 4, max = 32)
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "只能包含字母、数字和下划线")
        String username,
        @NotBlank
        @Size(min = 8, max = 72)
        String password,
        @NotBlank
        @Size(max = 80)
        String nickname,
        @NotBlank @Email
        @Size(max = 255)
        String email
) {
}
