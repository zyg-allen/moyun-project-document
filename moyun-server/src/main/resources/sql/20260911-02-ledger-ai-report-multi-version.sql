-- =====================================================================
-- v11.55 AI 财务分析异步任务化 + 报告多版本（2026-09-11）
--
-- 背景：
--   1. LLM 生成长（快速模型 30-60s+），同步接口前端转圈卡顿
--   2. 旧设计"每月一条覆盖式快照"（UNIQUE user_id+period），
--      用户重复分析会丢掉上一版报告，无法对比
--
-- 本脚本：去掉唯一约束改普通索引，报告每次生成 INSERT 新版本
--（快照命中逻辑改为"该 period 最新一条指纹匹配"，代码侧同步）
-- =====================================================================

-- 1. 唯一约束 → 普通索引（幂等：先查索引名）
SELECT COUNT(1) AS uk_exists
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'ledger_ai_analysis_report'
  AND index_name = 'uk_user_period';

-- 若上面=1 则执行（MySQL 不支持 DROP INDEX IF EXISTS，需人工确认）：
ALTER TABLE `ledger_ai_analysis_report`
    -- DROP INDEX uk_user_period,
    ADD INDEX idx_user_period (user_id, period);

-- 2. 版本说明列：无（复用 create_time/update_time 区分同 period 多版本）
-- 3. 验证：同 (user_id, period) 可存在多行
SHOW INDEX FROM ledger_ai_analysis_report;
