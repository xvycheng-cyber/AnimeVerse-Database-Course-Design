package com.animeverse.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class ReviewVO {

    private Long id;
    private Long userId;
    private String username;
    private String nickname;
    private String avatarUrl;
    private Long animeId;
    private String animeTitle;
    private Integer rating;
    private String content;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
