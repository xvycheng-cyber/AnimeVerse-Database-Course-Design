USE anime_platform;
DROP FUNCTION IF EXISTS fn_visible_review_average;
DELIMITER $$
CREATE FUNCTION fn_visible_review_average(p_anime_id BIGINT UNSIGNED)
RETURNS DECIMAL(4,2)
READS SQL DATA
BEGIN
    DECLARE v_average DECIMAL(4,2) DEFAULT 0.00;

    SELECT COALESCE(ROUND(AVG(rating), 2), 0.00)
    INTO v_average
    FROM anime_review
    WHERE anime_id = p_anime_id
      AND status = 1;

    RETURN v_average;
END$$
DELIMITER ;
