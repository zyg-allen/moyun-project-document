-- ============================================================
-- 出库流水增加「额外成本」（运费 / 包装费）
-- 只影响本笔成交的毛利，不摊进商品成本价
-- ============================================================
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_stock_flow' AND COLUMN_NAME = 'extra_cost') = 0,
    'ALTER TABLE ledger_stock_flow ADD COLUMN extra_cost DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT ''额外成本（运费/包装费），只影响本笔毛利'' AFTER sale_price',
    'SELECT ''ledger_stock_flow.extra_cost 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;