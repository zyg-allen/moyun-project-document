-- ============================================================
-- 清理商品表遗留字段
--   category       被 category_id（两级类目表）完全取代，恒为 NULL
--   is_secondhand  与 source_type = used_resale 语义重复，恒为 0
-- 保留 condition_grade（二手成色，有实际定价价值）
-- ============================================================
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_product' AND COLUMN_NAME = 'category') > 0,
    'ALTER TABLE ledger_product DROP COLUMN category',
    'SELECT ''ledger_product.category 已删除'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_product' AND COLUMN_NAME = 'is_secondhand') > 0,
    'ALTER TABLE ledger_product DROP COLUMN is_secondhand',
    'SELECT ''ledger_product.is_secondhand 已删除'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;