-- ============================================================
-- 20260914-01 AI 财务分析报告：范围字段 + 多版本冗余收敛 + 口径唯一
-- 背景：
--   v11.55 多版本历史导致同 (user_id, period) 反复生成反复 INSERT，冗余；
--   3m/6m/year 维度实时计算不落库（每次进入页面都重算烧 token），
--   表无范围字段，历史列表无法区分维度。
-- 改造（v11.72）：
--   1) 新增 analysis_range 字段：month/3m/6m/year 四维度独立快照
--   2) 存量收敛：同 (user_id, period) 仅保留最新一份
--   3) 唯一约束 uk_user_period_range：同用户+同月+同范围仅一份，
--      重新分析覆盖更新（查询与覆盖口径一致）
-- ============================================================

-- 1. 新增范围字段（存量数据全部为 month 维度快照，默认回填）
ALTER TABLE `moyun-db`.ledger_ai_analysis_report
    ADD COLUMN analysis_range varchar(10) NOT NULL DEFAULT 'month'
        COMMENT '分析范围: month-本月/3m-近3个月/6m-近6个月/year-近12个月' AFTER period;

-- 2. 存量冗余收敛：同 (user_id, period) 多版本仅保留最新一条（id 最大）
DELETE r FROM `moyun-db`.ledger_ai_analysis_report r
JOIN `moyun-db`.ledger_ai_analysis_report newer
  ON newer.user_id = r.user_id
 AND newer.period = r.period
 AND newer.id > r.id;

-- 3. 口径唯一约束（覆盖式更新，杜绝同月同范围多行）
ALTER TABLE `moyun-db`.ledger_ai_analysis_report
    ADD UNIQUE KEY uk_user_period_range (user_id, period, analysis_range);
