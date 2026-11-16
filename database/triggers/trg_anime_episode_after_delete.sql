USE anime_platform;
DROP TRIGGER IF EXISTS trg_anime_episode_after_delete;
DELIMITER $$
CREATE TRIGGER trg_anime_episode_after_delete
AFTER DELETE ON anime_episode
FOR EACH ROW
BEGIN
    UPDATE anime
    SET episode_count = GREATEST(episode_count - 1, 0)
    WHERE id = OLD.anime_id;
END$$
DELIMITER ;

