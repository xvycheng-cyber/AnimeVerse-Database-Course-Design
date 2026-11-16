package com.animeverse.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;

public record WatchHistoryRequest(
        @NotNull Long episodeId,
        @NotNull @Min(0) @Max(36000) Integer progressSeconds,
        Boolean completed
) {
}
