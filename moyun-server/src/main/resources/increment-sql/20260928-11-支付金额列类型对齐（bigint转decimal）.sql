-- =============================================================================
-- 支付侧金额列类型对齐：bigint → decimal(18,2)（元，全项目统一口径）
-- -----------------------------------------------------------------------------
-- 排查方式：用 init-sql 建全新库，与现网库逐列比对，发现 8 列类型不一致。
--
-- **以代码与 DDL 为准**判定目标态：
--   · 金额单位全项目统一为「元」（README「金额口径全项目统一为元（v11.31 起）」）；
--   · 实体侧即 BigDecimal：`PayOrder.amount` 为 `BigDecimal`；
--   · 代码不做分/元换算——唯一换算是 `WechatPayChannel` 调微信 API 时
--     `yuan.multiply(100).intValueExact()`（**出参转分**，不是读库换算）；
--     `LedgerAiAnalysisServiceImpl` 等的 `multiply(100)` 是**百分比**，与金额无关；
--   · `moyun-db-ddl.sql` 中这 8 列已是 `decimal(18,2)`，且 `DdlConventionGuardTest`
--     强制金额列精度。
--   ⇒ 现网库这 8 列仍是 `bigint`，属**漏迁移**（值为元，仅存储类型不对）。
--
-- 变更（8 列，全部 bigint → decimal(18,2)）：
--   pay_ledger_entry.amount / balance_after
--   pay_order.amount
--   pay_user_account.balance / total_income / total_withdraw
--   pay_withdraw_order.amount / fee
--
-- 数据影响：bigint → decimal(18,2) 为**无损宽化**（整数值直接成为 N.00），无需数据搬迁。
--          复现原定义见各列 COMMENT 与下方 DEFAULT。
--
-- 幂等性：`information_schema.COLUMNS` 前置判断（仅当当前类型仍为 bigint 时才 MODIFY），
--         可重复执行，第二次全部 skip。
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 通用写法：逐列判断 + MODIFY（保留原 COMMENT 与 NOT NULL/DEFAULT）
-- ---------------------------------------------------------------------------
SET @ddl := (
  SELECT IF(COUNT(*) = 1,
    'ALTER TABLE `pay_user_account` MODIFY COLUMN `balance` decimal(18,2) NOT NULL DEFAULT ''0.00'' COMMENT ''账户余额（元，原子增量维护）''',
    'SELECT ''skip: pay_user_account.balance 已非 bigint'' AS msg')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pay_user_account' AND COLUMN_NAME = 'balance' AND DATA_TYPE = 'bigint'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := (
  SELECT IF(COUNT(*) = 1,
    'ALTER TABLE `pay_user_account` MODIFY COLUMN `total_income` decimal(18,2) NOT NULL DEFAULT ''0.00'' COMMENT ''累计收入（元）''',
    'SELECT ''skip: pay_user_account.total_income 已非 bigint'' AS msg')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pay_user_account' AND COLUMN_NAME = 'total_income' AND DATA_TYPE = 'bigint'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := (
  SELECT IF(COUNT(*) = 1,
    'ALTER TABLE `pay_user_account` MODIFY COLUMN `total_withdraw` decimal(18,2) NOT NULL DEFAULT ''0.00'' COMMENT ''累计提现（元）''',
    'SELECT ''skip: pay_user_account.total_withdraw 已非 bigint'' AS msg')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pay_user_account' AND COLUMN_NAME = 'total_withdraw' AND DATA_TYPE = 'bigint'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := (
  SELECT IF(COUNT(*) = 1,
    'ALTER TABLE `pay_order` MODIFY COLUMN `amount` decimal(18,2) NOT NULL COMMENT ''支付金额（元）''',
    'SELECT ''skip: pay_order.amount 已非 bigint'' AS msg')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pay_order' AND COLUMN_NAME = 'amount' AND DATA_TYPE = 'bigint'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := (
  SELECT IF(COUNT(*) = 1,
    'ALTER TABLE `pay_ledger_entry` MODIFY COLUMN `amount` decimal(18,2) NOT NULL COMMENT ''变动金额（元，正负号表方向）''',
    'SELECT ''skip: pay_ledger_entry.amount 已非 bigint'' AS msg')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pay_ledger_entry' AND COLUMN_NAME = 'amount' AND DATA_TYPE = 'bigint'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := (
  SELECT IF(COUNT(*) = 1,
    'ALTER TABLE `pay_ledger_entry` MODIFY COLUMN `balance_after` decimal(18,2) DEFAULT NULL COMMENT ''变动后余额（元）''',
    'SELECT ''skip: pay_ledger_entry.balance_after 已非 bigint'' AS msg')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pay_ledger_entry' AND COLUMN_NAME = 'balance_after' AND DATA_TYPE = 'bigint'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := (
  SELECT IF(COUNT(*) = 1,
    'ALTER TABLE `pay_withdraw_order` MODIFY COLUMN `amount` decimal(18,2) NOT NULL COMMENT ''提现金额（元）''',
    'SELECT ''skip: pay_withdraw_order.amount 已非 bigint'' AS msg')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pay_withdraw_order' AND COLUMN_NAME = 'amount' AND DATA_TYPE = 'bigint'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := (
  SELECT IF(COUNT(*) = 1,
    'ALTER TABLE `pay_withdraw_order` MODIFY COLUMN `fee` decimal(18,2) NOT NULL DEFAULT ''0.00'' COMMENT ''手续费（元）''',
    'SELECT ''skip: pay_withdraw_order.fee 已非 bigint'' AS msg')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pay_withdraw_order' AND COLUMN_NAME = 'fee' AND DATA_TYPE = 'bigint'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 复核 SQL（应全部为 decimal(18,2)，且 0 行为 bigint）
-- ---------------------------------------------------------------------------
-- SELECT table_name, column_name, column_type FROM information_schema.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE()
--    AND ((table_name='pay_user_account' AND column_name IN ('balance','total_income','total_withdraw'))
--      OR (table_name='pay_order' AND column_name='amount')
--      OR (table_name='pay_ledger_entry' AND column_name IN ('amount','balance_after'))
--      OR (table_name='pay_withdraw_order' AND column_name IN ('amount','fee')))
--  ORDER BY table_name, column_name;
