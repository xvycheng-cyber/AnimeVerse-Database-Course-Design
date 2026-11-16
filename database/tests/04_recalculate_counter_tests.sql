USE anime_platform;

START TRANSACTION;

SET @test_anime_id = (
    SELECT a.id
    FROM anime a
    CROSS JOIN sys_user u
    LEFT JOIN anime_review r
      ON r.anime_id = a.id AND r.user_id = u.id
    WHERE r.id IS NULL
    ORDER BY a.id, u.id
    LIMIT 1
);
SET @test_user_id = (
    SELECT u.id
    FROM sys_user u
    WHERE NOT EXISTS (
        SELECT 1
        FROM anime_review r
        WHERE r.anime_id = @test_anime_id AND r.user_id = u.id
    )
    ORDER BY u.id
    LIMIT 1
);

SELECT 'recalculate_test_fixture_available' AS check_name,
       CONCAT(@test_anime_id, ':', @test_user_id) AS actual_value,
       IF(@test_anime_id IS NOT NULL AND @test_user_id IS NOT NULL, 'PASS', 'FAIL') AS result;

SELECT review_count, rating_count, average_rating
INTO @before_reviews, @before_ratings, @before_average
FROM anime
WHERE id = @test_anime_id;

INSERT INTO anime_review (anime_id, user_id, rating, content, status)
VALUES (@test_anime_id, @test_user_id, 1, 'hidden counter recalculation regression', 0);

UPDATE anime a
LEFT JOIN (
    SELECT anime_id,
           COUNT(*) AS review_count,
           COUNT(*) AS rating_count,
           ROUND(AVG(rating), 2) AS average_rating
    FROM anime_review
    WHERE status = 1
    GROUP BY anime_id
) r ON r.anime_id = a.id
SET a.review_count = COALESCE(r.review_count, 0),
    a.rating_count = COALESCE(r.rating_count, 0),
    a.average_rating = COALESCE(r.average_rating, 0.00)
WHERE a.id = @test_anime_id;

SELECT 'hidden_review_excluded_from_recalculation' AS check_name,
       CONCAT(review_count, ':', rating_count, ':', average_rating) AS actual_value,
       IF(
           review_count = @before_reviews
           AND rating_count = @before_ratings
           AND average_rating = @before_average,
           'PASS',
           'FAIL'
       ) AS result
FROM anime
WHERE id = @test_anime_id;

ROLLBACK;
