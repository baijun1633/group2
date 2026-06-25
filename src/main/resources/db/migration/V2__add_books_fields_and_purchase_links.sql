-- 书籍表扩展字段迁移脚本
-- 执行时间: 2026-06-25

-- 添加页数字段
ALTER TABLE `books` ADD COLUMN `pages` INT COMMENT '页数' AFTER `stock`;

-- 添加标签字段（JSON格式）
ALTER TABLE `books` ADD COLUMN `tags` TEXT COMMENT '标签(JSON格式)' AFTER `pages`;

-- 添加平均评分字段
ALTER TABLE `books` ADD COLUMN `avg_rating` DECIMAL(3,2) DEFAULT 0.00 COMMENT '平均评分(1-5分)' AFTER `tags`;

-- 添加评分人数字段
ALTER TABLE `books` ADD COLUMN `rating_count` INT DEFAULT 0 COMMENT '评分人数' AFTER `avg_rating`;

-- 添加系列信息字段
ALTER TABLE `books` ADD COLUMN `series_info` VARCHAR(200) COMMENT '系列信息' AFTER `rating_count`;

-- 创建购书链接表
CREATE TABLE IF NOT EXISTS `purchase_links` (
    `link_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '链接ID',
    `book_id` BIGINT NOT NULL COMMENT '书籍ID',
    `platform` VARCHAR(50) NOT NULL COMMENT '平台名称(如: 京东、当当、淘宝)',
    `url` VARCHAR(500) NOT NULL COMMENT '购买链接',
    `price` DECIMAL(10,2) COMMENT '平台价格',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`link_id`),
    KEY `idx_book_id` (`book_id`),
    CONSTRAINT `fk_purchase_links_book` FOREIGN KEY (`book_id`) REFERENCES `books` (`book_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购书链接表';

-- 创建图书评分表
CREATE TABLE IF NOT EXISTS `book_ratings` (
    `rating_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评分ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `book_id` BIGINT NOT NULL COMMENT '书籍ID',
    `score` TINYINT NOT NULL COMMENT '评分(1-5分)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`rating_id`),
    UNIQUE KEY `uk_user_book` (`user_id`, `book_id`),
    KEY `idx_book_id` (`book_id`),
    CONSTRAINT `fk_ratings_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ratings_book` FOREIGN KEY (`book_id`) REFERENCES `books` (`book_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书评分表';
