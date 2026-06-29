-- =====================================================================
-- Phase 9 P2 - 高危操作二次验证
-- 概要设计文档 line 308: 图谱数据删除、推荐算法参数修改、用户权限变更、
-- 批量数据导入等高危操作需输入二级操作密码完成二次校验
--
-- 本脚本：
--   1. 给 users 表新增 second_password 字段（BCrypt 哈希，可空）
--   2. 给所有管理员账号预置二级密码 = "admin123"（与登录密码一致，便于测试）
-- =====================================================================

ALTER TABLE users
  ADD COLUMN second_password VARCHAR(100) NULL COMMENT '二级操作密码(BCrypt)，用于高危操作二次验证' AFTER password;

-- 为现有管理员账号预置二级密码 "admin123"
UPDATE users
SET second_password = '$2a$10$0YBbVSeuqf0Cdpfo7jTLDOUA0e9YMf.BndTEnorOI1wZQzdv18U4W'
WHERE role IN ('ADMIN', 'BOOK_ADMIN', 'OPS_ADMIN', 'COMMUNITY_ADMIN')
  AND second_password IS NULL;

-- 验证
SELECT user_id, username, role,
       CASE WHEN second_password IS NOT NULL THEN 'SET' ELSE 'NULL' END AS second_password_status
FROM users
WHERE role LIKE '%_ADMIN' OR role = 'ADMIN';
