USE anime_platform;

-- This statement is expected to fail CHECK constraint error 3819.
INSERT INTO anime_review (anime_id, user_id, rating, content)
VALUES (30, 2, 11, 'invalid rating negative test');

