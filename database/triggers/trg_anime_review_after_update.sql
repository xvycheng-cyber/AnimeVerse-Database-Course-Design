USE anime_platform;
DROP TRIGGER IF EXISTS trg_anime_review_after_update;
DELIMITER $$
CREATE TRIGGER trg_anime_review_after_update
AFTER UPDATE ON anime_review
FOR EACH ROW
BEGIN
    IF OLD.anime_id = NEW.anime_id THEN
        UPDATE anime a
        SET a.rating_count = (
                SELECT COUNT(*)
                FROM anime_review r
                WHERE r.anime_id = a.id AND r.status = 1
            ),
            a.review_count = (
                SELECT COUNT(*)
                FROM anime_review r
                WHERE r.anime_id = a.id AND r.status = 1
            ),
            a.average_rating = COALESCE((
                SELECT ROUND(AVG(r.rating), 2)
                FROM anime_review r
                WHERE r.anime_id = a.id AND r.status = 1
            ), 0.00)
        WHERE a.id = NEW.anime_id;
    ELSE
        UPDATE anime a
        SET a.rating_count = (
                SELECT COUNT(*)
                FROM anime_review r
                WHERE r.anime_id = a.id AND r.status = 1
            ),
            a.review_count = (
                SELECT COUNT(*)
                FROM anime_review r
                WHERE r.anime_id = a.id AND r.status = 1
            ),
            a.average_rating = COALESCE((
                SELECT ROUND(AVG(r.rating), 2)
                FROM anime_review r
                WHERE r.anime_id = a.id AND r.status = 1
            ), 0.00)
        WHERE a.id IN (OLD.anime_id, NEW.anime_id);
    END IF;
END$$
DELIMITER ;
