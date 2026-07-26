-- ============================================
-- 墨韵智库 - 面试空间建表 DDL（最终合并版）
-- ============================================
-- 合并来源脚本：
--   07_reading_interview_init.sql                   （面试空间初始建表：portal_interview_question/portal_interview_category/portal_interview_submission/portal_interview_experience/portal_interview_resume_template）
--   20_resume_template_like.sql                     （portal_interview_resume_template_like 建表）
--   21_submission_featured.sql                      （portal_interview_submission 新增 is_featured/featured_time 字段及 idx_submission_featured 索引）
--   51_interview_ddl_fix.sql                        （portal_interview_company 建表；portal_interview_submission 新增 content/answer_type/is_success/note/create_by/update_by/update_time/remark 字段及 idx_user_question 索引；portal_interview_experience 新增 summary/cover_image/is_top 字段及 status 注释更新；portal_interview_resume_template 新增 file_type/file_size/is_premium/usage_guide/tags 字段）
--   52_user_resume.sql                              （portal_user_resume 建表）
--   69_mock_interview_init.sql                       （portal_mock_interview/portal_mock_interview_qa 建表）
-- 涉及表：
--   portal_interview_question, portal_interview_category, portal_interview_experience,
--   portal_interview_company, portal_interview_submission,
--   portal_interview_resume_template, portal_interview_resume_template_like,
--   portal_user_resume, portal_mock_interview, portal_mock_interview_qa
-- 说明：
--   - 本文件仅包含建表 DDL（CREATE TABLE IF NOT EXISTS），不含 INSERT 数据、菜单注入语句
--   - 所有 ALTER TABLE 已合并入对应的 CREATE TABLE
--   - 21 号脚本中 is_featured AFTER `note`，note 字段由 51 号脚本补齐，本文件按 51→21 顺序合并
--   - 67 号脚本中的 sys_menu 菜单注入、sys_role_menu 权限分配见 10_menus 目录
-- ============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 面试题目分类表
-- 来源：07 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_interview_category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name` VARCHAR(200) NOT NULL COMMENT '分类名称',
    `slug` VARCHAR(200) DEFAULT NULL COMMENT '分类标识',
    `description` TEXT COMMENT '分类描述',
    `icon` VARCHAR(500) DEFAULT NULL COMMENT '图标URL',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `question_count` INT DEFAULT 0 COMMENT '题目数量',
    `status` VARCHAR(20) DEFAULT 'active' COMMENT '状态:active,inactive',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试题目分类表';

-- ----------------------------
-- 面试题目表
-- 来源：07 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_interview_question` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title` VARCHAR(500) NOT NULL COMMENT '题目标题',
    `description` TEXT COMMENT '题目描述',
    `difficulty` VARCHAR(20) DEFAULT 'medium' COMMENT '难度:easy,medium,hard',
    `category_id` BIGINT DEFAULT NULL COMMENT '分类ID',
    `tags` VARCHAR(500) DEFAULT NULL COMMENT '标签，逗号分隔',
    `companies` VARCHAR(500) DEFAULT NULL COMMENT '公司，逗号分隔',
    `acceptance_rate` DECIMAL(5,2) DEFAULT 0.00 COMMENT '通过率',
    `submission_count` BIGINT DEFAULT 0 COMMENT '提交次数',
    `like_count` BIGINT DEFAULT 0 COMMENT '点赞数',
    `hint` TEXT COMMENT '提示',
    `solution` TEXT COMMENT '参考答案',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `status` VARCHAR(20) DEFAULT 'active' COMMENT '状态:active,inactive',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_difficulty` (`difficulty`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试题目表';

-- ----------------------------
-- 题目提交记录表
-- 来源：07 建表 + 51 扩展 + 21 扩展
-- 51 补齐：content/answer_type/is_success/note/create_by/update_by/update_time/remark
-- 21 扩展：is_featured/featured_time（is_featured AFTER `note`，note 由 51 补齐）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_interview_submission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `question_id` BIGINT NOT NULL COMMENT '题目ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `code` TEXT COMMENT '提交的代码',
    -- 51 扩展：文字答案、答案类型
    `content` TEXT COMMENT '提交的文字答案',
    `language` VARCHAR(50) DEFAULT 'java' COMMENT '编程语言',
    `answer_type` VARCHAR(20) DEFAULT 'code' COMMENT '答案类型：code/text/design',
    `status` VARCHAR(50) DEFAULT 'pending' COMMENT '状态:accepted,wrong_answer,time_limit,compile_error',
    `is_success` TINYINT(1) DEFAULT 0 COMMENT '是否通过',
    `runtime` INT DEFAULT NULL COMMENT '运行时间（毫秒）',
    `memory_usage` INT DEFAULT NULL COMMENT '内存使用（KB）',
    `note` TEXT COMMENT '备注/笔记',
    -- 21 扩展：精选笔记
    `is_featured` TINYINT(1) DEFAULT 0 COMMENT '是否被精选（后台采纳为优质笔记）',
    `featured_time` DATETIME DEFAULT NULL COMMENT '精选时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_question_id` (`question_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    -- 51 扩展索引：加速个人答题历史查询
    KEY `idx_user_question` (`user_id`, `question_id`),
    -- 21 扩展索引：精选笔记查询
    KEY `idx_submission_featured` (`is_featured`, `featured_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目提交记录表';

-- ----------------------------
-- 面经表
-- 来源：07 建表 + 51 扩展（summary/cover_image/is_top 字段；status 注释更新为审核状态机）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_interview_experience` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `title` VARCHAR(500) NOT NULL COMMENT '面经标题',
    `company` VARCHAR(200) NOT NULL COMMENT '公司',
    `position` VARCHAR(200) DEFAULT NULL COMMENT '岗位',
    `year` INT DEFAULT NULL COMMENT '年份',
    `month` INT DEFAULT NULL COMMENT '月份',
    -- 51 扩展：内容摘要
    `summary` VARCHAR(500) DEFAULT NULL COMMENT '内容摘要',
    `content` TEXT NOT NULL COMMENT '面经内容',
    -- 51 扩展：封面图
    `cover_image` VARCHAR(500) DEFAULT NULL COMMENT '封面图URL',
    `tags` VARCHAR(500) DEFAULT NULL COMMENT '标签',
    -- 51 扩展：置顶
    `is_top` TINYINT(1) DEFAULT 0 COMMENT '是否置顶',
    `view_count` BIGINT DEFAULT 0 COMMENT '浏览数',
    `like_count` BIGINT DEFAULT 0 COMMENT '点赞数',
    `comment_count` BIGINT DEFAULT 0 COMMENT '评论数',
    `status` VARCHAR(20) DEFAULT 'published' COMMENT '状态:draft,pending,published,rejected,archived',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_company` (`company`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面经表';

-- ----------------------------
-- 面试公司标签表
-- 来源：51 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_interview_company` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name` VARCHAR(200) NOT NULL COMMENT '公司名称',
    `slug` VARCHAR(200) DEFAULT NULL COMMENT '公司标识',
    `logo` VARCHAR(500) DEFAULT NULL COMMENT '公司Logo URL',
    `description` TEXT COMMENT '公司描述',
    `industry` VARCHAR(100) DEFAULT NULL COMMENT '所属行业',
    `question_count` INT DEFAULT 0 COMMENT '相关题目数',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `status` VARCHAR(20) DEFAULT 'active' COMMENT '状态:active,inactive',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_slug` (`slug`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试公司标签表';

-- ----------------------------
-- 简历模板表
-- 来源：07 建表 + 51 扩展（file_type/file_size/is_premium/usage_guide/tags 字段）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_interview_resume_template` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title` VARCHAR(500) NOT NULL COMMENT '模板标题',
    `description` TEXT COMMENT '模板描述',
    `cover` VARCHAR(500) DEFAULT NULL COMMENT '封面URL',
    `download_url` VARCHAR(500) DEFAULT NULL COMMENT '下载地址',
    `category` VARCHAR(200) DEFAULT NULL COMMENT '分类',
    -- 51 扩展：文件元信息、付费标记、使用指南、标签
    `file_type` VARCHAR(20) DEFAULT NULL COMMENT '文件类型：docx/pdf/psd',
    `file_size` BIGINT DEFAULT NULL COMMENT '文件大小（字节）',
    `is_premium` TINYINT(1) DEFAULT 0 COMMENT '是否付费模板',
    `usage_guide` TEXT COMMENT '使用指南',
    `tags` VARCHAR(500) DEFAULT NULL COMMENT '标签，逗号分隔',
    `like_count` BIGINT DEFAULT 0 COMMENT '点赞数',
    `download_count` BIGINT DEFAULT 0 COMMENT '下载次数',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `status` VARCHAR(20) DEFAULT 'active' COMMENT '状态:active,inactive',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历模板表';

-- ----------------------------
-- 简历模板点赞表
-- 来源：20 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_interview_resume_template_like` (
    `id`          BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `template_id` BIGINT NOT NULL COMMENT '简历模板ID',
    `user_id`     BIGINT NOT NULL COMMENT '用户ID',
    `create_by`   VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    `update_by`   VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`      VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_user` (`template_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历模板点赞表';

-- ----------------------------
-- 用户简历表
-- 来源：52 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_user_resume` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`         BIGINT       NOT NULL                COMMENT '用户ID',
    `title`           VARCHAR(100) NOT NULL DEFAULT '我的简历' COMMENT '简历名称',
    `parent_id`       BIGINT       DEFAULT NULL            COMMENT '父简历ID（版本历史关联，首次创建为 NULL）',
    `version_no`      INT          NOT NULL DEFAULT 1      COMMENT '版本号',
    -- 基本信息
    `name`            VARCHAR(50)  DEFAULT NULL            COMMENT '姓名',
    `gender`          VARCHAR(10)  DEFAULT NULL            COMMENT '性别：男/女',
    `birth_date`      DATE         DEFAULT NULL            COMMENT '出生日期',
    `phone`           VARCHAR(20)  DEFAULT NULL            COMMENT '联系电话',
    `email`           VARCHAR(100) DEFAULT NULL            COMMENT '邮箱',
    `avatar`          VARCHAR(255) DEFAULT NULL            COMMENT '头像URL',
    -- 结构化内容（JSON 字符串）
    `job_intention`   TEXT         COMMENT '求职意向（JSON：期望职位/城市/薪资/类型）',
    `educations`      TEXT         COMMENT '教育经历（JSON 数组：学校/专业/学历/时间/描述）',
    `works`           TEXT         COMMENT '工作经历（JSON 数组：公司/职位/时间/描述）',
    `projects`        TEXT         COMMENT '项目经历（JSON 数组：名称/角色/时间/描述/链接）',
    `skills`          TEXT         COMMENT '技能列表（JSON 数组：名称/等级/分类）',
    `self_intro`      TEXT         COMMENT '自我介绍',
    -- 评分
    `score`           INT          DEFAULT NULL            COMMENT '评分（0-100）',
    `score_detail`    TEXT         COMMENT '评分明细（JSON 数组）',
    `scored_time`     DATETIME     DEFAULT NULL            COMMENT '评分时间',
    -- 导出
    `file_url`        VARCHAR(255) DEFAULT NULL            COMMENT 'PDF 导出文件URL',
    `export_time`     DATETIME     DEFAULT NULL            COMMENT '最后导出时间',
    -- 状态
    `status`          VARCHAR(20)  NOT NULL DEFAULT 'draft' COMMENT '状态：draft/published/archived',
    -- 公共字段
    `create_by`       VARCHAR(64)  DEFAULT ''              COMMENT '创建者',
    `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`       VARCHAR(64)  DEFAULT ''              COMMENT '更新者',
    `update_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`          VARCHAR(500) DEFAULT NULL            COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_parent_id` (`parent_id`),
    -- 复合索引：(parent_id, version_no) 用于按根简历查询版本历史；同时作为唯一约束防止并发复制产生重复版本号。
    -- 注意：parent_id 可为 NULL（根简历），MySQL 中 NULL 不参与唯一性约束，根简历的 versionNo=1 需应用层保证。
    UNIQUE KEY `uk_parent_version` (`parent_id`, `version_no`),
    KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户简历';

-- ----------------------------
-- 模拟面试会话表
-- 来源：69 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_mock_interview` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT       NOT NULL                COMMENT '面试用户ID',
    `position`   VARCHAR(64)  DEFAULT NULL            COMMENT '面试岗位（如 后端开发/前端开发）',
    `scene`      VARCHAR(64)  DEFAULT NULL            COMMENT '面试场景（如 算法/系统设计/项目深挖，对应题目分类）',
    `status`     VARCHAR(16)  NOT NULL DEFAULT 'in_progress' COMMENT '状态 in_progress/finished',
    `total_qa`   INT          NOT NULL DEFAULT 0      COMMENT '题目总数',
    `score`      INT          DEFAULT NULL            COMMENT '面试总分（0-100，结束面试时计算）',
    `summary`    TEXT                                  COMMENT 'AI 生成的面试总结',
    `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_time` (`user_id`, `create_time`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模拟面试会话';

-- ----------------------------
-- 模拟面试问答表
-- 来源：69 建表
-- 注：question_id 为关联 portal_interview_question.id 的冗余外键，
-- 用于 AI 评分时回查题目 tags/solution 作为评分参考要点
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_mock_interview_qa` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `interview_id`  BIGINT       NOT NULL                COMMENT '面试会话ID',
    `question_id`   BIGINT       DEFAULT NULL             COMMENT '关联题目ID（portal_interview_question.id）',
    `question_idx`  INT          NOT NULL                 COMMENT '题目序号（从 0 开始）',
    `question`      VARCHAR(1000) NOT NULL                COMMENT '面试问题（快照自题目标题）',
    `user_answer`   TEXT                                  COMMENT '用户回答',
    `ai_feedback`   TEXT                                  COMMENT 'AI 反馈（规则化生成）',
    `score`         INT          DEFAULT NULL             COMMENT '本题评分（0-100）',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_interview` (`interview_id`),
    KEY `idx_question_idx` (`interview_id`, `question_idx`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模拟面试问答';

-- ============================================
-- 补齐遗漏的 7 张面试空间表
-- ============================================
-- 遗漏原因说明：
--   这 7 张表在前一轮建表 DDL 合并时被遗漏。原因是它们的审计字段
--   （create_by/create_time/update_by/update_time/remark）并非通过建表
--   语句直接添加，而是由 28_alter_tables_add_base_fields.sql 通过
--   AddColumnIfNotExists 存储过程以 ALTER TABLE 方式幂等补充的
--   （29_alter_all_tables_base_fields.sql 仅覆盖 28 号未涉及的 25 张表，
--   与本节 7 张表无重叠，故无需合并）。
--   原始建表来源分散在两个脚本：
--     · 07_reading_interview_init.sql：
--         portal_interview_bookmark / portal_interview_question_like
--         portal_interview_attempt / portal_interview_experience_like
--     · 51_interview_ddl_fix.sql：
--         portal_interview_comment / portal_interview_comment_like
--         portal_interview_question_company
--   其中 portal_interview_comment 在 51 号建表时已包含全部 5 个审计字段，
--   28 号脚本的存储过程对其执行的是幂等兜底（已存在则跳过）。
--   本节按"建表 + 28 号审计字段补充"的最终状态合并，避免重复字段。
-- ============================================

-- ----------------------------
-- 题目收藏表
-- 来源：07 建表 + 28 补审计字段（create_by/update_by/update_time/remark）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_interview_bookmark` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `question_id` BIGINT NOT NULL COMMENT '题目ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `note` TEXT COMMENT '笔记',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_question_user` (`question_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目收藏表';

-- ----------------------------
-- 题目点赞表
-- 来源：07 建表 + 28 补审计字段（create_by/update_by/update_time/remark）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_interview_question_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `question_id` BIGINT NOT NULL COMMENT '题目ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_question_user` (`question_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目点赞表';

-- ----------------------------
-- 做题记录表
-- 来源：07 建表 + 28 补全部 5 个审计字段（create_by/create_time/update_by/update_time/remark）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_interview_attempt` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `question_id` BIGINT NOT NULL COMMENT '题目ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `attempt_count` INT DEFAULT 1 COMMENT '尝试次数',
    `last_attempt_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '最后尝试时间',
    `status` VARCHAR(30) DEFAULT 'attempted' COMMENT '状态:not_attempted,attempted,solved',
    `first_solved_at` DATETIME DEFAULT NULL COMMENT '首次解决时间',
    `last_solved_at` DATETIME DEFAULT NULL COMMENT '最后解决时间',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_question_user` (`question_id`, `user_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='做题记录表';

-- ----------------------------
-- 面经点赞表
-- 来源：07 建表 + 28 补审计字段（create_by/update_by/update_time/remark）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_interview_experience_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `experience_id` BIGINT NOT NULL COMMENT '面经ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_experience_user` (`experience_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面经点赞表';

-- ----------------------------
-- 面经评论表
-- 来源：51 建表（已含全部 5 个审计字段）+ 28 幂等兜底
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_interview_comment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `experience_id` BIGINT NOT NULL COMMENT '面经ID',
    `user_id` BIGINT NOT NULL COMMENT '评论用户ID',
    `parent_id` BIGINT DEFAULT NULL COMMENT '父评论ID（支持两级回复）',
    `reply_to_user_id` BIGINT DEFAULT NULL COMMENT '回复目标用户ID',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `like_count` BIGINT DEFAULT 0 COMMENT '点赞数',
    `status` VARCHAR(20) DEFAULT 'published' COMMENT '状态:pending,published,rejected',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_experience_id` (`experience_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面经评论表';

-- ----------------------------
-- 面经评论点赞表
-- 来源：51 建表 + 28 补审计字段（create_by/update_by/update_time/remark）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_interview_comment_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `comment_id` BIGINT NOT NULL COMMENT '评论ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_comment_user` (`comment_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面经评论点赞表';

-- ----------------------------
-- 题目-公司关联表（多对多）
-- 来源：51 建表 + 28 补审计字段（create_by/update_by/update_time/remark）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_interview_question_company` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `question_id` BIGINT NOT NULL COMMENT '题目ID',
    `company_id` BIGINT NOT NULL COMMENT '公司ID',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_question_company` (`question_id`, `company_id`),
    KEY `idx_company_id` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目-公司关联表';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================
-- 面试空间建表 DDL 合并完成
-- ============================================
