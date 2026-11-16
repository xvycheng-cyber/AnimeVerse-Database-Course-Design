package com.animeverse.mapper;

import com.animeverse.entity.AnimeReview;
import com.animeverse.vo.ReviewVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface AnimeReviewMapper extends BaseMapper<AnimeReview> {

    @Select("""
            <script>
            SELECT r.id, r.user_id, u.username, u.nickname, u.avatar_url,
                   r.anime_id, a.title AS anime_title, r.rating, r.content, r.status,
                   r.created_at, r.updated_at
            FROM anime_review r
            JOIN sys_user u ON u.id = r.user_id
            JOIN anime a ON a.id = r.anime_id
            WHERE 1 = 1
            <if test="animeId != null">AND r.anime_id = #{animeId}</if>
            <if test="userId != null">AND r.user_id = #{userId}</if>
            <if test="keyword != null and keyword != ''">
              AND (u.username LIKE CONCAT('%', #{keyword}, '%')
                   OR u.nickname LIKE CONCAT('%', #{keyword}, '%')
                   OR a.title LIKE CONCAT('%', #{keyword}, '%')
                   OR r.content LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="status != null">AND r.status = #{status}</if>
            <if test="!includeHidden">AND r.status = 1</if>
            ORDER BY r.created_at DESC, r.id DESC
            </script>
            """)
    IPage<ReviewVO> selectReviewPage(
            Page<ReviewVO> page,
            @Param("animeId") Long animeId,
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            @Param("status") Integer status,
            @Param("includeHidden") boolean includeHidden
    );

    @Select("""
            SELECT r.id, r.user_id, u.username, u.nickname, u.avatar_url,
                   r.anime_id, a.title AS anime_title, r.rating, r.content, r.status,
                   r.created_at, r.updated_at
            FROM anime_review r
            JOIN sys_user u ON u.id = r.user_id
            JOIN anime a ON a.id = r.anime_id
            WHERE r.id = #{reviewId}
            """)
    ReviewVO selectReviewViewById(@Param("reviewId") Long reviewId);

    @Select("SELECT COUNT(*) FROM anime_review WHERE anime_id = #{animeId}")
    long countByAnimeId(@Param("animeId") Long animeId);
}
