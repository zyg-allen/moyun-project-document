-- =====================================================
-- 墨韵·智库 - 面试空间模块 DDL 脚本 v2.0
-- 生成时间: 2026-06-15
-- 编码: utf8mb4
-- 引擎: InnoDB
-- 说明: 面试空间 13 张核心表 + 通用标签表 portal_entity_tag
--       以及 portal_tag 表扩展字段的 ALTER 语句
-- =====================================================

-- =====================================================
-- 1. 面试分类表 portal_interview_category
-- =====================================================
DROP TABLE IF EXISTS `portal_interview_category`;
CREATE TABLE `portal_interview_category` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`            VARCHAR(255)    NOT NULL                COMMENT '分类名称',
    `slug`            VARCHAR(255)    DEFAULT NULL            COMMENT '分类标识 / URL slug',
    `description`     TEXT            DEFAULT NULL            COMMENT '分类描述',
    `icon`            VARCHAR(500)    DEFAULT NULL            COMMENT '分类图标',
    `sort`            INT             DEFAULT 0               COMMENT '排序',
    `question_count`  INT             DEFAULT 0               COMMENT '题目数量',
    `status`          VARCHAR(32)     DEFAULT '0'             COMMENT '状态（0 正常 1 停用）',
    `create_by`       VARCHAR(64)     DEFAULT ''              COMMENT '创建者',
    `create_time`     DATETIME        DEFAULT NULL            COMMENT '创建时间',
    `update_by`       VARCHAR(64)     DEFAULT ''              COMMENT '更新者',
    `update_time`     DATETIME        DEFAULT NULL            COMMENT '更新时间',
    `remark`          VARCHAR(500)    DEFAULT NULL            COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_category_status` (`status`),
    KEY `idx_category_sort` (`sort`),
    KEY `idx_category_slug` (`slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试分类表';


-- =====================================================
-- 2. 面试题目表 portal_interview_question
-- =====================================================
DROP TABLE IF EXISTS `portal_interview_question`;
CREATE TABLE `portal_interview_question` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title`            VARCHAR(255)    NOT NULL                COMMENT '题目标题',
    `description`      TEXT            DEFAULT NULL            COMMENT '题目描述',
    `difficulty`       VARCHAR(32)     DEFAULT NULL            COMMENT '难度（easy/medium/hard）',
    `category_id`      BIGINT UNSIGNED DEFAULT 0               COMMENT '所属分类 ID',
    `tags`             VARCHAR(500)    DEFAULT NULL            COMMENT '标签字符串（逗号分隔）',
    `companies`        VARCHAR(500)    DEFAULT NULL            COMMENT '来源公司（逗号分隔）',
    `acceptance_rate`  DECIMAL(10,2)   DEFAULT 0.00            COMMENT '通过率 / 接受率',
    `submission_count` BIGINT UNSIGNED DEFAULT 0               COMMENT '提交次数',
    `like_count`       BIGINT UNSIGNED DEFAULT 0               COMMENT '点赞数',
    `hint`             TEXT            DEFAULT NULL            COMMENT '提示',
    `solution`         TEXT            DEFAULT NULL            COMMENT '参考答案 / 解析',
    `sort`             INT             DEFAULT 0               COMMENT '排序',
    `status`           VARCHAR(32)     DEFAULT '0'             COMMENT '状态（0 正常 1 停用）',
    `create_by`        VARCHAR(64)     DEFAULT ''              COMMENT '创建者',
    `create_time`      DATETIME        DEFAULT NULL            COMMENT '创建时间',
    `update_by`        VARCHAR(64)     DEFAULT ''              COMMENT '更新者',
    `update_time`      DATETIME        DEFAULT NULL            COMMENT '更新时间',
    `remark`           VARCHAR(500)    DEFAULT NULL            COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_question_category` (`category_id`),
    KEY `idx_question_difficulty` (`difficulty`),
    KEY `idx_question_status` (`status`),
    KEY `idx_question_sort` (`sort`),
    KEY `idx_question_title` (`title`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试题目表';


-- =====================================================
-- 3. 面试提交记录表 portal_interview_submission
-- =====================================================
DROP TABLE IF EXISTS `portal_interview_submission`;
CREATE TABLE `portal_interview_submission` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `question_id`   BIGINT UNSIGNED DEFAULT 0               COMMENT '题目 ID',
    `user_id`       BIGINT UNSIGNED DEFAULT 0               COMMENT '用户 ID',
    `code`          TEXT            DEFAULT NULL            COMMENT '提交的代码',
    `content`       TEXT            DEFAULT NULL            COMMENT '提交的文字内容/回答',
    `language`      VARCHAR(64)     DEFAULT NULL            COMMENT '编程语言/回答类型',
    `answer_type`   VARCHAR(32)     DEFAULT NULL            COMMENT '回答类型（code/text/choice）',
    `status`        VARCHAR(32)     DEFAULT NULL            COMMENT '提交状态',
    `is_success`    TINYINT(1)      DEFAULT 0               COMMENT '是否成功/通过',
    `runtime`       INT             DEFAULT 0               COMMENT '运行耗时 (ms)',
    `memory_usage`  INT             DEFAULT 0               COMMENT '内存使用 (KB)',
    `note`          TEXT            DEFAULT NULL            COMMENT '备注/用户笔记',
    `create_time`   DATETIME        DEFAULT NULL            COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_submission_question` (`question_id`),
    KEY `idx_submission_user` (`user_id`),
    KEY `idx_submission_status` (`status`),
    KEY `idx_submission_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试提交记录表';


-- =====================================================
-- 4. 面试题目书签表 portal_interview_bookmark
-- =====================================================
DROP TABLE IF EXISTS `portal_interview_bookmark`;
CREATE TABLE `portal_interview_bookmark` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `question_id` BIGINT UNSIGNED DEFAULT 0               COMMENT '题目 ID',
    `user_id`     BIGINT UNSIGNED DEFAULT 0               COMMENT '用户 ID',
    `note`        VARCHAR(500)    DEFAULT NULL            COMMENT '书签备注',
    `create_time` DATETIME        DEFAULT NULL            COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_bookmark_question` (`question_id`),
    KEY `idx_bookmark_user` (`user_id`),
    KEY `idx_bookmark_user_question` (`user_id`, `question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试题目书签表';


-- =====================================================
-- 5. 面试题目点赞表 portal_interview_question_like
-- =====================================================
DROP TABLE IF EXISTS `portal_interview_question_like`;
CREATE TABLE `portal_interview_question_like` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `question_id` BIGINT UNSIGNED DEFAULT 0               COMMENT '题目 ID',
    `user_id`     BIGINT UNSIGNED DEFAULT 0               COMMENT '用户 ID',
    `create_time` DATETIME        DEFAULT NULL            COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_ql_question` (`question_id`),
    KEY `idx_ql_user` (`user_id`),
    KEY `idx_ql_user_question` (`user_id`, `question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试题目点赞表';


-- =====================================================
-- 6. 面试尝试表 portal_interview_attempt
-- =====================================================
DROP TABLE IF EXISTS `portal_interview_attempt`;
CREATE TABLE `portal_interview_attempt` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `question_id`     BIGINT UNSIGNED DEFAULT 0               COMMENT '题目 ID',
    `user_id`         BIGINT UNSIGNED DEFAULT 0               COMMENT '用户 ID',
    `attempt_count`   INT             DEFAULT 0               COMMENT '尝试次数',
    `status`          VARCHAR(32)     DEFAULT NULL            COMMENT '当前状态',
    `last_attempt_at` DATETIME        DEFAULT NULL            COMMENT '最后尝试时间',
    `first_solved_at` DATETIME        DEFAULT NULL            COMMENT '首次解决时间',
    `last_solved_at`  DATETIME        DEFAULT NULL            COMMENT '最后解决时间',
    PRIMARY KEY (`id`),
    KEY `idx_attempt_question` (`question_id`),
    KEY `idx_attempt_user` (`user_id`),
    KEY `idx_attempt_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试尝试表';


-- =====================================================
-- 7. 面试经历/面经表 portal_interview_experience
-- =====================================================
DROP TABLE IF EXISTS `portal_interview_experience`;
CREATE TABLE `portal_interview_experience` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`       BIGINT UNSIGNED DEFAULT 0               COMMENT '作者用户 ID',
    `title`         VARCHAR(255)    NOT NULL                COMMENT '面经标题',
    `company`       VARCHAR(255)    DEFAULT NULL            COMMENT '公司名称',
    `position`      VARCHAR(255)    DEFAULT NULL            COMMENT '应聘岗位',
    `year`          INT             DEFAULT 0               COMMENT '面试年份',
    `month`         INT             DEFAULT 0               COMMENT '面试月份',
    `summary`       TEXT            DEFAULT NULL            COMMENT '摘要',
    `content`       TEXT            DEFAULT NULL            COMMENT '面经正文',
    `cover_image`   VARCHAR(500)    DEFAULT NULL            COMMENT '封面图',
    `tags`          VARCHAR(500)    DEFAULT NULL            COMMENT '标签（逗号分隔）',
    `is_top`        TINYINT(1)      DEFAULT 0               COMMENT '是否置顶',
    `view_count`    BIGINT UNSIGNED DEFAULT 0               COMMENT '浏览数',
    `like_count`    BIGINT UNSIGNED DEFAULT 0               COMMENT '点赞数',
    `comment_count` BIGINT UNSIGNED DEFAULT 0               COMMENT '评论数',
    `status`        VARCHAR(32)     DEFAULT '0'             COMMENT '状态（0 正常 1 停用）',
    `create_time`   DATETIME        DEFAULT NULL            COMMENT '创建时间',
    `update_time`   DATETIME        DEFAULT NULL            COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_experience_user` (`user_id`),
    KEY `idx_experience_company` (`company`),
    KEY `idx_experience_status` (`status`),
    KEY `idx_experience_year_month` (`year`, `month`),
    KEY `idx_experience_is_top` (`is_top`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试经历/面经表';


-- =====================================================
-- 8. 面经点赞表 portal_interview_experience_like
-- =====================================================
DROP TABLE IF EXISTS `portal_interview_experience_like`;
CREATE TABLE `portal_interview_experience_like` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `experience_id`  BIGINT UNSIGNED DEFAULT 0               COMMENT '面经 ID',
    `user_id`        BIGINT UNSIGNED DEFAULT 0               COMMENT '用户 ID',
    `create_time`    DATETIME        DEFAULT NULL            COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_el_experience` (`experience_id`),
    KEY `idx_el_user` (`user_id`),
    KEY `idx_el_user_experience` (`user_id`, `experience_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面经点赞表';


-- =====================================================
-- 9. 面试评论表 portal_interview_comment
-- =====================================================
DROP TABLE IF EXISTS `portal_interview_comment`;
CREATE TABLE `portal_interview_comment` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `experience_id`  BIGINT UNSIGNED DEFAULT 0               COMMENT '关联的面经 ID',
    `user_id`        BIGINT UNSIGNED DEFAULT 0               COMMENT '评论用户 ID',
    `parent_id`      BIGINT UNSIGNED DEFAULT 0               COMMENT '父评论 ID（0 为顶层）',
    `reply_to_user_id` BIGINT UNSIGNED DEFAULT 0             COMMENT '被回复用户 ID',
    `content`        TEXT            DEFAULT NULL            COMMENT '评论内容',
    `like_count`     BIGINT UNSIGNED DEFAULT 0               COMMENT '点赞数',
    `sort`           INT             DEFAULT 0               COMMENT '排序',
    `status`         VARCHAR(32)     DEFAULT '0'             COMMENT '状态（0 正常 1 停用）',
    `create_time`    DATETIME        DEFAULT NULL            COMMENT '创建时间',
    `update_time`    DATETIME        DEFAULT NULL            COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_comment_experience` (`experience_id`),
    KEY `idx_comment_user` (`user_id`),
    KEY `idx_comment_parent` (`parent_id`),
    KEY `idx_comment_status` (`status`),
    KEY `idx_comment_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试评论表';


-- =====================================================
-- 10. 面试评论点赞表 portal_interview_comment_like
-- =====================================================
DROP TABLE IF EXISTS `portal_interview_comment_like`;
CREATE TABLE `portal_interview_comment_like` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `comment_id`  BIGINT UNSIGNED DEFAULT 0               COMMENT '评论 ID',
    `user_id`     BIGINT UNSIGNED DEFAULT 0               COMMENT '用户 ID',
    `create_time` DATETIME        DEFAULT NULL            COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_cl_comment` (`comment_id`),
    KEY `idx_cl_user` (`user_id`),
    KEY `idx_cl_user_comment` (`user_id`, `comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试评论点赞表';


-- =====================================================
-- 11. 面试简历模板表 portal_interview_resume_template
-- =====================================================
DROP TABLE IF EXISTS `portal_interview_resume_template`;
CREATE TABLE `portal_interview_resume_template` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title`          VARCHAR(255)    NOT NULL                COMMENT '模板标题',
    `description`    TEXT            DEFAULT NULL            COMMENT '模板描述',
    `cover`          VARCHAR(500)    DEFAULT NULL            COMMENT '封面图',
    `download_url`   VARCHAR(500)    DEFAULT NULL            COMMENT '下载地址',
    `category`       VARCHAR(255)    DEFAULT NULL            COMMENT '分类（IT/金融/设计...）',
    `file_type`      VARCHAR(32)     DEFAULT NULL            COMMENT '文件类型（doc/docx/pdf）',
    `file_size`      BIGINT UNSIGNED DEFAULT 0               COMMENT '文件大小 (字节)',
    `is_premium`     TINYINT(1)      DEFAULT 0               COMMENT '是否高级/付费模板',
    `usage_guide`    TEXT            DEFAULT NULL            COMMENT '使用指南',
    `like_count`     BIGINT UNSIGNED DEFAULT 0               COMMENT '点赞数',
    `download_count` BIGINT UNSIGNED DEFAULT 0               COMMENT '下载数',
    `sort`           INT             DEFAULT 0               COMMENT '排序',
    `status`         VARCHAR(32)     DEFAULT '0'             COMMENT '状态（0 正常 1 停用）',
    `create_time`    DATETIME        DEFAULT NULL            COMMENT '创建时间',
    `update_time`    DATETIME        DEFAULT NULL            COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_resume_category` (`category`),
    KEY `idx_resume_status` (`status`),
    KEY `idx_resume_sort` (`sort`),
    KEY `idx_resume_is_premium` (`is_premium`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试简历模板表';


-- =====================================================
-- 12. 面试公司表 portal_interview_company
-- =====================================================
DROP TABLE IF EXISTS `portal_interview_company`;
CREATE TABLE `portal_interview_company` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`           VARCHAR(255)    NOT NULL                COMMENT '公司名称',
    `slug`           VARCHAR(255)    DEFAULT NULL            COMMENT '公司标识 / URL slug',
    `logo`           VARCHAR(500)    DEFAULT NULL            COMMENT '公司 Logo',
    `description`    TEXT            DEFAULT NULL            COMMENT '公司描述',
    `industry`       VARCHAR(255)    DEFAULT NULL            COMMENT '所属行业',
    `question_count` INT             DEFAULT 0               COMMENT '关联题目数量',
    `sort`           INT             DEFAULT 0               COMMENT '排序',
    `status`         VARCHAR(32)     DEFAULT '0'             COMMENT '状态（0 正常 1 停用）',
    `create_time`    DATETIME        DEFAULT NULL            COMMENT '创建时间',
    `update_time`    DATETIME        DEFAULT NULL            COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_company_industry` (`industry`),
    KEY `idx_company_status` (`status`),
    KEY `idx_company_slug` (`slug`),
    KEY `idx_company_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试公司表';


-- =====================================================
-- 13. 题目-公司关联表 portal_interview_question_company
-- =====================================================
DROP TABLE IF EXISTS `portal_interview_question_company`;
CREATE TABLE `portal_interview_question_company` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `question_id` BIGINT UNSIGNED DEFAULT 0               COMMENT '题目 ID',
    `company_id`  BIGINT UNSIGNED DEFAULT 0               COMMENT '公司 ID',
    `sort`        INT             DEFAULT 0               COMMENT '排序',
    `create_time` DATETIME        DEFAULT NULL            COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_qc_question` (`question_id`),
    KEY `idx_qc_company` (`company_id`),
    KEY `idx_qc_question_company` (`question_id`, `company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目-公司关联表';


-- =====================================================
-- 14. 通用实体标签关联表 portal_entity_tag
-- =====================================================
DROP TABLE IF EXISTS `portal_entity_tag`;
CREATE TABLE `portal_entity_tag` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tag_id`      BIGINT UNSIGNED NOT NULL                COMMENT '标签 ID（引用 portal_tag.id）',
    `entity_type` VARCHAR(32)     NOT NULL                COMMENT '实体类型（article/interview_question/experience/...）',
    `entity_id`   BIGINT UNSIGNED NOT NULL                COMMENT '实体 ID',
    `sort`        INT             DEFAULT 0               COMMENT '排序',
    `create_time` DATETIME        DEFAULT NULL            COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tag_entity` (`tag_id`, `entity_type`, `entity_id`),
    KEY `idx_entity` (`entity_type`, `entity_id`),
    KEY `idx_entity_create_time` (`entity_type`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通用实体标签关联表';


-- =====================================================
-- 15. 扩展 portal_tag 表（通用标签系统）
-- =====================================================
-- 注意：如果 portal_tag 表已经存在，请执行以下 ALTER 语句
--       否则请先创建 portal_tag 表（见项目已有 DDL）

ALTER TABLE `portal_tag`
    ADD COLUMN `module`          VARCHAR(32)     DEFAULT NULL COMMENT '所属模块（article/book/.../common 通用）' AFTER `status`,
    ADD COLUMN `reference_count` BIGINT UNSIGNED DEFAULT 0    COMMENT '被引用次数' AFTER `module`,
    ADD INDEX `idx_module` (`module`),
    ADD INDEX `idx_reference_count` (`reference_count` DESC);

-- =====================================================
-- DDL 脚本结束
-- =====================================================
