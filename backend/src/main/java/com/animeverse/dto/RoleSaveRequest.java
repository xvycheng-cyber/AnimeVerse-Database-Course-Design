package com.animeverse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RoleSaveRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,31}$")
        String roleCode,
        @NotBlank @Size(max = 50) String roleName,
        @Size(max = 500) String description
) {
}
