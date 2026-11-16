USE anime_platform;
DROP FUNCTION IF EXISTS fn_user_watch_minutes;
DELIMITER $$
CREATE FUNCTION fn_user_watch_minutes(p_user_id BIGINT UNSIGNED)
RETURNS INT UNSIGNED
READS SQL DATA
BEGIN
    DECLARE v_minutes INT UNSIGNED DEFAULT 0;

    SELECT COALESCE(FLOOR(SUM(progress_seconds) / 60), 0)
    INTO v_minutes
    FROM user_watch_history
    WHERE user_id = p_user_id;

    RETURN v_minutes;
END$$
DELIMITER ;

