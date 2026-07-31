-- 来源：all-db-ddl.sql 行2872-3835（已剔除 INSERT 种子数据，种子数据见 80 段）
-- 用途：门户互动社交表 DDL（portal_like / portal_message* / portal_mock_interview* / portal_notification_bak / portal_order / portal_pk_challenge / portal_reading_* / portal_report / portal_shop_* / portal_study_plan* / portal_task / portal_tip_order / portal_topic_* / portal_user* / portal_vip_package / portal_wallet* / portal_writing_* / portal_wrong_question）

DROP TABLE IF EXISTS `portal_like`;
CREATE TABLE `portal_like` (
                               `id` bigint NOT NULL AUTO_INCREMENT COMMENT '点赞ID',
                               `user_id` bigint NOT NULL COMMENT '用户ID（门户用户ID）',
                               `article_id` bigint NOT NULL COMMENT '文章ID',
                               `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                               `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                               `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                               `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                               PRIMARY KEY (`id`),
                               UNIQUE KEY `uk_user_article` (`user_id`,`article_id`),
                               KEY `idx_user_id` (`user_id`),
                               KEY `idx_article_id` (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户点赞表（文章）';


--
-- Table structure for table `portal_message`
--

DROP TABLE IF EXISTS `portal_message`;
CREATE TABLE `portal_message` (
                                  `id` bigint NOT NULL AUTO_INCREMENT,
                                  `session_id` bigint NOT NULL COMMENT '会话ID',
                                  `sender_id` bigint NOT NULL COMMENT '发送者',
                                  `sender_type` varchar(16) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'portal' COMMENT '发送者类型 portal/sys',
                                  `receiver_id` bigint NOT NULL COMMENT '接收者',
                                  `receiver_type` varchar(16) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'portal' COMMENT '接收者类型 portal/sys',
                                  `content` text COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息内容',
                                  `msg_type` varchar(16) COLLATE utf8mb4_0900_ai_ci DEFAULT 'text' COMMENT 'text/image/file',
                                  `is_read` tinyint DEFAULT '0' COMMENT '是否已读',
                                  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                  PRIMARY KEY (`id`),
                                  KEY `idx_session_time` (`session_id`,`create_time`),
                                  KEY `idx_receiver_type_read` (`receiver_id`,`receiver_type`,`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='私信消息';


--
-- Table structure for table `portal_message_session`
--

DROP TABLE IF EXISTS `portal_message_session`;
CREATE TABLE `portal_message_session` (
                                          `id` bigint NOT NULL AUTO_INCREMENT,
                                          `user_a` bigint NOT NULL COMMENT '用户A（较小ID）',
                                          `user_a_type` varchar(16) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'portal' COMMENT 'A方用户类型 portal/sys',
                                          `user_b` bigint NOT NULL COMMENT '用户B（较大ID）',
                                          `user_b_type` varchar(16) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'portal' COMMENT 'B方用户类型 portal/sys',
                                          `last_message_id` bigint DEFAULT NULL COMMENT '最后一条消息ID',
                                          `last_message_content` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '最后消息内容预览',
                                          `last_message_time` datetime DEFAULT NULL COMMENT '最后消息时间',
                                          `unread_a` int DEFAULT '0' COMMENT 'A未读数',
                                          `unread_b` int DEFAULT '0' COMMENT 'B未读数',
                                          `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                          `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                          PRIMARY KEY (`id`),
                                          UNIQUE KEY `uk_users_type` (`user_a`,`user_b`,`user_a_type`,`user_b_type`),
                                          KEY `idx_user_a` (`user_a`),
                                          KEY `idx_user_b` (`user_b`),
                                          KEY `idx_last_time` (`last_message_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='私信会话';


--
-- Table structure for table `portal_mock_interview`
--

DROP TABLE IF EXISTS `portal_mock_interview`;
CREATE TABLE `portal_mock_interview` (
                                         `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                         `user_id` bigint NOT NULL COMMENT '面试用户ID',
                                         `position` varchar(64) DEFAULT NULL COMMENT '面试岗位（如 后端开发/前端开发）',
                                         `scene` varchar(64) DEFAULT NULL COMMENT '面试场景（如 算法/系统设计/项目深挖，对应题目分类）',
                                         `status` varchar(16) NOT NULL DEFAULT 'in_progress' COMMENT '状态 in_progress/finished',
                                         `total_qa` int NOT NULL DEFAULT '0' COMMENT '题目总数',
                                         `score` int DEFAULT NULL COMMENT '面试总分（0-100，结束面试时计算）',
                                         `summary` text COMMENT 'AI 生成的面试总结',
                                         `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                         `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                         `is_personalized` tinyint(1) DEFAULT '0' COMMENT '是否基于画像抽题（0随机 1画像驱动）',
                                         `profile_snapshot` text COMMENT '抽题时的画像快照 JSON（含薄弱点列表，便于回溯分析）',
                                         `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                         PRIMARY KEY (`id`),
                                         KEY `idx_user_time` (`user_id`,`create_time`),
                                         KEY `idx_status` (`status`),
                                         KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='模拟面试会话';


--
-- Table structure for table `portal_mock_interview_qa`
--

DROP TABLE IF EXISTS `portal_mock_interview_qa`;
CREATE TABLE `portal_mock_interview_qa` (
                                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                            `interview_id` bigint NOT NULL COMMENT '面试会话ID',
                                            `question_id` bigint DEFAULT NULL COMMENT '关联题目ID（portal_interview_question.id）',
                                            `question_idx` int NOT NULL COMMENT '题目序号（从 0 开始）',
                                            `question` varchar(1000) NOT NULL COMMENT '面试问题（快照自题目标题）',
                                            `user_answer` text COMMENT '用户回答',
                                            `ai_feedback` text COMMENT 'AI 反馈（规则化生成）',
                                            `score` int DEFAULT NULL COMMENT '本题评分（0-100）',
                                            `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                            `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                            PRIMARY KEY (`id`),
                                            KEY `idx_interview` (`interview_id`),
                                            KEY `idx_question_idx` (`interview_id`,`question_idx`),
                                            KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='模拟面试问答';


--
-- Table structure for table `portal_notification_bak`
--

DROP TABLE IF EXISTS `portal_notification_bak`;
CREATE TABLE `portal_notification_bak` (
                                           `id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知ID',
                                           `user_id` bigint NOT NULL COMMENT '用户ID（门户用户ID）',
                                           `type` varchar(50) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '类型：comment/like/follow/system/order',
                                           `title` varchar(200) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '通知标题',
                                           `content` text COLLATE utf8mb4_0900_ai_ci COMMENT '通知内容',
                                           `data` json DEFAULT NULL COMMENT '通知数据（JSON格式）',
                                           `is_read` tinyint(1) DEFAULT '0' COMMENT '是否已读',
                                           `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                           `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                           `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                           `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                           `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                           PRIMARY KEY (`id`),
                                           KEY `idx_user_id` (`user_id`),
                                           KEY `idx_type` (`type`),
                                           KEY `idx_is_read` (`is_read`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户通知表';


DROP TABLE IF EXISTS `portal_order`;
CREATE TABLE `portal_order` (
                                `id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单ID',
                                `order_no` varchar(64) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单号',
                                `user_id` bigint NOT NULL COMMENT '用户ID（门户用户ID）',
                                `type` varchar(50) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '类型：vip/recharge/product',
                                `product_id` bigint DEFAULT NULL COMMENT '商品ID',
                                `amount` decimal(10,2) NOT NULL COMMENT '金额',
                                `status` varchar(20) COLLATE utf8mb4_0900_ai_ci DEFAULT 'pending' COMMENT '状态：pending/paid/cancelled/refunded',
                                `pay_method` varchar(50) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '支付方式：wechat/alipay',
                                `paid_at` datetime DEFAULT NULL COMMENT '支付时间',
                                `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                PRIMARY KEY (`id`),
                                UNIQUE KEY `uk_order_no` (`order_no`),
                                KEY `idx_user_id` (`user_id`),
                                KEY `idx_type` (`type`),
                                KEY `idx_status` (`status`),
                                KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户订单表';


--
-- Table structure for table `portal_pk_challenge`
--

DROP TABLE IF EXISTS `portal_pk_challenge`;
CREATE TABLE `portal_pk_challenge` (
                                       `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                       `challenger_id` bigint NOT NULL COMMENT '发起方用户ID',
                                       `opponent_id` bigint NOT NULL COMMENT '应战方用户ID',
                                       `status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT '状态:pending/accepted/declined/ongoing/finished',
                                       `winner_id` bigint DEFAULT NULL COMMENT '胜者用户ID（平局为NULL）',
                                       `challenger_score` int NOT NULL DEFAULT '0' COMMENT '发起方得分（通过题数）',
                                       `opponent_score` int NOT NULL DEFAULT '0' COMMENT '应战方得分（通过题数）',
                                       `question_ids` varchar(500) NOT NULL COMMENT '题目ID列表，逗号分隔',
                                       `scene` varchar(20) NOT NULL DEFAULT '1v1' COMMENT '场景:1v1=好友PK / company=公司题目挑战',
                                       `company_id` bigint DEFAULT NULL COMMENT '公司ID（scene=company 时关联 portal_interview_company）',
                                       `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发起时间',
                                       `finished_time` datetime DEFAULT NULL COMMENT '结束时间',
                                       PRIMARY KEY (`id`),
                                       KEY `idx_challenger_id` (`challenger_id`),
                                       KEY `idx_opponent_id` (`opponent_id`),
                                       KEY `idx_status` (`status`),
                                       KEY `idx_company_id` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='PK 对战表（异步对战）';


--
-- Table structure for table `portal_reading_preference`
--

DROP TABLE IF EXISTS `portal_reading_preference`;
CREATE TABLE `portal_reading_preference` (
                                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                             `user_id` bigint NOT NULL COMMENT '用户ID',
                                             `font_size` int DEFAULT '18' COMMENT '正文字号（px，12-32）',
                                             `line_height` decimal(3,1) DEFAULT '1.8' COMMENT '行距（倍，1.2-3.0）',
                                             `theme` varchar(20) COLLATE utf8mb4_0900_ai_ci DEFAULT 'default' COMMENT '阅读主题：default=跟随 / light=亮色 / dark=暗色 / sepia=护眼黄',
                                             `font_family` varchar(50) COLLATE utf8mb4_0900_ai_ci DEFAULT 'system' COMMENT '字体：system=系统默认 / serif=衬线 / song=宋体 / hei=黑体',
                                             `letter_spacing` decimal(3,1) DEFAULT '0.0' COMMENT '字间距（px，-1.0-5.0）',
                                             `paragraph_spacing` decimal(4,1) DEFAULT '1.2' COMMENT '段间距（em，0.5-5.0）',
                                             `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                             `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                             `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                             `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                             `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                             `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                             PRIMARY KEY (`id`),
                                             UNIQUE KEY `uk_user_id` (`user_id`),
                                             KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户阅读偏好表';

--


-- Table structure for table `portal_reading_progress`


DROP TABLE IF EXISTS `portal_reading_progress`;
CREATE TABLE `portal_reading_progress` (
                                           `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                           `user_id` bigint NOT NULL COMMENT '用户ID',
                                           `book_id` bigint NOT NULL COMMENT '书籍ID',
                                           `current_chapter_id` bigint DEFAULT NULL COMMENT '当前阅读章节ID',
                                           `current_chapter_no` int DEFAULT '0' COMMENT '当前章节序号',
                                           `chapter_offset` int DEFAULT '0' COMMENT '章节内滚动偏移（像素）',
                                           `last_read_time` datetime DEFAULT NULL COMMENT '最后阅读时间',
                                           `reading_duration_ms` bigint DEFAULT '0' COMMENT '累计阅读时长（毫秒）',
                                           `status` varchar(30) DEFAULT 'want_to_read' COMMENT '状态:want_to_read,reading,finished',
                                           `progress` int DEFAULT '0' COMMENT '阅读进度百分比',
                                           `pages_read` int DEFAULT '0' COMMENT '已读页数',
                                           `start_date` date DEFAULT NULL COMMENT '开始阅读日期',
                                           `finish_date` date DEFAULT NULL COMMENT '完成日期',
                                           `note` text COMMENT '阅读笔记',
                                           `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                           `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                           `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                           `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                           `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                           `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                           PRIMARY KEY (`id`),
                                           UNIQUE KEY `uk_user_book` (`user_id`,`book_id`),
                                           KEY `idx_user_id` (`user_id`),
                                           KEY `idx_status` (`status`),
                                           KEY `idx_last_read_time` (`last_read_time`),
                                           KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='阅读进度表';


-- Table structure for table `portal_report`


DROP TABLE IF EXISTS `portal_report`;
CREATE TABLE `portal_report` (
                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT '举报ID',
                                 `report_type` varchar(32) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '举报类型：spam/inappropriate/infringement/fraud/other',
                                 `target_url` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '举报目标URL',
                                 `target_type` varchar(32) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '举报目标类型：comment/article/user 等，为空表示通用举报（仅 target_url）',
                                 `target_id` bigint DEFAULT NULL COMMENT '举报目标ID（评论/文章/用户ID，配合 target_type 使用）',
                                 `description` varchar(2000) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '问题描述',
                                 `contact` varchar(100) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '联系方式（可选）',
                                 `images` varchar(1000) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '图片证据（JSON数组，最多3张）',
                                 `user_id` bigint DEFAULT NULL COMMENT '举报人用户ID',
                                 `username` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '举报人用户名（冗余）',
                                 `ip` varchar(128) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '举报人IP',
                                 `status` varchar(20) COLLATE utf8mb4_0900_ai_ci DEFAULT 'pending' COMMENT '处理状态：pending/processing/resolved/rejected',
                                 `handler` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '处理人',
                                 `handle_result` varchar(1000) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '处理结果说明',
                                 `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
                                 `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                 `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                 `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                 PRIMARY KEY (`id`),
                                 KEY `idx_report_type` (`report_type`),
                                 KEY `idx_status` (`status`),
                                 KEY `idx_user_id` (`user_id`),
                                 KEY `idx_create_time` (`create_time`),
                                 KEY `idx_target` (`target_type`,`target_id`),
                                 KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户举报记录表';


-- Table structure for table `portal_shop_exchange`


DROP TABLE IF EXISTS `portal_shop_exchange`;
CREATE TABLE `portal_shop_exchange` (
                                        `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                        `user_id` bigint NOT NULL COMMENT '兑换用户ID',
                                        `item_id` bigint NOT NULL COMMENT '商品ID',
                                        `points_cost` int NOT NULL COMMENT '消耗积分（冗余，便于查询）',
                                        `status` varchar(16) NOT NULL DEFAULT 'pending' COMMENT '状态 pending/fulfilled/failed',
                                        `address` varchar(500) DEFAULT NULL COMMENT '收货地址（实物商品）',
                                        `exchange_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '兑换时间',
                                        `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                        `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                        `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                        `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                        `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                        `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                        PRIMARY KEY (`id`),
                                        KEY `idx_user` (`user_id`),
                                        KEY `idx_item` (`item_id`),
                                        KEY `idx_status` (`status`),
                                        KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='积分兑换记录表';


-- Table structure for table `portal_shop_item`


DROP TABLE IF EXISTS `portal_shop_item`;
CREATE TABLE `portal_shop_item` (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                    `name` varchar(128) NOT NULL COMMENT '商品名称',
                                    `description` varchar(500) DEFAULT NULL COMMENT '商品描述',
                                    `cover` varchar(500) DEFAULT NULL COMMENT '商品封面URL',
                                    `type` varchar(32) NOT NULL DEFAULT 'virtual' COMMENT '商品类型 virtual/physical',
                                    `points_cost` int NOT NULL DEFAULT '0' COMMENT '兑换所需积分',
                                    `stock` int NOT NULL DEFAULT '0' COMMENT '库存（-1表示不限）',
                                    `status` varchar(16) NOT NULL DEFAULT 'active' COMMENT '状态 active/inactive',
                                    `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                    PRIMARY KEY (`id`),
                                    KEY `idx_type_status` (`type`,`status`),
                                    KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='积分商城商品表';

--
-- Table structure for table `portal_study_plan`
--

DROP TABLE IF EXISTS `portal_study_plan`;
CREATE TABLE `portal_study_plan` (
                                     `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                     `user_id` bigint NOT NULL COMMENT '用户ID',
                                     `title` varchar(128) NOT NULL COMMENT '计划标题',
                                     `plan_type` varchar(32) DEFAULT NULL COMMENT '计划类型 daily_question/weekly_reading/custom',
                                     `target_count` int DEFAULT NULL COMMENT '目标数量',
                                     `target_category` varchar(64) DEFAULT NULL COMMENT '目标分类',
                                     `start_date` date DEFAULT NULL COMMENT '开始日期',
                                     `end_date` date DEFAULT NULL COMMENT '结束日期',
                                     `status` varchar(16) NOT NULL DEFAULT 'active' COMMENT '状态 active/completed/abandoned',
                                     `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                     PRIMARY KEY (`id`),
                                     KEY `idx_user` (`user_id`),
                                     KEY `idx_status` (`status`),
                                     KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学习计划';


--
-- Table structure for table `portal_study_plan_log`
--

DROP TABLE IF EXISTS `portal_study_plan_log`;
CREATE TABLE `portal_study_plan_log` (
                                         `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                         `plan_id` bigint NOT NULL COMMENT '计划ID',
                                         `user_id` bigint NOT NULL COMMENT '用户ID',
                                         `log_date` date NOT NULL COMMENT '日志日期',
                                         `done_count` int NOT NULL DEFAULT '0' COMMENT '当日完成数量',
                                         `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                         PRIMARY KEY (`id`),
                                         UNIQUE KEY `uk_plan_date` (`plan_id`,`log_date`),
                                         KEY `idx_user_date` (`user_id`,`log_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='计划每日进度';


--
-- Table structure for table `portal_task`
--

DROP TABLE IF EXISTS `portal_task`;
CREATE TABLE `portal_task` (
                               `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                               `code` varchar(64) NOT NULL COMMENT '任务编码（唯一，用于埋点触发，如 daily_checkin）',
                               `name` varchar(128) NOT NULL COMMENT '任务名称',
                               `description` varchar(500) DEFAULT NULL COMMENT '任务描述',
                               `task_type` varchar(32) NOT NULL DEFAULT 'daily' COMMENT '任务类型 daily/once/achievement',
                               `reward_points` int NOT NULL DEFAULT '0' COMMENT '完成奖励积分',
                               `target_count` int NOT NULL DEFAULT '1' COMMENT '目标完成次数',
                               `icon` varchar(500) DEFAULT NULL COMMENT '任务图标URL',
                               `status` varchar(16) NOT NULL DEFAULT 'active' COMMENT '状态 active/inactive',
                               `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                               `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                               `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                               `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                               `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                               PRIMARY KEY (`id`),
                               UNIQUE KEY `uk_code` (`code`),
                               KEY `idx_type_status` (`task_type`,`status`),
                               KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务定义表';

--
-- Table structure for table `portal_tip_order`
--

DROP TABLE IF EXISTS `portal_tip_order`;
CREATE TABLE `portal_tip_order` (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                    `user_id` bigint NOT NULL COMMENT '打赏者用户ID',
                                    `author_id` bigint NOT NULL COMMENT '被打赏者用户ID',
                                    `target_type` varchar(32) NOT NULL COMMENT '打赏对象类型 article/column/article_paid',
                                    `target_id` bigint NOT NULL COMMENT '打赏对象ID',
                                    `amount` decimal(10,2) NOT NULL COMMENT '打赏金额',
                                    `message` varchar(200) DEFAULT NULL COMMENT '打赏留言',
                                    `status` varchar(16) NOT NULL DEFAULT 'pending' COMMENT '状态 pending/paid/refunded',
                                    `pay_method` varchar(32) DEFAULT NULL COMMENT '支付方式',
                                    `paid_time` datetime DEFAULT NULL COMMENT '支付时间',
                                    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    PRIMARY KEY (`id`),
                                    KEY `idx_author` (`author_id`),
                                    KEY `idx_target` (`target_type`,`target_id`),
                                    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='打赏订单（复用为付费阅读购买记录，target_type=article_paid）';


--
-- Table structure for table `portal_topic`
--

DROP TABLE IF EXISTS `portal_topic`;
CREATE TABLE `portal_topic` (
                                `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                `title` varchar(128) NOT NULL COMMENT '话题标题',
                                `description` varchar(500) DEFAULT NULL COMMENT '话题描述/导语',
                                `cover` varchar(500) DEFAULT NULL COMMENT '封面图 URL',
                                `creator_id` bigint NOT NULL COMMENT '发起人 portal_user.id（必须是认证创作者）',
                                `status` varchar(20) NOT NULL DEFAULT 'active' COMMENT '状态：active 活跃/archived 归档/deleted 删除',
                                `pinned` tinyint NOT NULL DEFAULT '0' COMMENT '是否置顶：0 否/1 是',
                                `view_count` int NOT NULL DEFAULT '0' COMMENT '浏览数',
                                `post_count` int NOT NULL DEFAULT '0' COMMENT '观点数',
                                `like_count` int NOT NULL DEFAULT '0' COMMENT '话题被赞数',
                                `is_featured` tinyint NOT NULL DEFAULT '0' COMMENT '是否精选：0 否/1 是',
                                `comment_count` int NOT NULL DEFAULT '0' COMMENT '评论数（一级评论）',
                                `last_post_time` datetime DEFAULT NULL COMMENT '最后观点时间',
                                `last_poster_id` bigint DEFAULT NULL COMMENT '最后观点用户',
                                `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                `updated_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
                                `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                PRIMARY KEY (`id`),
                                KEY `idx_creator_time` (`creator_id`,`created_time`),
                                KEY `idx_status_pinned_last` (`status`,`pinned`,`last_post_time`),
                                KEY `idx_last_post` (`last_post_time`),
                                KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='话题主表';


--
-- Table structure for table `portal_topic_comment`
--

DROP TABLE IF EXISTS `portal_topic_comment`;
CREATE TABLE `portal_topic_comment` (
                                        `id` bigint NOT NULL AUTO_INCREMENT,
                                        `target_type` varchar(20) NOT NULL COMMENT '目标类型：topic 话题评论 / post 观点评论',
                                        `target_id` bigint NOT NULL COMMENT '目标 ID',
                                        `author_id` bigint NOT NULL COMMENT '评论者 portal_user.id',
                                        `content` varchar(2000) NOT NULL COMMENT '评论内容',
                                        `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父评论 ID（0=一级评论）',
                                        `root_id` bigint NOT NULL DEFAULT '0' COMMENT '根评论 ID（一级评论 root_id=0）',
                                        `reply_to` bigint DEFAULT NULL COMMENT '被回复的用户 ID',
                                        `reply_to_content` varchar(200) DEFAULT '' COMMENT '被回复内容摘要',
                                        `like_count` int NOT NULL DEFAULT '0',
                                        `reply_count` int NOT NULL DEFAULT '0' COMMENT '回复数（仅一级评论维护）',
                                        `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '软删',
                                        `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        `updated_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
                                        PRIMARY KEY (`id`),
                                        KEY `idx_target_type_id_parent` (`target_type`,`target_id`,`parent_id`,`created_time`),
                                        KEY `idx_root` (`root_id`,`created_time`),
                                        KEY `idx_author_time` (`author_id`,`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='话题评论（多态）';


--
-- Table structure for table `portal_topic_comment_like`
--

DROP TABLE IF EXISTS `portal_topic_comment_like`;
CREATE TABLE `portal_topic_comment_like` (
                                             `id` bigint NOT NULL AUTO_INCREMENT,
                                             `comment_id` bigint NOT NULL,
                                             `user_id` bigint NOT NULL,
                                             `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                             PRIMARY KEY (`id`),
                                             UNIQUE KEY `uk_comment_user` (`comment_id`,`user_id`),
                                             KEY `idx_user_time` (`user_id`,`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='话题评论点赞';


--
-- Table structure for table `portal_topic_like`
--

DROP TABLE IF EXISTS `portal_topic_like`;
CREATE TABLE `portal_topic_like` (
                                     `id` bigint NOT NULL AUTO_INCREMENT,
                                     `topic_id` bigint NOT NULL,
                                     `user_id` bigint NOT NULL,
                                     `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     PRIMARY KEY (`id`),
                                     UNIQUE KEY `uk_topic_user` (`topic_id`,`user_id`),
                                     KEY `idx_user_time` (`user_id`,`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='话题点赞';


--
-- Table structure for table `portal_topic_post`
--

DROP TABLE IF EXISTS `portal_topic_post`;
CREATE TABLE `portal_topic_post` (
                                     `id` bigint NOT NULL AUTO_INCREMENT,
                                     `topic_id` bigint NOT NULL COMMENT '所属话题',
                                     `user_id` bigint NOT NULL COMMENT '发布者 portal_user.id',
                                     `content` text NOT NULL COMMENT '观点内容（Markdown）',
                                     `images` json DEFAULT NULL COMMENT '图片 URL 列表，最多 9 张',
                                     `parent_post_id` bigint DEFAULT NULL COMMENT '父观点 ID（楼中楼，NULL 为一级观点）',
                                     `reply_to_user_id` bigint DEFAULT NULL COMMENT '回复的用户 ID',
                                     `floor` int NOT NULL DEFAULT '0' COMMENT '楼层号',
                                     `like_count` int NOT NULL DEFAULT '0',
                                     `comment_count` int NOT NULL DEFAULT '0',
                                     `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '软删：0 否/1 是',
                                     `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     `updated_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
                                     PRIMARY KEY (`id`),
                                     UNIQUE KEY `uk_topic_floor` (`topic_id`,`floor`),
                                     KEY `idx_topic_time` (`topic_id`,`created_time`),
                                     KEY `idx_user_time` (`user_id`,`created_time`),
                                     KEY `idx_parent` (`parent_post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='话题观点（楼层）';


--
-- Table structure for table `portal_topic_post_like`
--

DROP TABLE IF EXISTS `portal_topic_post_like`;
CREATE TABLE `portal_topic_post_like` (
                                          `id` bigint NOT NULL AUTO_INCREMENT,
                                          `post_id` bigint NOT NULL,
                                          `user_id` bigint NOT NULL,
                                          `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                          PRIMARY KEY (`id`),
                                          UNIQUE KEY `uk_post_user` (`post_id`,`user_id`),
                                          KEY `idx_user_time` (`user_id`,`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='话题观点点赞';


--
-- Table structure for table `portal_user`
--

DROP TABLE IF EXISTS `portal_user`;
CREATE TABLE `portal_user` (
                               `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
                               `user_id` bigint DEFAULT NULL COMMENT '关联后台用户ID',
                               `username` varchar(50) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
                               `nickname` varchar(50) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '昵称',
                               `email` varchar(100) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '邮箱',
                               `phone` varchar(20) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '手机号',
                               `password` varchar(200) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '密码',
                               `avatar` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '头像URL',
                               `bio` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '个人简介',
                               `position` varchar(100) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '职位',
                               `wechat` varchar(100) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '微信号',
                               `gender` varchar(20) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '性别：male-男，female-女，other-其他',
                               `birthday` varchar(20) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '生日：YYYY-MM-DD格式',
                               `location` varchar(100) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '所在城市：如北京市',
                               `website` varchar(200) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '个人网站URL',
                               `github` varchar(100) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'GitHub用户名或完整URL',
                               `company` varchar(200) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '公司名称',
                               `school` varchar(200) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '学校名称',
                               `language` varchar(20) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '语言偏好：zh-CN，en-US等',
                               `timezone` varchar(50) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '时区：如Asia/Shanghai',
                               `notify_like` tinyint(1) DEFAULT '1' COMMENT '是否接收点赞通知',
                               `notify_comment` tinyint(1) DEFAULT '1' COMMENT '是否接收评论通知',
                               `notify_follow` tinyint(1) DEFAULT '1' COMMENT '是否接收关注通知',
                               `notify_system` tinyint(1) DEFAULT '1' COMMENT '是否接收系统通知',
                               `privacy_follow` tinyint(1) DEFAULT '1' COMMENT '是否允许被关注',
                               `privacy_bookmark` tinyint(1) DEFAULT '1' COMMENT '是否公开收藏夹',
                               `privacy_email` tinyint(1) DEFAULT '0' COMMENT '是否公开邮箱',
                               `privacy_phone` tinyint(1) DEFAULT '0' COMMENT '是否公开手机号',
                               `role` varchar(20) COLLATE utf8mb4_0900_ai_ci DEFAULT 'user' COMMENT '角色：user/admin',
                               `is_certified_creator` tinyint NOT NULL DEFAULT '0' COMMENT '是否认证创作者：0 否/1 是',
                               `vip_expire_at` datetime DEFAULT NULL COMMENT 'VIP过期时间',
                               `is_phone_verified` tinyint(1) DEFAULT '0' COMMENT '是否已验证手机号',
                               `is_wechat_verified` tinyint(1) DEFAULT '0' COMMENT '是否已验证微信',
                               `two_factor_enabled` tinyint(1) DEFAULT '0' COMMENT '是否开启两步验证',
                               `status` char(1) COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '帐号状态（0正常 1停用）',
                               `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
                               `login_ip` varchar(128) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '最后登录IP',
                               `login_date` datetime DEFAULT NULL COMMENT '最后登录时间',
                               `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                               `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                               `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                               `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                               PRIMARY KEY (`id`),
                               UNIQUE KEY `uk_username` (`username`),
                               KEY `idx_user_id` (`user_id`),
                               KEY `idx_email` (`email`),
                               KEY `idx_phone` (`phone`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户用户表';

--
-- Table structure for table `portal_user_badge`
--

DROP TABLE IF EXISTS `portal_user_badge`;
CREATE TABLE `portal_user_badge` (
                                     `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
                                     `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
                                     `achievement_id` bigint unsigned NOT NULL COMMENT '成就ID',
                                     `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                     `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '获得时间',
                                     `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                     `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                     `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                     PRIMARY KEY (`id`),
                                     UNIQUE KEY `uk_user_achievement` (`user_id`,`achievement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户徽章记录表';


--
-- Table structure for table `portal_user_growth`
--

DROP TABLE IF EXISTS `portal_user_growth`;
CREATE TABLE `portal_user_growth` (
                                      `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
                                      `user_id` bigint unsigned NOT NULL COMMENT '门户用户ID（portal_user.id）',
                                      `growth_value` int unsigned DEFAULT '0' COMMENT '成长值（累计，只增不减）',
                                      `level` int DEFAULT '1' COMMENT '当前等级',
                                      `title` varchar(50) DEFAULT '初出茅庐' COMMENT '当前头衔',
                                      `season_value` int unsigned DEFAULT '0' COMMENT '本季成长值（赛季排名用）',
                                      `points` bigint NOT NULL DEFAULT '0' COMMENT '积分余额（可消耗，与成长值解耦）',
                                      `supplement_card_count` int NOT NULL DEFAULT '0' COMMENT '补签卡数量（每月赠送1张，补签消耗）',
                                      `last_card_grant_month` varchar(7) DEFAULT NULL COMMENT '最后赠送补签卡月份（YYYY-MM，幂等控制）',
                                      `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                      `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                      `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                      `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                      PRIMARY KEY (`id`),
                                      UNIQUE KEY `uk_user` (`user_id`),
                                      KEY `idx_season` (`season_value` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户成长值总表';


--
-- Table structure for table `portal_user_resume`
--

DROP TABLE IF EXISTS `portal_user_resume`;
CREATE TABLE `portal_user_resume` (
                                      `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                      `user_id` bigint NOT NULL COMMENT '用户ID',
                                      `title` varchar(100) NOT NULL DEFAULT '我的简历' COMMENT '简历名称',
                                      `parent_id` bigint DEFAULT NULL COMMENT '父简历ID（版本历史关联，首次创建为 NULL）',
                                      `version_no` int NOT NULL DEFAULT '1' COMMENT '版本号',
                                      `name` varchar(50) DEFAULT NULL COMMENT '姓名',
                                      `gender` varchar(10) DEFAULT NULL COMMENT '性别：男/女',
                                      `birth_date` date DEFAULT NULL COMMENT '出生日期',
                                      `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
                                      `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
                                      `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
                                      `job_intention` text COMMENT '求职意向（JSON：期望职位/城市/薪资/类型）',
                                      `educations` text COMMENT '教育经历（JSON 数组：学校/专业/学历/时间/描述）',
                                      `works` text COMMENT '工作经历（JSON 数组：公司/职位/时间/描述）',
                                      `projects` text COMMENT '项目经历（JSON 数组：名称/角色/时间/描述/链接）',
                                      `skills` text COMMENT '技能列表（JSON 数组：名称/等级/分类）',
                                      `self_intro` text COMMENT '自我介绍',
                                      `score` int DEFAULT NULL COMMENT '评分（0-100）',
                                      `score_detail` text COMMENT '评分明细（JSON 数组）',
                                      `scored_time` datetime DEFAULT NULL COMMENT '评分时间',
                                      `file_url` varchar(255) DEFAULT NULL COMMENT 'PDF 导出文件URL',
                                      `export_time` datetime DEFAULT NULL COMMENT '最后导出时间',
                                      `status` varchar(20) NOT NULL DEFAULT 'draft' COMMENT '状态：draft/published/archived',
                                      `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                      `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                      `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                      `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                      `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                      PRIMARY KEY (`id`),
                                      UNIQUE KEY `uk_parent_version` (`parent_id`,`version_no`),
                                      KEY `idx_user_id` (`user_id`),
                                      KEY `idx_parent_id` (`parent_id`),
                                      KEY `idx_user_status` (`user_id`,`status`),
                                      KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户简历';


--
-- Table structure for table `portal_user_stats`
--

DROP TABLE IF EXISTS `portal_user_stats`;
CREATE TABLE `portal_user_stats` (
                                     `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
                                     `user_id` bigint unsigned NOT NULL COMMENT '门户用户ID',
                                     `article_count` int DEFAULT '0' COMMENT '发布文章数',
                                     `article_view_sum` bigint DEFAULT '0' COMMENT '文章总浏览量',
                                     `article_like_sum` bigint DEFAULT '0' COMMENT '文章总获赞数',
                                     `article_bookmark_sum` bigint DEFAULT '0' COMMENT '文章总收藏数',
                                     `article_word_sum` bigint DEFAULT '0' COMMENT '累计创作字数',
                                     `book_finished` int DEFAULT '0' COMMENT '读完的书',
                                     `booklist_count` int DEFAULT '0' COMMENT '创建书单数',
                                     `quote_count` int DEFAULT '0' COMMENT '发布金句数',
                                     `reading_minutes` bigint DEFAULT '0' COMMENT '累计阅读时长(分钟)',
                                     `question_solved` int DEFAULT '0' COMMENT '解题数',
                                     `note_count` int DEFAULT '0' COMMENT '笔记数',
                                     `experience_count` int DEFAULT '0' COMMENT '面经数',
                                     `note_adopted` int DEFAULT '0' COMMENT '笔记被精选数',
                                     `follower_count` int DEFAULT '0' COMMENT '粉丝数',
                                     `following_count` int DEFAULT '0' COMMENT '关注数',
                                     `comment_count` int DEFAULT '0' COMMENT '跨模块评论总数',
                                     `total_like_received` bigint DEFAULT '0' COMMENT '跨模块总获赞',
                                     `checkin_streak` int DEFAULT '0' COMMENT '连续签到天数',
                                     `last_checkin_date` date DEFAULT NULL COMMENT '最后签到日期',
                                     `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                     `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                     `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                     `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                     `mock_interview_count` int DEFAULT '0' COMMENT '模拟面试次数',
                                     `avg_mock_score` int DEFAULT '0' COMMENT '模拟面试平均分',
                                     `weak_tags` text COMMENT '薄弱知识点 JSON 数组（如 [{"tagId":1,"tagName":"Spring","failRate":0.6}]）',
                                     `weak_tags_updated_time` datetime DEFAULT NULL COMMENT '薄弱点最后计算时间',
                                     PRIMARY KEY (`id`),
                                     UNIQUE KEY `uk_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户用户统计聚合表';


--
-- Table structure for table `portal_user_task`
--

DROP TABLE IF EXISTS `portal_user_task`;
CREATE TABLE `portal_user_task` (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                    `user_id` bigint NOT NULL COMMENT '用户ID',
                                    `task_id` bigint NOT NULL COMMENT '任务ID',
                                    `progress` int NOT NULL DEFAULT '0' COMMENT '当前进度',
                                    `completed` tinyint NOT NULL DEFAULT '0' COMMENT '是否已完成 0/1',
                                    `claimed` tinyint NOT NULL DEFAULT '0' COMMENT '是否已领取奖励 0/1',
                                    `completed_time` datetime DEFAULT NULL COMMENT '完成时间',
                                    `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                    PRIMARY KEY (`id`),
                                    UNIQUE KEY `uk_user_task` (`user_id`,`task_id`),
                                    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户任务进度表';


--
-- Table structure for table `portal_vip_package`
--

DROP TABLE IF EXISTS `portal_vip_package`;
CREATE TABLE `portal_vip_package` (
                                      `id` bigint NOT NULL AUTO_INCREMENT COMMENT '套餐ID',
                                      `name` varchar(100) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '套餐名称',
                                      `price` decimal(10,2) NOT NULL COMMENT '价格',
                                      `original_price` decimal(10,2) DEFAULT NULL COMMENT '原价',
                                      `duration` int NOT NULL COMMENT '有效期（天）',
                                      `description` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '套餐描述',
                                      `features` json DEFAULT NULL COMMENT '功能列表（JSON数组）',
                                      `popular` tinyint(1) DEFAULT '0' COMMENT '是否热门',
                                      `sort` int DEFAULT '0' COMMENT '排序',
                                      `status` varchar(20) COLLATE utf8mb4_0900_ai_ci DEFAULT 'active' COMMENT '状态：active/inactive',
                                      `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                      `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                      `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                      `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                      `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                      PRIMARY KEY (`id`),
                                      KEY `idx_status` (`status`),
                                      KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户VIP套餐表';


DROP TABLE IF EXISTS `portal_wallet`;
CREATE TABLE `portal_wallet` (
                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT '钱包ID',
                                 `user_id` bigint NOT NULL COMMENT '用户ID（门户用户ID）',
                                 `balance` decimal(10,2) DEFAULT '0.00' COMMENT '余额',
                                 `frozen_balance` decimal(10,2) DEFAULT '0.00' COMMENT '冻结余额',
                                 `total_recharge` decimal(10,2) DEFAULT '0.00' COMMENT '累计充值',
                                 `total_withdraw` decimal(10,2) DEFAULT '0.00' COMMENT '累计提现',
                                 `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                 `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                 `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                 `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                 `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `uk_user_id` (`user_id`),
                                 KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户钱包表';


--
-- Table structure for table `portal_wallet_transaction`
--

DROP TABLE IF EXISTS `portal_wallet_transaction`;
CREATE TABLE `portal_wallet_transaction` (
                                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT '交易ID',
                                             `user_id` bigint NOT NULL COMMENT '用户ID（门户用户ID）',
                                             `type` varchar(50) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '类型：recharge/consume/refund/withdraw',
                                             `amount` decimal(10,2) NOT NULL COMMENT '金额',
                                             `balance_before` decimal(10,2) NOT NULL COMMENT '交易前余额',
                                             `balance_after` decimal(10,2) NOT NULL COMMENT '交易后余额',
                                             `description` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '描述',
                                             `order_id` bigint DEFAULT NULL COMMENT '关联订单ID',
                                             `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                             `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                             `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                             `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                             `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                             PRIMARY KEY (`id`),
                                             KEY `idx_user_id` (`user_id`),
                                             KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户钱包交易记录表';


--
-- Table structure for table `portal_writing_contest`
--

DROP TABLE IF EXISTS `portal_writing_contest`;
CREATE TABLE `portal_writing_contest` (
                                          `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                          `title` varchar(128) NOT NULL COMMENT '活动标题',
                                          `description` text COMMENT '活动描述',
                                          `theme` varchar(128) DEFAULT NULL COMMENT '征文主题',
                                          `cover` varchar(500) DEFAULT NULL COMMENT '封面',
                                          `start_time` datetime DEFAULT NULL COMMENT '活动开始时间',
                                          `end_time` datetime DEFAULT NULL COMMENT '投稿截止时间',
                                          `vote_end_time` datetime DEFAULT NULL COMMENT '投票截止时间',
                                          `prize` varchar(500) DEFAULT NULL COMMENT '奖品说明',
                                          `status` varchar(16) NOT NULL DEFAULT 'draft' COMMENT 'draft/collecting/voting/ended',
                                          `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                          `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                          `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                          PRIMARY KEY (`id`),
                                          KEY `idx_status` (`status`),
                                          KEY `idx_start_time` (`start_time`),
                                          KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='创作挑战/征文活动';


--
-- Table structure for table `portal_writing_prompt`
--

DROP TABLE IF EXISTS `portal_writing_prompt`;
CREATE TABLE `portal_writing_prompt` (
                                         `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                         `prompt_date` date NOT NULL COMMENT 'prompt 日期（唯一）',
                                         `title` varchar(128) NOT NULL COMMENT 'prompt 标题',
                                         `description` text COMMENT 'prompt 描述',
                                         `category` varchar(32) DEFAULT NULL COMMENT '分类（如：生活/职场/情感/虚构/哲思）',
                                         `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                         `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                         PRIMARY KEY (`id`),
                                         UNIQUE KEY `uk_prompt_date` (`prompt_date`),
                                         KEY `idx_category` (`category`),
                                         KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='每日写作 prompt';

--
-- Table structure for table `portal_wrong_question`
--

DROP TABLE IF EXISTS `portal_wrong_question`;
CREATE TABLE `portal_wrong_question` (
                                         `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                         `user_id` bigint NOT NULL COMMENT '用户ID',
                                         `question_id` bigint NOT NULL COMMENT '题目ID',
                                         `attempt_id` bigint DEFAULT NULL COMMENT '最近一次答题ID',
                                         `status` varchar(16) NOT NULL DEFAULT 'wrong' COMMENT '状态 wrong/reviewing/mastered',
                                         `wrong_count` int NOT NULL DEFAULT '1' COMMENT '答错次数',
                                         `last_wrong_time` datetime DEFAULT NULL COMMENT '最近答错时间',
                                         `next_review_time` datetime DEFAULT NULL COMMENT '下次复习时间（艾宾浩斯）',
                                         `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                         `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                         PRIMARY KEY (`id`),
                                         UNIQUE KEY `uk_user_question` (`user_id`,`question_id`),
                                         KEY `idx_user_status` (`user_id`,`status`),
                                         KEY `idx_user_review` (`user_id`,`next_review_time`),
                                         KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='错题本';


--
