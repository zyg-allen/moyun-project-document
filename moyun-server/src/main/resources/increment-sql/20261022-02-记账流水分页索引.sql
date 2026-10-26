-- ==============================================================================
-- 20261022-02 记账流水分页复合索引
-- 背景：分页固定按 (user_id, status) 过滤、按 transaction_date/id 倒序；
--       原 idx_user_status_id 缺少 date，数据量上来后会 filesort。
-- 幂等：索引存在则跳过；不涉及 sys_menu。
-- ==============================================================================

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ledger_transaction'
    AND INDEX_NAME = 'idx_user_status_date'
);

SET @ddl := IF(@idx_exists = 0,
  'ALTER TABLE `ledger_transaction` ADD INDEX `idx_user_status_date` (`user_id`,`status`,`transaction_date`,`id`)',
  'SELECT ''idx_user_status_date already exists'' AS msg');

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
