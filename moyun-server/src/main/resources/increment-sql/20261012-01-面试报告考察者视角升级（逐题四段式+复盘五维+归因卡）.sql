-- =============================================================================
-- 面试报告「考察者视角」升级（2026-10-12 报告评审整改）
-- -----------------------------------------------------------------------------
-- 背景：id=2 实测复盘发现报告除对话回放外各 tab 内容空洞——薄弱点/建议出现
--   「针对薄弱点『你好，欢迎参加今天的面试…』做专项复习」类胡给文案；
--   整场复盘曾静默失败（output_tokens=0 误记 success）且兜底文案模板化。
--   本次从「问题列表」升级为「考察者对被面试人的客观评价与行动建议」。
--
-- 变更（3 个场景配置行）：
--   ① voice_interview:answer_analysis——逐题四段式 intent/framework/gap/advice
--     （考察意图/参考框架/差距分析/针对性补法）+ max_tokens 2048→2560；
--   ② voice_interview:report_review——考察者视角 verdict（结论判词）/profile（候选人画像）/
--     competencies（五维能力剖面）/weakPoints 归因卡（type|evidence|impact|remedy）/
--     suggestionViews（动作+验收口径+优先级）+【话术铁律】6 条 + retry 0→3、max_tokens 2560→4600；
--   ③ voice_interview:industry_insight——retry_count 1→3（与复盘同因：单次抖动不应终局失败）；
--   ④ report_review 输出防护放宽 max_output_tokens 2000→4096（12 组字段输出体量）。
--
-- 配套（代码侧同批，不在本脚本范围）：网关空输出判失败、寒暄轮隔离、
--   聚合兜底去毒、VO 扩展、前端概要/逐题/待提升三区展示升级。
--
-- 幂等：UPDATE 语义天然幂等（同值重跑无副作用）。
-- 回滚：执行前按「变更前快照」备份 user_prompt_template/max_tokens/retry_count，
--   反向 UPDATE 即可恢复。
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 0) 变更前快照（只查不改；回滚依据）
-- ---------------------------------------------------------------------------
SELECT '变更前：三个场景行当前状态' AS stage,
       scene_code,
       LENGTH(IFNULL(user_prompt_template, '')) AS tpl_len,
       max_tokens,
       retry_count,
       max_output_tokens
  FROM ai_scene_config
 WHERE scene_code IN ('voice_interview:answer_analysis',
                      'voice_interview:report_review',
                      'voice_interview:industry_insight')
   AND deleted = 0;

-- ---------------------------------------------------------------------------
-- 1) answer_analysis：逐题四段式 + max_tokens 2560
-- ---------------------------------------------------------------------------
UPDATE ai_scene_config
   SET user_prompt_template = '{{context}}\n候选人的语音转写回答见用户消息（可能口语化、有转写噪音）。\n请以严格的技术面试官标准分析该回答，只输出如下 JSON（不要任何其他文字）：\n{\n  "score": 0-100的整数,\n  "dimensions": {"relevance": 0-100, "professionalism": 0-100, "fluency": 0-100, "interactivity": 0-100, "confidence": 0-100, "logic": 0-100},\n维度定义：relevance=回答与问题的相关性；professionalism=技术深度与专业度；fluency=表达流畅度；interactivity=互动性（举例/对比/坦诚沟通）；confidence=自信笃定程度；logic=逻辑条理与结构。\n  "feedback": "两到三句中文点评，先肯定再指出问题",\n  "intent": "考察意图：本题在验证候选人的什么能力（面试官视角一句话，≤40字，如「验证高并发场景下的缓存一致性设计能力」）",\n  "framework": ["参考要点框架：一份好回答应覆盖的要点，3-5条，每条一句话，只写客观知识点不针对候选人"],\n  "gap": "差距分析：候选人回答对照参考框架缺了什么/错了什么，必须引用其原话作证据，1-2句",\n  "advice": "针对性补法：动词开头的具体动作，≤50字（如「重读XX源码并手写其执行流程图」），禁止「加强学习」这类空话",\n  "flaws": ["回答中暴露的具体漏洞或模糊点，每条一句话，最多3条，没有则空数组"],\n  "level": "junior或mid或senior，对候选人当前真实水平的判断",\n  "followupWorth": true或false，该回答是否存在值得追问的漏洞,\n  "followupQuestion": "若followupWorth为true，给出一句针对漏洞的追问；必须引用候选人回答中的具体表述",\n  "guidance": "若回答明显跑偏，给出一句引导性提示，否则为空字符串"\n}\n打分参考：完全跑题<30；浅层正确但无细节50-65；有正确框架和部分细节65-80；深入准确有取舍权衡80+。\n四段式硬约束：intent/framework/gap/advice 每题必填；gap 禁止复述问题原文，必须引用候选人原话作证据；advice 动词开头且可验证；评表现不评人格。\n\n{{data:候选人语音转写回答|transcript}}',
       description = 'task 拆行：候选人回答深度分析（原 VoiceInterviewHandler.answer_analysis 提示词逐字收编；2026-10-12 报告评审升级：逐题四段式 intent/framework/gap/advice，考察者视角）',
       max_tokens = 2560
 WHERE scene_code = 'voice_interview:answer_analysis' AND deleted = 0;

