USE anime_platform;

-- The functional unique index normalizes NULL release dates for uniqueness.
INSERT INTO anime (title, studio_id, release_date, status) VALUES
    ('__NULL_RELEASE_TITLE_UNIQUENESS__', 1, NULL, 'UPCOMING'),
    ('__NULL_RELEASE_TITLE_UNIQUENESS__', 1, NULL, 'UPCOMING');
