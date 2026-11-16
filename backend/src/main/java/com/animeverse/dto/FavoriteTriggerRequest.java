package com.animeverse.dto;

import jakarta.validation.constraints.NotNull;

public record FavoriteTriggerRequest(
        @NotNull Long animeId,
        @NotNull Boolean enable
) {
}
