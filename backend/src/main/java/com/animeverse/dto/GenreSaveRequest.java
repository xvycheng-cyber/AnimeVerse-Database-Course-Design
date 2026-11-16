package com.animeverse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GenreSaveRequest(
        @NotBlank @Size(max = 40) String name,
        @Size(max = 50) String slug,
        @Size(max = 255) String description
) {
}
