package com.animeverse.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class FavoriteVO {

    private Long animeId;
    private String title;
    private String coverUrl;
    private String status;
    private String studioName;
    private LocalDateTime createdAt;
}
