-- =====================================================================
-- 支付模块·金额单位统一为元（v11.31）
-- 墨韵智库 · moyun
-- 执行库：moyun-db
-- 背景：pay 模块（V11.0）原以 BIGINT"分"存储；全项目金额规范（项目开发规范 §2.3.1）
--       统一为人民币元 DECIMAL(18,2)。与记账模块 20260904-03 同口径收口。
--
-- 二选一执行（先核对存量数据口径再选）：
--   方案A：数据为空或已是元口径 → 纯改类型+注释，不动数据（推荐，开发/测试库常用）
--   方案B：存量数据为分口径     → ALTER→DECIMAL(20,2) 保留原值 → UPDATE /100 分转元 → ALTER→DECIMAL(18,2) 定型
--
-- 口径核对方法（任选其一）：
--   SELECT MAX(amount), MIN(amount) FROM `moyun-db`.pay_order;  -- 出现 >10000 的量级基本为分；全 0/空表直接走方案A
-- 注意：执行后需重启 moyun-server（实体已 BigDecimal 化，v11.31 同步发布）
-- =====================================================================

-- =============================== 方案A ===============================
-- 适用：数据为空，或存量数据已是元口径（禁止再 ÷100，否则金额缩小 100 倍！）

-- 1. pay_order: amount
ALTER TABLE `moyun-db`.pay_order MODIFY COLUMN amount DECIMAL(18,2) NOT NULL COMMENT '支付金额（元）';

-- 2. pay_user_account: balance, total_income, total_withdraw
ALTER TABLE `moyun-db`.pay_user_account MODIFY COLUMN balance DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '可用余额（元）';
ALTER TABLE `moyun-db`.pay_user_account MODIFY COLUMN total_income DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '累计收入（元，含打赏所得）';
ALTER TABLE `moyun-db`.pay_user_account MODIFY COLUMN total_withdraw DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '累计提现（元）';

-- 3. pay_ledger_entry: amount, balance_after
ALTER TABLE `moyun-db`.pay_ledger_entry MODIFY COLUMN amount DECIMAL(18,2) NOT NULL COMMENT '金额（元）';
ALTER TABLE `moyun-db`.pay_ledger_entry MODIFY COLUMN balance_after DECIMAL(18,2) NULL COMMENT '交易后余额（元；PLATFORM 分录不追踪余额，为 NULL）';

-- 4. pay_withdraw_order: amount, fee（本期预留表，同口径处理）
ALTER TABLE `moyun-db`.pay_withdraw_order MODIFY COLUMN amount DECIMAL(18,2) NOT NULL COMMENT '提现金额（元）';
ALTER TABLE `moyun-db`.pay_withdraw_order MODIFY COLUMN fee DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '手续费（元）';

-- =============================== 方案B ===============================
-- 适用：存量数据为分口径（需 ÷100 换算为元）
-- 执行顺序：先 ALTER→DECIMAL(20,2) 保留原始整数 → UPDATE /100 分转元 → ALTER→DECIMAL(18,2) 定型
-- ★ 与方案A 互斥，选了方案B 就不要执行方案A，反之亦然 ★

-- 1. pay_order: amount
-- ALTER TABLE `moyun-db`.pay_order MODIFY COLUMN amount DECIMAL(20,2) NOT NULL COMMENT '支付金额（元）';
-- UPDATE `moyun-db`.pay_order SET amount = amount / 100.0;
-- ALTER TABLE `moyun-db`.pay_order MODIFY COLUMN amount DECIMAL(18,2) NOT NULL COMMENT '支付金额（元）';

-- 2. pay_user_account: balance, total_income, total_withdraw
-- ALTER TABLE `moyun-db`.pay_user_account MODIFY COLUMN balance DECIMAL(20,2) NOT NULL DEFAULT 0.00 COMMENT '可用余额（元）';
-- ALTER TABLE `moyun-db`.pay_user_account MODIFY COLUMN total_income DECIMAL(20,2) NOT NULL DEFAULT 0.00 COMMENT '累计收入（元，含打赏所得）';
-- ALTER TABLE `moyun-db`.pay_user_account MODIFY COLUMN total_withdraw DECIMAL(20,2) NOT NULL DEFAULT 0.00 COMMENT '累计提现（元）';
-- UPDATE `moyun-db`.pay_user_account SET balance = balance / 100.0, total_income = total_income / 100.0, total_withdraw = total_withdraw / 100.0;
-- ALTER TABLE `moyun-db`.pay_user_account MODIFY COLUMN balance DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '可用余额（元）';
-- ALTER TABLE `moyun-db`.pay_user_account MODIFY COLUMN total_income DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '累计收入（元，含打赏所得）';
-- ALTER TABLE `moyun-db`.pay_user_account MODIFY COLUMN total_withdraw DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '累计提现（元）';

-- 3. pay_ledger_entry: amount, balance_after
-- ALTER TABLE `moyun-db`.pay_ledger_entry MODIFY COLUMN amount DECIMAL(20,2) NOT NULL COMMENT '金额（元）';
-- ALTER TABLE `moyun-db`.pay_ledger_entry MODIFY COLUMN balance_after DECIMAL(20,2) NULL COMMENT '交易后余额（元；PLATFORM 分录不追踪余额，为 NULL）';
-- UPDATE `moyun-db`.pay_ledger_entry SET amount = amount / 100.0, balance_after = balance_after / 100.0;
-- ALTER TABLE `moyun-db`.pay_ledger_entry MODIFY COLUMN amount DECIMAL(18,2) NOT NULL COMMENT '金额（元）';
-- ALTER TABLE `moyun-db`.pay_ledger_entry MODIFY COLUMN balance_after DECIMAL(18,2) NULL COMMENT '交易后余额（元；PLATFORM 分录不追踪余额，为 NULL）';

-- 4. pay_withdraw_order: amount, fee（本期预留表，若有存量数据同样换算）
-- ALTER TABLE `moyun-db`.pay_withdraw_order MODIFY COLUMN amount DECIMAL(20,2) NOT NULL COMMENT '提现金额（元）';
-- ALTER TABLE `moyun-db`.pay_withdraw_order MODIFY COLUMN fee DECIMAL(20,2) NOT NULL DEFAULT 0.00 COMMENT '手续费（元）';
-- UPDATE `moyun-db`.pay_withdraw_order SET amount = amount / 100.0, fee = fee / 100.0;
-- ALTER TABLE `moyun-db`.pay_withdraw_order MODIFY COLUMN amount DECIMAL(18,2) NOT NULL COMMENT '提现金额（元）';
-- ALTER TABLE `moyun-db`.pay_withdraw_order MODIFY COLUMN fee DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '手续费（元）';

-- =============================== 收尾验证 ===============================
-- 守恒验证（执行后人工核对：平台 + 用户 = 总额）
SELECT pay_no,
       SUM(CASE WHEN account_role = 'PLATFORM' THEN amount ELSE 0 END) AS platform_yuan,
       SUM(CASE WHEN account_role = 'USER'     THEN amount ELSE 0 END) AS user_yuan,
       SUM(amount) AS total_yuan
FROM `moyun-db`.pay_ledger_entry
GROUP BY pay_no
HAVING COUNT(*) > 1
ORDER BY id DESC
LIMIT 20;