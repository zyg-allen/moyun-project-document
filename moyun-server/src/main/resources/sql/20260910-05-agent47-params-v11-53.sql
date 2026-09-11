-- =====================================================================
-- v11.53 财务分析 LLM 链路修复：绑定 Agent 参数调优（2026-09-10）
--
-- 背景（日志实证）：
--   1. LLM 返回对话文本（"您好，我需要先获取你的基础财务数据"）→ JSON 解析失败 → 降级模板综述
--      根因：Agent 47「财务分析师」人设为对话式，把后台批处理任务带偏成聊天
--      （代码侧已修复：mergePersona 注入任务边界声明，见 AbstractAiSceneHandler）
--   2. 部分调用 JSON 输出被截断（Unexpected end-of-input）→ max_tokens 偏小
--   3. 财务分析要求稳定输出结构化 JSON，temperature 过高易发散
--
-- 本脚本：仅调整技术参数，不覆盖用户在管理页编辑的人设内容
-- =====================================================================

-- 1. 查看当前配置（执行后人工确认）
SELECT id, name, model_config_id, temperature, max_tokens
FROM ai_agent WHERE id = 47;

-- 2. max_tokens 条件提升：JSON 报告（summary+risks+suggestions）实测需要 2000+ tokens，
--    低于 4096 时截断风险高
UPDATE ai_agent
SET max_tokens = 4096
WHERE id = 47
  AND (max_tokens IS NULL OR max_tokens < 4096);

-- 3. temperature 条件收敛：结构化输出场景建议 ≤ 0.3（高于时收敛，仅针对财务分析师）
UPDATE ai_agent
SET temperature = 0.3
WHERE id = 47
  AND (temperature IS NULL OR temperature > 0.3);

-- 4. 验证：两行应显示 max_tokens=4096, temperature=0.3
SELECT id, name, temperature, max_tokens FROM ai_agent WHERE id = 47;
