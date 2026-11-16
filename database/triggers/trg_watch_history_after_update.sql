USE anime_platform;
DROP TRIGGER IF EXISTS trg_watch_history_after_update;
DELIMITER $$
CREATE TRIGGER trg_watch_history_after_update
AFTER UPDATE ON user_watch_history
FOR EACH ROW
BEGIN
    DECLARE v_old_anime_id BIGINT UNSIGNED;
    DECLARE v_new_anime_id BIGINT UNSIGNED;

    IF OLD.episode_id <> NEW.episode_id THEN
        SELECT anime_id INTO v_old_anime_id
        FROM anime_episode
        WHERE id = OLD.episode_id;

        SELECT anime_id INTO v_new_anime_id
        FROM anime_episode
        WHERE id = NEW.episode_id;

        IF v_old_anime_id <> v_new_anime_id THEN
            UPDATE anime
            SET view_count = GREATEST(view_count - 1, 0)
            WHERE id = v_old_anime_id;

            UPDATE anime
            SET view_count = view_count + 1
            WHERE id = v_new_anime_id;
        END IF;
    END IF;
END$$
DELIMITER ;
