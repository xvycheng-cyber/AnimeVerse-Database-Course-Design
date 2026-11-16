USE anime_platform;
CREATE OR REPLACE VIEW v_studio_statistics AS
SELECT
    s.id AS studio_id,
    s.studio_name,
    s.studio_name_en,
    s.founded_year,
    COUNT(DISTINCT a.id) AS anime_count,
    COALESCE(ROUND(AVG(CASE WHEN a.rating_count > 0 THEN a.average_rating END), 2), 0.00) AS average_rating,
    COALESCE(SUM(a.favorite_count), 0) AS favorite_total,
    COALESCE(SUM(a.review_count), 0) AS review_total,
    COALESCE(SUM(a.view_count), 0) AS view_total,
    MAX(a.release_date) AS latest_release_date
FROM anime_studio s
LEFT JOIN anime a ON a.studio_id = s.id
GROUP BY s.id, s.studio_name, s.studio_name_en, s.founded_year;

