package com.animeverse.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class WatchHistoryVO {

    private Long id;
    private Long animeId;
    private String animeTitle;
    private String coverUrl;
    private Long episodeId;
    private Integer episodeNumber;
    private String episodeTitle;
    private Integer progressSeconds;
    private Integer durationSeconds;
    private Boolean completed;
    private LocalDateTime lastWatchedAt;

    public LocalDateTime getWatchedAt() {
        return lastWatchedAt;
    }
}
