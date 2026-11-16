USE anime_platform;
DROP PROCEDURE IF EXISTS sp_get_anime_ranking;
DELIMITER $$
CREATE PROCEDURE sp_get_anime_ranking(IN p_limit INT)
BEGIN
    SET p_limit = LEAST(GREATEST(COALESCE(p_limit, 10), 1), 100);

    SELECT
        RANK() OVER (
            ORDER BY a.average_rating DESC,
                     a.favorite_count DESC,
                     a.review_count DESC,
                     a.view_count DESC
        ) AS ranking,
        a.id AS anime_id,
        a.title,
        a.cover_url,
        a.release_date,
        a.status,
        s.studio_name,
        a.average_rating,
        a.favorite_count,
        a.review_count,
        a.view_count,
        fn_anime_weighted_score(a.id) AS weighted_score
    FROM anime a
    JOIN anime_studio s ON s.id = a.studio_id
    ORDER BY ranking, a.id
    LIMIT p_limit;
END$$
DELIMITER ;
