-- ============================================================
-- AI能力统一接入层（ai2 包）数据表
-- 依据：《AI能力统一接入层 — 完整方案文档.md》V2.0 §3.1 / §3.2
-- 说明：
--   1) 表前缀 ai2_ 与包 com.moyun.ext.ai2 对应，与 AI 底座的 ai_scene_config（场景绑定）职责区分：
--      - ai_scene_config（底座，已有）：场景与 Agent/模型/知识库/工具/工作流的【绑定关系】
--      - ai2_scene_registry（本层，新增）：统一网关的场景【注册与执行配置】（Handler/模板/限流/降级/缓存）
--   2) ai2 层只读复用 AI 底座服务（AiSceneResolver/LLMService/ModelConfigService），不修改底座任何代码
-- ============================================================

-- ------------------------------------------------------------
-- AI 场景注册表（统一网关核心配置）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ai2_scene_registry (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scene_code VARCHAR(50) NOT NULL UNIQUE COMMENT '场景代码: voice_interview/resume_parse/resume_optimize/question_generate/finance_analysis/sensitive_word/daily_topic',
    scene_name VARCHAR(100) NOT NULL COMMENT '场景名称',
    scene_category VARCHAR(30) NOT NULL COMMENT '分类: chat/analysis/generation/classification',
    description VARCHAR(500) COMMENT '场景描述',

    -- ===== 绑定配置（复用 AI 底座 ai_scene_config 的绑定解析，这里仅冗余记录） =====
    bind_type VARCHAR(20) NOT NULL DEFAULT 'agent' COMMENT '绑定类型: agent/workflow/model/knowledge_only',
    agent_id BIGINT COMMENT 'bind_type=agent 时使用',
    workflow_id BIGINT COMMENT 'bind_type=workflow 时使用',
    model_id BIGINT COMMENT 'bind_type=model 时使用',
    knowledge_base_ids JSON COMMENT '知识库ID列表（任何类型都可关联）',
    tool_ids JSON COMMENT '工具ID列表（任何类型都可关联）',

    -- ===== Handler 配置 =====
    handler_bean_name VARCHAR(100) NOT NULL COMMENT '对应的Spring Bean名称',
    handler_method VARCHAR(50) DEFAULT 'execute' COMMENT '执行方法名',

    -- ===== Prompt 配置 =====
    system_prompt_template TEXT COMMENT '系统提示词模板（支持占位符 {{variable}}）',
    user_prompt_template TEXT COMMENT '用户提示词模板',
    prompt_placeholders JSON COMMENT '占位符说明 {key: description}',

    -- ===== 输出配置 =====
    output_mode VARCHAR(20) DEFAULT 'sync' COMMENT '输出模式: sync/stream/both',
    output_schema JSON COMMENT '输出结构定义',
    output_parser VARCHAR(50) COMMENT '解析器: json/markdown/custom',

    -- ===== 策略配置 =====
    max_tokens INT DEFAULT 2048,
    temperature DECIMAL(2,1) DEFAULT 0.7,
    timeout_seconds INT DEFAULT 30,
    retry_count INT DEFAULT 3,
    rate_limit_key VARCHAR(50) COMMENT '限流Key',
    rate_limit_count INT DEFAULT 100 COMMENT '限流次数',
    rate_limit_time INT DEFAULT 60 COMMENT '限流时间窗口(秒)',

    -- ===== 降级配置 =====
    fallback_model_id BIGINT COMMENT '备用模型ID',
    fallback_response TEXT COMMENT '兜底回复（AI不可用时返回）',
    enable_cache TINYINT DEFAULT 0 COMMENT '是否启用缓存',
    cache_ttl INT DEFAULT 3600 COMMENT '缓存时间(秒)',

    -- ===== 状态 =====
    is_active TINYINT DEFAULT 1,
    is_default TINYINT DEFAULT 0,
    priority INT DEFAULT 0 COMMENT '优先级（数字越大越优先）',
    version VARCHAR(20) DEFAULT 'v1' COMMENT '版本号',
    weight INT DEFAULT 100 COMMENT '灰度权重',
    parent_id BIGINT COMMENT '父配置ID（用于继承）',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_scene_code (scene_code),
    INDEX idx_is_active (is_active)
) COMMENT 'AI能力统一接入层-场景注册表';

-- ------------------------------------------------------------
-- AI 调用日志表（可观测性）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ai2_execute_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    request_id VARCHAR(64) NOT NULL COMMENT '请求ID',
    scene_code VARCHAR(50) NOT NULL COMMENT '场景代码',
    handler_name VARCHAR(100) COMMENT 'Handler名称',
    bind_type VARCHAR(20) COMMENT '绑定类型',
    model_used VARCHAR(100) COMMENT '使用的模型',
    agent_used VARCHAR(100) COMMENT '使用的Agent',
    token_used INT DEFAULT 0 COMMENT 'Token消耗',
    tool_calls JSON COMMENT '工具调用记录',
    input_summary VARCHAR(500) COMMENT '输入摘要',
    output_summary VARCHAR(500) COMMENT '输出摘要',
    status VARCHAR(20) DEFAULT 'success' COMMENT 'success/fail/timeout',
    error_msg TEXT COMMENT '错误信息',
    elapsed_ms BIGINT COMMENT '耗时(毫秒)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_request_id (request_id),
    INDEX idx_scene_code (scene_code),
    INDEX idx_created_at (created_at)
) COMMENT 'AI能力统一接入层-执行日志表';

-- ------------------------------------------------------------
-- 默认场景配置（7个场景：底座已有5个 + 新增2个）
-- 模型选择统一走底座 AiSceneResolver.resolveChatModel(scene_code)（Agent绑定→直绑模型→默认模型），
-- 此处 bind_type 默认 agent 仅作登记
-- ------------------------------------------------------------
INSERT INTO ai2_scene_registry
    (scene_code, scene_name, scene_category, description, bind_type, handler_bean_name,
     output_mode, output_parser, rate_limit_count, rate_limit_time, is_active)
VALUES
    ('voice_interview', 'AI语音面试', 'chat', '面试问答评估与追问（流式）', 'agent', 'voiceInterviewHandler',
     'both', 'json', 60, 60, 1),
    ('resume_parse', '简历解析', 'analysis', '简历文本结构化提取', 'model', 'resumeParseHandler',
     'sync', 'json', 30, 60, 1),
    ('resume_optimize', '简历优化', 'analysis', '岗位匹配 + 深度优化建议', 'agent', 'resumeOptimizeHandler',
     'sync', 'json', 30, 60, 1),
    ('question_generate', '智能出题', 'generation', '基于知识点生成面试题', 'agent', 'questionGenerateHandler',
     'sync', 'json', 30, 60, 1),
    ('finance_analysis', 'AI财务分析', 'analysis', '多维指标聚合 + 财务健康评分 + 综述', 'model', 'financeAnalysisHandler',
     'sync', 'json', 20, 60, 1),
    ('sensitive_word', '敏感词检测', 'classification', '文本敏感词识别与风险分级', 'model', 'sensitiveWordHandler',
     'sync', 'json', 200, 60, 1),
    ('daily_topic', '今日主题', 'generation', '每日主题生成', 'model', 'dailyTopicHandler',
     'sync', 'json', 50, 60, 1);
