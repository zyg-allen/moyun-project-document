-- =============================================================================
-- 补建漏建的表 sys_config_log（参数配置变更审计日志）
-- -----------------------------------------------------------------------------
-- 缺陷（v13.27 排查发现）：
--   `SysConfigServiceImpl.updateConfig` 在**同一事务内**写入变更审计：
--     configLogMapper.insert(configLog)   // L193
--   但**现网库不存在 `sys_config_log` 表**（DDL `moyun-db-ddl.sql` L3833 有定义 → 属"库漏建"）。
--   后果：后台【参数设置】任何一次修改都会因 `Table 'moyun-db.sys_config_log' doesn't exist`
--   抛异常并**整体回滚** → 参数改不了。
--
-- 处置：按 DDL 定义补建该表（列/索引/注释与 DDL 完全一致），幂等可重跑。
--
-- 幂等性：`information_schema.TABLES` + `KEY_COLUMN_USAGE` 前置判断，
--         表已存在或索引已存在则跳过（不报 1050/1061）。
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. 建表（仅当不存在）
-- ---------------------------------------------------------------------------
SET @tbl_exists := (
  SELECT COUNT(*) FROM information_schema.TABLES
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_config_log'
);
SET @ddl := IF(@tbl_exists = 0,
  'CREATE TABLE `sys_config_log` (
     `id` bigint NOT NULL AUTO_INCREMENT COMMENT ''日志主键'',
     `config_id` bigint NOT NULL COMMENT ''参数主键（逻辑关联 sys_config.config_id，无物理外键）'',
     `config_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '''' COMMENT ''参数键名（冗余快照，便于审计检索）'',
     `old_value` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT ''变更前键值'',
     `new_value` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT ''变更后键值'',
     `operate_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT ''UPDATE'' COMMENT ''操作类型（UPDATE）'',
     `oper_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '''' COMMENT ''操作人员'',
     `oper_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '''' COMMENT ''操作IP'',
     `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'',
     PRIMARY KEY (`id`),
     KEY `idx_config_id` (`config_id`),
     KEY `idx_config_key` (`config_key`)
   ) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT=''参数配置变更日志表（sys_config 审计留痕）''',
  'SELECT ''skip: sys_config_log 已存在'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 2. 索引兜底（表已存在但索引缺失时补齐；1061 前置判断）
-- ---------------------------------------------------------------------------
SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_config_log' AND INDEX_NAME = 'idx_config_id'
);
SET @ddl := IF(@idx_exists = 0,
  'ALTER TABLE `sys_config_log` ADD KEY `idx_config_id` (`config_id`)',
  'SELECT ''skip: idx_config_id 已存在'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_config_log' AND INDEX_NAME = 'idx_config_key'
);
SET @ddl := IF(@idx_exists = 0,
  'ALTER TABLE `sys_config_log` ADD KEY `idx_config_key` (`config_key`)',
  'SELECT ''skip: idx_config_key 已存在'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 3. 复核 SQL（应返回 1 行、3 个索引）
-- ---------------------------------------------------------------------------
-- SELECT COUNT(*) AS table_exists FROM information_schema.TABLES
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_config_log';
-- SELECT index_name, GROUP_CONCAT(column_name) FROM information_schema.STATISTICS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_config_log' GROUP BY index_name;
