-- 来源：all-db-ddl.sql 行1948-2471（已剔除 INSERT 种子数据，种子数据见 80 段）
-- 用途：门户扩展表 DDL（portal_code_run / portal_column* / portal_comment* / portal_contest* / portal_creator_* / portal_entity_tag / portal_feed_* / portal_feedback / portal_follow / portal_friend_link / portal_growth_* / portal_help_*）

DROP TABLE IF EXISTS `portal_code_run`;
CREATE TABLE `portal_code_run` (
                                   `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                   `user_id` bigint NOT NULL COMMENT '运行者用户ID',
                                   `language` varchar(16) NOT NULL COMMENT '编程语言 java/python/javascript',
                                   `code` mediumtext NOT NULL COMMENT '用户提交的源代码',
                                   `stdin` text COMMENT '标准输入内容',
                                   `output` mediumtext COMMENT '标准输出（截断至 1MB）',
                                   `error_msg` mediumtext COMMENT '错误输出 / 编译错误信息',
                                   `status` varchar(16) NOT NULL DEFAULT 'running' COMMENT '运行状态 running/success/failed/timeout',
                                   `runtime_ms` int DEFAULT NULL COMMENT '运行耗时（毫秒）',
                                   `mem_kb` int DEFAULT NULL COMMENT '内存占用（KB，粗略估算）',
                                   `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   PRIMARY KEY (`id`),
                                   KEY `idx_user_time` (`user_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='代码运行记录';


--
-- Table structure for table `portal_column`
--

DROP TABLE IF EXISTS `portal_column`;
CREATE TABLE `portal_column` (
                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                 `user_id` bigint NOT NULL COMMENT '创作者',
                                 `title` varchar(128) NOT NULL COMMENT '专栏名',
                                 `subtitle` varchar(256) DEFAULT NULL COMMENT '副标题',
                                 `description` text COMMENT '专栏简介',
                                 `cover` varchar(500) DEFAULT NULL COMMENT '封面',
                                 `category_id` bigint DEFAULT NULL COMMENT '分类',
                                 `status` varchar(16) NOT NULL DEFAULT 'draft' COMMENT 'draft/published/archived',
                                 `article_count` int NOT NULL DEFAULT '0' COMMENT '文章数',
                                 `subscribe_count` int NOT NULL DEFAULT '0' COMMENT '订阅数',
                                 `view_count` int NOT NULL DEFAULT '0' COMMENT '浏览数',
                                 `is_finished` tinyint NOT NULL DEFAULT '0' COMMENT '是否完结',
                                 `price` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '专栏会员价，0=免费',
                                 `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                 `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                 PRIMARY KEY (`id`),
                                 KEY `idx_user` (`user_id`),
                                 KEY `idx_status` (`status`),
                                 KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='专栏';


--
-- Table structure for table `portal_column_article`
--

DROP TABLE IF EXISTS `portal_column_article`;
CREATE TABLE `portal_column_article` (
                                         `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                         `column_id` bigint NOT NULL COMMENT '专栏ID',
                                         `article_id` bigint NOT NULL COMMENT '文章ID',
                                         `sort_order` int NOT NULL DEFAULT '0' COMMENT '专栏内顺序',
                                         `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                         PRIMARY KEY (`id`),
                                         UNIQUE KEY `uk_column_article` (`column_id`,`article_id`),
                                         KEY `idx_column_sort` (`column_id`,`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='专栏-文章关联';


--
-- Table structure for table `portal_column_subscribe`
--

DROP TABLE IF EXISTS `portal_column_subscribe`;
CREATE TABLE `portal_column_subscribe` (
                                           `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                           `column_id` bigint NOT NULL COMMENT '专栏ID',
                                           `user_id` bigint NOT NULL COMMENT '订阅用户ID',
                                           `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                           PRIMARY KEY (`id`),
                                           UNIQUE KEY `uk_column_user` (`column_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='专栏订阅';


--
-- Table structure for table `portal_comment`
--

DROP TABLE IF EXISTS `portal_comment`;
CREATE TABLE `portal_comment` (
                                  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评论ID',
                                  `article_id` bigint NOT NULL COMMENT '文章ID',
                                  `author_id` bigint NOT NULL COMMENT '评论者ID（门户用户ID）',
                                  `content` text COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '评论内容',
                                  `parent_id` bigint DEFAULT '0' COMMENT '父评论ID',
                                  `root_id` bigint DEFAULT '0' COMMENT '根评论ID（一级评论ID）',
                                  `reply_to` bigint DEFAULT NULL COMMENT '回复的用户ID',
                                  `reply_to_content` varchar(200) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '被回复的内容摘要',
                                  `like_count` bigint DEFAULT '0' COMMENT '点赞数',
                                  `status` char(1) COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
                                  `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                  `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                  `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                  PRIMARY KEY (`id`),
                                  KEY `idx_article_id` (`article_id`),
                                  KEY `idx_author_id` (`author_id`),
                                  KEY `idx_parent_id` (`parent_id`),
                                  KEY `idx_article_root` (`article_id`,`root_id`),
                                  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户评论表';


--
-- Table structure for table `portal_comment_like`
--

DROP TABLE IF EXISTS `portal_comment_like`;
CREATE TABLE `portal_comment_like` (
                                       `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
                                       `user_id` bigint NOT NULL COMMENT '用户ID（门户用户ID）',
                                       `comment_id` bigint NOT NULL COMMENT '评论ID',
                                       `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                       `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                       `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                       `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                       `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                       PRIMARY KEY (`id`),
                                       UNIQUE KEY `uk_user_comment` (`user_id`,`comment_id`),
                                       KEY `idx_user_id` (`user_id`),
                                       KEY `idx_comment_id` (`comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户评论点赞表';


--
-- Table structure for table `portal_contest_submission`
--

DROP TABLE IF EXISTS `portal_contest_submission`;
CREATE TABLE `portal_contest_submission` (
                                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                             `contest_id` bigint NOT NULL COMMENT '活动ID',
                                             `user_id` bigint NOT NULL COMMENT '投稿用户ID',
                                             `article_id` bigint NOT NULL COMMENT '投稿文章ID',
                                             `status` varchar(16) NOT NULL DEFAULT 'pending' COMMENT 'pending/shortlisted/eliminated/winner',
                                             `vote_count` int NOT NULL DEFAULT '0' COMMENT '投票数',
                                             `rank` int DEFAULT NULL COMMENT '排名',
                                             `remark` varchar(500) DEFAULT NULL COMMENT '备注（评审意见等）',
                                             `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                             `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                             PRIMARY KEY (`id`),
                                             UNIQUE KEY `uk_contest_user` (`contest_id`,`user_id`),
                                             UNIQUE KEY `uk_contest_article` (`contest_id`,`article_id`),
                                             KEY `idx_contest` (`contest_id`),
                                             KEY `idx_user` (`user_id`),
                                             KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='活动投稿';


--
-- Table structure for table `portal_contest_vote`
--

DROP TABLE IF EXISTS `portal_contest_vote`;
CREATE TABLE `portal_contest_vote` (
                                       `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                       `submission_id` bigint NOT NULL COMMENT '投稿ID',
                                       `user_id` bigint NOT NULL COMMENT '投票用户ID',
                                       `contest_id` bigint NOT NULL COMMENT '活动ID（冗余便于按活动统计）',
                                       `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '投票时间',
                                       PRIMARY KEY (`id`),
                                       UNIQUE KEY `uk_submission_user` (`submission_id`,`user_id`),
                                       KEY `idx_submission` (`submission_id`),
                                       KEY `idx_contest` (`contest_id`),
                                       KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='活动投稿投票记录';


--
-- Table structure for table `portal_creator_certification`
--

DROP TABLE IF EXISTS `portal_creator_certification`;
CREATE TABLE `portal_creator_certification` (
                                                `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                                `user_id` bigint NOT NULL COMMENT '申请用户ID',
                                                `real_name` varchar(64) NOT NULL COMMENT '真实姓名',
                                                `cert_type` varchar(32) NOT NULL COMMENT '认证类型 identity/creator/expert',
                                                `cert_no` varchar(64) DEFAULT NULL COMMENT '证件号',
                                                `cert_image` varchar(500) DEFAULT NULL COMMENT '证件照URL',
                                                `intro` text COMMENT '自我介绍',
                                                `works` varchar(500) DEFAULT NULL COMMENT '代表作链接',
                                                `status` varchar(16) NOT NULL DEFAULT 'pending' COMMENT '审核状态 pending/approved/rejected',
                                                `auditor_id` bigint DEFAULT NULL COMMENT '审核人ID',
                                                `audit_remark` varchar(500) DEFAULT NULL COMMENT '审核备注',
                                                `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
                                                `audited_time` datetime DEFAULT NULL COMMENT '审核时间',
                                                `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                                PRIMARY KEY (`id`),
                                                KEY `idx_user` (`user_id`),
                                                KEY `idx_status` (`status`),
                                                KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='创作者认证';


--
-- Table structure for table `portal_creator_settlement`
--

DROP TABLE IF EXISTS `portal_creator_settlement`;
CREATE TABLE `portal_creator_settlement` (
                                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                             `creator_id` bigint NOT NULL COMMENT '创作者用户ID',
                                             `period` varchar(16) NOT NULL COMMENT '结算周期，格式 yyyy-MM，如 2026-07',
                                             `tip_income` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '打赏收入（当月已支付打赏总额）',
                                             `paid_read_income` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '付费阅读收入（当月已支付购买总额）',
                                             `column_income` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '专栏订阅收入（当月已支付订阅总额）',
                                             `total_income` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '总收入（三项之和）',
                                             `platform_fee` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '平台抽成（total_income * platform_fee_rate）',
                                             `creator_income` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '创作者实得（total_income - platform_fee）',
                                             `status` varchar(16) NOT NULL DEFAULT 'pending' COMMENT '状态 pending/confirmed/paid',
                                             `paid_time` datetime DEFAULT NULL COMMENT '打款时间',
                                             `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                             `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                             `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                             PRIMARY KEY (`id`),
                                             UNIQUE KEY `uk_creator_period` (`creator_id`,`period`),
                                             KEY `idx_creator` (`creator_id`),
                                             KEY `idx_period` (`period`),
                                             KEY `idx_status` (`status`),
                                             KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='创作者分成结算';


--
-- Table structure for table `portal_entity_tag`
--

DROP TABLE IF EXISTS `portal_entity_tag`;
CREATE TABLE `portal_entity_tag` (
                                     `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
                                     `tag_id` bigint unsigned NOT NULL COMMENT '标签ID（引用 portal_tag.id）',
                                     `entity_type` varchar(32) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '实体类型（article/interview_question/interview_experience/interview_resume_template/book 等）',
                                     `entity_id` bigint unsigned NOT NULL COMMENT '实体ID',
                                     `sort` int DEFAULT '0' COMMENT '排序',
                                     `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                     `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                     `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                     `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                     PRIMARY KEY (`id`),
                                     UNIQUE KEY `uk_tag_entity` (`tag_id`,`entity_type`,`entity_id`),
                                     KEY `idx_entity` (`entity_type`,`entity_id`),
                                     KEY `idx_entity_create` (`entity_type`,`create_time`),
                                     KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通用实体标签关联表';


--
-- Table structure for table `portal_feed_event`
--

DROP TABLE IF EXISTS `portal_feed_event`;
CREATE TABLE `portal_feed_event` (
                                     `id` bigint NOT NULL AUTO_INCREMENT,
                                     `user_id` bigint NOT NULL COMMENT '事件发布者',
                                     `event_type` varchar(32) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'publish_article/publish_experience/new_column/checkin等',
                                     `target_type` varchar(32) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'article/experience/column/book等',
                                     `target_id` bigint NOT NULL COMMENT '目标对象ID',
                                     `title` varchar(256) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '目标标题',
                                     `summary` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '动态摘要',
                                     `cover` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '封面图',
                                     `created_time` datetime NOT NULL,
                                     PRIMARY KEY (`id`),
                                     KEY `idx_user_time` (`user_id`,`created_time`),
                                     KEY `idx_type_time` (`event_type`,`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='动态事件流';


--
-- Table structure for table `portal_feed_inbox`
--

DROP TABLE IF EXISTS `portal_feed_inbox`;
CREATE TABLE `portal_feed_inbox` (
                                     `id` bigint NOT NULL AUTO_INCREMENT,
                                     `user_id` bigint NOT NULL COMMENT '接收者',
                                     `event_id` bigint NOT NULL COMMENT '动态事件ID',
                                     `created_time` datetime NOT NULL,
                                     PRIMARY KEY (`id`),
                                     KEY `idx_user_time` (`user_id`,`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='动态收件箱';


--
-- Table structure for table `portal_feedback`
--

DROP TABLE IF EXISTS `portal_feedback`;
CREATE TABLE `portal_feedback` (
                                   `id` bigint NOT NULL AUTO_INCREMENT COMMENT '反馈ID',
                                   `feedback_type` varchar(32) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '反馈类型：suggestion/bug/experience/other',
                                   `subject` varchar(200) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '反馈主题',
                                   `description` varchar(2000) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '反馈详细描述',
                                   `contact` varchar(100) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '联系方式（可选）',
                                   `user_id` bigint DEFAULT NULL COMMENT '反馈人用户ID',
                                   `username` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '反馈人用户名（冗余）',
                                   `ip` varchar(128) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '反馈人IP',
                                   `status` varchar(20) COLLATE utf8mb4_0900_ai_ci DEFAULT 'pending' COMMENT '处理状态：pending/processing/resolved/rejected',
                                   `handler` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '处理人',
                                   `handle_result` varchar(1000) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '处理结果说明',
                                   `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
                                   `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                   `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                   `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                   PRIMARY KEY (`id`),
                                   KEY `idx_feedback_type` (`feedback_type`),
                                   KEY `idx_status` (`status`),
                                   KEY `idx_user_id` (`user_id`),
                                   KEY `idx_create_time` (`create_time`),
                                   KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户意见反馈表';


--
-- Table structure for table `portal_follow`
--

DROP TABLE IF EXISTS `portal_follow`;
CREATE TABLE `portal_follow` (
                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关注ID',
                                 `follower_id` bigint NOT NULL COMMENT '关注者ID（门户用户ID）',
                                 `following_id` bigint NOT NULL COMMENT '被关注者ID（门户用户ID）',
                                 `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                 `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                 `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                 `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `uk_follower_following` (`follower_id`,`following_id`),
                                 KEY `idx_follower_id` (`follower_id`),
                                 KEY `idx_following_id` (`following_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户关注表';


--
-- Table structure for table `portal_friend_link`
--

DROP TABLE IF EXISTS `portal_friend_link`;
CREATE TABLE `portal_friend_link` (
                                      `id` bigint NOT NULL AUTO_INCREMENT COMMENT '链接ID',
                                      `name` varchar(100) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '链接名称',
                                      `url` varchar(500) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '链接地址',
                                      `description` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '链接描述',
                                      `logo` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'Logo URL',
                                      `sort` int DEFAULT '0' COMMENT '排序',
                                      `status` varchar(20) COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '状态：0正常 1停用',
                                      `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                      `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                      `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                      `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                      `del_flag` char(1) DEFAULT '0' COMMENT '删除状态：0存在 1已删除',
                                      PRIMARY KEY (`id`),
                                      KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户友情链接表';

--
-- Table structure for table `portal_growth_log`
--

DROP TABLE IF EXISTS `portal_growth_log`;
CREATE TABLE `portal_growth_log` (
                                     `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
                                     `user_id` bigint unsigned NOT NULL COMMENT '获得成长值的用户ID',
                                     `target_user_id` bigint unsigned DEFAULT NULL COMMENT '目标用户ID（如被点赞的内容作者）',
                                     `module` varchar(32) NOT NULL COMMENT '来源模块: article/reading/interview/all',
                                     `action` varchar(64) NOT NULL COMMENT '行为: publish_article/solve_question/finish_book/...',
                                     `entity_type` varchar(32) DEFAULT NULL COMMENT '实体类型: article/book/question/note/experience',
                                     `entity_id` bigint DEFAULT NULL COMMENT '实体ID',
                                     `growth_delta` int NOT NULL COMMENT '成长值变化（正数增加，负数减少）',
                                     `description` varchar(255) DEFAULT NULL COMMENT '描述',
                                     `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                     `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                     `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                     `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                     PRIMARY KEY (`id`),
                                     KEY `idx_user_time` (`user_id`,`create_time`),
                                     KEY `idx_module_action` (`module`,`action`),
                                     KEY `idx_entity` (`entity_type`,`entity_id`),
                                     KEY `idx_target_user` (`target_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='成长事件流水表';


--
-- Table structure for table `portal_growth_rule`
--

DROP TABLE IF EXISTS `portal_growth_rule`;
CREATE TABLE `portal_growth_rule` (
                                      `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
                                      `module` varchar(32) NOT NULL COMMENT '模块: article/reading/interview/all',
                                      `action` varchar(64) NOT NULL COMMENT '行为编码',
                                      `growth_delta` int NOT NULL COMMENT '成长值',
                                      `daily_limit` int DEFAULT '0' COMMENT '每日上限（0=不限）',
                                      `description` varchar(255) DEFAULT NULL COMMENT '描述',
                                      `status` char(1) DEFAULT '0' COMMENT '状态（0启用 1停用）',
                                      `sort` int DEFAULT '0' COMMENT '排序',
                                      `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                      `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                      `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                      `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                      PRIMARY KEY (`id`),
                                      UNIQUE KEY `uk_module_action` (`module`,`action`)
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='成长规则配置表';

--
-- Table structure for table `portal_help_article`
--

DROP TABLE IF EXISTS `portal_help_article`;
CREATE TABLE `portal_help_article` (
                                       `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文章ID',
                                       `category_id` bigint NOT NULL COMMENT '分类ID',
                                       `title` varchar(200) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '问题标题',
                                       `content` text COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '答案内容（支持纯文本）',
                                       `view_count` int DEFAULT '0' COMMENT '查看次数',
                                       `like_count` int DEFAULT '0' COMMENT '点赞次数',
                                       `sort` int DEFAULT '0' COMMENT '排序（升序）',
                                       `is_featured` tinyint DEFAULT '0' COMMENT '是否精选：0=否 1=是',
                                       `status` varchar(20) COLLATE utf8mb4_0900_ai_ci DEFAULT 'published' COMMENT '状态：published/draft',
                                       `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                       `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                       `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                       `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                       `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                       `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                       PRIMARY KEY (`id`),
                                       KEY `idx_category_id` (`category_id`),
                                       KEY `idx_status` (`status`),
                                       KEY `idx_is_featured` (`is_featured`),
                                       KEY `idx_sort` (`sort`),
                                       KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='帮助中心文章表';

--
-- Table structure for table `portal_help_category`
--

DROP TABLE IF EXISTS `portal_help_category`;
CREATE TABLE `portal_help_category` (
                                        `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
                                        `name` varchar(100) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类名称',
                                        `icon` varchar(100) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '图标（lucide 图标名）',
                                        `description` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '分类描述',
                                        `sort` int DEFAULT '0' COMMENT '排序（升序）',
                                        `status` varchar(20) COLLATE utf8mb4_0900_ai_ci DEFAULT 'active' COMMENT '状态：active/inactive',
                                        `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                        `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                        `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                        `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                        `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                        `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                        PRIMARY KEY (`id`),
                                        KEY `idx_status` (`status`),
                                        KEY `idx_sort` (`sort`),
                                        KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='帮助中心分类表';

--
-- Table structure for table `portal_interview_attempt`
--

