-- =============================================================================
-- 面试族 4 行场景配置落库（批次 1 收编配套，v13.46）
-- -----------------------------------------------------------------------------
-- 背景：批次 1 把 3 处「直连模型」的 LLM 调用收编进网关，需要对应配置行：
--   · voice_interview:opening_fallback  ← 原 generateOpening 代码拼提示词
--   · voice_interview:hint              ← 原 requestHint 代码拼提示词
--   · voice_interview:report_review     ← 原 enhanceReportByAgent 硬编码系统提示词
--   · voice_interview:industry_insight  ← 发展方向（懒生成，新功能）
--
-- 口径（与 init-sql/moyun-db-dml-init.sql 双轨一致，铁律 10）：
--   · agent_id 一律用 `name` 子查询 —— 防双库 id 分叉再次悬空（v13.38 教训）
--   · 系统提示词不写 `system_prompt_template`（**该列已废弃**）：
--     人设由 Agent 表承载（网关经 input.agentPersona 注入 mergePersona），
--     任务指令与字段规范全部进 `user_prompt_template`
--   · enabled=1：与代码收编同批生效（回滚时一并回滚）
--   · output_parser：纯文本任务='text'、JSON 任务='json'（查库中现有取值确认语义）
--
-- 幂等：全部 `INSERT ... SELECT ... WHERE NOT EXISTS`（按 scene_code 判存），
--       可重复执行（连跑两次 exit 0）。
-- 可回滚：DELETE FROM ai_scene_config WHERE scene_code IN (...4 项...);
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 0) 修复前快照（只查不改）
-- ---------------------------------------------------------------------------
SELECT '修复前：面试族已存在的 task 行' AS stage, scene_code, enabled, output_parser, max_tokens
  FROM ai_scene_config
 WHERE scene_code LIKE 'voice_interview%'
 ORDER BY scene_code;

-- ---------------------------------------------------------------------------
-- 1) voice_interview:opening_fallback —— warmup 失败后的降级开场（纯文本）
-- ---------------------------------------------------------------------------
INSERT INTO ai_scene_config
(scene_code, scene_name, description, scene_category, agent_id, model_config_id,
 knowledge_library_ids, tool_ids, workflow_id, config_json,
 handler_bean_name, handler_method, system_prompt_template, user_prompt_template, prompt_placeholders,
 output_mode, output_schema, output_parser, max_tokens, temperature, timeout_seconds, retry_count,
 rate_limit_key, rate_limit_count, rate_limit_time, daily_token_limit, enable_output_filter,
 fallback_model_id, fallback_response, enable_cache, cache_ttl, version, weight, priority,
 is_default, enabled, open_api, create_time, update_time, deleted)
SELECT
 'voice_interview:opening_fallback', 'AI 语音面试-开场降级',
 'task 拆行：warmup 失败后的简版开场白+首题（v13.46 批次 1 自代码 generateOpening 迁移；V1.1#1「收编不删除」——warmup 失败多为瞬时网络抖动，删兜底=一次抖动一场面试开不了头）',
 'chat', (SELECT id FROM ai_agent WHERE name='AI面试官·默认' AND deleted=0 ORDER BY id LIMIT 1), NULL,
 NULL, NULL, NULL, NULL, 'defaultSceneExecutor', 'execute', NULL,
 '你是一位专业的技术面试官，即将开始一场 {{context}} 岗位的面试（难度：{{difficulty}}）。请用一两句话自然开场，并直接提出第一个问题。要求：① 开场简洁友好，不做冗长自我介绍；② 第一个问题应贴合岗位与难度，可先请候选人做自我介绍或简述与该岗位最相关的经历；③ 只输出开场白与问题本身，不要输出任何解释、标题或 JSON 结构。',
 '{"context": "岗位名称", "difficulty": "难度", "agentPersona": "面试官人设（系统提示词）", "resumeDigest": "候选人简历摘要"}',
 'sync', NULL, 'text', 512, 0.7, 60, 0,
 NULL, 30, 60, NULL, 1, NULL, '面试官开场生成失败，请稍后重试', 0, NULL, 1, 1, 0, 1, 1, 0, NOW(), NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_scene_config WHERE scene_code = 'voice_interview:opening_fallback');

-- ---------------------------------------------------------------------------
-- 2) voice_interview:hint —— 一句话思考引导（纯文本；计费=免费，两级配额）
-- ---------------------------------------------------------------------------
INSERT INTO ai_scene_config
(scene_code, scene_name, description, scene_category, agent_id, model_config_id,
 knowledge_library_ids, tool_ids, workflow_id, config_json,
 handler_bean_name, handler_method, system_prompt_template, user_prompt_template, prompt_placeholders,
 output_mode, output_schema, output_parser, max_tokens, temperature, timeout_seconds, retry_count,
 rate_limit_key, rate_limit_count, rate_limit_time, daily_token_limit, enable_output_filter,
 fallback_model_id, fallback_response, enable_cache, cache_ttl, version, weight, priority,
 is_default, enabled, open_api, create_time, update_time, deleted)
