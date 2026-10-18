-- ============================================================
-- 出库登记：售卖渠道
--
-- 出库三种场景，都要从库存里严格扣减并留痕：
--   ① 卖出   → 有成交价 + 有渠道，计入销售额/毛利
--   ② 自用   → 成交价 0，渠道记「自用」，仍计入统计（不虚增销售额）
--   ③ 损耗   → 过期/损坏等不可抗拒损失，成交价 0，不影响销售额
--
-- 本表即「出库单」：一条流水 = 一次出库登记（轻量，不做 SKU/电商订单）。
--
-- 幂等：information_schema 守卫。
-- ============================================================

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_stock_flow' AND COLUMN_NAME = 'channel_id');
SET @sql := IF(@has = 0,
  'ALTER TABLE `ledger_stock_flow` ADD COLUMN `channel_id` bigint DEFAULT NULL COMMENT ''售卖渠道ID（ledger_sale_channel.id，卖出时填）'' AFTER `sale_price`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
