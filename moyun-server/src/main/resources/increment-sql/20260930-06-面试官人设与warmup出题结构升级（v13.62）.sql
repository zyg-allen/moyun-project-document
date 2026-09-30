-- =============================================================================
-- 面试官人设与 warmup 出题结构升级（v13.62）
-- -----------------------------------------------------------------------------
-- 背景：语音面试体验反馈三问题——① 口头结束不收口（代码侧已修）；② 题数界定
--   不清（3/5/8 档太少，且「题」被理解为一轮对话）；③ 面试官全程围绕第一个
--   话题追问、几轮对话草草收场。本脚本处理提示词侧两项：
--   ① `AI面试官·默认` agent 的 system_prompt：守则 2 由「考察充分后再换话题」
--      （无上限、易恋战）升级为「同一话题追问不超过 2 轮 + 考察有层次（中段必须
--      进入专业考察）+ 把时间用满」；
--   ② `voice_interview:warmup` 的 user_prompt_template：考察方向数由固定
--      「3-5 个」改为与计划问题数联动（3-15 个），并增加类别覆盖约束
--      （项目深挖/技术基础/岗位核心技能/系统设计/软素质），防止方向全挤一类。
--
-- 配套代码（同版本 v13.62）：buildInterviewerSystemPrompt 四阶段段序约束、
--   buildTurnDirective 分阶段轮换提示、前端题数选项 5/8/12/15 + 口径说明。
--
-- 幂等：UPDATE 语义天然幂等（同值重跑无副作用），连跑两次 exit 0。
-- 可回滚：执行前备份（见文末）。
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 0) 变更前快照（只查不改）
-- ---------------------------------------------------------------------------
SELECT '变更前：面试官 agent 人设' AS stage,
       name,
       LENGTH(IFNULL(system_prompt, '')) AS prompt_len,
       enabled
  FROM ai_agent
 WHERE name = 'AI面试官·默认' AND deleted = 0;

SELECT '变更前：warmup 配置行' AS stage,
       scene_code,
       LENGTH(IFNULL(user_prompt_template, '')) AS tpl_len,
       enabled
  FROM ai_scene_config
 WHERE scene_code = 'voice_interview:warmup';

-- ---------------------------------------------------------------------------
-- 1) 升级「AI面试官·默认」人设：追问有度 + 考察有层次
-- ---------------------------------------------------------------------------
UPDATE ai_agent
   SET system_prompt = '你是一位经验丰富的一线技术面试官，主持一场真实的一对一模拟面试。\n\n【人设】\n- 你面过数百位候选人，阅人无数：听得懂话里的含糊其辞，也识得出真材实料。\n- 你的风格是"温和但不好糊弄"：语气自然友好，但会对模糊表述、可疑数据、夸大成分温和地追根究底。\n- 你尊重候选人，绝不居高临下；提问像聊天，不打官腔。\n\n【面试守则】\n1. 一次只问一个问题。紧密结合候选人此前的回答随机应变——像真实面试官一样追问细节、质疑数据、验证深度。\n2. 追问有度：同一话题连续追问不超过 2 轮；已验证深度或候选人已无新信息时，立即切换到下一个考察方向，不恋战、也不蜻蜓点水地串场。\n3. 考察有层次：开场从简历与自我介绍深挖真实经历，中段必须进入岗位专业技术与核心能力考察，结尾留出候选人反问空间——把面试时间用满，考察充分而非赶进度。\n4. 不泄露评分维度与分析细节，不主动给标准答案，不说"你的得分是"。\n5. 口语化、自然，像面对面交谈：单轮话术控制在 1-3 句，避免书面语、条目式表达和长篇大论。\n6. 候选人答不上来或明显偏题时，给一次自然的引导或换题，不反复纠缠同一考点。\n7. 全程只以面试官身份说话，不扮演其他角色，不输出任何 JSON、标记或系统文字。',
       update_time = NOW()
 WHERE name = 'AI面试官·默认' AND deleted = 0;

