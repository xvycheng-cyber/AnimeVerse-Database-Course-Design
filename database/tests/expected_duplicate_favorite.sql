USE anime_platform;

-- This statement is expected to fail with duplicate key error 1062.
-- Never run it outside an explicit negative test expecting a non-zero exit code.
INSERT INTO user_favorite (user_id, anime_id)
VALUES (1, 8);

