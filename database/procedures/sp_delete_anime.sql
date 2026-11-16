USE anime_platform;
DROP PROCEDURE IF EXISTS sp_delete_anime;
DELIMITER $$
CREATE PROCEDURE sp_delete_anime(
    IN p_anime_id BIGINT UNSIGNED,
    OUT p_deleted_rows INT
)
BEGIN
    DECLARE v_exists INT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    IF p_anime_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'anime id is required';
    END IF;

    SELECT COUNT(*) INTO v_exists
    FROM anime
    WHERE id = p_anime_id;

    IF v_exists = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'anime does not exist';
    END IF;

    START TRANSACTION;
    DELETE FROM anime WHERE id = p_anime_id;
    SET p_deleted_rows = ROW_COUNT();
    COMMIT;
END$$
DELIMITER ;

