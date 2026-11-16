package com.animeverse.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UserRolesRequest(@NotEmpty List<Long> roleIds) {
}
