-- =============================================================================
-- 20260927-03 数据库规范统一：金额列精度 + collation 归一（v13.12）
-- =============================================================================
-- 背景（报告六 §6.5「数据库设计」）：
--   ① 金额 precision 混用：12 个门户/结算金额列仍是 decimal(10,2)，而 pay/ledger 侧
--      已在 A-4 批统一为 decimal(18,2)（元）。本脚本把这 12 列宽化到 decimal(18,2)。
--      · 仅**宽化**（10,2 → 18,2 同为 2 位小数），不改语义、不丢精度、无需数据迁移。
--      · AI 计费列（cost / cost_yuan / input_price / output_price / total_cost）按 token 单价
--        计价、需要 6 位小数，**有意保留**，见 DdlConventionGuardTest 白名单。
--   ② collation 四套并存：0900_ai_ci(152 表) / general_ci(29) / unicode_ci(5) / bin(1)。
--      本脚本把 general_ci 与 unicode_ci 表统一到 utf8mb4_0900_ai_ci（项目主流口径）。
--      · ledger_ai_analysis_report 的 utf8mb4_bin 为**有意设计**（data_fingerprint 数据指纹 /
--        JSON 快照需精确匹配，建表语句即写明 COLLATE=utf8mb4_bin），保留不动。
--      · 说明：本项为**一致性**整改——跨 collation 的隐患是"字符串列之间的比较/连接会报
--        1267 Illegal mix of collations"，仓库内暂未发现此类连接（故非线上缺陷），
--        但保留混用会让后续新写的跨域 JOIN 踩坑。
--
-- 影响面：34 张表的表级/列级 collation + 12 个列的类型宽化；不改表结构语义、不改索引。
-- 幂等性：MODIFY / CONVERT 均可重复执行（结果收敛），但**非**"可重复执行即无副作用"——
--         CONVERT 会重建表（大表请评估窗口期）。
-- 执行记录：dev 库已执行并复核（见文件末尾复核 SQL）。
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 一、金额列精度：decimal(10,2) → decimal(18,2)（12 列）
-- -----------------------------------------------------------------------------
ALTER TABLE `portal_article` MODIFY COLUMN `price` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '付费价格，0=免费';
ALTER TABLE `portal_book` MODIFY COLUMN `price` decimal(18,2) DEFAULT '0.00' COMMENT '书籍单价（元）';
ALTER TABLE `portal_book_chapter` MODIFY COLUMN `price` decimal(18,2) DEFAULT '0.00' COMMENT '章节单价（元，VIP章节购买）';
ALTER TABLE `portal_column` MODIFY COLUMN `price` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '专栏会员价，0=免费';
ALTER TABLE `portal_creator_settlement` MODIFY COLUMN `tip_income` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '打赏收入（当月已支付打赏总额）';
ALTER TABLE `portal_creator_settlement` MODIFY COLUMN `paid_read_income` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '付费阅读收入（当月已支付购买总额）';
ALTER TABLE `portal_creator_settlement` MODIFY COLUMN `column_income` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '专栏订阅收入（当月已支付订阅总额）';
ALTER TABLE `portal_creator_settlement` MODIFY COLUMN `total_income` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '总收入（三项之和）';
ALTER TABLE `portal_creator_settlement` MODIFY COLUMN `platform_fee` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '平台抽成（total_income * platform_fee_rate）';
ALTER TABLE `portal_creator_settlement` MODIFY COLUMN `creator_income` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '创作者实得（total_income - platform_fee）';
ALTER TABLE `portal_tip_order` MODIFY COLUMN `amount` decimal(18,2) NOT NULL COMMENT '打赏金额';
ALTER TABLE `portal_tip_order` MODIFY COLUMN `refund_amount` decimal(18,2) DEFAULT NULL COMMENT '退款金额';

-- -----------------------------------------------------------------------------
-- 二、collation 归一：general_ci / unicode_ci → utf8mb4_0900_ai_ci（34 张表）
-- -----------------------------------------------------------------------------
ALTER TABLE `ai_agent_workflow_relation` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `ai_reference_feedback` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `ai_workflow` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `ai_workflow_execution` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `ai_workflow_version` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `ledger_asset_account` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `ledger_budget` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `ledger_category` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `ledger_liability_account` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `ledger_memo` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `ledger_net_worth_snapshot` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `ledger_saving_plan` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `ledger_saving_record` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `ledger_schedule_log` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `ledger_schedule_task` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `ledger_tip_order` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `ledger_transaction` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `pay_ledger_entry` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `pay_notification` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `pay_notify_log` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `pay_order` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `pay_user_account` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `pay_user_bank_card` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `pay_withdraw_order` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `portal_ai_task` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `portal_tip_order` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `portal_voice_interview_event` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `sys_platform` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `vip_api_registry` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `vip_benefit` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `vip_benefit_usage` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `vip_tier` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `vip_tier_benefit` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `vip_user_card` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- =============================================================================
-- 三、复核 SQL（执行后应满足：非 0900_ai_ci 的表只剩 ledger_ai_analysis_report 的 bin）
-- =============================================================================
-- SELECT TABLE_COLLATION, COUNT(*) FROM information_schema.TABLES
--   WHERE TABLE_SCHEMA = DATABASE() GROUP BY TABLE_COLLATION;
--   -- 期望：utf8mb4_0900_ai_ci = 187，utf8mb4_bin = 1（dev 库实测 187/1）
--
-- SELECT COUNT(*) FROM information_schema.COLUMNS
--   WHERE TABLE_SCHEMA = DATABASE() AND COLLATION_NAME IN ('utf8mb4_general_ci','utf8mb4_unicode_ci');
--   -- 期望：0
--
-- SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE FROM information_schema.COLUMNS
--   WHERE TABLE_SCHEMA = DATABASE() AND COLUMN_NAME IN
--     ('price','amount','refund_amount','tip_income','paid_read_income','column_income',
--      'total_income','platform_fee','creator_income')
--     AND DATA_TYPE = 'decimal' AND NUMERIC_SCALE = 2
--   ORDER BY TABLE_NAME, COLUMN_NAME;
--   -- 期望：全部 COLUMN_TYPE = decimal(18,2)，无 decimal(10,2)
