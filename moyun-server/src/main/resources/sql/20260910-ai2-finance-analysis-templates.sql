-- =============================================================
-- v11.48 finance_analysis 场景配置种子模板（ai_scene_config）
-- 目的：让"管理页改提示词/输出结构，下次调用立即生效"端到端可用——
--   此前该场景配置行只有路由基础字段，三个模板字段全 NULL，
--   Handler 永远回落 Java 硬编码默认值，管理页填的配置走不到 LLM。
-- 幂等与安全：
--   1) 仅在目标字段为 NULL/空串时填充（WHERE 条件），管理员已在管理页
--      填写的自定义模板不会被覆盖，可重复执行；
--   2) 模板内容与 FinanceAnalysisHandler 的 DEFAULT_* 回落值保持一致，
--      管理员可随时在管理页（AI能力 → 场景配置 → 执行配置）修改，
--      注册中心 getConfig 直查库（v11.48），改完下次调用立即生效，无需重启。
-- =============================================================

-- -------------------------------------------------------------
-- 1. 系统提示词模板（仅空值时填充）
-- -------------------------------------------------------------
UPDATE `moyun-db`.ai_scene_config
SET system_prompt_template = '你是一位专业、友善的个人财务顾问。基于用户提供的记账数据做财务分析，只输出 JSON，禁止输出 JSON 以外的任何内容：
{"summary": "财务综述(3-5句，口语化、引用数据、无Markdown)",
 "healthScore": 0到100整数（财务健康评分，参考：储蓄率>20%且负债率<40%为健康；持续入不敷出为危险）,
 "risks": [{"level":"high|medium|low","title":"风险标题","detail":"具体说明，引用数据"}],
 "suggestions": [{"icon":"emoji图标","title":"建议标题","detail":"可执行建议，引用数据"}]}
数值均以用户提供为准，不要自行计算修改。数据不足时基于已有数据客观分析，不臆造。'
WHERE scene_code = 'finance_analysis'
  AND (system_prompt_template IS NULL OR system_prompt_template = '');

-- -------------------------------------------------------------
-- 2. 用户提示词模板（仅空值时填充）
-- -------------------------------------------------------------
UPDATE `moyun-db`.ai_scene_config
SET user_prompt_template = '统计窗口：{{window}}

用户财务数据（含规则引擎计算的精确指标，数值请直接引用）：
{{ledgerContext}}'
WHERE scene_code = 'finance_analysis'
  AND (user_prompt_template IS NULL OR user_prompt_template = '');

-- -------------------------------------------------------------
-- 3. 输出结构定义（仅空值时填充；JSON 列，内容须为合法 JSON）
-- -------------------------------------------------------------
UPDATE `moyun-db`.ai_scene_config
SET output_schema = '{"summary": "string", "healthScore": "int 0-100",
 "risks": [{"level": "high|medium|low", "title": "string", "detail": "string"}],
 "suggestions": [{"icon": "string", "title": "string", "detail": "string"}]}'
WHERE scene_code = 'finance_analysis'
  AND (output_schema IS NULL OR output_schema = '');

-- -------------------------------------------------------------
-- 4. 占位符说明（仅空值时填充；JSON 列）——管理页编辑模板时的变量参考
-- -------------------------------------------------------------
UPDATE `moyun-db`.ai_scene_config
SET prompt_placeholders = '{"ledgerContext": "业务侧组装的财务上下文 JSON（画像/核心指标护栏/收入来源/支出结构Top5/负债明细时点事实）",
 "window": "统计窗口文案，如：本月（自 2026-09-01 起，含数据 3 个月）"}'
WHERE scene_code = 'finance_analysis'
  AND (prompt_placeholders IS NULL OR prompt_placeholders = '');
