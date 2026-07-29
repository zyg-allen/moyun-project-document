-- 来源：all-db-ddl.sql 行2472-2871（已剔除 INSERT 种子数据，种子数据见 80 段）
-- 用途：门户面试学习表 DDL（portal_interview_* 全系列）

DROP TABLE IF EXISTS `portal_interview_attempt`;
CREATE TABLE `portal_interview_attempt` (
                                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                            `question_id` bigint NOT NULL COMMENT '题目ID',
                                            `user_id` bigint NOT NULL COMMENT '用户ID',
                                            `attempt_count` int DEFAULT '1' COMMENT '尝试次数',
                                            `last_attempt_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '最后尝试时间',
                                            `status` varchar(30) DEFAULT 'attempted' COMMENT '状态:not_attempted,attempted,solved',
                                            `first_solved_at` datetime DEFAULT NULL COMMENT '首次解决时间',
                                            `last_solved_at` datetime DEFAULT NULL COMMENT '最后解决时间',
                                            `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                            `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                            `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                            `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                            `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                            `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                            PRIMARY KEY (`id`),
                                            UNIQUE KEY `uk_question_user` (`question_id`,`user_id`),
                                            KEY `idx_user_id` (`user_id`),
                                            KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='做题记录表';


--
-- Table structure for table `portal_interview_bookmark`
--

DROP TABLE IF EXISTS `portal_interview_bookmark`;
CREATE TABLE `portal_interview_bookmark` (
                                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                             `question_id` bigint NOT NULL COMMENT '题目ID',
                                             `user_id` bigint NOT NULL COMMENT '用户ID',
                                             `note` text COMMENT '笔记',
                                             `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
                                             `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                             `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                             `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                             `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                             `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                             PRIMARY KEY (`id`),
                                             UNIQUE KEY `uk_question_user` (`question_id`,`user_id`),
                                             KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目收藏表';


--
-- Table structure for table `portal_interview_category`
--

DROP TABLE IF EXISTS `portal_interview_category`;
CREATE TABLE `portal_interview_category` (
                                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                             `name` varchar(200) NOT NULL COMMENT '分类名称',
                                             `slug` varchar(200) DEFAULT NULL COMMENT '分类标识',
                                             `description` text COMMENT '分类描述',
                                             `icon` varchar(500) DEFAULT NULL COMMENT '图标URL',
                                             `sort` int DEFAULT '0' COMMENT '排序',
                                             `question_count` int DEFAULT '0' COMMENT '题目数量',
                                             `status` varchar(20) DEFAULT 'active' COMMENT '状态:active,inactive',
                                             `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                             `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                             `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                             `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                             `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                             `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                             PRIMARY KEY (`id`),
                                             KEY `idx_status` (`status`),
                                             KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面试题目分类表';

--
-- Table structure for table `portal_interview_comment`
--

DROP TABLE IF EXISTS `portal_interview_comment`;
CREATE TABLE `portal_interview_comment` (
                                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                            `experience_id` bigint NOT NULL COMMENT '面经ID',
                                            `user_id` bigint NOT NULL COMMENT '评论用户ID',
                                            `parent_id` bigint DEFAULT NULL COMMENT '父评论ID（支持两级回复）',
                                            `reply_to_user_id` bigint DEFAULT NULL COMMENT '回复目标用户ID',
                                            `content` text NOT NULL COMMENT '评论内容',
                                            `like_count` bigint DEFAULT '0' COMMENT '点赞数',
                                            `status` varchar(20) DEFAULT 'published' COMMENT '状态:pending,published,rejected',
                                            `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                            `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                            `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                            `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                            `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                            `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                            PRIMARY KEY (`id`),
                                            KEY `idx_experience_id` (`experience_id`),
                                            KEY `idx_user_id` (`user_id`),
                                            KEY `idx_parent_id` (`parent_id`),
                                            KEY `idx_status` (`status`),
                                            KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面经评论表';


--
-- Table structure for table `portal_interview_comment_like`
--

DROP TABLE IF EXISTS `portal_interview_comment_like`;
CREATE TABLE `portal_interview_comment_like` (
                                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                                 `comment_id` bigint NOT NULL COMMENT '评论ID',
                                                 `user_id` bigint NOT NULL COMMENT '用户ID',
                                                 `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
                                                 PRIMARY KEY (`id`),
                                                 UNIQUE KEY `uk_comment_user` (`comment_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面经评论点赞表';


--
-- Table structure for table `portal_interview_company`
--

DROP TABLE IF EXISTS `portal_interview_company`;
CREATE TABLE `portal_interview_company` (
                                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                            `name` varchar(200) NOT NULL COMMENT '公司名称',
                                            `slug` varchar(200) DEFAULT NULL COMMENT '公司标识',
                                            `logo` varchar(500) DEFAULT NULL COMMENT '公司Logo URL',
                                            `description` text COMMENT '公司描述',
                                            `industry` varchar(100) DEFAULT NULL COMMENT '所属行业',
                                            `question_count` int DEFAULT '0' COMMENT '相关题目数',
                                            `sort` int DEFAULT '0' COMMENT '排序',
                                            `status` varchar(20) DEFAULT 'active' COMMENT '状态:active,inactive',
                                            `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                            `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                            `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                            `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                            `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                            `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                            PRIMARY KEY (`id`),
                                            UNIQUE KEY `uk_slug` (`slug`),
                                            KEY `idx_status` (`status`),
                                            KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面试公司标签表';


--
-- Table structure for table `portal_interview_experience`
--

DROP TABLE IF EXISTS `portal_interview_experience`;
CREATE TABLE `portal_interview_experience` (
                                               `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                               `user_id` bigint NOT NULL COMMENT '用户ID',
                                               `title` varchar(500) NOT NULL COMMENT '面经标题',
                                               `company` varchar(200) NOT NULL COMMENT '公司',
                                               `position` varchar(200) DEFAULT NULL COMMENT '岗位',
                                               `year` int DEFAULT NULL COMMENT '年份',
                                               `month` int DEFAULT NULL COMMENT '月份',
                                               `summary` varchar(500) DEFAULT NULL COMMENT '内容摘要',
                                               `content` text NOT NULL COMMENT '面经内容',
                                               `cover_image` varchar(500) DEFAULT NULL COMMENT '封面图URL',
                                               `tags` varchar(500) DEFAULT NULL COMMENT '标签',
                                               `is_top` tinyint(1) DEFAULT '0' COMMENT '是否置顶',
                                               `view_count` bigint DEFAULT '0' COMMENT '浏览数',
                                               `like_count` bigint DEFAULT '0' COMMENT '点赞数',
                                               `comment_count` bigint DEFAULT '0' COMMENT '评论数',
                                               `status` varchar(20) DEFAULT 'published' COMMENT '状态:draft,pending,published,rejected,archived',
                                               `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                               `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                               `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                               `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                               `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                               `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                               PRIMARY KEY (`id`),
                                               KEY `idx_user_id` (`user_id`),
                                               KEY `idx_company` (`company`),
                                               KEY `idx_status` (`status`),
                                               KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面经表';


--
-- Table structure for table `portal_interview_experience_like`
--

DROP TABLE IF EXISTS `portal_interview_experience_like`;
CREATE TABLE `portal_interview_experience_like` (
                                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                                    `experience_id` bigint NOT NULL COMMENT '面经ID',
                                                    `user_id` bigint NOT NULL COMMENT '用户ID',
                                                    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
                                                    `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                                    `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                                    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                                    PRIMARY KEY (`id`),
                                                    UNIQUE KEY `uk_experience_user` (`experience_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面经点赞表';


--
-- Table structure for table `portal_interview_position`
--

DROP TABLE IF EXISTS `portal_interview_position`;
CREATE TABLE `portal_interview_position` (
                                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                             `code` varchar(64) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '岗位编码（如 java_backend）',
                                             `name` varchar(100) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '岗位名称（如 Java后端工程师）',
                                             `industry` varchar(50) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '所属行业（如 互联网/金融/制造）',
                                             `level` varchar(32) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '岗位级别（junior/mid/senior）',
                                             `required_skills` text COLLATE utf8mb4_0900_ai_ci COMMENT '必备技能 JSON 数组（如 ["Spring","MySQL","Redis"]，与 portal_tag.name 对齐）',
                                             `hot_companies` text COLLATE utf8mb4_0900_ai_ci COMMENT '热门公司 JSON 数组（如 ["阿里","腾讯","字节"]）',
                                             `description` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '岗位描述',
                                             `sort` int DEFAULT '0' COMMENT '排序',
                                             `status` varchar(16) COLLATE utf8mb4_0900_ai_ci DEFAULT 'active' COMMENT '状态 active/inactive',
                                             `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                             `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                             `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                             `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                             `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                             `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                             PRIMARY KEY (`id`),
                                             UNIQUE KEY `uk_code` (`code`),
                                             KEY `idx_status_sort` (`status`,`sort`),
                                             KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面试岗位字典表';

--
-- Table structure for table `portal_interview_question`
--

DROP TABLE IF EXISTS `portal_interview_question`;
CREATE TABLE `portal_interview_question` (
                                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                             `title` varchar(500) NOT NULL COMMENT '题目标题',
                                             `description` text COMMENT '题目描述',
                                             `difficulty` varchar(20) DEFAULT 'medium' COMMENT '难度:easy,medium,hard',
                                             `category_id` bigint DEFAULT NULL COMMENT '分类ID',
                                             `tags` varchar(500) DEFAULT NULL COMMENT '标签，逗号分隔',
                                             `companies` varchar(500) DEFAULT NULL COMMENT '公司，逗号分隔',
                                             `acceptance_rate` decimal(5,2) DEFAULT '0.00' COMMENT '通过率',
                                             `submission_count` bigint DEFAULT '0' COMMENT '提交次数',
                                             `like_count` bigint DEFAULT '0' COMMENT '点赞数',
                                             `hint` text COMMENT '提示',
                                             `solution` text COMMENT '参考答案',
                                             `sort` int DEFAULT '0' COMMENT '排序',
                                             `status` varchar(20) DEFAULT 'active' COMMENT '状态:active,inactive',
                                             `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                             `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                             `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                             `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                             `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                             `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                             PRIMARY KEY (`id`),
                                             KEY `idx_category_id` (`category_id`),
                                             KEY `idx_difficulty` (`difficulty`),
                                             KEY `idx_status` (`status`),
                                             KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面试题目表';


--
-- Table structure for table `portal_interview_question_company`
--

DROP TABLE IF EXISTS `portal_interview_question_company`;
CREATE TABLE `portal_interview_question_company` (
                                                     `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                                     `question_id` bigint NOT NULL COMMENT '题目ID',
                                                     `company_id` bigint NOT NULL COMMENT '公司ID',
                                                     `sort` int DEFAULT '0' COMMENT '排序',
                                                     `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                     PRIMARY KEY (`id`),
                                                     UNIQUE KEY `uk_question_company` (`question_id`,`company_id`),
                                                     KEY `idx_company_id` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目-公司关联表';


--
-- Table structure for table `portal_interview_question_like`
--

DROP TABLE IF EXISTS `portal_interview_question_like`;
CREATE TABLE `portal_interview_question_like` (
                                                  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                                  `question_id` bigint NOT NULL COMMENT '题目ID',
                                                  `user_id` bigint NOT NULL COMMENT '用户ID',
                                                  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
                                                  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                                  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                                  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                                  PRIMARY KEY (`id`),
                                                  UNIQUE KEY `uk_question_user` (`question_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目点赞表';


--
-- Table structure for table `portal_interview_resume_template`
--

DROP TABLE IF EXISTS `portal_interview_resume_template`;
CREATE TABLE `portal_interview_resume_template` (
                                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                                    `title` varchar(500) NOT NULL COMMENT '模板标题',
                                                    `description` text COMMENT '模板描述',
                                                    `cover` varchar(500) DEFAULT NULL COMMENT '封面URL',
                                                    `download_url` varchar(500) DEFAULT NULL COMMENT '下载地址',
                                                    `category` varchar(200) DEFAULT NULL COMMENT '分类',
                                                    `file_type` varchar(20) DEFAULT NULL COMMENT '文件类型：docx/pdf/psd',
                                                    `file_size` bigint DEFAULT NULL COMMENT '文件大小（字节）',
                                                    `is_premium` tinyint(1) DEFAULT '0' COMMENT '是否付费模板',
                                                    `usage_guide` text COMMENT '使用指南',
                                                    `tags` varchar(500) DEFAULT NULL COMMENT '标签，逗号分隔',
                                                    `like_count` bigint DEFAULT '0' COMMENT '点赞数',
                                                    `download_count` bigint DEFAULT '0' COMMENT '下载次数',
                                                    `sort` int DEFAULT '0' COMMENT '排序',
                                                    `status` varchar(20) DEFAULT 'active' COMMENT '状态:active,inactive',
                                                    `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                                    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                    `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                                    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                                    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                                    PRIMARY KEY (`id`),
                                                    KEY `idx_status` (`status`),
                                                    KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='简历模板表';


--
-- Table structure for table `portal_interview_resume_template_like`
--

DROP TABLE IF EXISTS `portal_interview_resume_template_like`;
CREATE TABLE `portal_interview_resume_template_like` (
                                                         `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                                         `template_id` bigint NOT NULL COMMENT '简历模板ID',
                                                         `user_id` bigint NOT NULL COMMENT '用户ID',
                                                         `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                                         `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
                                                         `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                                         `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                         `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                                         PRIMARY KEY (`id`),
                                                         UNIQUE KEY `uk_template_user` (`template_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='简历模板点赞表';


--
-- Table structure for table `portal_interview_submission`
--

DROP TABLE IF EXISTS `portal_interview_submission`;
CREATE TABLE `portal_interview_submission` (
                                               `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                               `question_id` bigint NOT NULL COMMENT '题目ID',
                                               `user_id` bigint NOT NULL COMMENT '用户ID',
                                               `code` text COMMENT '提交的代码',
                                               `content` text COMMENT '提交的文字答案',
                                               `language` varchar(50) DEFAULT 'java' COMMENT '编程语言',
                                               `answer_type` varchar(20) DEFAULT 'code' COMMENT '答案类型：code/text/design',
                                               `status` varchar(50) DEFAULT 'pending' COMMENT '状态:accepted,wrong_answer,time_limit,compile_error',
                                               `is_success` tinyint(1) DEFAULT '0' COMMENT '是否通过',
                                               `runtime` int DEFAULT NULL COMMENT '运行时间（毫秒）',
                                               `memory_usage` int DEFAULT NULL COMMENT '内存使用（KB）',
                                               `note` text COMMENT '备注/笔记',
                                               `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
                                               `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                               `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                               `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                               `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                               `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                               PRIMARY KEY (`id`),
                                               KEY `idx_question_id` (`question_id`),
                                               KEY `idx_user_id` (`user_id`),
                                               KEY `idx_status` (`status`),
                                               KEY `idx_user_question` (`user_id`,`question_id`),
                                               KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目提交记录表';


--
-- Table structure for table `portal_like`
--

