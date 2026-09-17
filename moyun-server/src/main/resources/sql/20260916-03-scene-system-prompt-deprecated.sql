-- =====================================================================
-- 20260916-03 v11.95 · 场景表系统提示词字段废弃标注（任务1）
--
-- 变更内容：
--   1. system_prompt_template 列标注废弃（MODIFY COMMENT，不删列、不动数据）：
--      v11.95 裁决——场景表不再承载系统提示词，人设统一由 ai_agent.system_prompt
--      承载（对话场景经 ChatContextBuilderService 组装，后台任务经 mergePersona
--      注入 agentPersona + 任务边界声明，无 Agent 场景用 Handler 内置默认提示词）。
--      场景表只保留治理配置（限流/熔断/输出解析/兜底）。
--   2. 代码侧消费方已清理（v11.95）：
--      - AbstractAiSceneHandler.buildSystemPrompt 不再读取场景模板（恒返回 null）
--      - KnowledgeQaHandler 提示词统一走 Agent（loadAgent 空壳兜底，agent 永不为 null）
--      - AiSceneConfig.systemPromptTemplate 标 @Deprecated
--      - FinanceAnalysisHandler 原走场景模板，现回落内置 DEFAULT_SYSTEM（原有兜底）
--
-- 规范：增量 DDL；仅改 COMMENT，存量数据保留（迁移期数据溯源），不 ALTER 数据
-- =====================================================================

-- 1. 列废弃标注（类型不变，仅更新注释）
ALTER TABLE `moyun-db`.ai_scene_config
    MODIFY COLUMN system_prompt_template text NULL
    COMMENT '[已废弃 v11.95] 人设统一走 ai_agent.system_prompt，场景表只留治理配置；存量数据仅溯源用';

-- 2. 校验
SELECT column_name, column_comment
FROM information_schema.columns
WHERE table_schema = 'moyun-db'
  AND table_name = 'ai_scene_config'
  AND column_name = 'system_prompt_template';
