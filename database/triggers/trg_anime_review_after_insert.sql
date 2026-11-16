USE anime_platform;
DROP TRIGGER IF EXISTS trg_anime_review_after_insert;
DELIMITER $$
CREATE TRIGGER trg_anime_review_after_insert
AFTER INSERT ON anime_review
FOR EACH ROW
BEGIN
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
END$$
DELIMITER ;