-- ---------------------------------------------------------------------------
-- 2) report_review：考察者视角五层报告 + retry 3 + max_tokens 4600
-- ---------------------------------------------------------------------------
UPDATE ai_scene_config
   SET user_prompt_template = '【任务】你是一位资深技术面试官，面试已结束。请基于下方候选人资料与整场对话记录，以**考察者视角**输出结构化复盘报告 JSON——读者是候选人本人，目标是让他清楚知道自己当前水平、差距在哪、下一步怎么补。\n【字段要求】\n1. verdict：结论判词，一句话定性整体水平与岗位差距方向，≤60字（如「具备中级后端的工程功底，系统设计与权衡表达是冲击高级的主要短板」）。\n2. profile：候选人画像段，2-4句：技术背景/项目复杂度，对照岗位要求定位（结合简历摘要与自我介绍轮）。\n3. overallComment：3-5句整场总评，结合岗位要求评价整体表现，指出最突出的特点，引用至少一处作答证据。\n4. levelEstimate：候选人水平定级，**只能是 junior / mid / senior 之一**（依据：回答深度、技术准确度、项目复杂度）。\n5. jobMatch：{rate: 0-100整数匹配度, reason: 1-2句依据（对照岗位要求与实际作答）}。\n6. competencies：五维能力剖面，固定5条 {key, label, score, comment, evidenceIdx}：key/label 依次为 depth=技术深度 / engineering=工程实践 / design=系统设计 / communication=沟通表达 / match=岗位匹配；score 为 0-100 整数；comment 2-3句评价，**必须锚定题号或候选人原话**；evidenceIdx 为证据题号数组；对话未直接覆盖的维度，comment 必须注明「本场未直接考察，以下为间接推断」，score 谨慎给，禁止臆造具体表现。\n7. highlights：2-4条真实亮点 {title: ≤12字, detail: 引用作答原话或题号的具体说明}。\n8. weakPoints：2-4条薄弱点**归因卡** {title: ≤12字, detail: 具体不足, type: knowledge(知识缺口)/expression(表达问题)/depth(深度不足)之一, evidence: 哪几题+原话摘要, impact: 该短板在真实面试/工作中会被如何解读, remedy: 动词开头的具体补法≤60字}。禁止把问题原文当薄弱点。\n9. suggestionViews：3-5条结构化行动建议 {action: 动词开头≤50字（如「手写缓存穿透/击穿/雪崩对策对比表」）, acceptance: 验收口径≤40字（能做到什么算过关）, priority: 1-3（1最高）}，按对求职影响排序。\n10. perQuestion：逐题分数修正 [{questionIdx: 题号, score: 0-100整数}]，**仅回填需修正的题**（逐题点评文本由系统复用逐题分析结果，你不需要重复产出）。\n11. dimensions：{relevance 切题度, professionalism 专业深度, fluency 表达流畅, interactivity 互动质量, confidence 自信度, logic 逻辑结构}，0-100整数。\n12. predictedQuestions：追问预测，**最多6条**（已问+未问合计），每条 {question, briefAnswer: 要点式简答≤80字, analysis: 为什么会被问≤60字, knowledgePoint: 考点标签2-6字, askedThisRound: true或false, askedScore: 本场得分（askedThisRound 为 true 时填，否则 null）}。\n   predictedQuestions 硬约束：① 只能基于简历摘要中的**真实内容**提问，禁止推断未提及的经历；② 禁止编造项目名、公司名或技术栈；③ 是否已问**必须以对话记录为准**；④ **未被问到的问题优先覆盖**「简历写了但本场未深挖的条目」与「岗位要求中简历未体现的差距项」。\n【话术铁律】① 评价必须锚定证据：引用题号或候选人原话，禁止无出处的定性（「基础薄弱」这类空话禁止）；② 用对比句式：岗位要求X→作答显示Y→差距是Z；③ 评表现不评人格：说「回答未涉及X」，不说「候选人能力差」；④ 区分「未考察」与「不足」：没问到的方向不得当弱点写；⑤ 标注【自我介绍轮】的轮次仅用于画像与表达评估，禁止作为技术能力证据引用；⑥ 无候选人实质回答的轮次零引用。\n只输出 JSON 对象，不要输出任何其他文本。\n\n【候选人资料与对话记录】\n目标岗位：{{position}}\n岗位要求：{{jd}}\n候选人简历摘要：\n{{resumeDigest}}\n整场对话记录（含每题初评分，供参考）：\n{{qaList}}',
       description = 'task 拆行：整场复盘 + 追问预测（一次调用双产出，V1.2 §3.2 裁决不单开——两触点输入同源，单开=输入token翻倍）（v13.46 批次 1 自代码 enhanceReportByAgent 迁移；v13.47 批次 2 扩 levelEstimate 结构化 + predictedQuestions 上限6 + perQuestion 瘦身；2026-10-12 报告评审升级：考察者视角 verdict/profile/competencies/薄弱点归因卡/结构化行动建议 + 话术铁律，retry 0→3、max_tokens 2560→4600）',
       retry_count = 3,
       max_tokens = 4600
 WHERE scene_code = 'voice_interview:report_review' AND deleted = 0;

