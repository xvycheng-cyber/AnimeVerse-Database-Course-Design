USE anime_platform;
DROP TRIGGER IF EXISTS trg_anime_episode_after_update;
DELIMITER $$
CREATE TRIGGER trg_anime_episode_after_update
AFTER UPDATE ON anime_episode
FOR EACH ROW
BEGIN
    DECLARE v_watch_count INT DEFAULT 0;

    IF OLD.anime_id <> NEW.anime_id THEN
        SELECT COUNT(*)
        INTO v_watch_count
        FROM user_watch_history
        WHERE episode_id = NEW.id;

        UPDATE anime
        SET episode_count = GREATEST(episode_count - 1, 0),
            view_count = GREATEST(view_count - v_watch_count, 0)
        WHERE id = OLD.anime_id;

        UPDATE anime
        SET episode_count = episode_count + 1,
            view_count = view_count + v_watch_count
        WHERE id = NEW.anime_id;
    END IF;
END$$
DELIMITER ;
