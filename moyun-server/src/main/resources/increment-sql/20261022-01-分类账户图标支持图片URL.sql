-- ============================================================
-- 分类/账户图标支持图片 URL：icon 列由 varchar(50) 扩宽到 varchar(255)
--
-- 原 icon 存 emoji key（如 food），现同时允许存上传图片地址
-- （如 /profile/upload/2026/10/18/xxx.png），50 长度不够。
-- 幂等：仅当当前长度小于 255 时才 MODIFY。
-- ============================================================

SET @need := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_category'
    AND COLUMN_NAME = 'icon' AND CHARACTER_MAXIMUM_LENGTH < 255);
SET @sql := IF(@need > 0,
  'ALTER TABLE `ledger_category` MODIFY COLUMN `icon` varchar(255) DEFAULT NULL COMMENT ''图标：emoji key 或图片URL''',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @need := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_asset_account'
    AND COLUMN_NAME = 'icon' AND CHARACTER_MAXIMUM_LENGTH < 255);
SET @sql := IF(@need > 0,
  'ALTER TABLE `ledger_asset_account` MODIFY COLUMN `icon` varchar(255) DEFAULT NULL COMMENT ''图标：emoji key 或图片URL''',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @need := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_liability_account'
    AND COLUMN_NAME = 'icon' AND CHARACTER_MAXIMUM_LENGTH < 255);
SET @sql := IF(@need > 0,
  'ALTER TABLE `ledger_liability_account` MODIFY COLUMN `icon` varchar(255) DEFAULT NULL COMMENT ''图标：emoji key 或图片URL''',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
