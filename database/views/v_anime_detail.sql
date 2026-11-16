USE anime_platform;
CREATE OR REPLACE VIEW v_anime_detail AS
SELECT
    a.id,
    a.title,
    a.title_jp,
    a.title_en,
    a.synopsis,
    a.cover_url,
    a.release_date,
    a.end_date,
    a.status,
    a.episode_count,
    a.average_rating,
    a.rating_count,
    a.favorite_count,
    a.review_count,
    a.view_count,
    s.id AS studio_id,
    s.studio_name,
    s.studio_name_en,
    COALESCE(g.genres, '') AS genres,
    COALESCE(g.genre_ids, '') AS genre_ids
FROM anime a
JOIN anime_studio s ON s.id = a.studio_id
LEFT JOIN (
    SELECT
        agr.anime_id,
        GROUP_CONCAT(DISTINCT ag.genre_name ORDER BY ag.id SEPARATOR ',') AS genres,
        GROUP_CONCAT(DISTINCT ag.id ORDER BY ag.id SEPARATOR ',') AS genre_ids
    FROM anime_genre_relation agr
    JOIN anime_genre ag ON ag.id = agr.genre_id
    GROUP BY agr.anime_id
) g ON g.anime_id = a.id;

