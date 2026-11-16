-- Reference data, roles and BCrypt-backed demo users.
-- INSERT IGNORE makes repeated initialization safe without overwriting existing rows.

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE anime_platform;
SET @seed_now = '2026-11-16 15:00:00';

INSERT IGNORE INTO sys_role (id, role_code, role_name, description, created_at) VALUES
    (1, 'ADMIN', '管理员', '内容、用户、审计与数据库实验室管理权限', @seed_now),
    (2, 'USER', '普通用户', '浏览、收藏、评论与观看记录权限', @seed_now);

-- BCrypt hashes were generated and verified with bcrypt 5.0.0.
-- admin / Admin123! and sakura / User123! are the only documented courses accounts.
INSERT IGNORE INTO sys_user
    (id, username, password_hash, nickname, email, avatar_url, status, last_login_at, created_at, updated_at)
VALUES
    (1, 'admin', '$2b$10$y1TuHGglRKug1C2QkMbnfuTEICEU2FaBPVbCmDpeALCzZNmtDSlxC', '星漫管理员', 'admin@animeverse.local', NULL, 1, @seed_now, @seed_now, @seed_now),
    (2, 'sakura', '$2b$10$obcUhC60aioolCblLdJEsukw/zkplRNI1I6UqPAuBxas2c42ts4fS', '樱', 'sakura@animeverse.local', NULL, 1, '2026-11-15 20:30:00', @seed_now, @seed_now),
    (3, 'user02', '$2b$10$pHNQVoqjjZ8EavMKWUiTve09d9Hct0X3bA1ej3VSPHzVn1SjZ1pYi', '夜空', 'user02@animeverse.local', NULL, 1, NULL, @seed_now, @seed_now),
    (4, 'user03', '$2b$10$ciAimFOATNadoBYNwsoQfe.rE/oHUR5aQ2VO7I/Y1K1h4Pqf9sNZm', '风铃', 'user03@animeverse.local', NULL, 1, NULL, @seed_now, @seed_now),
    (5, 'user04', '$2b$10$7.puH8bZkUFxC619Fr4iu.EGg4mBcw3ifuH8wwLtgO2auzIAVczBy', '白羽', 'user04@animeverse.local', NULL, 1, NULL, @seed_now, @seed_now),
    (6, 'user05', '$2b$10$y9YNQj5/RIsO6AAYehhyUet/wRCVLDUN4iWU6MR54ZhnmGESDeeXq', '青岚', 'user05@animeverse.local', NULL, 1, NULL, @seed_now, @seed_now),
    (7, 'user06', '$2b$10$fqmsKyehYBjywtPzSK/Pju14tBqOaSyK10RErVBq.Moa0xjnzALWq', '星野', 'user06@animeverse.local', NULL, 1, NULL, @seed_now, @seed_now),
    (8, 'user07', '$2b$10$8Rw85SAHlVmTx2f6wRkPEOhlpb34FJeWwLX7K.0.VUDpTD.3N7Azi', '月见', 'user07@animeverse.local', NULL, 1, NULL, @seed_now, @seed_now),
    (9, 'user08', '$2b$10$yxpPYN4n6dENBw7CTH2KVOJtvjT2Z4PT1ZdIqf7PRa9eWKfKZSMom', '夏目', 'user08@animeverse.local', NULL, 1, NULL, @seed_now, @seed_now),
    (10, 'user09', '$2b$10$IYjbWQBXHPfaUdUkLslHRu5AmvdfsXh3jtVecNKoFTVPNZR.kTB4C', '千叶', 'user09@animeverse.local', NULL, 1, NULL, @seed_now, @seed_now);

INSERT IGNORE INTO sys_user_role (user_id, role_id, assigned_at, assigned_by) VALUES
    (1, 1, @seed_now, 1),
    (1, 2, @seed_now, 1),
    (2, 2, @seed_now, 1),
    (3, 2, @seed_now, 1),
    (4, 2, @seed_now, 1),
    (5, 2, @seed_now, 1),
    (6, 2, @seed_now, 1),
    (7, 2, @seed_now, 1),
    (8, 2, @seed_now, 1),
    (9, 2, @seed_now, 1),
    (10, 2, @seed_now, 1);

INSERT IGNORE INTO anime_studio
    (id, studio_name, studio_name_en, country, founded_year, website_url, description, created_at, updated_at)
