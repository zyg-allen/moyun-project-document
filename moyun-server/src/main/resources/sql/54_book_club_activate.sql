-- =====================================================
-- 共读活动模块 - 激活/补齐脚本（幂等）
-- 说明：共读 4 张表已在 07_reading_interview_init.sql 中创建，
--       本脚本幂等补齐共读功能所需字段，可重复执行。
-- 版本: v1.0
-- 日期: 2026-07-02
-- =====================================================

-- ----------------------------------------------------------------
-- 幂等工具：仅在列不存在时才添加（MySQL 原生不支持 ADD COLUMN IF NOT EXISTS，
-- 故借助 information_schema 判断）
-- ----------------------------------------------------------------
DROP PROCEDURE IF EXISTS `p_add_column_if_not_exists`;
DELIMITER $$
CREATE PROCEDURE `p_add_column_if_not_exists`(
    IN p_table  VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_def    TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table
          AND COLUMN_NAME = p_column
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_def);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- =====================================================
-- 一、共读打卡记录表 portal_book_club_record 补齐字段
-- =====================================================

-- 打卡内容（如缺失则补齐为 TEXT）
CALL p_add_column_if_not_exists(
    'portal_book_club_record',
    'content',
    'TEXT COMMENT ''打卡内容'' AFTER `day`'
);

-- 点赞数（如缺失则补齐为 INT）
CALL p_add_column_if_not_exists(
    'portal_book_club_record',
    'like_count',
    'INT DEFAULT 0 COMMENT ''点赞数'' AFTER `images`'
);

-- 记录类型：reflection=读后感，excerpt=摘抄（共读记录区分读后感/摘抄，原表缺失，新增）
CALL p_add_column_if_not_exists(
    'portal_book_club_record',
    'record_type',
    'VARCHAR(20) DEFAULT ''reflection'' COMMENT ''记录类型:reflection-读后感,excerpt-摘抄'' AFTER `images`'
);

-- =====================================================
-- 二、共读活动表 portal_book_club_activity 状态注释加固
--     status 原注释已为 upcoming/ongoing/ended，此处幂等重置注释，保证语义清晰。
--     MODIFY COLUMN 天然幂等，可重复执行。
-- =====================================================
ALTER TABLE `portal_book_club_activity`
    MODIFY COLUMN `status` VARCHAR(20) DEFAULT 'upcoming' COMMENT '状态:upcoming-未开始,ongoing-进行中,ended-已结束';

-- 为 record_type 增加索引（如已存在则跳过：先删后建保证幂等）
-- 注意：MySQL 不支持 CREATE INDEX IF NOT EXISTS，这里用 DROP + ADD 模式
SET @idx_exists := (
    SELECT COUNT(1) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'portal_book_club_record'
      AND INDEX_NAME = 'idx_record_type'
);
SET @drop_idx := IF(@idx_exists > 0, 'ALTER TABLE `portal_book_club_record` DROP INDEX `idx_record_type`', 'SELECT 1');
PREPARE stmt FROM @drop_idx;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
ALTER TABLE `portal_book_club_record` ADD INDEX `idx_record_type` (`record_type`);

-- ----------------------------------------------------------------
-- 清理临时存储过程
-- ----------------------------------------------------------------
DROP PROCEDURE IF EXISTS `p_add_column_if_not_exists`;

-- =====================================================
-- 脚本执行完成
-- =====================================================
