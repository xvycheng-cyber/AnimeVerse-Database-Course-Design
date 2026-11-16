USE anime_platform;

-- Favorite trigger test, rolled back.
START TRANSACTION;
SELECT favorite_count INTO @fav_before FROM anime WHERE id = 30;
INSERT INTO user_favorite (user_id, anime_id) VALUES (1, 30);
SELECT 'favorite_insert_increment' AS check_name,
       favorite_count - @fav_before AS delta,
       IF(favorite_count = @fav_before + 1, 'PASS', 'FAIL') AS result
FROM anime WHERE id = 30;
DELETE FROM user_favorite WHERE user_id = 1 AND anime_id = 30;
SELECT 'favorite_delete_decrement' AS check_name,
       favorite_count AS current_count,
       IF(favorite_count = @fav_before, 'PASS', 'FAIL') AS result
FROM anime WHERE id = 30;
ROLLBACK;

-- Review insert/update/hide/delete trigger test, rolled back.
START TRANSACTION;
SELECT review_count, rating_count, average_rating
INTO @rev_before, @rating_before, @avg_before
FROM anime WHERE id = 30;

INSERT INTO anime_review (anime_id, user_id, rating, content)
VALUES (30, 1, 8, 'trigger validation review');
SET @test_review_id = LAST_INSERT_ID();
SELECT 'review_insert_increment' AS check_name,
       review_count - @rev_before AS review_delta,
       rating_count - @rating_before AS rating_delta,
       IF(review_count = @rev_before + 1 AND rating_count = @rating_before + 1, 'PASS', 'FAIL') AS result
FROM anime WHERE id = 30;

UPDATE anime_review SET rating = 10 WHERE id = @test_review_id;
SELECT 'review_update_rating' AS check_name,
       average_rating AS current_average,
       fn_visible_review_average(30) AS source_average,
       IF(average_rating = fn_visible_review_average(30), 'PASS', 'FAIL') AS result
FROM anime WHERE id = 30;

UPDATE anime_review SET rating = 7 WHERE id = @test_review_id;
UPDATE anime_review SET rating = 9 WHERE id = @test_review_id;
UPDATE anime_review SET rating = 8 WHERE id = @test_review_id;
SELECT 'review_update_no_rounding_drift' AS check_name,
       average_rating AS current_average,
       fn_visible_review_average(30) AS source_average,
       IF(average_rating = fn_visible_review_average(30), 'PASS', 'FAIL') AS result
FROM anime WHERE id = 30;

UPDATE anime_review SET status = 0 WHERE id = @test_review_id;
SELECT 'review_hide_decrement' AS check_name,
       review_count AS current_count,
       IF(review_count = @rev_before, 'PASS', 'FAIL') AS result
FROM anime WHERE id = 30;

UPDATE anime_review SET status = 1 WHERE id = @test_review_id;
SELECT 'review_restore_increment' AS check_name,
       review_count AS current_count,
       IF(review_count = @rev_before + 1, 'PASS', 'FAIL') AS result
FROM anime WHERE id = 30;

DELETE FROM anime_review WHERE id = @test_review_id;
SELECT 'review_delete_restore' AS check_name,
       review_count AS current_count,
       rating_count AS current_rating_count,
       IF(review_count = @rev_before AND rating_count = @rating_before, 'PASS', 'FAIL') AS result
FROM anime WHERE id = 30;
ROLLBACK;

-- Favorite key-change trigger test, rolled back.
START TRANSACTION;
SELECT MIN(a.id)
INTO @fav_move_a
FROM anime a
WHERE a.id <= 30
  AND NOT EXISTS (
      SELECT 1 FROM user_favorite f
      WHERE f.user_id = 1 AND f.anime_id = a.id
  );
SELECT MIN(a.id)
INTO @fav_move_b
FROM anime a
WHERE a.id <= 30
  AND a.id <> @fav_move_a
  AND NOT EXISTS (
      SELECT 1 FROM user_favorite f
      WHERE f.user_id = 1 AND f.anime_id = a.id
  );
SELECT favorite_count INTO @fav_move_a_before FROM anime WHERE id = @fav_move_a;
SELECT favorite_count INTO @fav_move_b_before FROM anime WHERE id = @fav_move_b;
INSERT INTO user_favorite (user_id, anime_id) VALUES (1, @fav_move_a);
UPDATE user_favorite
SET anime_id = @fav_move_b
WHERE user_id = 1 AND anime_id = @fav_move_a;
SELECT 'favorite_move_counters' AS check_name,
       (SELECT favorite_count FROM anime WHERE id = @fav_move_a) AS old_count,
       (SELECT favorite_count FROM anime WHERE id = @fav_move_b) AS new_count,
       IF(
           (SELECT favorite_count FROM anime WHERE id = @fav_move_a) = @fav_move_a_before
           AND (SELECT favorite_count FROM anime WHERE id = @fav_move_b) = @fav_move_b_before + 1,
           'PASS',
           'FAIL'
       ) AS result;
