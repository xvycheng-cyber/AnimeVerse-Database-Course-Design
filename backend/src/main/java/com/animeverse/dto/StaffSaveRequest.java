package com.animeverse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record StaffSaveRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 120) String originalName,
        @Size(max = 64) String nationality,
        LocalDate birthDate,
        @Size(max = 5000) String biography,
        @Size(max = 500) String avatarUrl,
        @Size(max = 120) String staffName,
        @Size(max = 120) String staffNameJp,
        @Size(max = 64) String country,
        @Size(max = 5000) String bio,
        @Size(max = 100) String position,
        @Size(max = 20) String gender
) {
}
