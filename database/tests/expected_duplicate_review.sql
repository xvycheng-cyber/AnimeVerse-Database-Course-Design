USE anime_platform;

-- This statement is expected to fail with duplicate key error 1062.
-- Never run it outside an explicit negative test expecting a non-zero exit code.
INSERT INTO anime_review (anime_id, user_id, rating, content)
VALUES (8, 1, 9, 'duplicate review negative test');

