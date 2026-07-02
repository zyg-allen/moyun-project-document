-- =====================================================
-- 读书空间 - 点赞功能恢复 & 书单关系修复
-- 1. 幂等添加 portal_book_quote.like_count / portal_book_list.like_count（如缺失）
-- 2. 幂等添加 portal_book_list_item.remark（如缺失）
-- 幂等设计：所有 ALTER 使用 AddColumnIfNotExists 存储过程
-- 执行顺序：本脚本必须在 07_reading_interview_init.sql 之后执行
-- =====================================================

-- 安全添加列的存储过程（与 28/29/51 脚本一致）
DROP PROCEDURE IF EXISTS AddColumnIfNotExists;
DELIMITER $$
CREATE PROCEDURE AddColumnIfNotExists(
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

-- =====================================================
-- 一、点赞数冗余字段（点赞表本身已在 07 脚本建立，此处仅补齐被点赞对象的 like_count）
-- 07 脚本中 portal_book_quote / portal_book_list 已包含 like_count，
-- 这里做幂等兜底，保证旧库升级时字段存在。
-- =====================================================

CALL AddColumnIfNotExists('portal_book_quote', 'like_count', 'BIGINT DEFAULT 0 COMMENT \'点赞数\'');
CALL AddColumnIfNotExists('portal_book_list', 'like_count', 'BIGINT DEFAULT 0 COMMENT \'点赞数\'');

-- =====================================================
-- 二、书单-书籍关联表备注字段（实体已使用 note 作为添加说明，remark 作为通用备注兜底）
-- =====================================================

CALL AddColumnIfNotExists('portal_book_list_item', 'remark', 'VARCHAR(500) DEFAULT NULL COMMENT \'备注\'');

-- =====================================================
-- 三、清理存储过程
-- =====================================================
DROP PROCEDURE IF EXISTS AddColumnIfNotExists;
