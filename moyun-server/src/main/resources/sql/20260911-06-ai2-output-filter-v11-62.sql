-- =====================================================================
-- v11.62 P1-3 输出内容过滤（2026-09-11）
--
-- 依据《AI底座企业级评估-代码实测结论与改进清单》P1-3：
--   ai_scene_config 加 enable_output_filter 列（网关输出过滤开关）。
--
-- 过滤语义：启用后，同步响应 data 的全部文本节点经 DFA 词树脱敏
--           （复用 sys_sensitive_word 词库，命中词替换为 *，保留原文格式）；
--           默认 0=关闭（存量场景行为不变），按场景按需开启。
-- 已知局限：流式（SSE）输出由 Handler 直发，不经此过滤（跨片词无法有效匹配）。
-- =====================================================================

-- 1. 场景配置表：输出过滤开关
ALTER TABLE `moyun-db`.ai_scene_config
    ADD COLUMN enable_output_filter TINYINT(1) DEFAULT 0 COMMENT '是否启用输出内容过滤（响应data文本经DFA词树脱敏；0=关闭）' AFTER daily_token_limit;

-- 2. 按需开启示例（C 端直出文本场景确认后取消注释执行；管理端改列同样即时生效——网关直查库）
-- UPDATE `moyun-db`.ai_scene_config SET enable_output_filter = 1 WHERE scene_code = 'finance_analysis' AND enabled = 1;

-- 3. 验证
SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT
FROM information_schema.columns
WHERE table_schema = 'moyun-db'
  AND table_name = 'ai_scene_config' AND column_name = 'enable_output_filter';
