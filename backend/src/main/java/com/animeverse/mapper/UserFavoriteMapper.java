package com.animeverse.mapper;

import com.animeverse.entity.UserFavorite;
import com.animeverse.vo.FavoriteVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface UserFavoriteMapper {

    @Insert("""
            INSERT INTO user_favorite (user_id, anime_id, created_at)
            VALUES (#{userId}, #{animeId}, #{createdAt})
            """)
    int insertFavorite(UserFavorite favorite);

    @Delete("DELETE FROM user_favorite WHERE user_id = #{userId} AND anime_id = #{animeId}")
    int deleteFavorite(
            @Param("userId") Long userId,
            @Param("animeId") Long animeId
    );

    @Select("""
            SELECT a.id AS anime_id, a.title, a.cover_url, a.status,
                   s.studio_name, f.created_at
            FROM user_favorite f
            JOIN anime a ON a.id = f.anime_id
            LEFT JOIN anime_studio s ON s.id = a.studio_id
            WHERE f.user_id = #{userId}
            ORDER BY f.created_at DESC
            """)
    List<FavoriteVO> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM user_favorite WHERE user_id = #{userId} AND anime_id = #{animeId}")
    long countByUserAndAnime(@Param("userId") Long userId, @Param("animeId") Long animeId);

    @Select("SELECT COUNT(*) FROM user_favorite WHERE anime_id = #{animeId}")
    long countByAnimeId(@Param("animeId") Long animeId);
}
