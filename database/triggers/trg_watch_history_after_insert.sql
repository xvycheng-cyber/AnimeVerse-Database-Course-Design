USE anime_platform;
DROP TRIGGER IF EXISTS trg_watch_history_after_insert;
DELIMITER $$
CREATE TRIGGER trg_watch_history_after_insert
AFTER INSERT ON user_watch_history
FOR EACH ROW
BEGIN
    UPDATE anime a
    JOIN anime_episode e ON e.id = NEW.episode_id
    SET a.view_count = a.view_count + 1
    WHERE a.id = e.anime_id;
END$$
DELIMITER ;

