package com.animeverse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlatformSaveRequest(
        @NotBlank @Size(max = 80) String name,
        @Size(max = 32) String code,
        @Size(max = 500) String websiteUrl,
        @Size(max = 64) String country,
        Boolean active,
        @Size(max = 500) String logoUrl,
        @Size(max = 5000) String description
) {
}
