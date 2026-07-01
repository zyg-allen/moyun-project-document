-- ============================================================
-- 读书模块 v1.0 第二阶段：书架 + 阅读偏好
-- 创建时间：2026-06-30
-- 说明：
--   1. 新建 portal_bookshelf 书架表（用户收藏书籍，含最近阅读章节）
--   2. 新建 portal_reading_preference 阅读偏好表（字号/行距/主题/字体）
--   注：portal_reading_progress 章节级扩展字段已在 42 号 SQL 创建，此处不再重复
-- ============================================================

-- -------------------------------------------------------
-- 1. 书架表（收藏书籍）
--    使用 CREATE TABLE IF NOT EXISTS 保证脚本可重入，避免生产环境误删用户书架数据
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS `portal_bookshelf` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `book_id` BIGINT NOT NULL COMMENT '书籍ID',
    `last_chapter_id` BIGINT DEFAULT NULL COMMENT '最后阅读章节ID（冗余，用于续读）',
    `last_chapter_no` INT DEFAULT 0 COMMENT '最后阅读章节序号',
    `sort` INT DEFAULT 0 COMMENT '排序（用户自定义书架顺序，越大越靠前）',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_book` (`user_id`, `book_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_book_id` (`book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户书架（收藏书籍）表';

-- -------------------------------------------------------
-- 2. 阅读偏好表（每用户单条，upsert）
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS `portal_reading_preference` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `font_size` INT DEFAULT 18 COMMENT '正文字号（px，12-32）',
    `line_height` DECIMAL(3,1) DEFAULT 1.8 COMMENT '行距（倍，1.2-3.0）',
    `theme` VARCHAR(20) DEFAULT 'default' COMMENT '阅读主题：default=跟随 / light=亮色 / dark=暗色 / sepia=护眼黄',
    `font_family` VARCHAR(50) DEFAULT 'system' COMMENT '字体：system=系统默认 / serif=衬线 / song=宋体 / hei=黑体',
    `letter_spacing` DECIMAL(3,1) DEFAULT 0.0 COMMENT '字间距（px，-1.0-5.0）',
    `paragraph_spacing` DECIMAL(4,1) DEFAULT 1.2 COMMENT '段间距（em，0.5-5.0）',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户阅读偏好表';

-- ============================================================
-- 脚本执行完成
-- ============================================================
SELECT '书架与阅读偏好表创建完成！' AS message;
