-- =====================================================================
-- 20260916-06 v11.95 · 模型表 JSON Mode 支持（任务4：结构化输出升级）
--
-- 变更内容：
--   1. ai_model_config 新增 supports_json_mode 字段（TINYINT(1)，缺省 0）。
--      置 1 的模型在结构化场景（ai_scene_config.output_schema 非空）下，
--      由后端自动下发原生 JSON Mode：
--        - OpenAI 兼容端点（openai/dashscope/deepseek/moonshot 等）：
--          response_format = json_object
--        - Ollama 原生端点：format = json
--      模型未开启（0/NULL）时静默走 Prompt 约束路径（v11.95 前既有行为）。
--
--   2. 代码级配套（无 DDL）：解析失败降级——首次输出无法提取 JSON 主体时，
--      追加「只输出合法 JSON」约束自动重试一次（AbstractAiSceneHandler.chatJson，
--      ★★ 兜底路径），覆盖 deep_optimize 报「AI 服务暂不可用」类解析失败场景。
--
-- 管理操作：按模型实际能力在「AI管理-模型配置」页逐个开启，例如：
--   UPDATE ai_model_config SET supports_json_mode = 1 WHERE model_name LIKE 'gpt-4o%';
-- =====================================================================

ALTER TABLE `moyun-db`.ai_model_config
    ADD COLUMN supports_json_mode TINYINT(1) DEFAULT 0 COMMENT '是否支持JSON Mode（结构化场景下发response_format强制JSON输出）' AFTER streaming_supported;

-- 校验
SELECT id, name, provider, model_name, streaming_supported, supports_json_mode
FROM `moyun-db`.ai_model_config;
