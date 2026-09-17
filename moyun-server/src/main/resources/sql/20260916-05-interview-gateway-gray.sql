-- =====================================================================
-- 20260916-05 v11.95 · 面试主干网关化灰度开关（任务3）
--
-- 变更内容：
--   1. 新增灰度开关 sys_config：ai.gateway.interview.enabled（缺省 false=直连，
--      行为与历史完全一致）。
--      true = 语音面试主干对话（InterviewAgentClientImpl.chatStream）前置网关治理：
--        - voice_interview 场景「场景×用户」Redis 限流
--          （ai_scene_config.rate_limit_count / rate_limit_time，即时生效）
--        - 场景日 Token 成本熔断前置检查（daily_token_limit）
--        - 每轮面试官话术落 ai_execute_log（scene_code=voice_interview，
--          handler_name=interviewMainTrunk，bind_type=agent）
--      灰度开启不改变滑窗消息体与模型调用链路（T2 生产方案：直连保性能，治理收口网关）。
--
-- 说明：
--   - 治理维度为「场景×用户」，用户取当前登录人，无登录上下文按 anonymous。
--   - 流式路径 Token 消费回传为已知局限（与网关 executeStream 同口径），仅前置配额检查。
--   - 本任务不含表结构变更；模型客户端实例缓存为代码级改动，无 DDL。
-- =====================================================================

INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time)
SELECT 'AI网关面试主干灰度开关', 'ai.gateway.interview.enabled', 'false', 'Y',
       'true=语音面试主干对话前置网关治理（voice_interview 场景限流+Token熔断+执行日志），false=直连（默认，行为与历史一致）',
       'admin', NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'ai.gateway.interview.enabled');

-- 校验
SELECT config_key, config_value FROM sys_config WHERE config_key = 'ai.gateway.interview.enabled';
