-- ============================================================
-- 读书模块 v1.0 第一阶段：章节体系 + 书籍扩展
-- 创建时间：2026-06-30
-- 说明：
--   1. 新建章节表 portal_book_chapter（LONGTEXT 正文，支持 VIP/定时发布）
--   2. 新建章节浏览记录表 portal_book_chapter_view（统计/防刷）
--   3. 扩展 portal_book 表：type/serial_status/word_count/chapter_count 等
--   4. 扩展 portal_reading_progress 表：章节级进度记忆
--   5. 扩展 portal_book_quote 表：关联章节
-- ============================================================

-- -------------------------------------------------------
-- 1. 新建章节表
-- -------------------------------------------------------
DROP TABLE IF EXISTS `portal_book_chapter`;
CREATE TABLE `portal_book_chapter` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `book_id` BIGINT NOT NULL COMMENT '所属书籍ID',
    `title` VARCHAR(500) NOT NULL COMMENT '章节标题',
    `content` LONGTEXT COMMENT '章节正文（HTML格式，上限4GB）',
    `content_markdown` TEXT COMMENT 'Markdown原始内容（上限64KB，单章足够）',
    `editor_mode` VARCHAR(20) DEFAULT 'richtext' COMMENT '编辑器模式：richtext/markdown',
    `word_count` INT DEFAULT 0 COMMENT '字数统计',
    `chapter_no` INT NOT NULL DEFAULT 0 COMMENT '章节序号（用于排序，从1开始）',
    `volume_id` BIGINT DEFAULT NULL COMMENT '所属分卷ID（可选，支持分卷管理）',
    `is_free` TINYINT(1) DEFAULT 1 COMMENT '是否免费：1=免费，0=VIP章节',
    `price` DECIMAL(10,2) DEFAULT 0.00 COMMENT '章节单价（元，VIP章节购买）',
    `is_published` TINYINT(1) DEFAULT 0 COMMENT '是否已发布：0=草稿，1=已发布',
    `publish_time` DATETIME DEFAULT NULL COMMENT '发布时间（支持定时发布）',
    `view_count` BIGINT DEFAULT 0 COMMENT '章节浏览量',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_book_chapter_no` (`book_id`, `chapter_no`),
    KEY `idx_book_id` (`book_id`),
    KEY `idx_publish_time` (`publish_time`),
    KEY `idx_is_published` (`is_published`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='书籍章节表';

-- -------------------------------------------------------
-- 2. 新建章节浏览记录表（用于阅读统计、防刷）
-- -------------------------------------------------------
DROP TABLE IF EXISTS `portal_book_chapter_view`;
CREATE TABLE `portal_book_chapter_view` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `chapter_id` BIGINT NOT NULL COMMENT '章节ID',
    `book_id` BIGINT NOT NULL COMMENT '书籍ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '用户ID（未登录为NULL）',
    `client_ip` VARCHAR(50) DEFAULT NULL COMMENT '客户端IP',
    `read_duration_ms` INT DEFAULT 0 COMMENT '阅读时长（毫秒）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
    PRIMARY KEY (`id`),
    KEY `idx_chapter_id` (`chapter_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='章节浏览记录表';

-- -------------------------------------------------------
-- 3. 扩展 portal_book 表：书籍类型、连载状态、字数、章节数等
-- -------------------------------------------------------
ALTER TABLE `portal_book`
    ADD COLUMN `type` VARCHAR(20) DEFAULT 'published' COMMENT '书籍类型：published=出版物，novel=网络小说，longform=长文'
    AFTER `status`;

ALTER TABLE `portal_book`
    ADD COLUMN `serial_status` VARCHAR(20) DEFAULT 'completed' COMMENT '连载状态：ongoing=连载中，completed=已完结，hiatus=暂停更新'
    AFTER `type`;

ALTER TABLE `portal_book`
    ADD COLUMN `word_count` BIGINT DEFAULT 0 COMMENT '总字数（章节字数之和）'
    AFTER `serial_status`;

ALTER TABLE `portal_book`
    ADD COLUMN `chapter_count` INT DEFAULT 0 COMMENT '总章节数'
    AFTER `word_count`;

ALTER TABLE `portal_book`
    ADD COLUMN `latest_chapter_id` BIGINT DEFAULT NULL COMMENT '最新章节ID（用于追更展示）'
    AFTER `chapter_count`;

ALTER TABLE `portal_book`
    ADD COLUMN `latest_chapter_title` VARCHAR(500) DEFAULT NULL COMMENT '最新章节标题'
    AFTER `latest_chapter_id`;

ALTER TABLE `portal_book`
    ADD COLUMN `last_update_time` DATETIME DEFAULT NULL COMMENT '最后更新时间（章节发布时同步）'
    AFTER `latest_chapter_title`;

ALTER TABLE `portal_book`
    ADD COLUMN `is_finished` TINYINT(1) DEFAULT 1 COMMENT '是否完结：1=完结，0=连载中（冗余字段，便于查询）'
    AFTER `last_update_time`;

-- 为新字段建立索引
ALTER TABLE `portal_book` ADD INDEX `idx_type` (`type`);
ALTER TABLE `portal_book` ADD INDEX `idx_serial_status` (`serial_status`);
ALTER TABLE `portal_book` ADD INDEX `idx_is_finished` (`is_finished`);
ALTER TABLE `portal_book` ADD INDEX `idx_word_count` (`word_count`);
ALTER TABLE `portal_book` ADD INDEX `idx_last_update_time` (`last_update_time`);

-- -------------------------------------------------------
-- 4. 扩展 portal_reading_progress 表：章节级进度记忆
-- -------------------------------------------------------
ALTER TABLE `portal_reading_progress`
    ADD COLUMN `current_chapter_id` BIGINT DEFAULT NULL COMMENT '当前阅读章节ID'
    AFTER `book_id`;

ALTER TABLE `portal_reading_progress`
    ADD COLUMN `current_chapter_no` INT DEFAULT 0 COMMENT '当前章节序号'
    AFTER `current_chapter_id`;

ALTER TABLE `portal_reading_progress`
    ADD COLUMN `chapter_offset` INT DEFAULT 0 COMMENT '章节内滚动偏移（像素）'
    AFTER `current_chapter_no`;

ALTER TABLE `portal_reading_progress`
    ADD COLUMN `last_read_time` DATETIME DEFAULT NULL COMMENT '最后阅读时间'
    AFTER `chapter_offset`;

ALTER TABLE `portal_reading_progress`
    ADD COLUMN `reading_duration_ms` BIGINT DEFAULT 0 COMMENT '累计阅读时长（毫秒）'
    AFTER `last_read_time`;

-- 为新字段建立索引
ALTER TABLE `portal_reading_progress` ADD INDEX `idx_last_read_time` (`last_read_time`);

-- -------------------------------------------------------
-- 5. 扩展 portal_book_quote 表：关联章节
-- -------------------------------------------------------
ALTER TABLE `portal_book_quote`
    ADD COLUMN `chapter_id` BIGINT DEFAULT NULL COMMENT '章节ID（关联 portal_book_chapter）'
    AFTER `book_id`;

ALTER TABLE `portal_book_quote`
    ADD INDEX `idx_chapter_id` (`chapter_id`);

-- -------------------------------------------------------
-- 6. 将现有书籍默认标记为出版物类型
-- -------------------------------------------------------
UPDATE `portal_book` SET `type` = 'published', `serial_status` = 'completed', `is_finished` = 1
WHERE `type` IS NULL OR `type` = '';
