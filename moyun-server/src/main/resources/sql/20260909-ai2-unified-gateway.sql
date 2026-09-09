-- ============================================================
-- AI 能力统一接入层 —— 增量 DDL（v11.41）
-- 依据：《AI能力统一接入层 — 完整方案文档.md》V2.0 §3.1 / §3.2
-- 说明：
--   1) 不新建 ai2_scene_registry，改为对已有 ai_scene_config 增量加列（执行层配置）
--      —— 绑定关系（agent_id/model_config_id/workflow_id/knowledge_library_ids/tool_ids）已存在，不重复
--   2) ai_execute_log 为独立日志表，新建
--   3) 表前缀统一 ai_（与 com.moyun.ext.ai 包对齐），废弃 ai2_ 前缀
-- ============================================================

-- ------------------------------------------------------------
-- 1. ai_scene_config 增量加列：执行层配置（Handler/Prompt/输出/策略/限流/降级/缓存）
-- 幂等说明：重复执行会报 Duplicate column，可忽略（或执行前确认列已存在）
-- ------------------------------------------------------------
ALTER TABLE `moyun-db`.ai_scene_config
    ADD COLUMN scene_category        varchar(30)      DEFAULT NULL COMMENT '场景分类: chat/analysis/generation/classification' AFTER description,
    ADD COLUMN handler_bean_name    varchar(100)     DEFAULT NULL COMMENT '对应Spring Bean名称（统一网关路由）' AFTER config_json,
    ADD COLUMN handler_method       varchar(50)      DEFAULT 'execute' COMMENT '执行方法名' AFTER handler_bean_name,
    ADD COLUMN system_prompt_template text           COMMENT '系统提示词模板（支持占位符 {{variable}}）' AFTER handler_method,
    ADD COLUMN user_prompt_template text             COMMENT '用户提示词模板' AFTER system_prompt_template,
    ADD COLUMN prompt_placeholders   json            DEFAULT NULL COMMENT '占位符说明 {key: description}' AFTER user_prompt_template,
    ADD COLUMN output_mode           varchar(20)      DEFAULT 'sync' COMMENT '输出模式: sync/stream/both' AFTER prompt_placeholders,
    ADD COLUMN output_schema         json            DEFAULT NULL COMMENT '输出结构定义' AFTER output_mode,
    ADD COLUMN output_parser        varchar(50)      DEFAULT NULL COMMENT '解析器: json/markdown/custom' AFTER output_schema,
    ADD COLUMN max_tokens            int             DEFAULT 2048 COMMENT '最大Token数' AFTER output_parser,
    ADD COLUMN temperature           decimal(2,1)    DEFAULT 0.7 COMMENT '温度参数' AFTER max_tokens,
    ADD COLUMN timeout_seconds      int             DEFAULT 30 COMMENT '超时秒数' AFTER temperature,
    ADD COLUMN retry_count           int             DEFAULT 3 COMMENT '重试次数' AFTER timeout_seconds,
    ADD COLUMN rate_limit_key        varchar(50)     DEFAULT NULL COMMENT '限流Key' AFTER retry_count,
    ADD COLUMN rate_limit_count      int             DEFAULT 100 COMMENT '限流次数' AFTER rate_limit_key,
    ADD COLUMN rate_limit_time       int             DEFAULT 60 COMMENT '限流时间窗口(秒)' AFTER rate_limit_count,
    ADD COLUMN fallback_model_id     bigint          DEFAULT NULL COMMENT '备用模型ID' AFTER rate_limit_time,
    ADD COLUMN fallback_response     text            COMMENT '兜底回复（AI不可用时返回）' AFTER fallback_model_id,
    ADD COLUMN enable_cache          tinyint(1)      DEFAULT 0 COMMENT '是否启用缓存' AFTER fallback_response,
    ADD COLUMN cache_ttl             int             DEFAULT 3600 COMMENT '缓存时间(秒)' AFTER enable_cache;

-- ------------------------------------------------------------
-- 2. 默认场景执行配置（为已有场景补充 handler_bean_name / scene_category）
-- 模型选择仍走 AiSceneResolver.resolveChatModel(scene_code)，bind_type 不再单独存储
-- ------------------------------------------------------------
UPDATE `moyun-db`.ai_scene_config SET scene_category = 'chat',        handler_bean_name = 'voiceInterviewHandler'    WHERE scene_code = 'voice_interview'   AND handler_bean_name IS NULL;
UPDATE `moyun-db`.ai_scene_config SET scene_category = 'analysis',   handler_bean_name = 'resumeParseHandler'        WHERE scene_code = 'resume_parse'      AND handler_bean_name IS NULL;
UPDATE `moyun-db`.ai_scene_config SET scene_category = 'analysis',   handler_bean_name = 'resumeOptimizeHandler'    WHERE scene_code = 'resume_optimize'    AND handler_bean_name IS NULL;
UPDATE `moyun-db`.ai_scene_config SET scene_category = 'generation', handler_bean_name = 'questionGenerateHandler'  WHERE scene_code = 'question_generate' AND handler_bean_name IS NULL;
UPDATE `moyun-db`.ai_scene_config SET scene_category = 'analysis',   handler_bean_name = 'financeAnalysisHandler'   WHERE scene_code = 'finance_analysis'  AND handler_bean_name IS NULL;