VALUES
    (1, '京都动画', 'Kyoto Animation', '日本', 1981, 'https://www.kyotoanimation.co.jp/', '日本动画制作公司，代表作品包括《凉宫春日的忧郁》《轻音少女》等。', @seed_now, @seed_now),
    (2, 'Madhouse', 'Madhouse, Inc.', '日本', 1972, 'https://www.madhouse.co.jp/', '日本动画制作公司，参与制作《死亡笔记》《葬送的芙莉莲》等电视动画。', @seed_now, @seed_now),
    (3, 'MAPPA', 'MAPPA Co., Ltd.', '日本', 2011, 'https://www.mappa.co.jp/', '日本动画制作公司，参与制作《咒术回战》《电锯人》等作品。', @seed_now, @seed_now),
    (4, 'ufotable', 'ufotable Inc.', '日本', 2000, 'https://www.ufotable.com/', '日本动画制作公司，参与制作《鬼灭之刃》《Fate/Zero》等作品。', @seed_now, @seed_now),
    (5, 'BONES', 'Bones, Inc.', '日本', 1998, 'https://www.bones.co.jp/', '日本动画制作公司，参与制作《钢之炼金术师 FULLMETAL ALCHEMIST》等作品。', @seed_now, @seed_now),
    (6, 'A-1 Pictures', 'A-1 Pictures Inc.', '日本', 2005, 'https://a1p.jp/', '日本动画制作公司，参与制作《刀剑神域》《四月是你的谎言》等作品。', @seed_now, @seed_now),
    (7, 'WIT STUDIO', 'WIT STUDIO, Inc.', '日本', 2012, 'https://www.witstudio.co.jp/', '日本动画制作公司，参与制作《进击的巨人》第一季等作品。', @seed_now, @seed_now),
    (8, 'Production I.G', 'Production I.G, Inc.', '日本', 1987, 'https://www.production-ig.co.jp/', '日本动画制作公司，参与制作《攻壳机动队 STAND ALONE COMPLEX》等作品。', @seed_now, @seed_now);

INSERT IGNORE INTO anime_genre (id, genre_name, genre_slug, description, created_at) VALUES
    (1, '动作', 'action', '战斗、竞技与高强度动作内容', @seed_now),
    (2, '冒险', 'adventure', '旅行、探索与成长型冒险内容', @seed_now),
    (3, '喜剧', 'comedy', '以幽默和轻松叙事为主的内容', @seed_now),
    (4, '剧情', 'drama', '重视人物关系与情感发展的内容', @seed_now),
    (5, '奇幻', 'fantasy', '包含魔法、异世界或架空世界观的内容', @seed_now),
    (6, '悬疑', 'mystery', '谜题、推理和悬念驱动的内容', @seed_now),
    (7, '恋爱', 'romance', '以恋爱关系为核心的内容', @seed_now),
    (8, '科幻', 'sci-fi', '以科学技术和未来设定为核心的内容', @seed_now),
    (9, '日常', 'slice-of-life', '关注日常生活与人际互动的内容', @seed_now),
    (10, '超自然', 'supernatural', '涉及灵异、异能或超自然现象的内容', @seed_now);

INSERT IGNORE INTO sys_user_profile
    (user_id, bio, birthday, preferred_genre_id, profile_visibility, updated_at)
VALUES
    (1, '负责平台内容与数据库实验管理。', NULL, 5, 1, @seed_now),
    (2, '喜欢奇幻、剧情与日常系动画。', '2001-04-12', 5, 1, @seed_now),
    (3, '偏爱悬疑和科幻作品。', '2000-09-03', 6, 1, @seed_now),
    (4, '动作与冒险作品爱好者。', '1999-12-20', 1, 1, @seed_now),
    (5, '关注长篇改编和人物群像。', '2002-02-14', 4, 1, @seed_now),
    (6, '日常系与喜剧向观众。', '2001-07-08', 9, 1, @seed_now),
    (7, '喜欢音乐题材和青春故事。', '2003-01-19', 7, 1, @seed_now),
    (8, '奇幻与超自然题材爱好者。', '1998-11-11', 10, 1, @seed_now),
    (9, '关注科幻与竞技类作品。', '2000-06-30', 8, 1, @seed_now),
    (10, '剧情向作品观众。', '2002-10-05', 4, 1, @seed_now);

INSERT IGNORE INTO streaming_platform
    (id, platform_name, platform_code, website_url, country, is_active, created_at, updated_at)
VALUES
    (1, 'Crunchyroll', 'crunchyroll', 'https://www.crunchyroll.com/', '美国', 1, @seed_now, @seed_now),
    (2, 'Netflix', 'netflix', 'https://www.netflix.com/', '美国', 1, @seed_now, @seed_now),
    (3, 'Hulu', 'hulu', 'https://www.hulu.com/', '美国', 1, @seed_now, @seed_now),
    (4, 'Amazon Prime Video', 'prime-video', 'https://www.primevideo.com/', '美国', 1, @seed_now, @seed_now),
    (5, 'bilibili', 'bilibili', 'https://www.bilibili.com/', '中国', 1, @seed_now, @seed_now),
    (6, 'iQIYI', 'iqiyi', 'https://www.iqiyi.com/', '中国', 1, @seed_now, @seed_now);
