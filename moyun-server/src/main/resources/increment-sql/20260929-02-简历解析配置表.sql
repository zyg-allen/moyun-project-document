-- =============================================================================
-- 简历解析配置表 portal_resume_parse_config（结构 + 种子）
-- -----------------------------------------------------------------------------
-- 背景（v13.38 简历解析重构）：
--   规则解析引擎 ResumeRuleParser 需要四类可维护词表，全部支持后台
--   「面试管理 → 简历解析配置」页维护：
--     · section  章节标题 → 大类映射（教育/工作/项目/技能/自评…）—— 决定「大类划分」
--     · skill    技能词域（补充岗位必备技能之外的通识技能）
--     · degree   学历词
--     · position 岗位词
--
--   ⚠️ 技能词域无需铺满：引擎会**自动聚合** portal_job_template.required_skills
--      作为主词域，本表只补充通识技能。
--   ⚠️ 表为空时引擎使用内置默认词典兜底 —— 配置问题绝不导致解析失败。
--
-- 铁律 10：本脚本与 init-sql（moyun-db-ddl.sql 建表 + moyun-db-dml-init.sql 种子）
--          **内容等价**，二者需同步修改。
--
-- 幂等性：CREATE TABLE IF NOT EXISTS + INSERT ... WHERE NOT EXISTS，
--         可重复执行（连跑两次 exit 0）。
-- =============================================================================

