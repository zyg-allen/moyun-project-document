-- ============================================================
-- 增量脚本（存量库执行件）
--
-- 对应 init-sql 的目标态：moyun-db-ddl.sql 中已含这三张表
-- （见 README「init-sql 是目标态，increment-sql 是存量库执行件，两者必须同步修改」）
--
-- ⚠️ 与 init 版的关键差异：
--   init 版为「重建目标态」会 DROP TABLE IF EXISTS；
--   本文件面向**已有数据的线上库**，一律 CREATE TABLE IF NOT EXISTS，
--   绝不 DROP —— 否则一次升级就会抹掉全部应收/应付/核销记录。
--
-- ⚠️ 末尾两条 ALTER 非幂等：MySQL 8.0 不支持 ADD COLUMN IF NOT EXISTS。
--   存量库执行前请先确认列不存在：
--     SHOW COLUMNS FROM ledger_transaction LIKE 'receivable_id';
--   已存在则跳过该 ALTER（报 1060 Duplicate column name 即表示已加过）。
-- ============================================================
-- ============================================================
-- 记账-债权债务（应收/应付）与核销流水
-- 新增时间：2026-10-15
--
-- 设计说明（与《库存投资管理模块_核心数据表SQL建表语句.sql》的差异，均为刻意取舍）：
--
-- 1) 表名统一加 ledger_ 前缀：库内该域名下 14 张表全部是 ledger_*，
--    参考文档里的裸名（receivable/payable/transaction）是本项目的 SQLite 原型设计稿，
--    直接照搬会破坏「按前缀识别业务域」的既有约定。
--
-- 2) 结算状态用 settle_status，而不是 status：
--    status 在全项目（ledger_asset_account / ledger_liability_account /
--    ledger_schedule_task …）已经是「行状态：1=正常 0=已删除」的固定语义。
--    若把 pending/partial/settled 塞进 status，同一列在不同表里含义不同，
--    后续所有通用逻辑（逻辑删除、状态过滤）都会踩雷。
--
-- 3) 金额口径：
--    total_amount 债务总额；paid_amount 已核销额；余额 = total - paid（不落列，避免三方不一致）。
--    settle_status 由 paid_amount 与 total_amount 的关系推导并冗余存储，便于列表直接筛选。
--
-- 4) 软删除：与全项目一致用 status + del_flag 双轨（此处只需 status）。
-- ============================================================

-- -----------------------------------------------------------
-- 1. 应收款（别人欠我的钱）
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ledger_obligation` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '往来ID',
    `user_id` bigint NOT NULL COMMENT '归属用户ID（主体）',
    `type` varchar(20) NOT NULL DEFAULT 'receivable' COMMENT '类型：receivable=应收 payable=应付（后期可加 investment）',
    `counterparty` varchar(100) NOT NULL COMMENT '对方名称',
    `counterparty_id` bigint DEFAULT NULL COMMENT '对方用户ID（对方也是本系统用户时）',
    `warehouse_id` bigint DEFAULT NULL COMMENT '关联仓库ID（赊销/赊购产生时）',
    `source_type` varchar(30) NOT NULL DEFAULT 'manual' COMMENT '来源：manual=手工 lend=借出 sale_credit=赊销 purchase_credit=赊购 borrow=借款',
    `source_id` bigint DEFAULT NULL COMMENT '来源单据ID',
    `total_amount` decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '总额（元）',
    `paid_amount` decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '已结金额（元）；未结余额 = total - paid，不落列',
    `settle_status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT '结算状态：pending=未结 partial=部分结 settled=已结清',
    `due_date` date DEFAULT NULL COMMENT '到期日',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '行状态：1=正常 0=已删除',
    `create_by` varchar(64) DEFAULT NULL,
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_type` (`user_id`,`type`,`status`),
    KEY `idx_settle` (`settle_status`),
    KEY `idx_source` (`source_type`,`source_id`),
    KEY `idx_due_date` (`due_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='记账-债权债务统一表（应收/应付，一张表+type）';

-- -----------------------------------------------------------
-- 3. 债务核销流水（每一次收款/还款都留痕）
--
-- 为什么单独一张表而不只累加 paid_amount：
--   ① 可追溯——用户要知道"这笔钱是分几次、每次多少、走的哪个账户、谁操作的"；
--   ② 可撤销——删掉一条核销记录就能回滚 paid_amount，不必猜原始值；
--   ③ 与库存/资金流水对账时，它是唯一的中间凭据。
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ledger_debt_repayment` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '核销流水ID',
    `user_id` bigint NOT NULL COMMENT '门户用户ID（冗余，便于按用户直接查全部核销）',
    `debt_type` varchar(20) NOT NULL COMMENT '债务类型：receivable=应收 payable=应付',
    `debt_id` bigint NOT NULL COMMENT '对应 ledger_receivable.id 或 ledger_payable.id',
    `amount` decimal(18,2) NOT NULL COMMENT '本次核销金额（元）',
    `account_id` bigint DEFAULT NULL COMMENT '资金账户ID（钱进/出哪个账户）',
    `transaction_id` bigint DEFAULT NULL COMMENT '关联的资金流水ID（ledger_transaction.id）',
    `repay_date` date NOT NULL COMMENT '核销日期',
    `operator_id` bigint DEFAULT NULL COMMENT '操作人用户ID（多人协作用于审计）',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '行状态：1=正常 0=已撤销',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_debt` (`debt_type`,`debt_id`,`status`),
    KEY `idx_user_date` (`user_id`,`repay_date`),
    KEY `idx_transaction` (`transaction_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='记账-债务核销流水';

-- -----------------------------------------------------------
-- 4. 扩展 ledger_transaction：建立与债权债务的双向关联
--
-- 用途：七种记账类型里的「借出/收款」挂 receivable_id，「借款/还款」挂 payable_id，
--       这样从一条流水能直接跳到对应债务，从一笔债务也能列出它的所有资金流水。
-- 不加外键约束：与库内既有 ledger_* 表一致（靠应用层保证），便于后续分库/归档。
-- -----------------------------------------------------------
-- 关联列（幂等：MySQL 8.0 无 ADD COLUMN IF NOT EXISTS，先查 information_schema 再决定）
SET @has_recv := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_transaction' AND COLUMN_NAME = 'receivable_id');
SET @sql := IF(@has_recv = 0,
  'ALTER TABLE `ledger_transaction` ADD COLUMN `receivable_id` bigint DEFAULT NULL COMMENT ''关联应收款ID（借出/收款时填写）'' AFTER `liability_id`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_pay := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_transaction' AND COLUMN_NAME = 'payable_id');
SET @sql := IF(@has_pay = 0,
  'ALTER TABLE `ledger_transaction` ADD COLUMN `payable_id` bigint DEFAULT NULL COMMENT ''关联应付款ID（借款/还款时填写）'' AFTER `receivable_id`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 索引（幂等）
SET @has_i1 := (SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_transaction' AND INDEX_NAME = 'idx_receivable');
SET @sql := IF(@has_i1 = 0,
  'ALTER TABLE `ledger_transaction` ADD KEY `idx_receivable` (`receivable_id`)',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_i2 := (SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_transaction' AND INDEX_NAME = 'idx_payable');
SET @sql := IF(@has_i2 = 0,
  'ALTER TABLE `ledger_transaction` ADD KEY `idx_payable` (`payable_id`)',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
