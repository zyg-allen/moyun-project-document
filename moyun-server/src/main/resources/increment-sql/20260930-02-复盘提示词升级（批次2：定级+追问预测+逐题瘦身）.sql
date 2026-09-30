-- =============================================================================
-- 面试报告复盘提示词升级（批次 2，v13.47）
-- -----------------------------------------------------------------------------
-- 变更：`voice_interview:report_review` 的 user_prompt_template 升级为批次 2 口径
--   ① 新增 `levelEstimate` 结构化定级（junior/mid/senior）——原只拼进 summary 文本；
--   ② 新增 `predictedQuestions`（上限 6 条）——「🔮 追问预测」tab 的数据源；
--   ③ `perQuestion` **瘦身**：由「逐题全量 {questionIdx,score,comment}」改为
--      「仅回填需修正的题目 {questionIdx,score}」，逐题点评文本复用 answer_analysis
--      已落库结果，不再由复盘重复产出（V1.2#2 输出防爆）。
--
-- 依据：方案 V1.2 §3.2（追问预测并入复盘，不单开）、§5.4（报告 schema）、§0.1#3。
--
-- 幂等：UPDATE 语义天然幂等（同值重跑无副作用），连跑两次 exit 0。
-- 可回滚：执行前备份（见文末）。
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 0) 变更前快照（只查不改）
-- ---------------------------------------------------------------------------
SELECT '变更前：report_review 提示词长度与关键字段' AS stage,
       scene_code,
       LENGTH(IFNULL(user_prompt_template, '')) AS tpl_len,
       max_tokens,
       enabled
  FROM ai_scene_config
 WHERE scene_code = 'voice_interview:report_review';

-- ---------------------------------------------------------------------------
-- 1) 升级 user_prompt_template（批次 2 口径）
-- ---------------------------------------------------------------------------
UPDATE ai_scene_config
   SET user_prompt_template = '【任务】你是一位资深技术面试官，面试已结束。请基于下方候选人资料与整场对话记录，输出结构化复盘报告 JSON。\n【字段要求】\n1. overallComment：3-5 句整场总评，结合岗位要求评价整体表现，指出最突出的特点。\n2. levelEstimate：候选人水平定级，**只能是 junior / mid / senior 之一**（依据：回答深度、技术准确度、项目复杂度）。\n3. jobMatch：{rate: 0-100 整数匹配度, reason: 1-2 句依据（对照岗位要求与实际作答）}。\n4. highlights：2-4 条真实亮点数组，每条 {title: 短标题≤12字, detail: 引用作答中的具体内容说明}。\n5. weakPoints：2-4 条薄弱点数组，每条 {title: 短标题≤12字, detail: 具体不足与影响，禁止复述问题原文}。\n6. suggestions：3-5 条可执行改进建议字符串数组，结合简历与岗位，每条不超过 60 字。\n7. perQuestion：逐题分数修正，格式 [{questionIdx: 题号, score: 0-100 整数}]，**仅回填你认为需要修正的题目**，无需逐题全列（逐题点评文本由系统复用逐题分析结果，你不需要重复产出）。\n8. dimensions：{relevance 切题度, professionalism 专业深度, fluency 表达流畅, interactivity 互动质量, confidence 自信度, logic 逻辑结构}，0-100 整数。\n9. predictedQuestions：追问预测，**最多 6 条**（已问 + 未问合计），每条 {question: 预测问题, briefAnswer: 要点式简答≤80字, analysis: 为什么会被问≤60字, knowledgePoint: 考点标签2-6字, askedThisRound: true或false, askedScore: 本场得分（askedThisRound 为 true 时填，否则 null）}。\n   predictedQuestions 硬约束：① 只能基于简历摘要中的**真实内容**提问，禁止推断未提及的经历；② 禁止编造项目名、公司名或技术栈；③ 是否已问**必须以对话记录为准**；④ **未被问到的问题优先覆盖**「简历写了但本场未深挖的条目」与「岗位要求中简历未体现的差距项」。\n只输出 JSON 对象，不要输出任何其他文本。\n\n【候选人资料与对话记录】\n目标岗位：{{position}}\n岗位要求：{{jd}}\n候选人简历摘要：\n{{resumeDigest}}\n整场对话记录（含每题初评分，供参考）：\n{{qaList}}',
       description = 'task 拆行：整场复盘 + 追问预测（一次调用双产出，V1.2 §3.2 裁决不单开——两触点输入同源，单开=输入token翻倍）（v13.46 批次 1 自代码 enhanceReportByAgent 迁移；v13.47 批次 2 扩 levelEstimate 结构化 + predictedQuestions 上限6 + perQuestion 瘦身为「仅回填需修正的题」；字段规范进 user_prompt_template——system_prompt_template 已废弃，人设由 Agent 表承载）'
 WHERE scene_code = 'voice_interview:report_review';

-- ---------------------------------------------------------------------------
-- 复核 SQL（期望：tpl_len 明显变长、含 predictedQuestions、enabled=1）
-- ---------------------------------------------------------------------------
SELECT '复核：report_review 升级结果' AS stage,
       scene_code,
       LENGTH(IFNULL(user_prompt_template, '')) AS tpl_len,
       CASE WHEN user_prompt_template LIKE '%predictedQuestions%' THEN '含预测' ELSE '缺预测' END AS has_predicted,
       CASE WHEN user_prompt_template LIKE '%levelEstimate%' THEN '含定级' ELSE '缺定级' END AS has_level,
       enabled
  FROM ai_scene_config
 WHERE scene_code = 'voice_interview:report_review';

-- ---------------------------------------------------------------------------
-- 备份建议（执行前）：
--   CREATE TABLE IF NOT EXISTS ai_scene_config_bak_v1347 AS
--     SELECT id, scene_code, user_prompt_template, description FROM ai_scene_config;
-- 回滚：从 ai_scene_config_bak_v1347 按 id 还原 user_prompt_template / description
-- ---------------------------------------------------------------------------
