package com.animeverse.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record EpisodeSaveRequest(
        @NotNull Long animeId,
        @NotNull @Min(1) Integer episodeNumber,
        @NotBlank @Size(max = 160) String title,
        @Size(max = 5000) String synopsis,
        @Size(max = 5000) String description,
        @Min(60) @Max(36000) Integer durationSeconds,
        @Min(1) @Max(600) Integer durationMinutes,
        @Size(max = 500) String videoUrl,
        LocalDate airDate,
        LocalDate releaseDate
) {
}
