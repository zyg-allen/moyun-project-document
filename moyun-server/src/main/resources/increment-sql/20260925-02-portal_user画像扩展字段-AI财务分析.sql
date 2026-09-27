-- =============================================================
-- 记账财务分析整改 v2：portal_user 画像扩展字段（AI 财务分析基础数据）
-- 日期：2026-09-25
-- 依据：docs/05-方案设计-分模块/2-记账模块/记账财务分析整改方案_0925_v2.md
--       3.2 用户画像查询 + 七、整改清单 #5（扩展用户画像字段）
-- 说明：画像字段为 AI 财务分析个性化上下文（风险偏好/行业/家庭负担/
--       负债分析/收入结构）与前端「个人信息维护」表单的数据基础；
--       全部可空，不迁移存量数据。
--
-- ── v13.18 修订（报告 §6.4「增量脚本不可重跑」）─────────────────────────
-- 原实现是单条多列 ALTER ... ADD COLUMN：库已迁移时重跑报 ERROR 1060
-- (Duplicate column name) 并**中断脚本**。现改为"逐列 + information_schema 前置判断"，
-- 本脚本可任意次重跑（已存在的列自动跳过；也容忍"上次执行中途失败"的部分迁移状态）。
-- 执行：需选择库（脚本用 DATABASE()）。
-- =============================================================

-- 1. industry
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_user' AND COLUMN_NAME = 'industry');
SET @sql := IF(@has = 0,
    'ALTER TABLE `portal_user` ADD COLUMN `industry` VARCHAR(100) NULL COMMENT ''行业（AI财务分析画像：行业分析）'' AFTER `school`',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. marital_status
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_user' AND COLUMN_NAME = 'marital_status');
SET @sql := IF(@has = 0,
    'ALTER TABLE `portal_user` ADD COLUMN `marital_status` VARCHAR(20) NULL COMMENT ''婚姻状况（AI财务分析画像：家庭负担；single/married/other）'' AFTER `industry`',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. has_mortgage
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_user' AND COLUMN_NAME = 'has_mortgage');
SET @sql := IF(@has = 0,
    'ALTER TABLE `portal_user` ADD COLUMN `has_mortgage` TINYINT NULL COMMENT ''是否有房贷（AI财务分析画像：负债分析；1=是 0=否）'' AFTER `marital_status`',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4. has_side_income
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_user' AND COLUMN_NAME = 'has_side_income');
SET @sql := IF(@has = 0,
    'ALTER TABLE `portal_user` ADD COLUMN `has_side_income` TINYINT NULL COMMENT ''是否有副业收入（AI财务分析画像：收入结构；1=是 0=否）'' AFTER `has_mortgage`',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5. income_types
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_user' AND COLUMN_NAME = 'income_types');
SET @sql := IF(@has = 0,
    'ALTER TABLE `portal_user` ADD COLUMN `income_types` VARCHAR(200) NULL COMMENT ''收入类型（AI财务分析画像：逗号分隔，如 salary,investment,rent）'' AFTER `has_side_income`',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 复核：期望 5 行
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_user'
  AND COLUMN_NAME IN ('industry', 'marital_status', 'has_mortgage', 'has_side_income', 'income_types')
ORDER BY ORDINAL_POSITION;
