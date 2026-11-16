USE anime_platform;
DROP PROCEDURE IF EXISTS sp_generate_studio_report;
DELIMITER $$
CREATE PROCEDURE sp_generate_studio_report()
BEGIN
    DECLARE v_done INT DEFAULT FALSE;
    DECLARE v_studio_name VARCHAR(120);
    DECLARE v_anime_count INT;
    DECLARE v_average_rating DECIMAL(4,2);
    DECLARE v_favorite_total BIGINT;

    DECLARE studio_cursor CURSOR FOR
        SELECT
            s.studio_name,
            COUNT(DISTINCT a.id),
            COALESCE(ROUND(AVG(CASE WHEN a.rating_count > 0 THEN a.average_rating END), 2), 0.00),
            COALESCE(SUM(a.favorite_count), 0)
        FROM anime_studio s
        LEFT JOIN anime a ON a.studio_id = s.id
        GROUP BY s.id, s.studio_name
        ORDER BY s.id;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = TRUE;

    DROP TEMPORARY TABLE IF EXISTS tmp_studio_report;
    CREATE TEMPORARY TABLE tmp_studio_report (
        studio_name VARCHAR(120) NOT NULL,
        anime_count INT NOT NULL,
        average_rating DECIMAL(4,2) NOT NULL,
        favorite_total BIGINT NOT NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

    OPEN studio_cursor;

    read_loop: LOOP
        FETCH studio_cursor
        INTO v_studio_name, v_anime_count, v_average_rating, v_favorite_total;

        IF v_done THEN
            LEAVE read_loop;
        END IF;

        INSERT INTO tmp_studio_report (
            studio_name, anime_count, average_rating, favorite_total
        )
        VALUES (
            v_studio_name, v_anime_count, v_average_rating, v_favorite_total
        );
    END LOOP;

    CLOSE studio_cursor;

    SELECT studio_name, anime_count, average_rating, favorite_total
    FROM tmp_studio_report
    ORDER BY anime_count DESC, studio_name;
END$$
DELIMITER ;