SELECT
 'voice_interview:hint', 'AI 语音面试-思考提示',
 'task 拆行：一句话思考引导（不泄答案）（v13.46 批次 1 自代码 requestHint 迁移；计费口径=免费，两级配额：每题3次/全场15次）',
 'chat', (SELECT id FROM ai_agent WHERE name='AI面试官·默认' AND deleted=0 ORDER BY id LIMIT 1), NULL,
 NULL, NULL, NULL, NULL, 'defaultSceneExecutor', 'execute', NULL,
 '候选人请求思考提示。请以面试官身份给一句简短的思考引导（提示回答方向或组织思路，不直接给出答案），40字以内，只输出这句话。',
 '{"question": "当前题目", "answer": "候选人已作答内容（可空）"}',
 'sync', NULL, 'text', 128, 0.7, 30, 0,
 NULL, 20, 60, NULL, 1, NULL, '别着急，可以从你熟悉的相关项目经历入手，按「背景→做法→结果」的思路组织回答。', 0, NULL, 1, 1, 0, 1, 1, 0, NOW(), NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_scene_config WHERE scene_code = 'voice_interview:hint');

-- ---------------------------------------------------------------------------
-- 3) voice_interview:report_review —— 整场复盘（JSON；字段规范进 user 模板）
-- ---------------------------------------------------------------------------
INSERT INTO ai_scene_config
(scene_code, scene_name, description, scene_category, agent_id, model_config_id,
 knowledge_library_ids, tool_ids, workflow_id, config_json,
 handler_bean_name, handler_method, system_prompt_template, user_prompt_template, prompt_placeholders,
 output_mode, output_schema, output_parser, max_tokens, temperature, timeout_seconds, retry_count,
 rate_limit_key, rate_limit_count, rate_limit_time, daily_token_limit, enable_output_filter,
 fallback_model_id, fallback_response, enable_cache, cache_ttl, version, weight, priority,
 is_default, enabled, open_api, create_time, update_time, deleted)
SELECT
 'voice_interview:report_review', 'AI 语音面试-整场复盘',
 'task 拆行：整场复盘（v13.46 批次 1 自代码 enhanceReportByAgent 迁移；提示词结构与原实现等价：7 字段含 perQuestion{questionIdx,score,comment}；字段规范进 user_prompt_template——system_prompt_template 已废弃，人设由 Agent 表承载；批次 2 再扩 levelEstimate 结构化与 predictedQuestions）',
 'chat', (SELECT id FROM ai_agent WHERE name='AI面试官·默认' AND deleted=0 ORDER BY id LIMIT 1), NULL,
 NULL, NULL, NULL, NULL, 'defaultSceneExecutor', 'execute', NULL,
 '【任务】你是一位资深技术面试官，面试已结束。请基于下方候选人资料与整场对话记录，输出结构化复盘报告 JSON。\n【字段要求】\n1. overallComment：3-5 句整场总评，结合岗位要求评价整体表现，指出最突出的特点。\n2. jobMatch：{rate: 0-100 整数匹配度, reason: 1-2 句依据（对照岗位要求与实际作答）}。\n3. highlights：2-4 条真实亮点数组，每条 {title: 短标题≤12字, detail: 引用作答中的具体内容说明}。\n4. weakPoints：2-4 条薄弱点数组，每条 {title: 短标题≤12字, detail: 具体不足与影响，禁止复述问题原文}。\n5. suggestions：3-5 条可执行改进建议字符串数组，结合简历与岗位，每条不超过 60 字。\n6. perQuestion：每道主问题一条 {questionIdx: 题号, score: 0-100 整数（评分要有区分度：优秀≥80、合格60-79、不合格<60）, comment: 1-2 句针对性点评≤80字}。\n7. dimensions：{relevance 切题度, professionalism 专业深度, fluency 表达流畅, interactivity 互动质量, confidence 自信度, logic 逻辑结构}，0-100 整数。\n只输出 JSON 对象，不要输出任何其他文本。\n\n【候选人资料与对话记录】\n目标岗位：{{position}}\n岗位要求：{{jd}}\n候选人简历摘要：\n{{resumeDigest}}\n整场对话记录（含每题初评分，供参考）：\n{{qaList}}',
 '{"position": "岗位名称", "jd": "岗位要求", "resumeDigest": "候选人简历摘要", "qaList": "整场对话记录（含每题初评分）"}',
 'sync', NULL, 'json', 2560, 0.7, 180, 0,
 NULL, 10, 60, NULL, 1, NULL, '报告生成中，请稍后重试', 0, NULL, 1, 3, 0, 1, 1, 0, NOW(), NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_scene_config WHERE scene_code = 'voice_interview:report_review');

