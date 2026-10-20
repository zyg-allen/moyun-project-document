-- ============================================================
-- 库存表（ledger_stock）合并进商品表（ledger_product）
--
-- 背景：按「一个商品只属于一个货架」的规则，ledger_stock 与 ledger_product
--       变成 1:1，两张表存同一件事（货架 + 数量 + 成本值），维护成本高。
--       → 把数量与成本值并入商品，删掉 ledger_stock。
--
-- 合并后模型：
--   仓库 → 货架 → 商品(数量/成本值) → 流水
--
-- ⚠️ 本脚本是「第二阶段」：
--   第一阶段先加列并回填（本文件上半段），代码切完后再执行 DROP。
--   DROP 语句默认注释掉，确认无误后手动放开。
-- ============================================================

-- 1) 商品表增加数量与库存成本值
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_product' AND COLUMN_NAME = 'quantity') = 0,
    'ALTER TABLE ledger_product ADD COLUMN quantity INT NOT NULL DEFAULT 0 COMMENT ''当前库存数量（原 ledger_stock.quantity）'' AFTER shelf_id',
    'SELECT ''ledger_product.quantity 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_product' AND COLUMN_NAME = 'cost_value') = 0,
    'ALTER TABLE ledger_product ADD COLUMN cost_value DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT ''库存成本价值（成本价×数量，原 ledger_stock.cost_value）'' AFTER quantity',
    'SELECT ''ledger_product.cost_value 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) 回填：从 ledger_stock 把数量与成本值搬到商品上
UPDATE ledger_product p
JOIN (
    SELECT product_id, SUM(quantity) AS qty, SUM(cost_value) AS cv
    FROM ledger_stock
    WHERE status = 1
    GROUP BY product_id
) s ON s.product_id = p.id
SET p.quantity = s.qty,
    p.cost_value = s.cv;

-- 3) 第二阶段：代码全部切到 product.quantity 之后再执行（确认后手动放开注释）
-- DROP TABLE IF EXISTS ledger_stock;