ROLLBACK;

-- Watch-history trigger test, rolled back.
START TRANSACTION;
SELECT view_count INTO @view_before FROM anime WHERE id = 30;
INSERT INTO user_watch_history (user_id, episode_id, progress_seconds, completed)
VALUES (1, 60, 720, 1);
SELECT 'watch_insert_increment' AS check_name,
       view_count - @view_before AS delta,
       IF(view_count = @view_before + 1, 'PASS', 'FAIL') AS result
FROM anime WHERE id = 30;
DELETE FROM user_watch_history WHERE user_id = 1 AND episode_id = 60;
SELECT 'watch_delete_decrement' AS check_name,
       view_count AS current_count,
       IF(view_count = @view_before, 'PASS', 'FAIL') AS result
FROM anime WHERE id = 30;
ROLLBACK;

-- Watch-history key-change trigger test, rolled back.
START TRANSACTION;
INSERT INTO anime_episode (anime_id, episode_number, title, air_date)
VALUES (1, 997, 'Episode 997 watch move test', NULL);
SET @watch_move_episode_a = LAST_INSERT_ID();
INSERT INTO anime_episode (anime_id, episode_number, title, air_date)
VALUES (2, 996, 'Episode 996 watch move test', NULL);
SET @watch_move_episode_b = LAST_INSERT_ID();
SELECT view_count INTO @watch_move_a_before FROM anime WHERE id = 1;
SELECT view_count INTO @watch_move_b_before FROM anime WHERE id = 2;
INSERT INTO user_watch_history (user_id, episode_id, progress_seconds, completed)
VALUES (10, @watch_move_episode_a, 120, 0);
SET @watch_move_id = LAST_INSERT_ID();
UPDATE user_watch_history
SET episode_id = @watch_move_episode_b
WHERE id = @watch_move_id;
SELECT 'watch_move_counters' AS check_name,
       (SELECT view_count FROM anime WHERE id = 1) AS old_count,
       (SELECT view_count FROM anime WHERE id = 2) AS new_count,
       IF(
           (SELECT view_count FROM anime WHERE id = 1) = @watch_move_a_before
           AND (SELECT view_count FROM anime WHERE id = 2) = @watch_move_b_before + 1,
           'PASS',
           'FAIL'
       ) AS result;
ROLLBACK;

-- Episode trigger test, rolled back.
START TRANSACTION;
SELECT episode_count INTO @episode_before FROM anime WHERE id = 1;
INSERT INTO anime_episode (anime_id, episode_number, title, air_date)
VALUES (1, 3, 'Episode 3 trigger test', NULL);
SET @test_episode_id = LAST_INSERT_ID();
SELECT 'episode_insert_increment' AS check_name,
       episode_count - @episode_before AS delta,
       IF(episode_count = @episode_before + 1, 'PASS', 'FAIL') AS result
FROM anime WHERE id = 1;
DELETE FROM anime_episode WHERE id = @test_episode_id;
SELECT 'episode_delete_decrement' AS check_name,
       episode_count AS current_count,
       IF(episode_count = @episode_before, 'PASS', 'FAIL') AS result
FROM anime WHERE id = 1;
ROLLBACK;

-- Episode key-change trigger test, rolled back.
START TRANSACTION;
SELECT episode_count INTO @episode_move_a_before FROM anime WHERE id = 1;
SELECT episode_count INTO @episode_move_b_before FROM anime WHERE id = 2;
INSERT INTO anime_episode (anime_id, episode_number, title, air_date)
VALUES (1, 998, 'Episode 998 move test', NULL);
SET @episode_move_id = LAST_INSERT_ID();
UPDATE anime_episode SET anime_id = 2 WHERE id = @episode_move_id;
SELECT 'episode_move_counters' AS check_name,
       (SELECT episode_count FROM anime WHERE id = 1) AS old_count,
       (SELECT episode_count FROM anime WHERE id = 2) AS new_count,
       IF(
           (SELECT episode_count FROM anime WHERE id = 1) = @episode_move_a_before
           AND (SELECT episode_count FROM anime WHERE id = 2) = @episode_move_b_before + 1,
           'PASS',
           'FAIL'
       ) AS result;
ROLLBACK;
