-- Cover artwork resolved against AniList by native title on 2026-11-16.
-- Only empty seed rows are updated so locally curated covers are preserved.

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE anime_platform;

UPDATE anime
SET cover_url = CASE id
    WHEN 2 THEN 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx5680-r3AI3Cwfv0Aq.png'
    WHEN 3 THEN 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx18507-RajfCYPpBfT3.png'
    WHEN 4 THEN 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx21827-ubzq619ZA2E9.png'
    WHEN 5 THEN 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx1535-kUgkcrfOrkUM.jpg'
    WHEN 6 THEN 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx136-gj0bbCpDNrKG.jpg'
    WHEN 7 THEN 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx20623-dUARfggnNDOe.jpg'
    WHEN 8 THEN 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx154587-qQTzQnEJJ3oB.jpg'
    WHEN 9 THEN 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx113415-LHBAeoZDIsnF.jpg'
    WHEN 10 THEN 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx127230-DdP4vAdssLoz.png'
    WHEN 11 THEN 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx21709-aqPBDxJPruYR.png'
    WHEN 12 THEN 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx136430-gsBsJjA7hGh9.jpg'
    WHEN 13 THEN 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx101922-WBsBl0ClmgYL.jpg'
    WHEN 14 THEN 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx10087-M4Hd9qrHGrXk.png'
    WHEN 15 THEN 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx19603-ycT0pyEgDVQu.jpg'
    WHEN 16 THEN 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx21221-exaYgjct2K2c.jpg'
    WHEN 17 THEN 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx5114-nSWCgQlmOMtj.jpg'
    WHEN 18 THEN 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx21459-nYh85uj2Fuwr.jpg'
    WHEN 19 THEN 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx21507-6YUSbh2m0N1p.jpg'
    WHEN 20 THEN 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx853-fiUtW8yohsSF.jpg'
    WHEN 21 THEN 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx11757-SxYDUzdr9rh2.jpg'
    WHEN 22 THEN 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx20665-TLgkL8T8IRFd.png'
    WHEN 23 THEN 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx9989-hImMg6kCMm6I.jpg'
    WHEN 24 THEN 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx21234-XmqW39aQ9o7O.jpg'
    WHEN 25 THEN 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx16498-buvcRTBx4NSm.jpg'
    WHEN 26 THEN 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx140960-Kb6R5nYQfjmP.jpg'
    WHEN 27 THEN 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx113717-9sNnN8WRgK15.jpg'
    WHEN 28 THEN 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx467-mBTtIoR13qs2.jpg'
    WHEN 29 THEN 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx20464-ooZUyBe4ptp9.png'
    WHEN 30 THEN 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx11771-uvr44RAwRxPw.jpg'
    ELSE cover_url
END
WHERE id BETWEEN 2 AND 30
  AND cover_url IS NULL;
