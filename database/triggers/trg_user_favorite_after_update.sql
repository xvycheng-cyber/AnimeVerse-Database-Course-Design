USE anime_platform;
DROP TRIGGER IF EXISTS trg_user_favorite_after_update;
DELIMITER $$
CREATE TRIGGER trg_user_favorite_after_update
AFTER UPDATE ON user_favorite
FOR EACH ROW
BEGIN
    IF OLD.anime_id <> NEW.anime_id THEN
        UPDATE anime
        SET favorite_count = GREATEST(favorite_count - 1, 0)
        WHERE id = OLD.anime_id;

        UPDATE anime
        SET favorite_count = favorite_count + 1
        WHERE id = NEW.anime_id;
    END IF;
END$$
DELIMITER ;