-- 新增场景（sensitive_word / daily_topic 需先在 AiSceneEnum 注册）
INSERT INTO `moyun-db`.ai_scene_config (scene_code, scene_name, description, scene_category, handler_bean_name, output_mode, output_parser, version, weight, priority, is_default, enabled)
SELECT 'sensitive_word', '敏感词检测', '文本敏感词识别与风险分级', 'classification', 'sensitiveWordHandler', 'sync', 'json', 'v1', 100, 0, 1, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `moyun-db`.ai_scene_config WHERE scene_code = 'sensitive_word' AND version = 'v1');

INSERT INTO `moyun-db`.ai_scene_config (scene_code, scene_name, description, scene_category, handler_bean_name, output_mode, output_parser, version, weight, priority, is_default, enabled)
SELECT 'daily_topic', '今日主题', '每日主题生成', 'generation', 'dailyTopicHandler', 'sync', 'json', 'v1', 100, 0, 1, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `moyun-db`.ai_scene_config WHERE scene_code = 'daily_topic' AND version = 'v1');

-- v11.43：业务存量场景若无配置行（原 UPDATE 仅命中已有行），补默认启用配置行，
-- 保证统一网关 getConfig() 可命中（业务网关调用 + /api/ai/execute 对外统一入口）
INSERT INTO `moyun-db`.ai_scene_config (scene_code, scene_name, description, scene_category, handler_bean_name, output_mode, output_parser, version, weight, priority, is_default, enabled)
SELECT 'finance_analysis', 'AI 财务分析', '多维指标聚合 + 财务健康评分 + LLM 综述', 'analysis', 'financeAnalysisHandler', 'sync', 'text', 'v1', 100, 0, 1, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `moyun-db`.ai_scene_config WHERE scene_code = 'finance_analysis' AND version = 'v1');

INSERT INTO `moyun-db`.ai_scene_config (scene_code, scene_name, description, scene_category, handler_bean_name, output_mode, output_parser, version, weight, priority, is_default, enabled)
SELECT 'voice_interview', 'AI 语音面试', '6阶段面试流程 + 智能评分', 'chat', 'voiceInterviewHandler', 'sync', 'json', 'v1', 100, 0, 1, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `moyun-db`.ai_scene_config WHERE scene_code = 'voice_interview' AND version = 'v1');

INSERT INTO `moyun-db`.ai_scene_config (scene_code, scene_name, description, scene_category, handler_bean_name, output_mode, output_parser, version, weight, priority, is_default, enabled)
SELECT 'resume_parse', '简历解析', '简历文本结构化提取', 'analysis', 'resumeParseHandler', 'sync', 'json', 'v1', 100, 0, 1, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `moyun-db`.ai_scene_config WHERE scene_code = 'resume_parse' AND version = 'v1');

INSERT INTO `moyun-db`.ai_scene_config (scene_code, scene_name, description, scene_category, handler_bean_name, output_mode, output_parser, version, weight, priority, is_default, enabled)
SELECT 'resume_optimize', '简历优化', '岗位匹配 + 深度优化建议', 'analysis', 'resumeOptimizeHandler', 'sync', 'json', 'v1', 100, 0, 1, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `moyun-db`.ai_scene_config WHERE scene_code = 'resume_optimize' AND version = 'v1');

INSERT INTO `moyun-db`.ai_scene_config (scene_code, scene_name, description, scene_category, handler_bean_name, output_mode, output_parser, version, weight, priority, is_default, enabled)
SELECT 'question_generate', '智能出题', '基于知识点生成面试题', 'generation', 'questionGenerateHandler', 'sync', 'json', 'v1', 100, 0, 1, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `moyun-db`.ai_scene_config WHERE scene_code = 'question_generate' AND version = 'v1');

-- ------------------------------------------------------------
-- 3. AI 调用日志表（独立新建，可观测性）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `moyun-db`.ai_execute_log (
    id              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    request_id      varchar(64)  NOT NULL COMMENT '请求ID',
    scene_code      varchar(50)  NOT NULL COMMENT '场景代码',
    handler_name    varchar(100) DEFAULT NULL COMMENT 'Handler名称',
    bind_type       varchar(20)  DEFAULT NULL COMMENT '绑定类型',
    model_used      varchar(100) DEFAULT NULL COMMENT '使用的模型',
    agent_used      varchar(100) DEFAULT NULL COMMENT '使用的Agent',
    token_used      int          DEFAULT 0 COMMENT 'Token消耗',
    tool_calls      json         DEFAULT NULL COMMENT '工具调用记录',
    input_summary   varchar(500) DEFAULT NULL COMMENT '输入摘要',
    output_summary  varchar(500) DEFAULT NULL COMMENT '输出摘要',
    status          varchar(20)  DEFAULT 'success' COMMENT 'success/fail/timeout',
    error_msg       text         COMMENT '错误信息',
    elapsed_ms      bigint       DEFAULT NULL COMMENT '耗时(毫秒)',
    create_time     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_request_id (request_id),
    KEY idx_scene_code (scene_code),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI能力调用日志表（可观测性）';
