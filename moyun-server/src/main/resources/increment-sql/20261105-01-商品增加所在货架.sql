-- ============================================================
-- 商品增加所在货架（一个商品只属于一个货架）
--
-- 背景：原模型里货架只存在 ledger_stock.shelf_id，
--       商品可以「按货架分布」，导致入库时选了别的货架会报没库存、
--       盘点还要先选货架等问题。
--       实际业务：一批货 5 件要放两个货架，就是**分别登记两个商品**
--       （3 件登记一个、2 件登记一个），由用户决定怎么拆。
--
-- 因此把货架上移到商品：ledger_product.shelf_id
--   · 0 表示未上架（兼容历史数据 / 用户先建档后上架）
--   · 已有商品的 shelf_id 从它当前库存最多的货架回填
-- ============================================================

-- 1) 加列
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_product' AND COLUMN_NAME = 'shelf_id') = 0,
    'ALTER TABLE ledger_product ADD COLUMN shelf_id BIGINT NOT NULL DEFAULT 0 COMMENT ''所在货架（0=未上架）'' AFTER warehouse_id',
    'SELECT ''ledger_product.shelf_id 已存在'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) 索引（按货架筛商品）
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_product' AND INDEX_NAME = 'idx_product_shelf') = 0,
    'ALTER TABLE ledger_product ADD INDEX idx_product_shelf (shelf_id)',
    'SELECT ''idx_product_shelf 已存在'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3) 回填：已有商品取它库存最多的那个货架
UPDATE ledger_product p
JOIN (
    SELECT s.product_id, s.shelf_id
    FROM ledger_stock s
    JOIN (
        SELECT product_id, MAX(quantity) AS mq
        FROM ledger_stock
        WHERE status = 1
        GROUP BY product_id
    ) t ON t.product_id = s.product_id AND t.mq = s.quantity
    WHERE s.status = 1
    GROUP BY s.product_id, s.shelf_id
) x ON x.product_id = p.id
SET p.shelf_id = x.shelf_id
WHERE p.shelf_id = 0;
