-- =====================================================================
-- v14.72 AI统一收口治理 · 批次1（2026-10-05）
-- 1) ai_scene_config 新增 enable_intent_routing 场景级开关（默认 0=关闭）：
--    意图分类仅在显式开启的场景生效，消除"结构化场景带 userInput 即被
--    网关追问"地雷（IntentClassifier 规则覆盖有限，UNKNOWN 置信度恒低
--    于追问阈值 0.6）。存量行默认关闭，行为零变化。
-- 2) 新增 workflow_agent_node 场景行：工作流 AgentNodeExecutor 的网关
--    治理专用场景（配置驱动、text 输出）。场景行存在且启用时 Agent 节点
--    走网关（限流/Token熔断/计量/执行日志全治理）；场景行缺失/停用时
--    兜底直连但补计量记账（渐进双通道，不破坏存量工作流）。
-- 配套代码：AiSceneConfig.enableIntentRouting / AiSceneEnum.WORKFLOW_AGENT_NODE /
--           AgentNodeExecutor 网关优先路由 / AiGatewayService 意图开关。
-- 幂等性：ADD COLUMN 带 information_schema 前置判断（守卫 §6.4，已有库
--         重跑不报 1060）；场景行 INSERT ... SELECT ... WHERE NOT EXISTS
--         （按 scene_code 判存，连跑两次 exit 0）。
-- 回滚：ALTER TABLE ai_scene_config DROP COLUMN enable_intent_routing;
--       DELETE FROM ai_scene_config WHERE scene_code = 'workflow_agent_node';
-- =====================================================================

-- ---------------------------------------------------------------------------
-- 1) enable_intent_routing 列（守卫：列已存在则跳过）
-- ---------------------------------------------------------------------------
SET @has_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'ai_scene_config'
                   AND COLUMN_NAME = 'enable_intent_routing');
SET @ddl := IF(@has_col = 0,
    'ALTER TABLE `ai_scene_config` ADD COLUMN `enable_intent_routing` tinyint(1) DEFAULT ''0'' COMMENT ''是否启用意图分类路由：1=开启（userInput自由文本参与意图分类与场景路由），0=关闭（默认，网关跳过分类）'' AFTER `enable_output_filter`',
    'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 2) workflow_agent_node 场景行（判存：scene_code 已存在则跳过）
-- ---------------------------------------------------------------------------
INSERT INTO ai_scene_config
    (`scene_code`, `scene_name`, `description`, `scene_category`, `handler_bean_name`, `handler_method`,
     `output_mode`, `output_parser`, `rate_limit_count`, `rate_limit_time`,
     `version`, `config_version`, `weight`, `priority`, `is_default`, `enabled`, `open_api`, `create_time`)
SELECT
    'workflow_agent_node', '工作流Agent节点', 'AI工作流 agent 节点的统一收口治理场景：AgentNodeExecutor 优先经本场景走网关（限流/Token熔断/计量/日志），未配置/停用时兜底直连并补计量', 'chat',
    'defaultSceneExecutor', 'execute', 'sync', 'text', 600, 60,
    'v1', 1, 100, 0, 1, 1, 0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM ai_scene_config WHERE scene_code = 'workflow_agent_node');

-- ---------------------------------------------------------------------------
-- 3) 复核（期望：列存在=1；场景行=1 且 enabled=1）
-- ---------------------------------------------------------------------------
SELECT '复核：enable_intent_routing 列（期望 1）' AS stage, COUNT(*) AS cnt
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_scene_config' AND COLUMN_NAME = 'enable_intent_routing';

SELECT '复核：workflow_agent_node 场景行（期望 1）' AS stage, scene_code, enabled, output_parser
  FROM ai_scene_config WHERE scene_code = 'workflow_agent_node';