CREATE TABLE IF NOT EXISTS `portal_resume_parse_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_type` varchar(20) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置类型：section 章节词 / skill 技能词 / degree 学历词 / position 岗位词',
  `item_key` varchar(50) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置键：section 时为目标大类 edu/work/project/skill/self/intention/basic/other；其余为词条本身',
  `item_name` varchar(100) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '显示名称（后台列表展示，如「教育背景」）',
  `keywords` varchar(1000) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '关键词（多个用英文逗号分隔；section 用于匹配章节标题，其余用于全文匹配）',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序（升序）',
  `status` varchar(20) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'active' COMMENT '状态：active 启用 / inactive 停用',
  `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  KEY `idx_type_status` (`config_type`,`status`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='简历解析配置（章节词典/技能词域/学历词/岗位词，规则解析用）';

-- ---------------------------------------------------------------------------
-- 种子数据（幂等：按 config_type + item_key 判重）
-- ---------------------------------------------------------------------------

-- ① 章节标题词典（section）—— 决定「大类划分」
INSERT INTO `portal_resume_parse_config` (`config_type`,`item_key`,`item_name`,`keywords`,`sort`,`status`,`create_by`,`remark`)
SELECT 'section','basic','基本信息','基本信息,个人信息,个人资料,基本资料',1,'active','admin','简历顶部基础信息区'
WHERE NOT EXISTS (SELECT 1 FROM `portal_resume_parse_config` WHERE `config_type`='section' AND `item_key`='basic');

INSERT INTO `portal_resume_parse_config` (`config_type`,`item_key`,`item_name`,`keywords`,`sort`,`status`,`create_by`,`remark`)
SELECT 'section','intention','求职意向','求职意向,求职目标,职业意向,期望职位,期望岗位,目标岗位',2,'active','admin','求职意向区（抽取 position/city）'
WHERE NOT EXISTS (SELECT 1 FROM `portal_resume_parse_config` WHERE `config_type`='section' AND `item_key`='intention');

INSERT INTO `portal_resume_parse_config` (`config_type`,`item_key`,`item_name`,`keywords`,`sort`,`status`,`create_by`,`remark`)
SELECT 'section','edu','教育背景','教育背景,教育经历,学习经历,教育信息,学历信息,教育与培训',3,'active','admin','教育经历区（条目：学校/专业/学历/起止时间）'
WHERE NOT EXISTS (SELECT 1 FROM `portal_resume_parse_config` WHERE `config_type`='section' AND `item_key`='edu');

INSERT INTO `portal_resume_parse_config` (`config_type`,`item_key`,`item_name`,`keywords`,`sort`,`status`,`create_by`,`remark`)
SELECT 'section','work','工作经历','工作经历,工作经验,职业经历,实习经历,工作履历,职业背景',4,'active','admin','工作经历区（条目：公司/职位/起止时间）'
WHERE NOT EXISTS (SELECT 1 FROM `portal_resume_parse_config` WHERE `config_type`='section' AND `item_key`='work');

INSERT INTO `portal_resume_parse_config` (`config_type`,`item_key`,`item_name`,`keywords`,`sort`,`status`,`create_by`,`remark`)
SELECT 'section','project','项目经历','项目经历,项目经验,项目实践,项目业绩,主要项目',5,'active','admin','项目经历区（条目：项目名/角色/起止时间）'
WHERE NOT EXISTS (SELECT 1 FROM `portal_resume_parse_config` WHERE `config_type`='section' AND `item_key`='project');

INSERT INTO `portal_resume_parse_config` (`config_type`,`item_key`,`item_name`,`keywords`,`sort`,`status`,`create_by`,`remark`)
SELECT 'section','skill','专业技能','专业技能,技能特长,技能清单,掌握技能,技能专长,IT技能,计算机技能',6,'active','admin','技能区（词域匹配，见 config_type=skill）'
WHERE NOT EXISTS (SELECT 1 FROM `portal_resume_parse_config` WHERE `config_type`='section' AND `item_key`='skill');

INSERT INTO `portal_resume_parse_config` (`config_type`,`item_key`,`item_name`,`keywords`,`sort`,`status`,`create_by`,`remark`)
SELECT 'section','self','自我评价','自我评价,个人评价,自我介绍,个人简介,自我描述,个人优势',7,'active','admin','自评区（整块取，几乎不会错）'
WHERE NOT EXISTS (SELECT 1 FROM `portal_resume_parse_config` WHERE `config_type`='section' AND `item_key`='self');

INSERT INTO `portal_resume_parse_config` (`config_type`,`item_key`,`item_name`,`keywords`,`sort`,`status`,`create_by`,`remark`)
SELECT 'section','other','其他区块','荣誉奖项,获奖情况,证书,资格证书,校园经历,校内职务,培训经历,语言能力,兴趣爱好',8,'active','admin','不解析字段，但原文保留在大类块中（内容永不丢失）'
WHERE NOT EXISTS (SELECT 1 FROM `portal_resume_parse_config` WHERE `config_type`='section' AND `item_key`='other');

-- ② 技能词域（skill）—— 仅补充通识技能；岗位必备技能由引擎自动聚合 portal_job_template.required_skills
INSERT INTO `portal_resume_parse_config` (`config_type`,`item_key`,`item_name`,`keywords`,`sort`,`status`,`create_by`,`remark`)
SELECT 'skill','通用技能','通用技能','Git,SVN,Maven,Gradle,Linux,Shell,Nginx,Tomcat,JUnit,Postman,Swagger,Figma,Axure,Visio,Office,Excel,PPT,数据分析,需求分析,项目管理,敏捷开发,Scrum,单元测试,性能优化,系统设计,微服务,分布式,高并发,负载均衡,消息队列,缓存,容器化,持续集成',1,'active','admin','通识技能（与岗位必备技能并集使用）'
WHERE NOT EXISTS (SELECT 1 FROM `portal_resume_parse_config` WHERE `config_type`='skill' AND `item_key`='通用技能');

-- ③ 学历词（degree）
INSERT INTO `portal_resume_parse_config` (`config_type`,`item_key`,`item_name`,`keywords`,`sort`,`status`,`create_by`,`remark`)
SELECT 'degree','学历层级','学历层级','博士,博士研究生,硕士,硕士研究生,研究生,MBA,本科,学士,大学本科,大专,专科,高职,中专,高中',1,'active','admin','按顺序优先匹配，长词优先'
WHERE NOT EXISTS (SELECT 1 FROM `portal_resume_parse_config` WHERE `config_type`='degree' AND `item_key`='学历层级');

-- ---------------------------------------------------------------------------
-- 复核 SQL
-- ---------------------------------------------------------------------------
-- SELECT config_type, COUNT(*) FROM portal_resume_parse_config WHERE del_flag='0' GROUP BY config_type;
-- SELECT item_key, item_name, keywords FROM portal_resume_parse_config WHERE config_type='section' ORDER BY sort;
