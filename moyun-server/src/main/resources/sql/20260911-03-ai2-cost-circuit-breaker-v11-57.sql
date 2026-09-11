-- =====================================================================
-- v11.57 P0-2 成本监控与熔断（2026-09-11）
--
-- 依据《AI底座企业级评估-代码实测结论与改进清单》P0-2：
--   ① ai_execute_log 加 cost_yuan 列（网关按 metadata 细分 token × 模型单价回填，
--      单价复用 ai_model_config.input_price/output_price——老链路已有列，本项目只缺网关侧核算）
--   ② ai_scene_config 加 daily_token_limit 列（场景日 Token 上限，网关 Redis 计数超限拒绝）
--
-- 熔断语义：场景级日累计（所有用户共享额度），保护平台总成本；
--           null/0 = 不限。用户级配额属后续增强（评估文档 P2）。
-- =====================================================================

-- 1. 执行日志表：成本列
ALTER TABLE `moyun-db`.ai_execute_log
    ADD COLUMN cost_yuan DECIMAL(12,6) DEFAULT NULL COMMENT '本次调用成本（元，细分token×模型单价）' AFTER token_used;

-- 2. 场景配置表：日 Token 上限（成本熔断）
ALTER TABLE `moyun-db`.ai_scene_config
    ADD COLUMN daily_token_limit INT DEFAULT NULL COMMENT '场景日Token上限（当日累计超限拒绝调用；null/0=不限）' AFTER rate_limit_time;

-- 3. 验证
SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT
FROM information_schema.columns
WHERE table_schema = 'moyun-db'
  AND table_name = 'ai_execute_log' AND column_name = 'cost_yuan';
SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT
FROM information_schema.columns
WHERE table_schema = 'moyun-db'
  AND table_name = 'ai_scene_config' AND column_name = 'daily_token_limit';
