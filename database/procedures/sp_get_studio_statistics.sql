USE anime_platform;
DROP PROCEDURE IF EXISTS sp_get_studio_statistics;
DELIMITER $$
CREATE PROCEDURE sp_get_studio_statistics(
    IN p_studio_id BIGINT UNSIGNED,
    OUT p_studio_name VARCHAR(120),
    OUT p_anime_count INT,
    OUT p_average_rating DECIMAL(4,2),
    OUT p_favorite_total BIGINT,
    OUT p_review_total BIGINT
)
BEGIN
    DECLARE v_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO v_exists
    FROM anime_studio
    WHERE id = p_studio_id;

    IF v_exists = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'studio does not exist';
    END IF;

    SELECT
        s.studio_name,
        COUNT(DISTINCT a.id),
        COALESCE(ROUND(AVG(CASE WHEN a.rating_count > 0 THEN a.average_rating END), 2), 0.00),
        COALESCE(SUM(a.favorite_count), 0),
        COALESCE(SUM(a.review_count), 0)
    INTO
        p_studio_name,
        p_anime_count,
        p_average_rating,
        p_favorite_total,
        p_review_total
    FROM anime_studio s
    LEFT JOIN anime a ON a.studio_id = s.id
    WHERE s.id = p_studio_id
    GROUP BY s.id, s.studio_name;
END$$
DELIMITER ;

