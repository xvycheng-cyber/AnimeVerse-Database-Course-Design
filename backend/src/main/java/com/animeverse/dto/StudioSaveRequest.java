package com.animeverse.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StudioSaveRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 160) String originalName,
        @Size(max = 64) String country,
        @Min(1900) @Max(2100) Integer foundedYear,
        @Size(max = 10000) String description,
        @Size(max = 500) String logoUrl,
        @Size(max = 500) String website,
        @Size(max = 120) String studioName,
        @Size(max = 160) String studioNameEn,
        @Size(max = 500) String websiteUrl
) {
}
