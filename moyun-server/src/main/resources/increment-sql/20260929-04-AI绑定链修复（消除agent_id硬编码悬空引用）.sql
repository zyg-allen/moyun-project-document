-- =============================================================================
-- AI 绑定链修复：消除 agent_id 硬编码导致的悬空引用（v13.38）
-- -----------------------------------------------------------------------------
-- 背景（报告七核实结论）：
--   会话链（`VoiceInterviewServiceImpl.start()`）通过 `sys_config.voice.interview.defaultAgentId`
--   解析面试官；旁路链（预热/逐题分析/自介）通过 `ai_scene_config.agent_id` 绑定。
--   但 init-sql 种子曾把这些引用**硬编码成迁移来源库的 id**（agent 48 / 47），
--   而 `ai_agent` 的 INSERT **不含 id 列**（自增分配，全新库恒为 1/2）
--   → **悬空引用** → `start()` 抛「AI 面试官未配置或不可用」→ **面试开不起来**。
--
--   已在 init-sql（moyun-db-dml-init.sql）修正为**按 name 子查询取 id**；
--   本脚本修复**已投产库**的同类悬空引用。
--
-- 适用：任何 agent_id 指向不存在 agent 的环境（本机 moyun-db2 即为此状态）。
-- 幂等性：按「当前值是否仍悬空」判断，可重复执行（连跑两次 exit 0）。
-- 可回滚：执行前请备份（见文末备份建议）。
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 0) 修复前快照（便于人工核对；只查不改）
-- ---------------------------------------------------------------------------
SELECT '修复前：悬空的场景 agent 引用' AS stage, s.scene_code, s.agent_id
  FROM ai_scene_config s
 WHERE s.agent_id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM ai_agent a WHERE a.id = s.agent_id);

SELECT '修复前：sys_config 悬空 agent' AS stage, c.config_key, c.config_value
  FROM sys_config c
 WHERE c.config_key = 'voice.interview.defaultAgentId'
   AND NOT EXISTS (SELECT 1 FROM ai_agent a WHERE a.id = CAST(c.config_value AS UNSIGNED));

-- ---------------------------------------------------------------------------
-- 1) 语音面试链路：绑定「AI面试官·默认」
--    覆盖 4 行：voice_interview（父） + :warmup + :answer_analysis + :self_intro
-- ---------------------------------------------------------------------------
UPDATE ai_scene_config
   SET agent_id = (SELECT id FROM ai_agent WHERE name = 'AI面试官·默认' AND deleted = 0 ORDER BY id LIMIT 1)
 WHERE scene_code LIKE 'voice_interview%'
   AND (agent_id IS NULL
        OR NOT EXISTS (SELECT 1 FROM ai_agent a WHERE a.id = ai_scene_config.agent_id))
   AND EXISTS (SELECT 1 FROM ai_agent WHERE name = 'AI面试官·默认' AND deleted = 0);

-- ---------------------------------------------------------------------------
-- 2) 会话链唯一入口：sys_config.voice.interview.defaultAgentId
--    （`VoiceInterviewServiceImpl` 依赖它解析面试官；悬空即开不了面）
-- ---------------------------------------------------------------------------
UPDATE sys_config
   SET config_value = (SELECT id FROM ai_agent WHERE name = 'AI面试官·默认' AND deleted = 0 ORDER BY id LIMIT 1)
 WHERE config_key = 'voice.interview.defaultAgentId'
   AND NOT EXISTS (SELECT 1 FROM ai_agent a WHERE a.id = CAST(sys_config.config_value AS UNSIGNED))
   AND EXISTS (SELECT 1 FROM ai_agent WHERE name = 'AI面试官·默认' AND deleted = 0);

-- ---------------------------------------------------------------------------
-- 3) 其它场景的同类悬空引用：财务分析 →「财务分析师」
-- ---------------------------------------------------------------------------
UPDATE ai_scene_config
   SET agent_id = (SELECT id FROM ai_agent WHERE name = '财务分析师' AND deleted = 0 ORDER BY id LIMIT 1)
 WHERE scene_code = 'finance_analysis'
   AND (agent_id IS NULL
        OR NOT EXISTS (SELECT 1 FROM ai_agent a WHERE a.id = ai_scene_config.agent_id))
   AND EXISTS (SELECT 1 FROM ai_agent WHERE name = '财务分析师' AND deleted = 0);

-- ---------------------------------------------------------------------------
-- 4) 收尾：清理「指向不存在 agent」的残余（无同名 agent 可绑时置空，避免半悬空）
--    置空优于悬空：网关会回落到默认模型，而悬空会让场景解析直接失败。
--
--    ⚠️ 本步骤含破坏性 SET，按《项目开发规范》6.4 必须先落 `_bak_` 备份表。
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ai_scene_config_bak_agent_id_v1338` AS
SELECT id, scene_code, agent_id, NOW() AS backup_time FROM ai_scene_config;

UPDATE ai_scene_config
   SET agent_id = NULL
 WHERE agent_id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM ai_agent a WHERE a.id = ai_scene_config.agent_id);

-- ---------------------------------------------------------------------------
-- 复核 SQL（期望：两个悬空计数均为 0）
-- ---------------------------------------------------------------------------
SELECT '复核：场景悬空 agent 引用（期望 0）' AS stage, COUNT(*) AS cnt
  FROM ai_scene_config s
 WHERE s.agent_id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM ai_agent a WHERE a.id = s.agent_id);

SELECT '复核：sys_config 悬空 agent（期望 0）' AS stage, COUNT(*) AS cnt
  FROM sys_config c
 WHERE c.config_key = 'voice.interview.defaultAgentId'
   AND NOT EXISTS (SELECT 1 FROM ai_agent a WHERE a.id = CAST(c.config_value AS UNSIGNED));

SELECT '复核：语音面试链路绑定' AS stage, scene_code, agent_id
  FROM ai_scene_config WHERE scene_code LIKE 'voice_interview%' ORDER BY scene_code;

SELECT '复核：面试官取值' AS stage, config_key, config_value
  FROM sys_config WHERE config_key = 'voice.interview.defaultAgentId';

-- ---------------------------------------------------------------------------
-- 备份建议（执行本脚本前先做，可回滚）：
--   CREATE TABLE ai_scene_config_bak_v1338 AS SELECT * FROM ai_scene_config;
--   CREATE TABLE sys_config_bak_v1338      AS SELECT * FROM sys_config;
-- 回滚（如需）：
--   UPDATE ai_scene_config s JOIN ai_scene_config_bak_v1338 b ON b.id=s.id SET s.agent_id=b.agent_id;
--   UPDATE sys_config      c JOIN sys_config_bak_v1338      b ON b.config_id=c.config_id SET c.config_value=b.config_value;
-- ---------------------------------------------------------------------------

-- ---------------------------------------------------------------------------
-- 遗留说明（本脚本**不处理**，需另行决策）：
--   1. `ai_agent.model_config_id = 17` 指向不存在的模型 → **不影响运行**：
--      `AgentModelRouter` 只查 `model_type='chat'` 并支持自动挑选默认模型；
--      且 `ai_model_config` 的 `is_default=1` 分别为 embedding 与 chat 各一行（**按类型区分，非二义**）。
--   2. `daily_token_limit` 为 NULL → 该场景**无每日 token 熔断**（`TokenCostGuard` 语义：null/0=不限），
--      属成本治理议题，需产品决策后再设阈值。
-- ---------------------------------------------------------------------------
