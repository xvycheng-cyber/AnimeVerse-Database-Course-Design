USE anime_platform;
CREATE OR REPLACE VIEW v_anime_popularity AS
SELECT
    ranked.id,
    ranked.title,
    ranked.studio_name,
    ranked.average_rating,
    ranked.favorite_count,
    ranked.review_count,
    ranked.view_count,
    ranked.weighted_score,
    ranked.popularity_rank
FROM (
    SELECT
        a.id,
        a.title,
        s.studio_name,
        a.average_rating,
        a.favorite_count,
        a.review_count,
        a.view_count,
        ROUND(a.average_rating * 0.70
              + LEAST(a.favorite_count, 100) * 0.20
              + LEAST(a.review_count, 100) * 0.10, 2) AS weighted_score,
        RANK() OVER (
            ORDER BY a.average_rating DESC,
                     a.favorite_count DESC,
                     a.review_count DESC,
                     a.view_count DESC
        ) AS popularity_rank
    FROM anime a
    JOIN anime_studio s ON s.id = a.studio_id
) ranked;

