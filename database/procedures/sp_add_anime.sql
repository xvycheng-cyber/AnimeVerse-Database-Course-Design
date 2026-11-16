USE anime_platform;
DROP PROCEDURE IF EXISTS sp_add_anime;
DELIMITER $$
CREATE PROCEDURE sp_add_anime(
    IN p_title VARCHAR(160),
    IN p_title_jp VARCHAR(160),
    IN p_title_en VARCHAR(160),
    IN p_studio_id BIGINT UNSIGNED,
    IN p_synopsis TEXT,
    IN p_cover_url VARCHAR(500),
    IN p_release_date DATE,
    IN p_status VARCHAR(20),
    OUT p_anime_id BIGINT UNSIGNED
)
BEGIN
    DECLARE v_studio_count INT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    IF p_title IS NULL OR CHAR_LENGTH(TRIM(p_title)) = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'anime title must not be empty';
    END IF;

    IF p_studio_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'studio id is required';
    END IF;

    SELECT COUNT(*) INTO v_studio_count
    FROM anime_studio
    WHERE id = p_studio_id;

    IF v_studio_count = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'studio does not exist';
    END IF;

    IF p_status IS NULL
       OR p_status NOT IN ('UPCOMING', 'AIRING', 'COMPLETED') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'invalid anime status';
    END IF;

    START TRANSACTION;

    INSERT INTO anime (
        title, title_jp, title_en, studio_id, synopsis, cover_url,
        release_date, status
    )
    VALUES (
        TRIM(p_title), NULLIF(TRIM(p_title_jp), ''), NULLIF(TRIM(p_title_en), ''),
        p_studio_id, NULLIF(TRIM(p_synopsis), ''), NULLIF(TRIM(p_cover_url), ''),
        p_release_date, p_status
    );

    SET p_anime_id = LAST_INSERT_ID();
    COMMIT;
END$$
DELIMITER ;

