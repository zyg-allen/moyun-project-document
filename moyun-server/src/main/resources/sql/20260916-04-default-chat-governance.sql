-- =====================================================================
-- 20260916-04 v11.95 · default_chat 场景治理行（任务2：智能体对话链路收口）
--
-- 变更内容：
--   1. 新增 default_chat 场景配置行，作为智能体动态对话链路
--      （/cms/ai/chat/stream、/cms/ai/chat/regenerate）的治理参数载体：
--      ChatController 读取本行 rate_limit_count / rate_limit_time 做
--      「场景 × 用户」Redis 限流（v11.95 前聊天链路无限流、无执行日志），
--      并将每次对话落 ai_execute_log（scene_code = default_chat）。
--   2. 限流通行值：60 次 / 3600 秒（与其他场景口径一致）。
--   3. 行为不变项：Agent 动态指定逻辑（agentId 请求参数）、RAG/工作流/
--      会话记忆编排均不动，对话链路不经过 AiGatewayService.execute 编排。
--
-- 说明：
--   - 该场景不注册 ai2 Handler Bean（handler_bean_name 仅为语义标注），
--     服务启动时注册中心一致性检查会输出一条 WARN
--     「场景 default_chat 有配置但未注册 Handler」，属预期，可忽略。
--   - open_api = 0：default_chat 为业务内部链路，禁止经 /api/ai/execute
--     通用入口外部调用（防绕过 DynamicChatServiceImpl 的会话编排）。
--   - 本行无 system_prompt_template（v11.95 任务1裁决：场景表不再承载
--     系统提示词，对话人设统一走 ai_agent.system_prompt）。
-- =====================================================================

INSERT INTO `moyun-db`.ai_scene_config
    (scene_code, scene_name, description, scene_category, handler_bean_name,
     output_mode, output_parser, rate_limit_count, rate_limit_time,
     version, weight, priority, is_default, enabled, open_api)
SELECT 'default_chat', '智能体对话',
       '智能体动态对话（/cms/ai/chat/*）：治理配置载体（限流/执行日志），Agent 由请求动态指定，人设走 ai_agent.system_prompt',
       'chat', 'dynamicChatBridge',
       'stream', 'text', 60, 3600,
       'v1', 100, 0, 1, 1, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `moyun-db`.ai_scene_config
                  WHERE scene_code = 'default_chat' AND version = 'v1');
