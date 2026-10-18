-- ============================================================
-- 记账-库存：出入库原因 + 提供者（归属）+ 盘点留痕
--
-- 对齐《记账App库存投资管理模块产品方案》4.3 / 4.4：
--   ① 状态管理（在库/已售/自用/损耗/转出）在批量模型下 = 出库原因
--      → ledger_stock_flow.reason
--   ② 提供者：谁把这批货放进仓库（归属人）
--      → ledger_product.provider_user_id
--   ③ 盘点（盘盈/盘亏）复用 flow_type='adjust'，增减量即差异，无需新表
--
-- 幂等：information_schema 守卫。
-- ============================================================

-- ① 出入库原因 ------------------------------------------------------
-- 取值：purchase 采购 / sale 销售 / self_use 自用 / loss 损耗 /
--       transfer 移库 / gift 赠送 / stocktake 盘点 / other 其他
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_stock_flow' AND COLUMN_NAME = 'reason');
SET @sql := IF(@has = 0,
  'ALTER TABLE `ledger_stock_flow` ADD COLUMN `reason` varchar(20) DEFAULT NULL COMMENT ''原因：purchase/sale/self_use/loss/transfer/gift/stocktake/other'' AFTER `flow_type`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ② 提供者（归属人）-------------------------------------------------
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_product' AND COLUMN_NAME = 'provider_user_id');
SET @sql := IF(@has = 0,
  'ALTER TABLE `ledger_product` ADD COLUMN `provider_user_id` bigint DEFAULT NULL COMMENT ''提供者：谁把这批货放进仓库（NULL=仓库主）'' AFTER `user_id`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ③ 流水按「仓库 + 时间」查月度统计走索引（已有 idx_warehouse_time，无需新增）
