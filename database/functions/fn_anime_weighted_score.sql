USE anime_platform;
DROP FUNCTION IF EXISTS fn_anime_weighted_score;
DELIMITER $$
CREATE FUNCTION fn_anime_weighted_score(p_anime_id BIGINT UNSIGNED)
RETURNS DECIMAL(6,2)
READS SQL DATA
BEGIN
    DECLARE v_score DECIMAL(6,2);

    SELECT ROUND(
               COALESCE(average_rating, 0.00) * 0.70
               + LEAST(COALESCE(favorite_count, 0), 100) * 0.20
               + LEAST(COALESCE(review_count, 0), 100) * 0.10,
               2
           )
    INTO v_score
    FROM anime
    WHERE id = p_anime_id;

    RETURN v_score;
END$$
DELIMITER ;

