package com.animeverse.vo;

import com.animeverse.entity.AnimeStaff;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record StaffVO(
        Long id,
        String name,
        String staffName,
        String staffNameJp,
        String originalName,
        String nationality,
        String country,
        LocalDate birthDate,
        String biography,
        String bio,
        String avatarUrl,
        LocalDateTime createdAt
) {
    public static StaffVO from(AnimeStaff staff) {
        return new StaffVO(
                staff.getId(),
                staff.getStaffName(),
                staff.getStaffName(),
                staff.getStaffNameJp(),
                staff.getStaffNameJp(),
                staff.getCountry(),
                staff.getCountry(),
                staff.getBirthDate(),
                staff.getBio(),
                staff.getBio(),
                staff.getAvatarUrl(),
                staff.getCreatedAt()
        );
    }
}