-- ---------------------------------------------------------------------------
-- 3) industry_insight：retry 1→3
-- ---------------------------------------------------------------------------
UPDATE ai_scene_config
   SET retry_count = 3
 WHERE scene_code = 'voice_interview:industry_insight' AND deleted = 0;

-- ---------------------------------------------------------------------------
-- 4) report_review 输出防护放宽（成本控制列，与 max_tokens 生成上限不同列）
-- ---------------------------------------------------------------------------
UPDATE ai_scene_config
   SET max_output_tokens = 4096
 WHERE scene_code = 'voice_interview:report_review' AND deleted = 0;

-- ---------------------------------------------------------------------------
-- 复核（期望：answer_analysis tpl 含「四段式硬约束」、max_tokens=2560；
--       report_review tpl 含「话术铁律」、retry=3、max_tokens=4600、max_output_tokens=4096；
--       industry_insight retry=3）
-- ---------------------------------------------------------------------------
SELECT '复核：升级结果' AS stage,
       scene_code,
       LENGTH(IFNULL(user_prompt_template, '')) AS tpl_len,
       user_prompt_template LIKE '%四段式硬约束%' AS has_fourpart,
       user_prompt_template LIKE '%话术铁律%' AS has_rules,
       max_tokens,
       retry_count,
       max_output_tokens
  FROM ai_scene_config
 WHERE scene_code IN ('voice_interview:answer_analysis',
                      'voice_interview:report_review',
                      'voice_interview:industry_insight')
   AND deleted = 0;
