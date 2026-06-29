-- =====================================================================
-- Phase 9 - 角色权限细分
-- 概要设计文档 line 118/266/304: 4 类登录角色（普通读者 + 3 类管理员）
--
-- 本脚本插入 3 个细分管理员账号用于测试：
--   book_admin_user      / admin123   图书管理员
--   ops_admin_user       / admin123   运营管理员
--   community_admin_user / admin123   社区管理员
--
-- BCrypt 密码哈希对应明文 admin123（与现有 admin_review 共用）
-- =====================================================================

INSERT INTO users (username, password, nickname, email, role, status, create_time, update_time)
SELECT * FROM (
  SELECT
    'book_admin_user' AS username,
    '$2a$10$0YBbVSeuqf0Cdpfo7jTLDOUA0e9YMf.BndTEnorOI1wZQzdv18U4W' AS password,
    'Book Admin' AS nickname,
    'book_admin@library.local' AS email,
    'BOOK_ADMIN' AS role,
    1 AS status,
    NOW() AS create_time,
    NOW() AS update_time
  UNION ALL SELECT
    'ops_admin_user',
    '$2a$10$0YBbVSeuqf0Cdpfo7jTLDOUA0e9YMf.BndTEnorOI1wZQzdv18U4W',
    'Ops Admin',
    'ops_admin@library.local',
    'OPS_ADMIN',
    1,
    NOW(),
    NOW()
  UNION ALL SELECT
    'community_admin_user',
    '$2a$10$0YBbVSeuqf0Cdpfo7jTLDOUA0e9YMf.BndTEnorOI1wZQzdv18U4W',
    'Community Admin',
    'community_admin@library.local',
    'COMMUNITY_ADMIN',
    1,
    NOW(),
    NOW()
) AS t
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'book_admin_user');

-- 验证
SELECT user_id, username, nickname, role, status FROM users WHERE role LIKE '%_ADMIN' OR role = 'ADMIN';
