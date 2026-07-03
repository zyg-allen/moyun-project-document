-- =====================================================
-- 付费阅读功能 DDL 脚本（任务 2.3）
-- 为 portal_article 表安全添加付费阅读字段，并预置平台抽成配置
-- 幂等设计：使用存储过程判断列是否存在，不存在才添加
-- 执行顺序：本脚本在 58_tip_init.sql 之后执行
-- =====================================================

-- -----------------------------------------------------
-- 存储过程：安全添加列（不存在时才添加）
-- 参考 28_alter_tables_add_base_fields.sql 风格
-- -----------------------------------------------------
DROP PROCEDURE IF EXISTS `AddColumnIfNotExists`;
DELIMITER $$
CREATE PROCEDURE `AddColumnIfNotExists`(
    IN p_table_name  VARCHAR(100),
    IN p_column_name VARCHAR(100),
    IN p_definition  VARCHAR(500)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = p_table_name
          AND COLUMN_NAME  = p_column_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table_name,
                          '` ADD COLUMN `', p_column_name, '` ', p_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- =====================================================
-- 一、portal_article 表添加付费阅读字段
-- =====================================================

-- 是否付费阅读 0/1
CALL AddColumnIfNotExists('portal_article', 'is_paid', "TINYINT NOT NULL DEFAULT 0 COMMENT '是否付费阅读 0=免费 1=付费'");

-- 付费内容（付费后可见的完整内容）
CALL AddColumnIfNotExists('portal_article', 'paid_content', "LONGTEXT COMMENT '付费内容（购买后可见）'");

-- 试读字数（未购买用户可预览的字数）
CALL AddColumnIfNotExists('portal_article', 'preview_length', "INT NOT NULL DEFAULT 0 COMMENT '试读字数（未购买可预览的字数）'");

-- 付费价格
CALL AddColumnIfNotExists('portal_article', 'price', "DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '付费价格，0=免费'");

-- =====================================================
-- 二、平台抽成比例配置（sys_config）
-- =====================================================
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '打赏-平台抽成比例', 'platform_fee_rate', '0.1', 'Y', 'admin', NOW(),
       '打赏/付费阅读平台抽成比例，默认0.1（10%），简化实现仅记录不做实际转账'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'platform_fee_rate');

-- =====================================================
-- 清理存储过程
-- =====================================================
DROP PROCEDURE IF EXISTS `AddColumnIfNotExists`;
