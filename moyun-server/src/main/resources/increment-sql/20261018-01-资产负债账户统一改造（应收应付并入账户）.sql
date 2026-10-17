-- ============================================================
-- 增量脚本：资产/负债账户表统一改造（方案A：应收/应付并入两张账户表）
--
-- 背景：废弃独立的 ledger_obligation（应收/应付）与 ledger_debt_repayment（核销流水），
-- 应收/应付统一为「账户」，落在 ledger_asset_account / ledger_liability_account，
-- 用 account_type 字段区分（第二层账户分类）。核销=记一笔流水+更新账户，不再单独留表。
--
-- 不考虑历史数据（新系统），故 DROP TABLE 旧表即可。
-- 幂等：ADD/DROP COLUMN 均带 information_schema 守卫。
-- ============================================================

-- ==================== ledger_asset_account ====================
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ledger_asset_account' AND COLUMN_NAME='account_type');
SET @sql := IF(@has=0, 'ALTER TABLE `ledger_asset_account` ADD COLUMN `account_type` varchar(20) NOT NULL DEFAULT ''fund'' COMMENT ''账户类型：fund资金/receivable应收/investment投资/fixed固定'' AFTER `name`', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ledger_asset_account' AND COLUMN_NAME='description');
SET @sql := IF(@has=0, 'ALTER TABLE `ledger_asset_account` ADD COLUMN `description` varchar(500) DEFAULT NULL COMMENT ''描述'' AFTER `account_type`', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ledger_asset_account' AND COLUMN_NAME='total_amount');
SET @sql := IF(@has=0, 'ALTER TABLE `ledger_asset_account` ADD COLUMN `total_amount` decimal(18,2) DEFAULT NULL COMMENT ''总额（应收/投资/固定类用）'' AFTER `balance`', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ledger_asset_account' AND COLUMN_NAME='paid_amount');
SET @sql := IF(@has=0, 'ALTER TABLE `ledger_asset_account` ADD COLUMN `paid_amount` decimal(18,2) DEFAULT NULL COMMENT ''已收（应收类用）'' AFTER `total_amount`', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ledger_asset_account' AND COLUMN_NAME='settle_status');
SET @sql := IF(@has=0, 'ALTER TABLE `ledger_asset_account` ADD COLUMN `settle_status` varchar(20) DEFAULT NULL COMMENT ''结算状态：pending/partial/settled'' AFTER `paid_amount`', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ledger_asset_account' AND COLUMN_NAME='due_date');
SET @sql := IF(@has=0, 'ALTER TABLE `ledger_asset_account` ADD COLUMN `due_date` date DEFAULT NULL COMMENT ''到期日（应收类用）'' AFTER `settle_status`', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ledger_asset_account' AND COLUMN_NAME='risk_level');
SET @sql := IF(@has=0, 'ALTER TABLE `ledger_asset_account` ADD COLUMN `risk_level` varchar(20) NOT NULL DEFAULT ''normal'' COMMENT ''风险标记：normal/attention/bad_debt'' AFTER `due_date`', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 废弃 type 字段
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ledger_asset_account' AND COLUMN_NAME='type');
SET @sql := IF(@has=1, 'ALTER TABLE `ledger_asset_account` DROP COLUMN `type`', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ==================== ledger_liability_account ====================
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ledger_liability_account' AND COLUMN_NAME='account_type');
SET @sql := IF(@has=0, 'ALTER TABLE `ledger_liability_account` ADD COLUMN `account_type` varchar(20) NOT NULL DEFAULT ''debt'' COMMENT ''账户类型：debt普通负债/credit赊账应付'' AFTER `name`', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ledger_liability_account' AND COLUMN_NAME='description');
SET @sql := IF(@has=0, 'ALTER TABLE `ledger_liability_account` ADD COLUMN `description` varchar(500) DEFAULT NULL COMMENT ''描述'' AFTER `account_type`', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ledger_liability_account' AND COLUMN_NAME='total_amount');
SET @sql := IF(@has=0, 'ALTER TABLE `ledger_liability_account` ADD COLUMN `total_amount` decimal(18,2) DEFAULT NULL COMMENT ''总额（赊账类用）'' AFTER `balance`', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ledger_liability_account' AND COLUMN_NAME='paid_amount');
SET @sql := IF(@has=0, 'ALTER TABLE `ledger_liability_account` ADD COLUMN `paid_amount` decimal(18,2) DEFAULT NULL COMMENT ''已还（赊账类用）'' AFTER `total_amount`', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ledger_liability_account' AND COLUMN_NAME='settle_status');
SET @sql := IF(@has=0, 'ALTER TABLE `ledger_liability_account` ADD COLUMN `settle_status` varchar(20) DEFAULT NULL COMMENT ''结算状态'' AFTER `paid_amount`', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ledger_liability_account' AND COLUMN_NAME='risk_level');
SET @sql := IF(@has=0, 'ALTER TABLE `ledger_liability_account` ADD COLUMN `risk_level` varchar(20) NOT NULL DEFAULT ''normal'' COMMENT ''风险标记'' AFTER `due_date`', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 废弃 type 字段
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ledger_liability_account' AND COLUMN_NAME='type');
SET @sql := IF(@has=1, 'ALTER TABLE `ledger_liability_account` DROP COLUMN `type`', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ==================== 删除旧表（应收/应付独立表 + 核销流水，方案A已并入账户） ====================
DROP TABLE IF EXISTS `ledger_obligation`;
DROP TABLE IF EXISTS `ledger_debt_repayment`;
