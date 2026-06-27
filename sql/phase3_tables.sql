-- ========================================
-- 第三期：用户互动与个性化基础 - 数据库表结构
-- ========================================

-- 1. 书架表
CREATE TABLE IF NOT EXISTS `shelves` (
    `shelf_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '书架ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `name` VARCHAR(100) NOT NULL COMMENT '书架名称',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '书架描述',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`shelf_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户书架表';

-- 2. 书架图书关联表
CREATE TABLE IF NOT EXISTS `shelf_books` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `shelf_id` BIGINT NOT NULL COMMENT '书架ID',
    `book_id` BIGINT NOT NULL COMMENT '图书ID',
    `reading_status` TINYINT DEFAULT 0 COMMENT '阅读状态：0-未读, 1-在读, 2-已读',
    `add_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_shelf_book` (`shelf_id`, `book_id`),
    INDEX `idx_book_id` (`book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='书架图书关联表';

-- 3. 阅读进度表
CREATE TABLE IF NOT EXISTS `reading_progress` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `book_id` BIGINT NOT NULL COMMENT '图书ID',
    `current_page` INT DEFAULT 0 COMMENT '当前页码',
    `total_pages` INT DEFAULT 0 COMMENT '总页数',
    `percentage` DECIMAL(5,2) DEFAULT 0.00 COMMENT '阅读百分比',
    `device` VARCHAR(50) DEFAULT NULL COMMENT '设备信息',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_book` (`user_id`, `book_id`),
    INDEX `idx_book_id` (`book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='阅读进度表';

-- 4. 用户画像表
CREATE TABLE IF NOT EXISTS `user_profiles` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `tag_vector` JSON DEFAULT NULL COMMENT '标签权重向量，如{"科幻":0.8,"文学":0.6}',
    `preferred_authors` JSON DEFAULT NULL COMMENT '偏好作者列表',
    `preferred_categories` JSON DEFAULT NULL COMMENT '偏好分类列表',
    `last_updated` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户画像表';