-- ---------------------------------------------------------------------------
-- 4) voice_interview:industry_insight —— 发展方向（JSON；懒生成）
-- ---------------------------------------------------------------------------
INSERT INTO ai_scene_config
(scene_code, scene_name, description, scene_category, agent_id, model_config_id,
 knowledge_library_ids, tool_ids, workflow_id, config_json,
 handler_bean_name, handler_method, system_prompt_template, user_prompt_template, prompt_placeholders,
 output_mode, output_schema, output_parser, max_tokens, temperature, timeout_seconds, retry_count,
 rate_limit_key, rate_limit_count, rate_limit_time, daily_token_limit, enable_output_filter,
 fallback_model_id, fallback_response, enable_cache, cache_ttl, version, weight, priority,
 is_default, enabled, open_api, create_time, update_time, deleted)
SELECT
 'voice_interview:industry_insight', 'AI 语音面试-发展方向',
 'task 拆行：发展方向建议（懒生成：首次打开 tab 才调用；输入追加本场报告上下文以与 resume_optimize:job_match 划清边界；预留 knowledge_library_ids 走 RAG 作为真动态来源）',
 'chat', (SELECT id FROM ai_agent WHERE name='AI面试官·默认' AND deleted=0 ORDER BY id LIMIT 1), NULL,
 NULL, NULL, NULL, NULL, 'defaultSceneExecutor', 'execute', NULL,
 '【任务】你是一位资深的职业发展顾问。请结合候选人的目标岗位、简历技能与本场面试暴露的真实薄弱点，输出个人化的发展方向分析 JSON。\n【字段要求】\n1. trends：技术趋势 3-4 条，每条 {title: ≤12字, detail: ≤60字, maturity: 成熟期/上升期/早期}。\n2. supplyDemand：{existing: [简历已具备的技能标签 3-8 个], missing: [建议补充的技能标签 3-8 个]}。\n3. actions：3 条行动建议，每条 {content: ≤80字可执行建议, relatedWeakPoint: 必须引用本场报告中的真实薄弱点原文}。\n【硬约束】① 严禁标注具体百分比、薪酬数字或时效性季度数据（无实时数据，编造时效数据会让用户误信）；\n② 每条行动建议必须锚定本场真实薄弱点，禁止「建议学习云原生」这类与个人无关的通用废话；\n③ 措辞为方向性判断，不承诺实时行业动态。只输出 JSON 对象，不要输出任何其他文本。\n\n【候选人资料】\n目标岗位：{{position}}\n岗位要求：{{jd}}\n简历技能：{{skills}}\n本场薄弱点：{{weakPoints}}\n水平定级：{{levelEstimate}}\n低分维度：{{lowDimensions}}',
 '{"position": "岗位名称", "jd": "岗位要求", "skills": "简历技能", "weakPoints": "本场薄弱点", "levelEstimate": "水平定级", "lowDimensions": "低分维度"}',
 'sync', NULL, 'json', 1536, 0.7, 120, 1,
 NULL, 5, 600, NULL, 1, NULL, '发展方向分析生成失败，请稍后重试', 0, 3600, 1, 1, 0, 1, 1, 0, NOW(), NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM ai_scene_config WHERE scene_code = 'voice_interview:industry_insight');

-- ---------------------------------------------------------------------------
-- 复核 SQL（期望：4 行 enabled=1、parser 正确、悬空 agent = 0）
-- ---------------------------------------------------------------------------
SELECT '复核：面试族配置行' AS stage, scene_code, enabled, output_parser, max_tokens,
       CASE WHEN agent_id IS NULL THEN 'NULL'
            WHEN EXISTS (SELECT 1 FROM ai_agent a WHERE a.id = s.agent_id) THEN CONCAT('agent=', agent_id)
            ELSE CONCAT('悬空=', agent_id) END AS agent_ref
  FROM ai_scene_config s
 WHERE scene_code LIKE 'voice_interview%'
 ORDER BY scene_code;

SELECT '复核：新增 4 行是否齐备（期望 4）' AS stage, COUNT(*) AS cnt
  FROM ai_scene_config
 WHERE scene_code IN ('voice_interview:opening_fallback','voice_interview:hint',
                      'voice_interview:report_review','voice_interview:industry_insight');

SELECT '复核：面试族悬空 agent 引用（期望 0）' AS stage, COUNT(*) AS cnt
  FROM ai_scene_config s
 WHERE s.agent_id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM ai_agent a WHERE a.id = s.agent_id);

-- ---------------------------------------------------------------------------
-- 备份建议（执行前）：
--   CREATE TABLE IF NOT EXISTS ai_scene_config_bak_v1346 AS SELECT * FROM ai_scene_config;
-- 回滚：
--   DELETE FROM ai_scene_config WHERE scene_code IN
--     ('voice_interview:opening_fallback','voice_interview:hint',
--      'voice_interview:report_review','voice_interview:industry_insight');
-- ---------------------------------------------------------------------------
