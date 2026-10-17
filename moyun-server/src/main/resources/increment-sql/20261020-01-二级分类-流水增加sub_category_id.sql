-- ============================================================
-- 记账-二级分类：ledger_transaction 增加 sub_category_id
--
-- 一级分类 category_id + 二级分类 sub_category_id 并存（都指向 ledger_category）。
-- 未选二级时 sub_category_id 为 null，报表按一级聚合兜底。
-- 幂等：information_schema 守卫。
-- ============================================================

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_transaction' AND COLUMN_NAME = 'sub_category_id');
SET @sql := IF(@has = 0,
  'ALTER TABLE `ledger_transaction` ADD COLUMN `sub_category_id` bigint DEFAULT NULL COMMENT ''二级分类ID（ledger_category.id，未选二级为 null）'' AFTER `category_id`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
