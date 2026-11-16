USE anime_platform;
DROP TRIGGER IF EXISTS trg_anime_episode_after_insert;
DELIMITER $$
CREATE TRIGGER trg_anime_episode_after_insert
AFTER INSERT ON anime_episode
FOR EACH ROW
BEGIN
    UPDATE anime
    SET episode_count = episode_count + 1
    WHERE id = NEW.anime_id;
END$$
DELIMITER ;

