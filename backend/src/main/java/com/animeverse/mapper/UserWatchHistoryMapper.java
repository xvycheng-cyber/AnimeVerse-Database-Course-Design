package com.animeverse.mapper;

import com.animeverse.entity.UserWatchHistory;
import com.animeverse.vo.WatchHistoryVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface UserWatchHistoryMapper extends BaseMapper<UserWatchHistory> {

    @Select("""
            SELECT h.id, a.id AS anime_id, a.title AS anime_title, a.cover_url,
                   e.id AS episode_id, e.episode_number, e.title AS episode_title,
                   h.progress_seconds, e.duration_seconds, h.completed, h.last_watched_at
            FROM user_watch_history h
            JOIN anime_episode e ON e.id = h.episode_id
            JOIN anime a ON a.id = e.anime_id
            WHERE h.user_id = #{userId}
            ORDER BY h.last_watched_at DESC
            """)
    List<WatchHistoryVO> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM user_watch_history WHERE user_id = #{userId} AND episode_id = #{episodeId}")
    long countByUserAndEpisode(@Param("userId") Long userId, @Param("episodeId") Long episodeId);

    @Select("SELECT COUNT(*) FROM user_watch_history WHERE episode_id = #{episodeId}")
    long countByEpisodeId(@Param("episodeId") Long episodeId);

    @Select("""
            SELECT COUNT(*)
            FROM user_watch_history h
            JOIN anime_episode e ON e.id = h.episode_id
            WHERE e.anime_id = #{animeId}
            """)
    long countByAnimeId(@Param("animeId") Long animeId);
}
