package com.animeverse.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record AnimeSaveRequest(
        @NotBlank @Size(max = 160) String title,
        @Size(max = 160) String originalTitle,
        @Size(max = 160) String titleJp,
        @Size(max = 160) String titleEn,
        @NotNull Long studioId,
        @Size(max = 500) String coverUrl,
        @Size(max = 5000) String synopsis,
        @Min(1900) @Max(2100) Integer releaseYear,
        LocalDate releaseDate,
        LocalDate endDate,
        @Pattern(regexp = "^(UPCOMING|AIRING|COMPLETED|ONGOING|FINISHED|PAUSED)$") String status,
        List<Long> genreIds,
        @Valid List<StaffCreditRequest> staff,
        @Valid List<PlatformLinkRequest> platforms
) {
    public record StaffCreditRequest(
            @NotNull Long staffId,
            @Size(max = 32) String roleType,
            @Size(max = 100) String roleName
    ) {
    }

    public record PlatformLinkRequest(
            @NotNull Long platformId,
            @Size(max = 500) String externalUrl,
            @Size(max = 500) String watchUrl,
            LocalDate availableFrom,
            LocalDate availableUntil
    ) {
    }
}