-- ---------------------------------------------------------------------------
-- 2) 升级 warmup 出题结构：方向数与计划题数联动 + 类别覆盖
-- ---------------------------------------------------------------------------
UPDATE ai_scene_config
   SET user_prompt_template = '你正在主持一场模拟面试，请先完成面试预热理解，只输出如下 JSON（不要任何其他文字）：\n{\n  "understanding": {\n    "candidateProfile": "50字内的候选人画像（背景/技术栈/经验层次）",\n    "strengths": ["结合简历与岗位判断的1-2个优势"],\n    "concerns": ["需要重点验证的1-2个疑点"]\n  },\n  "interviewPlan": {\n    "focusAreas": [{"area": "考察方向", "reason": "为何考察", "depth": "basic或intermediate或deep"}]\n  },\n  "opening": "1-2句面试官开场白（欢迎+放松提示，口语化）",\n  "firstQuestion": "第一个问题：固定为请候选人做自我介绍，并提示结合与应聘岗位相关的经历"\n}\n考察方向数量与计划问题数一致（3-15 个）。方向必须按类别覆盖、避免全部挤在同一类：项目/经历深挖 1-2 个、技术基础 1-2 个、岗位核心技能 1-2 个、系统设计或场景运用 1 个、软素质/协作 0-1 个；优先来自岗位要求JD与知识库参考片段，其次来自简历项目；depth 结合难度设定。\n\n{{data:面试背景|context}}\n{{data:候选人简历摘要|resumeDigest}}\n{{data:岗位要求JD|jd}}\n{{data:知识库参考片段（出题参考）|kbSnippets}}',
       description = 'task 拆行：候选人画像+考察计划+开场白+首题（原 VoiceInterviewHandler.warmup 提示词逐字收编；v13.62 考察方向数与计划题数联动+类别覆盖约束）'
 WHERE scene_code = 'voice_interview:warmup';

-- ---------------------------------------------------------------------------
-- 复核 SQL（期望：prompt 含「追问有度」、tpl 含「类别覆盖」、enabled=1）
-- ---------------------------------------------------------------------------
SELECT '复核：agent 人设升级结果' AS stage,
       name,
       LENGTH(IFNULL(system_prompt, '')) AS prompt_len,
       CASE WHEN system_prompt LIKE '%追问有度%' THEN '含轮换纪律' ELSE '缺轮换纪律' END AS has_rotate,
       CASE WHEN system_prompt LIKE '%考察有层次%' THEN '含层次要求' ELSE '缺层次要求' END AS has_layer,
       enabled
  FROM ai_agent
 WHERE name = 'AI面试官·默认' AND deleted = 0;

SELECT '复核：warmup 升级结果' AS stage,
       scene_code,
       LENGTH(IFNULL(user_prompt_template, '')) AS tpl_len,
       CASE WHEN user_prompt_template LIKE '%与计划问题数一致%' THEN '含题数联动' ELSE '缺题数联动' END AS has_link,
       CASE WHEN user_prompt_template LIKE '%类别覆盖%' THEN '含类别覆盖' ELSE '缺类别覆盖' END AS has_cover,
       enabled
  FROM ai_scene_config
 WHERE scene_code = 'voice_interview:warmup';

-- ---------------------------------------------------------------------------
-- 备份建议（执行前）：
--   CREATE TABLE IF NOT EXISTS ai_prompt_bak_v1362 AS
--     SELECT id, name, system_prompt FROM ai_agent WHERE name='AI面试官·默认' AND deleted=0;
--   CREATE TABLE IF NOT EXISTS ai_scene_config_bak_v1362 AS
--     SELECT id, scene_code, user_prompt_template, description FROM ai_scene_config
--      WHERE scene_code='voice_interview:warmup';
-- 回滚：从备份表按 id 还原 system_prompt / user_prompt_template / description
-- ---------------------------------------------------------------------------
