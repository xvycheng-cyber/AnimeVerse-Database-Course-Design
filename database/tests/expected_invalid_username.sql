USE anime_platform;

INSERT INTO sys_user (
    username, password_hash, nickname, email, status
) VALUES (
    'abc',
    '$2b$10$abcdefghijklmnopqrstuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuu',
    'invalid username test',
    'invalid-username@animeverse.local',
    1
);
