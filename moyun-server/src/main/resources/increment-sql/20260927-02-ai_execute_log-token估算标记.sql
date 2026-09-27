-- ============================================================
-- 增量脚本：20260927-02-ai_execute_log token 估算标记
-- 目的：ai_execute_log 增加 token_estimated 列，区分"服务端真实 usage"与"本地分词估算"。
--
-- 背景（为什么必须加）：
--   langchain4j 1.0.0-beta3 的 OpenAiStreamingChatModel 既不下发
--   stream_options: {"include_usage": true}，builder 也无该选项（已用 javap + class 常量池检索证实），
--   因此 OpenAI 兼容端点的流式调用拿不到 usage（ChatResponse.tokenUsage() 恒为 null）。
--   原实现只在 tokenUsage != null 时才累计 → **流式 Token 全部漏计**：
--   场景日配额（成本熔断）被绕过、成本看板里流式场景恒为 0。
--
--   修复方式：TokenMeter 在 usage 缺失时用本地分词（OpenAiTokenizer/jtokkit）估算，
--   并置 token_estimated=1。**估算值不得与真实值混用**，故必须落库区分。
--
--   同时：AiMetadata 增加 tokenEstimated（API 响应可见），网关同步/流式两条路径都已接入。
--
-- 幂等性说明：重复执行会在 ADD COLUMN 处报 "Duplicate column name"，属预期（列已存在即无需迁移）。
-- 前置条件：无需备份（仅新增列，不修改既有数据；历史行 token_estimated 为 0/NULL，
--           与新语义一致——历史值都是服务端真实回传的）。
-- ============================================================

ALTER TABLE `ai_execute_log`
    ADD COLUMN `token_estimated` tinyint(1) DEFAULT 0 COMMENT 'Token是否为本地估算：1=估算（服务端未回传usage，本地分词得出），0/NULL=服务端真实值' AFTER `output_tokens`;

-- 复核：以下应显示新列
-- SHOW COLUMNS FROM ai_execute_log LIKE 'token_estimated';
