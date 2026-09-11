-- =====================================================================
-- v11.54 财务分析 LLM 调用超时修复：模型17（deepseek-v4-pro）超时提升（2026-09-11）
--
-- 日志实证（2026-09-11 09:29~09:34）：
--   1. RestClientException → SocketTimeoutException(okhttp Http2Stream StreamTimeout)
--      + langchain4j RetryUtils 重试 3 次（attempt 1/2/3 of 3）——读超时不够
--   2. 另一并发请求：HTTP 200 但 content 为空（无任何 WARN——推理模型只出
--      reasoning_content / 触发内容审查，旧代码静默失败）
--   3. 管理页简单测试（"你好"级）3 秒 HTTP 200 —— API 连通正常，
--      是长上下文+大JSON生成（v11.49 富上下文/富输出）耗时超过模型客户端读超时
--
-- 修复：
--   代码（AbstractAiSceneHandler.chatDetailed）：空内容不再静默 return，
--   记录 WARN 并回落默认模型
--   本脚本：模型17 timeout <180 提升至 180 秒（覆盖长生成时间）
--
-- 注意：langchain4j SyncRequestExecutor 内部硬编码重试 3 次，timeout=180 时
--   最坏等待 ≈ 9 分钟。若仍频繁超时，建议为 finance_analysis 换绑非推理
--   快速模型（长JSON生成场景吞吐优先）
-- =====================================================================

-- 1. 执行前确认当前值
SELECT id, name, model_name, timeout, temperature, max_tokens
FROM ai_model_config WHERE id = 17;

-- 2. 条件提升（仅当小于 180 时）
UPDATE ai_model_config
SET timeout = 180
WHERE id = 17
  AND (timeout IS NULL OR timeout < 180);

-- 3. 验证
SELECT id, name, model_name, timeout FROM ai_model_config WHERE id = 17;
