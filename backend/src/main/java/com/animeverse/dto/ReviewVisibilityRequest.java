package com.animeverse.dto;

import jakarta.validation.constraints.NotNull;

public record ReviewVisibilityRequest(@NotNull Boolean hidden) {
}
