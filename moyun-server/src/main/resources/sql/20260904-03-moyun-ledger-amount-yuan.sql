-- ===================================================================
-- 记账模块·金额单位统一为元（v11.26）
-- 将所有金额列从 BIGINT（分）改为 DECIMAL(18,2)（元）
-- 执行顺序：先 ALTER→DECIMAL(20,2) 保留原始整数→ UPDATE /100 → ALTER→DECIMAL(18,2) 定型
-- ===================================================================

-- 1. ledger_asset_account: balance, valuation
ALTER TABLE `moyun-db`.ledger_asset_account MODIFY COLUMN balance DECIMAL(20,2) NOT NULL DEFAULT 0.00 COMMENT '当前余额（元）';
ALTER TABLE `moyun-db`.ledger_asset_account MODIFY COLUMN valuation DECIMAL(20,2) NULL COMMENT '估值（元）';
UPDATE `moyun-db`.ledger_asset_account SET balance = balance / 100.0, valuation = valuation / 100.0;
ALTER TABLE `moyun-db`.ledger_asset_account MODIFY COLUMN balance DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '当前余额（元）';
ALTER TABLE `moyun-db`.ledger_asset_account MODIFY COLUMN valuation DECIMAL(18,2) NULL COMMENT '估值（元）';

-- 2. ledger_liability_account: balance, principal, monthly_payment
ALTER TABLE `moyun-db`.ledger_liability_account MODIFY COLUMN balance DECIMAL(20,2) NOT NULL DEFAULT 0.00 COMMENT '当前欠款（元）';
ALTER TABLE `moyun-db`.ledger_liability_account MODIFY COLUMN principal DECIMAL(20,2) NULL COMMENT '初始本金（元）';
ALTER TABLE `moyun-db`.ledger_liability_account MODIFY COLUMN monthly_payment DECIMAL(20,2) NULL COMMENT '每期还款额（元）';
UPDATE `moyun-db`.ledger_liability_account SET balance = balance / 100.0, principal = principal / 100.0, monthly_payment = monthly_payment / 100.0;
ALTER TABLE `moyun-db`.ledger_liability_account MODIFY COLUMN balance DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '当前欠款（元）';
ALTER TABLE `moyun-db`.ledger_liability_account MODIFY COLUMN principal DECIMAL(18,2) NULL COMMENT '初始本金（元）';
ALTER TABLE `moyun-db`.ledger_liability_account MODIFY COLUMN monthly_payment DECIMAL(18,2) NULL COMMENT '每期还款额（元）';

-- 3. ledger_transaction: amount, balance_after, target_balance_after, liability_balance_after
ALTER TABLE `moyun-db`.ledger_transaction MODIFY COLUMN amount DECIMAL(20,2) NOT NULL COMMENT '金额（元；adjust可为负，其余恒为正）';
ALTER TABLE `moyun-db`.ledger_transaction MODIFY COLUMN balance_after DECIMAL(20,2) NULL COMMENT '主账户交易后余额快照（元）';
ALTER TABLE `moyun-db`.ledger_transaction MODIFY COLUMN target_balance_after DECIMAL(20,2) NULL COMMENT '转账目标账户交易后余额快照（元）';
ALTER TABLE `moyun-db`.ledger_transaction MODIFY COLUMN liability_balance_after DECIMAL(20,2) NULL COMMENT '关联负债交易后欠款快照（元）';
UPDATE `moyun-db`.ledger_transaction SET
  amount = amount / 100.0,
  balance_after = balance_after / 100.0,
  target_balance_after = target_balance_after / 100.0,
  liability_balance_after = liability_balance_after / 100.0;
ALTER TABLE `moyun-db`.ledger_transaction MODIFY COLUMN amount DECIMAL(18,2) NOT NULL COMMENT '金额（元；adjust可为负，其余恒为正）';
ALTER TABLE `moyun-db`.ledger_transaction MODIFY COLUMN balance_after DECIMAL(18,2) NULL COMMENT '主账户交易后余额快照（元）';
ALTER TABLE `moyun-db`.ledger_transaction MODIFY COLUMN target_balance_after DECIMAL(18,2) NULL COMMENT '转账目标账户交易后余额快照（元）';
ALTER TABLE `moyun-db`.ledger_transaction MODIFY COLUMN liability_balance_after DECIMAL(18,2) NULL COMMENT '关联负债交易后欠款快照（元）';

-- 4. ledger_budget: amount
ALTER TABLE `moyun-db`.ledger_budget MODIFY COLUMN amount DECIMAL(20,2) NOT NULL COMMENT '预算金额（元）';
UPDATE `moyun-db`.ledger_budget SET amount = amount / 100.0;
ALTER TABLE `moyun-db`.ledger_budget MODIFY COLUMN amount DECIMAL(18,2) NOT NULL COMMENT '预算金额（元）';

-- 5. ledger_net_worth_snapshot: total_asset, total_liability, net_worth
ALTER TABLE `moyun-db`.ledger_net_worth_snapshot MODIFY COLUMN total_asset DECIMAL(20,2) NOT NULL DEFAULT 0.00 COMMENT '总资产（元）';
ALTER TABLE `moyun-db`.ledger_net_worth_snapshot MODIFY COLUMN total_liability DECIMAL(20,2) NOT NULL DEFAULT 0.00 COMMENT '总负债（元）';
ALTER TABLE `moyun-db`.ledger_net_worth_snapshot MODIFY COLUMN net_worth DECIMAL(20,2) NOT NULL DEFAULT 0.00 COMMENT '净资产（元）= total_asset - total_liability';
UPDATE `moyun-db`.ledger_net_worth_snapshot SET
  total_asset = total_asset / 100.0,
  total_liability = total_liability / 100.0,
  net_worth = net_worth / 100.0;
ALTER TABLE `moyun-db`.ledger_net_worth_snapshot MODIFY COLUMN total_asset DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '总资产（元）';
ALTER TABLE `moyun-db`.ledger_net_worth_snapshot MODIFY COLUMN total_liability DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '总负债（元）';
ALTER TABLE `moyun-db`.ledger_net_worth_snapshot MODIFY COLUMN net_worth DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '净资产（元）= total_asset - total_liability';