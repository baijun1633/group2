-- 用户表扩展字段迁移脚本
-- 执行时间: 2026-06-25

-- 添加角色字段（默认为 USER）
ALTER TABLE `users` ADD COLUMN `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色: ADMIN-管理员, USER-普通用户' AFTER `avatar`;

-- 添加偏好标签字段（JSON格式）
ALTER TABLE `users` ADD COLUMN `preference_tags` TEXT COMMENT '偏好标签(JSON格式)' AFTER `role`;

-- 创建管理员账号（密码为 admin123 的 BCrypt 加密）
-- 密码: admin123 -> BCrypt: $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH
-- 请在执行后手动插入管理员账号，或通过注册接口注册后修改 role 为 ADMIN

-- 示例：插入管理员账号
-- INSERT INTO `users` (`username`, `password`, `nickname`, `role`, `status`, `create_time`, `update_time`)
-- VALUES ('admin', '$2a$10$xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx', '系统管理员', 'ADMIN', 1, NOW(), NOW());
