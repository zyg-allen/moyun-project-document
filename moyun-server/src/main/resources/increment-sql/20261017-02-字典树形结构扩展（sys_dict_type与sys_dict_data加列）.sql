-- =============================================================================
-- 字典树形结构扩展：sys_dict_type / sys_dict_data 加列
-- -----------------------------------------------------------------------------
-- 背景：现有字典为 dict_type 1 对多 dict_data 的扁平两级结构，需要扩展「树形字典」：
--   · sys_dict_type  加 platform_code（端代码，下拉选择 sys_platform.platform_code，空=全局）
--                     加 is_tree（结构类型：0=普通字典 1=树形字典）
--   · sys_dict_data  加 platform_code（端代码，下拉选择）
--                     加 parent_dict_id（父字典编码，自关联 dict_code，0=一级）
--                     加 is_first_level（是否一级：0=否 1=是）
--   存量数据全部落到「普通/全局」默认语义，不影响既有普通字典功能。
--
-- 幂等性：information_schema.COLUMNS 前置判断（ADD COLUMN 重复执行报 1060），可重复执行。
-- =============================================================================

-- ---------- sys_dict_type.platform_code ----------
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_dict_type' AND COLUMN_NAME = 'platform_code');
SET @ddl := IF(@has = 0,
  'ALTER TABLE `sys_dict_type` ADD COLUMN `platform_code` varchar(50) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT ''端代码（sys_platform.platform_code，空=全局）'' AFTER `status`',
  'SELECT ''skip: sys_dict_type.platform_code 已存在'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- sys_dict_type.is_tree ----------
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_dict_type' AND COLUMN_NAME = 'is_tree');
SET @ddl := IF(@has = 0,
  'ALTER TABLE `sys_dict_type` ADD COLUMN `is_tree` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT ''0'' COMMENT ''结构类型（0=普通字典 1=树形字典）'' AFTER `platform_code`',
  'SELECT ''skip: sys_dict_type.is_tree 已存在'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- sys_dict_data.platform_code ----------
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_dict_data' AND COLUMN_NAME = 'platform_code');
SET @ddl := IF(@has = 0,
  'ALTER TABLE `sys_dict_data` ADD COLUMN `platform_code` varchar(50) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT ''端代码（sys_platform.platform_code，空=全局）'' AFTER `status`',
  'SELECT ''skip: sys_dict_data.platform_code 已存在'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- sys_dict_data.parent_dict_id ----------
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_dict_data' AND COLUMN_NAME = 'parent_dict_id');
SET @ddl := IF(@has = 0,
  'ALTER TABLE `sys_dict_data` ADD COLUMN `parent_dict_id` bigint NOT NULL DEFAULT 0 COMMENT ''父字典编码（sys_dict_data.dict_code，0=一级）'' AFTER `platform_code`',
  'SELECT ''skip: sys_dict_data.parent_dict_id 已存在'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- sys_dict_data.is_first_level ----------
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_dict_data' AND COLUMN_NAME = 'is_first_level');
SET @ddl := IF(@has = 0,
  'ALTER TABLE `sys_dict_data` ADD COLUMN `is_first_level` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT ''0'' COMMENT ''是否一级（0=否 1=是）'' AFTER `parent_dict_id`',
  'SELECT ''skip: sys_dict_data.is_first_level 已存在'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 复核 SQL
-- ---------------------------------------------------------------------------
-- SELECT table_name, column_name, column_type, is_nullable, column_default, column_comment
-- FROM information_schema.COLUMNS
-- WHERE TABLE_SCHEMA = DATABASE()
--   AND table_name IN ('sys_dict_type','sys_dict_data')
--   AND column_name IN ('platform_code','is_tree','parent_dict_id','is_first_level')
-- ORDER BY table_name, ordinal_position;
