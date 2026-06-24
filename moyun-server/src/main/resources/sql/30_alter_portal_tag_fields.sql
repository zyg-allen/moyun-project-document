-- =============================================
-- 墨韵智库 - portal_tag 表补充 module / reference_count 字段
-- 执行顺序: 30 (在 29_alter_all_tables_base_fields.sql 之后执行)
-- 描述: portal_tag 表在 03_portal_init.sql 原始 DDL 中缺少 module / reference_count 字段，
--       但 Entity（PortalTag）和 Mapper XML 已引用。本脚本为已存在的数据库补充字段。
-- 兼容: MySQL 8+（使用存储过程安全添加，已存在则跳过，可重复执行）
-- =============================================

-- 安全添加字段的存储过程
DROP PROCEDURE IF EXISTS proc_add_column_if_not_exists;
DELIMITER $$
CREATE PROCEDURE proc_add_column_if_not_exists(
    IN p_table_name VARCHAR(100),
    IN p_column_name VARCHAR(100),
    IN p_column_definition VARCHAR(500),
    IN p_after_column VARCHAR(100)
)
BEGIN
    DECLARE col_count INT;
    SELECT COUNT(*) INTO col_count
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table_name
      AND COLUMN_NAME = p_column_name;

    IF col_count = 0 THEN
        IF p_after_column IS NOT NULL AND p_after_column != '' THEN
            SET @sql = CONCAT('ALTER TABLE `', p_table_name, '` ADD COLUMN ', p_column_definition, ' AFTER `', p_after_column, '`');
        ELSE
            SET @sql = CONCAT('ALTER TABLE `', p_table_name, '` ADD COLUMN ', p_column_definition);
        END IF;
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SELECT CONCAT('已添加字段: ', p_table_name, '.', p_column_name) AS message;
    ELSE
        SELECT CONCAT('字段已存在，跳过: ', p_table_name, '.', p_column_name) AS message;
    END IF;
END$$
DELIMITER ;

-- 安全添加索引的存储过程
DROP PROCEDURE IF EXISTS proc_add_index_if_not_exists;
DELIMITER $$
CREATE PROCEDURE proc_add_index_if_not_exists(
    IN p_table_name VARCHAR(100),
    IN p_index_name VARCHAR(100),
    IN p_index_definition VARCHAR(500)
)
BEGIN
    DECLARE idx_count INT;
    SELECT COUNT(*) INTO idx_count
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table_name
      AND INDEX_NAME = p_index_name;

    IF idx_count = 0 THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table_name, '` ADD INDEX `', p_index_name, '` (', p_index_definition, ')');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SELECT CONCAT('已添加索引: ', p_table_name, '.', p_index_name) AS message;
    ELSE
        SELECT CONCAT('索引已存在，跳过: ', p_table_name, '.', p_index_name) AS message;
    END IF;
END$$
DELIMITER ;

-- =============================================
-- 1. 补充 module 字段
-- =============================================
CALL proc_add_column_if_not_exists(
    'portal_tag',
    'module',
    "`module` varchar(50) DEFAULT NULL COMMENT '所属模块（article/interview_question/interview_experience/interview_resume_template 等，null 表示通用）'",
    'status'
);

-- =============================================
-- 2. 补充 reference_count 字段
-- =============================================
CALL proc_add_column_if_not_exists(
    'portal_tag',
    'reference_count',
    "`reference_count` bigint unsigned DEFAULT 0 COMMENT '被引用次数（冗余计数列，绑定/解绑时同步维护）'",
    'module'
);

-- =============================================
-- 3. 补充索引
-- =============================================
CALL proc_add_index_if_not_exists('portal_tag', 'idx_module', '`module`');
CALL proc_add_index_if_not_exists('portal_tag', 'idx_reference_count', '`reference_count` DESC');

-- =============================================
-- 4. 清理存储过程
-- =============================================
DROP PROCEDURE IF EXISTS proc_add_column_if_not_exists;
DROP PROCEDURE IF EXISTS proc_add_index_if_not_exists;

-- =============================================
-- 5. 完成提示
-- =============================================
SELECT 'portal_tag 表字段补充完成！' AS message;
