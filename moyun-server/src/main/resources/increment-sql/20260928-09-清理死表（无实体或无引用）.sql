-- =============================================================================
-- 清理死表：删除「无实体、无任何代码引用」的遗留表
-- -----------------------------------------------------------------------------
-- 判定规则（用户裁决，v13.27）：**以代码为准** ——
--   · 表有实体且被代码引用  → 必须存在于 DDL（新库可装）；
--   · 表**无实体或实体无任何引用** → 属死表，**表 + 实体 + Mapper + DDL 定义一并删除**；
--   · 迁移备份表（`*_bak_*`）不属业务表，不进 DDL，也不由本脚本处理。
--
-- 本次删除的 9 张表：全仓（Java / Mapper XML / 前端）**引用计数均为 0**
--   （逐表用 @TableName 实体反查 + 关键字精确统计确认）：
--
--     ai_chart_recommendation_rule      0 引用（7 行遗留数据）
--     ai_data_insight                   0 引用
--     ai_document_chunk_metadata        0 引用
--     ai_sql_template                   0 引用（5 行遗留数据）
--     portal_book_chapter_view          0 引用
--     portal_order                      0 引用
--     portal_user_task                  0 引用
--     portal_task                       实体 PortalTask 无任何引用（7 行遗留数据）→ 视为死表
--     sys_job_scan_issue                实体 + Mapper 均无任何引用（原 system/scan 页已删）
--
-- 配套已同步删除：`PortalTask.java`、`SysJobScanIssue.java`、`SysJobScanIssueMapper.java`
--                （+ XML）、以及 DDL 中 `portal_task` / `sys_job_scan_issue` 两处 CREATE TABLE。
--
-- 数据备份：3 张有数据的表已 mysqldump 至 `.archive/drops-20260928/`（可恢复）。
--
-- 幂等性：`information_schema.TABLES` 前置判断 + `DROP TABLE IF EXISTS`，可重复执行。
-- =============================================================================

SET @t := 'ai_chart_recommendation_rule';
SET @exists := (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t);
SET @ddl := IF(@exists = 1, CONCAT('DROP TABLE `', @t, '`'), CONCAT('SELECT ''skip: ', @t, ' 不存在'' AS msg'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @t := 'ai_data_insight';
SET @exists := (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t);
SET @ddl := IF(@exists = 1, CONCAT('DROP TABLE `', @t, '`'), CONCAT('SELECT ''skip: ', @t, ' 不存在'' AS msg'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @t := 'ai_document_chunk_metadata';
SET @exists := (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t);
SET @ddl := IF(@exists = 1, CONCAT('DROP TABLE `', @t, '`'), CONCAT('SELECT ''skip: ', @t, ' 不存在'' AS msg'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @t := 'ai_sql_template';
SET @exists := (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t);
SET @ddl := IF(@exists = 1, CONCAT('DROP TABLE `', @t, '`'), CONCAT('SELECT ''skip: ', @t, ' 不存在'' AS msg'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @t := 'portal_book_chapter_view';
SET @exists := (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t);
SET @ddl := IF(@exists = 1, CONCAT('DROP TABLE `', @t, '`'), CONCAT('SELECT ''skip: ', @t, ' 不存在'' AS msg'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @t := 'portal_order';
SET @exists := (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t);
SET @ddl := IF(@exists = 1, CONCAT('DROP TABLE `', @t, '`'), CONCAT('SELECT ''skip: ', @t, ' 不存在'' AS msg'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @t := 'portal_user_task';
SET @exists := (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t);
SET @ddl := IF(@exists = 1, CONCAT('DROP TABLE `', @t, '`'), CONCAT('SELECT ''skip: ', @t, ' 不存在'' AS msg'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @t := 'portal_task';
SET @exists := (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t);
SET @ddl := IF(@exists = 1, CONCAT('DROP TABLE `', @t, '`'), CONCAT('SELECT ''skip: ', @t, ' 不存在'' AS msg'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @t := 'sys_job_scan_issue';
SET @exists := (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t);
SET @ddl := IF(@exists = 1, CONCAT('DROP TABLE `', @t, '`'), CONCAT('SELECT ''skip: ', @t, ' 不存在'' AS msg'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 复核 SQL（应全部返回 0）
-- ---------------------------------------------------------------------------
-- SELECT COUNT(*) AS remaining FROM information_schema.TABLES
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN
--   ('ai_chart_recommendation_rule','ai_data_insight','ai_document_chunk_metadata','ai_sql_template',
--    'portal_book_chapter_view','portal_order','portal_user_task','portal_task','sys_job_scan_issue');
