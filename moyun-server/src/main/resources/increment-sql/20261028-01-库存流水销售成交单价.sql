-- ============================================================
-- 库存流水：销售成交单价
--
-- 出库原因为「销售」时，可选填成交单价。
-- 有了它，仓库报表就能算真实销售额与毛利，而不只是销售成本；
-- 又不必引入「销售单据」这种进销存重量，仍然是一个输入框的轻量。
--
-- 销售额     = SUM(sale_price × 出库数量)  （reason=sale）
-- 销售成本   = SUM(cost_before - cost_after)（reason=sale）
-- 毛利       = 销售额 - 销售成本
--
-- 幂等：information_schema 守卫。
-- ============================================================

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_stock_flow' AND COLUMN_NAME = 'sale_price');
SET @sql := IF(@has = 0,
  'ALTER TABLE `ledger_stock_flow` ADD COLUMN `sale_price` decimal(18,2) DEFAULT NULL COMMENT ''销售成交单价（仅出库原因为销售时填写）'' AFTER `cost_after`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
