package com.animeverse.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @NotBlank @Size(max = 80) String nickname,
        @Email @Size(max = 255) String email,
        @Size(max = 500) String avatarUrl
) {
}
