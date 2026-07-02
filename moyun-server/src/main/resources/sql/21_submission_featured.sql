-- =============================================
-- V21: 面试提交笔记精选功能（支持 note_adopted 成长事件）
-- 注意：note 字段由 51_interview_ddl_fix.sql 补齐
-- 本脚本使用幂等写法，避免重复执行报错
-- @author moyun
-- =============================================

-- 安全添加列的存储过程
DROP PROCEDURE IF EXISTS AddColumnIfNotExistsV21;
DELIMITER $$
CREATE PROCEDURE AddColumnIfNotExistsV21(
    IN tableName VARCHAR(100),
    IN columnName VARCHAR(100),
    IN columnDefinition VARCHAR(500)
)
BEGIN
    DECLARE columnExists INT DEFAULT 0;
    SELECT COUNT(*) INTO columnExists
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = tableName
      AND COLUMN_NAME = columnName;
    IF columnExists = 0 THEN
        SET @sql = CONCAT('ALTER TABLE `', tableName, '` ADD COLUMN `', columnName, '` ', columnDefinition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL AddColumnIfNotExistsV21('portal_interview_submission', 'is_featured', 'TINYINT(1) DEFAULT 0 COMMENT \'是否被精选（后台采纳为优质笔记）\' AFTER `note`');
CALL AddColumnIfNotExistsV21('portal_interview_submission', 'featured_time', 'DATETIME DEFAULT NULL COMMENT \'精选时间\' AFTER `is_featured`');

-- 为精选笔记查询添加索引（幂等）
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_interview_submission' AND INDEX_NAME = 'idx_submission_featured');
SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE `portal_interview_submission` ADD INDEX `idx_submission_featured` (`is_featured`, `featured_time`)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

DROP PROCEDURE IF EXISTS AddColumnIfNotExistsV21;
