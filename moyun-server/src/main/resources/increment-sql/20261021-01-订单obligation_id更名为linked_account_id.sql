-- ============================================================
-- 订单表：obligation_id 重命名为 linked_account_id
--
-- 方案A 后「赊账」建的是应收/应付账户（ledger_asset_account / ledger_liability_account），
-- 不再是已删除的 ledger_obligation，列名同步更名以免误导。
-- 幂等：information_schema 守卫，旧列不存在则跳过。
-- ============================================================

-- ① 销售订单：列改名
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_sale_order' AND COLUMN_NAME = 'obligation_id');
SET @sql := IF(@has > 0,
  'ALTER TABLE `ledger_sale_order` CHANGE COLUMN `obligation_id` `linked_account_id` bigint DEFAULT NULL COMMENT ''赊账时关联的应收/应付账户ID（ledger_asset_account/ledger_liability_account）''',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ② 销售订单：索引改名
SET @has := (SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_sale_order' AND INDEX_NAME = 'idx_obligation');
SET @sql := IF(@has > 0,
  'ALTER TABLE `ledger_sale_order` RENAME INDEX `idx_obligation` TO `idx_linked_account`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ③ 采购订单：列改名
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_purchase_order' AND COLUMN_NAME = 'obligation_id');
SET @sql := IF(@has > 0,
  'ALTER TABLE `ledger_purchase_order` CHANGE COLUMN `obligation_id` `linked_account_id` bigint DEFAULT NULL COMMENT ''赊账时关联的应收/应付账户ID（ledger_asset_account/ledger_liability_account）''',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ④ 采购订单：索引改名
SET @has := (SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_purchase_order' AND INDEX_NAME = 'idx_obligation');
SET @sql := IF(@has > 0,
  'ALTER TABLE `ledger_purchase_order` RENAME INDEX `idx_obligation` TO `idx_linked_account`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
