-- ============================================================
-- AI 面试全链路动态配置化（2026-09-07）
-- 依据：《AI面试动态配置化与场景配置中心实施计划.md》阶段 A1
-- 内容：
--   1. 新表 ×3：ai_scene_config / portal_job_template / portal_interview_config
--   2. ALTER ×4：简历解析置信度 / 题目绑定岗位模板 / 面试6阶段状态机 / QA表LLM评分
--   3. 菜单：5450 场景配置（AI基础配置）+ 5455 岗位模板 + 5460 面试配置（面试管理）
--   4. 种子数据：voice_interview 场景 + 默认面试配置 + Java/前端岗位模板示例
-- 幂等性：新表 IF NOT EXISTS；菜单/种子 DELETE+INSERT 可重复执行；
--         ALTER 语句仅首次执行（重复执行会报 Duplicate column，忽略即可）
-- ============================================================

-- ============================================================
-- 一、新表 ×3
-- ============================================================

-- 1.1 AI 场景配置表（AI 模块规范：AiBaseEntity 三件套 create_time/update_time/deleted）
CREATE TABLE IF NOT EXISTS `moyun-db`.ai_scene_config (
    id                     bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    scene_code             varchar(50)  NOT NULL COMMENT '场景代码：voice_interview/resume_optimize/question_generate',
    scene_name             varchar(100) NOT NULL COMMENT '场景名称',
    description            varchar(500) DEFAULT NULL COMMENT '场景描述',
    agent_id               bigint       DEFAULT NULL COMMENT '绑定的智能体（ai_agent.id，启用状态才生效）',
    model_config_id        bigint       DEFAULT NULL COMMENT '直接绑定模型（ai_model_config.id，agent_id 为空时生效）',
    knowledge_library_ids  varchar(500) DEFAULT NULL COMMENT '知识库ID列表 JSON数组',
    tool_ids               varchar(500) DEFAULT NULL COMMENT '工具ID列表 JSON数组',
    workflow_id            bigint       DEFAULT NULL COMMENT '绑定工作流（ai_workflow.id）',
    config_json            text         COMMENT '场景策略配置 JSON（如 dynamicMode）',
    version                varchar(20)  NOT NULL DEFAULT 'v1' COMMENT '版本号',
    weight                 int          NOT NULL DEFAULT 100 COMMENT '灰度权重（同场景多版本按权重轮盘赌）',
    priority               int          NOT NULL DEFAULT 0 COMMENT '优先级（全 0 时取 is_default，再按 priority DESC）',
    is_default             tinyint(1)   NOT NULL DEFAULT 0 COMMENT '是否默认版本',
    enabled                tinyint(1)   NOT NULL DEFAULT 1 COMMENT '是否启用',
    create_time            datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time            datetime     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted                tinyint(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_scene_version (scene_code, version),
    KEY idx_scene_code (scene_code),
    KEY idx_agent_id (agent_id),
    KEY idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI场景配置表（业务场景与Agent/模型/知识库/工作流动态绑定）';

-- 1.2 岗位模板表（portal 规范：BaseEntity 五件套 + del_flag）
CREATE TABLE IF NOT EXISTS `moyun-db`.portal_job_template (
    id             bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name           varchar(100) NOT NULL COMMENT '模板名称（如：Java后端工程师）',
    category       varchar(50)  DEFAULT NULL COMMENT '岗位类别（技术/产品/运营/设计等）',
    position_code  varchar(50)  DEFAULT NULL COMMENT '岗位编码（对齐 portal_voice_interview.position）',
    description    varchar(500) DEFAULT NULL COMMENT '模板描述',
    jd_text        text         COMMENT '岗位 JD 原文（用于 LLM 关键词提取与出题上下文）',
    keywords       varchar(500) DEFAULT NULL COMMENT '岗位关键词，逗号分隔（LLM 提取 + 人工维护）',
    difficulty     varchar(20)  NOT NULL DEFAULT 'medium' COMMENT '难度:easy,medium,hard（对齐 portal_interview_question.difficulty）',
    question_count int          NOT NULL DEFAULT 5 COMMENT '默认出题数量',
    weights        varchar(100) DEFAULT '{"job":40,"resume":30,"weak":20,"random":10}' COMMENT '出题权重 JSON（job岗位核心/resume简历深挖/weak薄弱点/random随机兜底）',
    status         varchar(20)  NOT NULL DEFAULT 'active' COMMENT '状态:active 启用/inactive 停用',
    create_by      varchar(64)  DEFAULT '' COMMENT '创建者',
    create_time    datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by      varchar(64)  DEFAULT '' COMMENT '更新者',
    update_time    datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    remark         varchar(500) DEFAULT NULL COMMENT '备注',
    del_flag       char(1)      NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
    PRIMARY KEY (id),
    KEY idx_category (category),
    KEY idx_status (status),
    KEY idx_del_flag (del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位模板表（JD/关键词/出题权重，支撑智能出题）';

-- 1.3 面试配置表（portal 规范：BaseEntity 五件套 + del_flag）
CREATE TABLE IF NOT EXISTS `moyun-db`.portal_interview_config (
    id                 bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
    config_name        varchar(100)  NOT NULL COMMENT '配置名称（如：标准技术面）',
    persona_type       varchar(20)   NOT NULL DEFAULT 'professional' COMMENT '面试官人设:professional/friendly/strict（对齐 voice_interview_style）',
    prompt_template    text          COMMENT '面试官提示词模板（支持 {{position}}/{{resumeDigest}} 等占位符，空则用 Agent 人设）',
    scoring_weights    varchar(500)  DEFAULT '{"selfIntro":{"structure":30,"awareness":25,"matching":25,"fluency":20},"llmRatio":70,"total":{"intro":20,"tech":80}}' COMMENT '评分权重 JSON（5技术维度+LLM融合比例+自我介绍4维度）',
    question_weights   varchar(100)  DEFAULT '{"job":40,"resume":30,"weak":20,"random":10}' COMMENT '出题权重 JSON',
    max_followups      int           NOT NULL DEFAULT 2 COMMENT '每题最大追问次数',
    followup_triggers  varchar(200)  DEFAULT '["vague_answer","contradiction","depth_needed"]' COMMENT '追问触发条件 JSON（模糊回答/前后矛盾/需深挖）',
    enable_self_intro  tinyint(1)    NOT NULL DEFAULT 0 COMMENT '是否启用自我介绍环节（0=旧流程兼容默认）',
    self_intro_duration int          NOT NULL DEFAULT 180 COMMENT '自我介绍建议时长（秒）',
    is_default         tinyint(1)    NOT NULL DEFAULT 0 COMMENT '是否默认配置（互斥，全局唯一）',
    status             varchar(20)   NOT NULL DEFAULT 'active' COMMENT '状态:active 启用/inactive 停用',
    create_by          varchar(64)   DEFAULT '' COMMENT '创建者',
    create_time        datetime      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by          varchar(64)   DEFAULT '' COMMENT '更新者',
    update_time        datetime      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    remark            varchar(500)  DEFAULT NULL COMMENT '备注',
    del_flag           char(1)       NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
    PRIMARY KEY (id),
    KEY idx_is_default (is_default),
    KEY idx_status (status),
    KEY idx_del_flag (del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试配置表（人设/提示词/评分权重/追问策略/自我介绍）';

-- ============================================================
-- 二、ALTER ×4（仅首次执行，重复执行报 Duplicate column 可忽略）
-- ============================================================

-- 2.1 简历表：解析置信度（LLM 结构化=85 / 规则兜底=60）
ALTER TABLE `moyun-db`.portal_user_resume
    ADD COLUMN parse_confidence tinyint DEFAULT NULL COMMENT '解析置信度（0-100：LLM 结构化=85，规则兜底=60，NULL=未解析）' AFTER score_detail;

-- 2.2 题库表：题目绑定岗位模板
ALTER TABLE `moyun-db`.portal_interview_question
    ADD COLUMN job_template_id bigint DEFAULT NULL COMMENT '所属岗位模板ID（portal_job_template.id）' AFTER category_id,
    ADD KEY idx_job_template_id (job_template_id);

-- 2.3 面试主表：6阶段状态机 + 自我介绍评分
ALTER TABLE `moyun-db`.portal_voice_interview
    ADD COLUMN phase varchar(30) DEFAULT NULL COMMENT '当前阶段:INTRO_WAITING/INTRO_RECEIVED/INTRO_FOLLOWUP/TECH_QUESTION/PROJECT_DEEP/SYSTEM_DESIGN/CANDIDATE_ASK/FINISHED（NULL=旧流程）' AFTER agent_id,
    ADD COLUMN intro_score_json text COMMENT '自我介绍评分 JSON（4维度+总分+评语，ScoringEngine 产出）' AFTER phase;

-- 2.4 问答表：LLM 结构化评分
ALTER TABLE `moyun-db`.portal_voice_interview_qa
    ADD COLUMN llm_score_json text COMMENT 'LLM 结构化评分 JSON（scores/total/strengths/weaknesses/comment）' AFTER rule_dimensions_json;

-- ============================================================
-- 三、菜单（幂等 DELETE+INSERT，5450-5464）
-- ============================================================

DELETE FROM `moyun-db`.sys_menu WHERE menu_id BETWEEN 5450 AND 5464;

-- 3.1 场景配置（挂在 AI基础配置 5238 下）
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5450, '场景配置', 5238, 10, 'scene', 'ai/scene/index', null, '', 1, 0, 'C', '0', '0', 'cms:ai:scene:list', 'component', 'admin', NOW(), '', null, 'AI场景配置中心：业务场景与Agent/模型/知识库/工作流动态绑定（版本+灰度权重）', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5451, '场景查询', 5450, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:scene:query', '#', 'admin', NOW(), '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5452, '场景新增', 5450, 2, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:scene:create', '#', 'admin', NOW(), '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5453, '场景修改', 5450, 3, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:scene:update', '#', 'admin', NOW(), '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5454, '场景删除', 5450, 4, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:scene:remove', '#', 'admin', NOW(), '', null, '', '0');

-- 3.2 岗位模板（挂在 门户管理5241 → 面试管理5192 下）
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5455, '岗位模板', 5192, 5, 'jobTemplate', 'cms/interview/jobTemplate/index', null, '', 1, 0, 'C', '0', '0', 'cms:interview:jobTemplate:list', 'dict', 'admin', NOW(), '', null, '岗位模板管理：JD/关键词（LLM提取）/出题权重/关联题目', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5456, '岗位模板查询', 5455, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:interview:jobTemplate:query', '#', 'admin', NOW(), '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5457, '岗位模板新增', 5455, 2, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:interview:jobTemplate:create', '#', 'admin', NOW(), '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5458, '岗位模板修改', 5455, 3, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:interview:jobTemplate:update', '#', 'admin', NOW(), '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5459, '岗位模板删除', 5455, 4, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:interview:jobTemplate:remove', '#', 'admin', NOW(), '', null, '', '0');

-- 3.3 面试配置（挂在 门户管理5241 → 面试管理5192 下）
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5460, '面试配置', 5192, 6, 'interviewConfig', 'cms/interview/interviewConfig/index', null, '', 1, 0, 'C', '0', '0', 'cms:interview:config:list', 'edit', 'admin', NOW(), '', null, '面试配置管理：人设/提示词模板/评分权重/追问策略/自我介绍环节', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5461, '面试配置查询', 5460, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:interview:config:query', '#', 'admin', NOW(), '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5462, '面试配置新增', 5460, 2, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:interview:config:create', '#', 'admin', NOW(), '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5463, '面试配置修改', 5460, 3, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:interview:config:update', '#', 'admin', NOW(), '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5464, '面试配置删除', 5460, 4, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:interview:config:remove', '#', 'admin', NOW(), '', null, '', '0');

-- 3.4 管理员角色授权（role_id=1）
DELETE FROM `moyun-db`.sys_role_menu WHERE role_id = 1 AND menu_id BETWEEN 5450 AND 5464;
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id, create_by, create_time, update_by, update_time, remark)
SELECT 1, m.menu_id, 'admin', NOW(), '', null, null
FROM `moyun-db`.sys_menu m
WHERE m.menu_id BETWEEN 5450 AND 5464;

-- ============================================================
-- 四、种子数据（幂等 DELETE+INSERT）
-- ============================================================

-- 4.1 voice_interview 场景：绑定 sys_config 现有默认 agent，上线当天行为零变化
DELETE FROM `moyun-db`.ai_scene_config WHERE scene_code = 'voice_interview';
INSERT INTO `moyun-db`.ai_scene_config
    (scene_code, scene_name, description, agent_id, model_config_id, knowledge_library_ids, tool_ids, workflow_id,
     config_json, version, weight, priority, is_default, enabled, create_time, update_time, deleted)
SELECT 'voice_interview', '语音面试', 'AI 语音模拟面试场景：智能出题 + 6阶段流程 + 权重评分 + 报告增强',
       (SELECT CAST(config_value AS UNSIGNED) FROM (SELECT config_value FROM `moyun-db`.sys_config WHERE config_key = 'voice.interview.defaultAgentId' AND config_value IS NOT NULL LIMIT 1) t),
       NULL, NULL, NULL, NULL,
       NULL, 'v1', 100, 0, 1, 1, NOW(), NOW(), 0
FROM DUAL;

-- 4.2 默认面试配置（enable_self_intro=0 保持旧行为，后台可开启 6 阶段流程）
DELETE FROM `moyun-db`.portal_interview_config WHERE is_default = 1 AND config_name = '默认面试配置';
INSERT INTO `moyun-db`.portal_interview_config
    (config_name, persona_type, prompt_template, scoring_weights, question_weights,
     max_followups, followup_triggers, enable_self_intro, self_intro_duration, is_default, status,
     create_by, create_time, update_by, update_time, remark, del_flag)
VALUES ('默认面试配置', 'professional', NULL,
        '{"selfIntro":{"structure":30,"awareness":25,"matching":25,"fluency":20},"llmRatio":70,"total":{"intro":20,"tech":80}}',
        '{"job":40,"resume":30,"weak":20,"random":10}',
        2, '["vague_answer","contradiction","depth_needed"]', 0, 180, 1, 'active',
        'admin', NOW(), '', null, '系统默认：llmRatio=LLM融合比例(0-100)；selfIntro=自我介绍4维权重；total=总分权重(intro/tech)；关闭自我介绍以兼容旧流程', '0');

-- 4.3 岗位模板示例 ×2
DELETE FROM `moyun-db`.portal_job_template WHERE name IN ('Java后端工程师（示例）', '前端工程师（示例）');
INSERT INTO `moyun-db`.portal_job_template
    (name, category, position_code, description, jd_text, keywords, difficulty, question_count, weights, status,
     create_by, create_time, update_by, update_time, remark, del_flag)
VALUES ('Java后端工程师（示例）', '技术', 'Java后端', 'Java 后端岗位面试模板示例',
        '岗位职责：负责核心业务系统的设计与开发，参与需求评审、技术方案设计；任职要求：精通 Java 及 JVM 原理，熟悉 Spring Boot / MyBatis 生态，掌握 MySQL 索引优化与事务隔离级别，熟悉 Redis 缓存方案与消息队列（RocketMQ/Kafka），了解分布式系统设计（CAP/分布式锁/幂等），具备高并发场景调优经验。',
        'Java,JVM,Spring Boot,MySQL,Redis,消息队列,分布式,高并发', 'medium', 5,
        '{"job":40,"resume":30,"weak":20,"random":10}', 'active',
        'admin', NOW(), '', null, '示例模板：可在后台编辑 JD 后点击「LLM 提取关键词」重新生成', '0'),
       ('前端工程师（示例）', '技术', '前端', '前端岗位面试模板示例',
        '岗位职责：负责 Web 前端功能开发与性能优化；任职要求：熟练掌握 JavaScript/TypeScript、Vue3 组合式 API 与响应式原理，理解浏览器渲染流程与事件循环，掌握 Webpack/Vite 构建优化，熟悉 HTTP 协议与前端安全（XSS/CSRF），有小程序/跨端经验者优先。',
        'JavaScript,TypeScript,Vue3,浏览器渲染,事件循环,Webpack,HTTP,XSS', 'medium', 5,
        '{"job":40,"resume":30,"weak":20,"random":10}', 'active',
        'admin', NOW(), '', null, '示例模板：可在后台编辑 JD 后点击「LLM 提取关键词」重新生成', '0');

-- ============================================================
-- 五、校验
-- ============================================================
SELECT id, scene_code, scene_name, agent_id, version, weight, is_default, enabled FROM `moyun-db`.ai_scene_config WHERE deleted = 0;
SELECT id, config_name, persona_type, enable_self_intro, is_default, status FROM `moyun-db`.portal_interview_config WHERE del_flag = '0';
SELECT id, name, category, difficulty, keywords, status FROM `moyun-db`.portal_job_template WHERE del_flag = '0';
SELECT menu_id, menu_name, parent_id, menu_type, perms FROM `moyun-db`.sys_menu WHERE menu_id BETWEEN 5450 AND 5464 ORDER BY menu_id;
SELECT COUNT(*) AS admin_granted FROM `moyun-db`.sys_role_menu WHERE role_id = 1 AND menu_id BETWEEN 5450 AND 5464;

-- ============================================================
-- 六、v11.30.5 面试报告分享（增量，可单独执行）
-- ============================================================
-- 报告本体已持久化于 portal_voice_interview.report JSON 列（完整），此处仅补分享三字段
ALTER TABLE `moyun-db`.`portal_voice_interview`
    ADD COLUMN `share_token`       VARCHAR(64) DEFAULT NULL COMMENT '报告分享令牌（NULL=未分享）' AFTER `report`,
    ADD COLUMN `share_expire_time` DATETIME    DEFAULT NULL COMMENT '分享过期时间' AFTER `share_token`,
    ADD COLUMN `share_count`       INT         NOT NULL DEFAULT 0 COMMENT '分享访问次数' AFTER `share_expire_time`,
    ADD UNIQUE KEY `uk_share_token` (`share_token`);