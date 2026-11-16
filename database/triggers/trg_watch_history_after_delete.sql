USE anime_platform;
DROP TRIGGER IF EXISTS trg_watch_history_after_delete;
DELIMITER $$
CREATE TRIGGER trg_watch_history_after_delete
AFTER DELETE ON user_watch_history
FOR EACH ROW
BEGIN
    UPDATE anime a
    JOIN anime_episode e ON e.id = OLD.episode_id
    SET a.view_count = GREATEST(a.view_count - 1, 0)
    WHERE a.id = e.anime_id;
END$$
DELIMITER ;

