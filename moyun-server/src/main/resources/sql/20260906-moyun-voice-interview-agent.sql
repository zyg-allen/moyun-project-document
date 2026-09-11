-- ============================================================
-- 语音面试 Agent 化深度改造（2026-09-06）
-- 1. portal_voice_interview 绑定面试官智能体
-- 2. portal_voice_interview_qa 记录问题来源与 LLM 深度分析
-- 3. 预置默认 AI 面试官 agent（后台可编辑，动态生效）
-- 4. sys_config 开关：默认面试官 / 动态出题模式
-- ============================================================

-- 1. 面试主表：绑定智能体（NULL = 旧数据，走原提示词逻辑）
ALTER TABLE portal_voice_interview
    ADD COLUMN agent_id bigint NULL COMMENT '面试官智能体ID（ai_agent.id，NULL=未绑定走默认逻辑）' AFTER resume_id,
    ADD KEY idx_agent (agent_id);

-- 2. 问答表：问题来源 + LLM 深度分析 JSON（心态/流畅度/红旗/完整性）
ALTER TABLE portal_voice_interview_qa
    ADD COLUMN question_source varchar(20) NULL COMMENT '问题来源 bank=题库/resume_project=简历锚定/llm=智能体生成' AFTER question_id,
    ADD COLUMN llm_analysis_json text NULL COMMENT 'LLM深度分析JSON（sentiment/fluency/redFlags/completeness）' AFTER rule_dimensions_json;

-- 3. 预置默认 AI 面试官（model_config_id=15 为当前默认 chat 模型；systemPrompt 支持占位符，后台可改）
INSERT INTO ai_agent (name, description, system_prompt, model_config_id, temperature, max_tokens,
                      enabled, max_history_turns, welcome_message, create_time, update_time, deleted)
VALUES ('AI面试官·默认', '语音面试默认面试官人设（勿删）。systemPrompt 支持 {{position}}/{{scene}}/{{difficulty}}/{{style}}/{{resumeDigest}}/{{profileGaps}}/{{levelEstimate}} 占位符，运行时自动渲染。修改后无需重启即生效。',
        '你是一位经验丰富的面试官，正在进行一场真实的一对一模拟面试。\n\n【面试设定】\n- 岗位：{{position}}\n- 场景：{{scene}}（技术面/项目面/HR面/综合面）\n- 难度：{{difficulty}}\n- 风格：{{style}}\n\n【候选人背景】\n- 简历项目摘要：\n{{resumeDigest}}\n- 本场已识别薄弱点：{{profileGaps}}\n- 当前水平评估：{{levelEstimate}}\n\n【面试守则】\n1. 一次只问一个问题，紧密结合候选人此前的回答随机应变，像真实面试官一样追问细节、质疑数据。\n2. 回答有明显漏洞或可深挖的亮点时优先追问；话题考察充分后换新话题；主问题目考察完毕后自然收尾。\n3. 不泄露评分维度与分析细节，不主动给标准答案。\n4. 口语化、自然，像面对面交谈，避免书面语和条目式表达。',
        15, 0.7, 2048, 1, 20,
        '你好，欢迎参加{{position}}岗位的模拟面试。我是今天的面试官，放松心态，我们像聊天一样开始。准备好了的话，我们直接进入第一个问题。',
        NOW(), NOW(), 0);

-- 4. sys_config：默认面试官 AgentID + 动态出题开关（后台 参数设置 可直接改）
INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time)
VALUES ('语音面试默认面试官AgentID', 'voice.interview.defaultAgentId',
        (SELECT id FROM (SELECT id FROM ai_agent WHERE name = 'AI面试官·默认' AND deleted = 0 ORDER BY id DESC LIMIT 1) t),
        'Y', '语音面试绑定的 ai_agent 主键；编辑「AI模块→智能体管理」对应 agent 的人设/提示词/模型即动态生效', 'admin', NOW());

INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time)
VALUES ('语音面试动态出题开关', 'voice.interview.dynamicMode', 'true', 'Y',
        'true=智能体动态出题（结合上下文随机应变），false=沿用预生成题单（旧行为）', 'admin', NOW());

-- 5. 校验
SELECT id, name FROM ai_agent WHERE deleted = 0;
SELECT config_key, config_value FROM sys_config WHERE config_key LIKE 'voice.interview.%';
