-- ============================================================
-- 回填应收/应付账户的 total_amount / paid_amount / settle_status
--
-- 背景：20261018-01 给 ledger_asset_account / ledger_liability_account
-- 新增了上面三列，但没有回填历史行，导致老账户这三列为 NULL：
--   - 应收「待收余额 = total - paid」算出来是 0，收款下拉显示「待收 0」
--   - 负债同理，结算状态无法推导
--
-- 口径：历史行视为「未核销」—— total = 当前 balance，paid = 0。
-- 幂等：只处理仍为 NULL 的行，重复执行不会二次覆盖。
-- ============================================================

-- ① 资产侧：仅应收款（fund/investment/fixed 不需要这三个字段）
UPDATE `ledger_asset_account`
SET `total_amount` = `balance`,
    `paid_amount` = 0,
    `settle_status` = 'pending'
WHERE `account_type` = 'receivable'
  AND (`total_amount` IS NULL OR `paid_amount` IS NULL OR `settle_status` IS NULL);

-- ② 负债侧：普通负债 + 赊账应付
UPDATE `ledger_liability_account`
SET `total_amount` = `balance`,
    `paid_amount` = 0,
    `settle_status` = 'pending'
WHERE (`total_amount` IS NULL OR `paid_amount` IS NULL OR `settle_status` IS NULL);
