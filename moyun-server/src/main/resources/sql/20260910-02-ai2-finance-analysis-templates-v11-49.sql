-- =============================================================
-- v11.49 finance_analysis 场景模板覆盖升级（ai_scene_config）
-- 背景：v11.48 种子模板产出"无数据支撑的空话"（用户反馈：结果太简单、没参考意义）。
--   v11.49 三管齐下：上下文增强（趋势/预算/应急基金/清偿测算）+ 输出契约富化
--   （risks.evidence / suggestions.expectedImpact）+ 模板反空话约束。
-- 幂等与安全：仅当字段值仍为 v11.48 旧种子（前缀匹配）时更新——
--   管理员已在管理页自定义的模板不会被覆盖；可重复执行。
-- =============================================================

-- -------------------------------------------------------------
-- 1. 系统提示词：升级为"资深顾问 + 反空话硬约束"模板
-- -------------------------------------------------------------
UPDATE `moyun-db`.ai_scene_config
SET system_prompt_template = '你是一位资深个人财务顾问，为用户提供基于真实记账数据的财务分析。

要求：
1. 每个结论必须引用具体数据（金额/百分比/月份），禁止"建议合理规划""量入为出"等无数据支撑的空话
2. 综述要讲"发现的故事"：钱从哪来、花到哪去、趋势如何、最值得注意的一件事
3. 风险按严重度排序，并给出数据依据（evidence，如"近3月餐饮 ¥6,500，环比+65%"）
4. 建议必须可执行、具体到动作，并量化预期效果（expectedImpact，如"每月约节省 ¥800"）
5. 只输出 JSON，禁止输出 JSON 以外的任何内容

输出结构：
{"summary": "财务综述，3-5句，口语化，无Markdown",
 "healthScore": 0到100整数（参考：储蓄率>20%且负债率<40%为健康；持续入不敷出为危险）,
 "risks": [{"level":"high|medium|low","title":"风险标题","detail":"具体说明","evidence":"数据依据，引用金额/百分比/月份"}],
 "suggestions": [{"icon":"emoji图标","title":"建议标题","detail":"具体可执行动作","expectedImpact":"量化预期，如每月约节省¥800"}]}

数值以用户提供为准，不要自行计算修改。数据不足时基于已有数据客观分析，不臆造。风险/建议各 2-5 条，按重要性排序。'
WHERE scene_code = 'finance_analysis'
  AND system_prompt_template LIKE '你是一位专业、友善的个人财务顾问%';

-- -------------------------------------------------------------
-- 2. 用户提示词：更新窗口说明（另附趋势数据）
-- -------------------------------------------------------------
UPDATE `moyun-db`.ai_scene_config
SET user_prompt_template = '统计窗口：{{window}}

用户财务数据（含规则引擎计算的精确指标与逐月趋势/预算执行/债务测算，数值请直接引用）：
{{ledgerContext}}'
WHERE scene_code = 'finance_analysis'
  AND user_prompt_template LIKE '统计窗口：{{window}}%'
  AND user_prompt_template NOT LIKE '%逐月趋势%';

-- -------------------------------------------------------------
-- 3. 输出结构：补充 evidence / expectedImpact 字段
-- -------------------------------------------------------------
UPDATE `moyun-db`.ai_scene_config
SET output_schema = '{"summary": "string",
 "healthScore": "int 0-100",
 "risks": [{"level": "high|medium|low", "title": "string", "detail": "string", "evidence": "string"}],
 "suggestions": [{"icon": "string", "title": "string", "detail": "string", "expectedImpact": "string"}]}'
WHERE scene_code = 'finance_analysis'
  AND (output_schema IS NULL OR output_schema = ''
       OR output_schema LIKE '{"summary": "string", "healthScore": "int 0-100",%');

-- -------------------------------------------------------------
-- 4. 占位符说明同步（仅旧种子时更新）
-- -------------------------------------------------------------
UPDATE `moyun-db`.ai_scene_config
SET prompt_placeholders = '{"ledgerContext": "业务侧组装的财务上下文 JSON（画像/核心指标护栏/收入来源/支出结构Top5/负债明细含清偿测算/逐月收支趋势/分类环比/预算执行）",
 "window": "统计窗口文案，如：本月（自 2026-09-01 起，含数据 3 个月；另附近6个月趋势数据）"}'
WHERE scene_code = 'finance_analysis'
  AND (prompt_placeholders IS NULL OR prompt_placeholders = ''
       OR prompt_placeholders LIKE '%业务侧组装的财务上下文 JSON（画像/核心指标护栏/收入来源/支出结构Top5/负债明细时点事实）%');
