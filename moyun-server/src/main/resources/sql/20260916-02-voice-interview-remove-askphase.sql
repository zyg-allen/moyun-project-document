-- =====================================================================
-- 20260916-02 语音面试 v11.94.1 · 移除反问段独立链路
--
-- 变更内容：
--   1. 删除反问段开关 sys_config（voice.interview.candidateAsk.enabled）：
--      v11.94.1 移除反问段独立输入模式（POST /{id}/ask 端点、前端 askPhase
--      输入模式、ask_invite 事件流），反问改为系统提示词承载——候选人可在
--      回答中口头反问，面试官简答后继续提问；问满后面试官口播
--      "你还有什么想了解的吗？"并自然收尾（融入对话流，非独立段）
--
-- 规范：增量 DML；无表结构变更；历史 candidate_ask/ask_invite 事件数据
--       存量保留（只增不改，链路追溯依据），仅读取代码随版本清理
-- =====================================================================

-- 1. 删除反问段开关（读取代码已随 v11.94.1 清理，无引用）
DELETE FROM sys_config WHERE config_key = 'voice.interview.candidateAsk.enabled';

-- 2. 校验
SELECT * FROM sys_config WHERE config_key LIKE 'voice.interview.%';
