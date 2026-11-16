package com.animeverse.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface StatisticsMapper {

    @Select("SELECT COUNT(*) FROM sys_user")
    long countUsers();

    @Select("SELECT COUNT(*) FROM anime")
    long countAnime();

    @Select("SELECT COUNT(*) FROM anime_studio")
    long countStudios();

    @Select("SELECT COUNT(*) FROM anime_episode")
    long countEpisodes();

    @Select("SELECT COUNT(*) FROM anime_review")
    long countReviews();

    @Select("SELECT COUNT(*) FROM user_favorite")
    long countFavorites();

    @Select("SELECT COUNT(*) FROM user_watch_history")
    long countWatchRecords();

    @Select("""
            SELECT DATE_FORMAT(created_at, '%Y-%m-%d') AS date, COUNT(*) AS total
            FROM sys_user
            WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY)
            GROUP BY DATE_FORMAT(created_at, '%Y-%m-%d')
            """)
    List<Map<String, Object>> countUserTrend(@Param("days") int days);

    @Select("""
            SELECT DATE_FORMAT(created_at, '%Y-%m-%d') AS date, COUNT(*) AS total
            FROM anime_review
            WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY)
            GROUP BY DATE_FORMAT(created_at, '%Y-%m-%d')
            """)
    List<Map<String, Object>> countReviewTrend(@Param("days") int days);

    @Select("""
            SELECT DATE_FORMAT(created_at, '%Y-%m-%d') AS date, COUNT(*) AS total
            FROM user_favorite
            WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY)
            GROUP BY DATE_FORMAT(created_at, '%Y-%m-%d')
            """)
    List<Map<String, Object>> countFavoriteTrend(@Param("days") int days);
}
