-- 第四期：社区功能 - 书评相关表
-- 执行时间: 2026-06-26

-- 创建书评表
CREATE TABLE IF NOT EXISTS `reviews` (
    `review_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '书评ID',
    `book_id` BIGINT NOT NULL COMMENT '书籍ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `content` TEXT NOT NULL COMMENT '书评内容(纯文本)',
    `markdown` TEXT COMMENT '书评内容(Markdown格式)',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-待审核, 1-已通过, 2-已拒绝, 3-已删除',
    `likes_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    `replies_count` INT NOT NULL DEFAULT 0 COMMENT '回复数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`review_id`),
    KEY `idx_book_id` (`book_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    CONSTRAINT `fk_reviews_book` FOREIGN KEY (`book_id`) REFERENCES `books` (`book_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_reviews_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='书评表';

-- 创建书评回复表
CREATE TABLE IF NOT EXISTS `review_replies` (
    `reply_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '回复ID',
    `review_id` BIGINT NOT NULL COMMENT '书评ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `content` TEXT NOT NULL COMMENT '回复内容',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`reply_id`),
    KEY `idx_review_id` (`review_id`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_replies_review` FOREIGN KEY (`review_id`) REFERENCES `reviews` (`review_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_replies_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='书评回复表';

-- 创建书评点赞表
CREATE TABLE IF NOT EXISTS `review_likes` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `review_id` BIGINT NOT NULL COMMENT '书评ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_review` (`user_id`, `review_id`),
    KEY `idx_review_id` (`review_id`),
    CONSTRAINT `fk_likes_review` FOREIGN KEY (`review_id`) REFERENCES `reviews` (`review_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_likes_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='书评点赞表';
