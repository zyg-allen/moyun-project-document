-- ============================================================
-- 仓库管理：最后动销时间 + 用户级仓库开关
--
-- ① ledger_product.last_moved_at
--    滞销判定的正确依据。此前报表用 create_time（上架时间）近似，
--    会把「上架很久但一直在卖」的商品误判成滞销。
--    刷新时机：入库/出库/校准/移库 任一导致库存变动的操作。
--
-- ② portal_user.warehouse_enabled
--    是否开启仓库管理功能。关闭后底部「入库/出库」入口隐藏，退回纯记账。
--    存在用户表而非本地，保证换设备/重装后仍然生效。
--
-- 幂等：information_schema 守卫 + 回填只动 NULL 行。
-- ============================================================

-- ① 最后动销时间 -----------------------------------------------------
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_product' AND COLUMN_NAME = 'last_moved_at');
SET @sql := IF(@has = 0,
  'ALTER TABLE `ledger_product` ADD COLUMN `last_moved_at` datetime DEFAULT NULL COMMENT ''最后动销时间（库存变动时刷新，用于滞销判定）'' AFTER `update_time`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 回填：历史商品无动销记录，用 update_time 兜底（比 create_time 更接近真实情况）
UPDATE `ledger_product`
SET `last_moved_at` = `update_time`
WHERE `last_moved_at` IS NULL;

-- ② 用户级仓库开关 ---------------------------------------------------
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_user' AND COLUMN_NAME = 'warehouse_enabled');
SET @sql := IF(@has = 0,
  'ALTER TABLE `portal_user` ADD COLUMN `warehouse_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT ''是否开启仓库管理功能'' AFTER `notify_schedule`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
