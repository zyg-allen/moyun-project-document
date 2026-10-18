-- ============================================================
-- P0：商品来源 + 成本总额 + 移动加权平均 + 多单位换算 + 采购流水标记
--
-- 依据：记账App-库存成本与商品来源-设计方案-V1.0.md
--
-- ① ledger_product
--    source_type   来源（4 类固定枚举，不可自定义）
--    source_party  供应商 / 赠送方（自由文本）
--    cost_total    成本总额（权威值，单价是派生）
--    extra_cost    其他成本（物流/包装/时间合并为一项）
--    base_unit     基础单位（库存与成本一律按它记）
--    pack_unit     包装单位（可选，如"箱"）
--    pack_size     换算系数（1 箱 = N 基础单位）
-- ② ledger_transaction
--    stock_in      1 = 采购入库产生的支出，消费统计时排除
--
-- 幂等：information_schema 守卫。
-- ============================================================

-- ① ledger_product ------------------------------------------------
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_product' AND COLUMN_NAME = 'source_type');
SET @sql := IF(@has = 0,
  'ALTER TABLE `ledger_product` ADD COLUMN `source_type` varchar(20) NOT NULL DEFAULT ''purchase'' COMMENT ''来源：purchase采购/self_made自产/used_resale自用转卖/gift赠品'' AFTER `source_channel`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_product' AND COLUMN_NAME = 'source_party');
SET @sql := IF(@has = 0,
  'ALTER TABLE `ledger_product` ADD COLUMN `source_party` varchar(100) DEFAULT NULL COMMENT ''供应商/赠送方（自由文本）'' AFTER `source_type`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_product' AND COLUMN_NAME = 'cost_total');
SET @sql := IF(@has = 0,
  'ALTER TABLE `ledger_product` ADD COLUMN `cost_total` decimal(18,2) DEFAULT NULL COMMENT ''成本总额（权威值）'' AFTER `cost_price`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_product' AND COLUMN_NAME = 'extra_cost');
SET @sql := IF(@has = 0,
  'ALTER TABLE `ledger_product` ADD COLUMN `extra_cost` decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT ''其他成本（物流/包装/时间合并）'' AFTER `cost_total`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_product' AND COLUMN_NAME = 'base_unit');
SET @sql := IF(@has = 0,
  'ALTER TABLE `ledger_product` ADD COLUMN `base_unit` varchar(20) DEFAULT NULL COMMENT ''基础单位（库存与成本按它记）'' AFTER `unit`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_product' AND COLUMN_NAME = 'pack_unit');
SET @sql := IF(@has = 0,
  'ALTER TABLE `ledger_product` ADD COLUMN `pack_unit` varchar(20) DEFAULT NULL COMMENT ''包装单位（如 箱）'' AFTER `base_unit`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_product' AND COLUMN_NAME = 'pack_size');
SET @sql := IF(@has = 0,
  'ALTER TABLE `ledger_product` ADD COLUMN `pack_size` int DEFAULT NULL COMMENT ''换算系数：1 包装单位 = N 基础单位'' AFTER `pack_unit`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ② ledger_transaction -------------------------------------------
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_transaction' AND COLUMN_NAME = 'stock_in');
SET @sql := IF(@has = 0,
  'ALTER TABLE `ledger_transaction` ADD COLUMN `stock_in` tinyint NOT NULL DEFAULT 0 COMMENT ''1=采购入库支出，消费统计时排除''',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ③ 回填：老数据 base_unit 取 unit，cost_total 取 cost_price
UPDATE `ledger_product` SET `base_unit` = `unit`
  WHERE `base_unit` IS NULL AND `unit` IS NOT NULL;
UPDATE `ledger_product` SET `cost_total` = `cost_price`
  WHERE `cost_total` IS NULL AND `cost_price` IS NOT NULL;
