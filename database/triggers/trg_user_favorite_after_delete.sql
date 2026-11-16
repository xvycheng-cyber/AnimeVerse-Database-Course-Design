USE anime_platform;
DROP TRIGGER IF EXISTS trg_user_favorite_after_delete;
DELIMITER $$
CREATE TRIGGER trg_user_favorite_after_delete
AFTER DELETE ON user_favorite
FOR EACH ROW
BEGIN
    UPDATE anime
    SET favorite_count = GREATEST(favorite_count - 1, 0)
    WHERE id = OLD.anime_id;
END$$
DELIMITER ;

