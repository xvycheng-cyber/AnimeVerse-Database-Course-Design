USE anime_platform;
DROP TRIGGER IF EXISTS trg_user_favorite_after_insert;
DELIMITER $$
CREATE TRIGGER trg_user_favorite_after_insert
AFTER INSERT ON user_favorite
FOR EACH ROW
BEGIN
    UPDATE anime
    SET favorite_count = favorite_count + 1
    WHERE id = NEW.anime_id;
END$$
DELIMITER ;

