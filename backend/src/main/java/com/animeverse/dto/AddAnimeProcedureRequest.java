package com.animeverse.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AddAnimeProcedureRequest(
        @NotBlank @Size(max = 160) String title,
        @Size(max = 160) String originalTitle,
        @Size(max = 160) String titleJp,
        @Size(max = 160) String titleEn,
        @NotNull Long studioId,
        @Size(max = 500) String coverUrl,
        @Size(max = 5000) String synopsis,
        @Size(max = 5000) String description,
        @Min(1900) @Max(2100) Integer releaseYear,
        LocalDate releaseDate,
        @Pattern(regexp = "^(UPCOMING|AIRING|COMPLETED|ONGOING|FINISHED|PAUSED)$") String status,
        @DecimalMin("0.0") @DecimalMax("10.0") BigDecimal rating
) {
}
