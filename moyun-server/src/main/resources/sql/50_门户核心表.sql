-- 来源：all-db-ddl.sql 行1225-1853（已剔除 INSERT 种子数据，种子数据见 80 段）
-- 用途：门户核心表 DDL（portal_achievement / portal_ad_slot / portal_article* / portal_book* / portal_bookmark / portal_bookshelf / portal_category / portal_tag）

DROP TABLE IF EXISTS `portal_achievement`;
CREATE TABLE `portal_achievement` (
                                      `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
                                      `code` varchar(64) NOT NULL COMMENT '成就编码',
                                      `name` varchar(100) NOT NULL COMMENT '成就名称',
                                      `description` varchar(255) DEFAULT NULL COMMENT '成就描述',
                                      `icon` varchar(500) DEFAULT NULL COMMENT '图标URL',
                                      `module` varchar(32) DEFAULT NULL COMMENT '所属模块: article/reading/interview/all',
                                      `condition_json` text COMMENT '达成条件JSON',
                                      `growth_reward` int DEFAULT '0' COMMENT '达成奖励成长值',
                                      `sort` int DEFAULT '0' COMMENT '排序',
                                      `status` char(1) DEFAULT '0' COMMENT '状态（0启用 1停用）',
                                      `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                      `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                      `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                      `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                      PRIMARY KEY (`id`),
                                      UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='成就定义表';

--
-- Table structure for table `portal_ad_slot`
--

DROP TABLE IF EXISTS `portal_ad_slot`;
CREATE TABLE `portal_ad_slot` (
                                  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '广告位ID',
                                  `slot_key` varchar(64) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '广告位标识，如 article_detail_bottom',
                                  `title` varchar(100) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '广告标题',
                                  `image` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '广告图片URL',
                                  `link` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '点击跳转链接',
                                  `content` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '广告文案',
                                  `sort` int DEFAULT '0' COMMENT '排序',
                                  `status` varchar(1) COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '状态：0=启用 1=停用',
                                  `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                  `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                  `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                  PRIMARY KEY (`id`),
                                  KEY `idx_slot_key` (`slot_key`),
                                  KEY `idx_status` (`status`),
                                  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户自研广告位表';


--
-- Table structure for table `portal_article`
--

DROP TABLE IF EXISTS `portal_article`;
CREATE TABLE `portal_article` (
                                  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文章ID',
                                  `title` varchar(500) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文章标题',
                                  `slug` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '文章URL别名，用于SEO语义化路径',
                                  `content` longtext COLLATE utf8mb4_0900_ai_ci COMMENT '文章内容（HTML格式）',
                                  `excerpt` varchar(1000) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '文章摘要',
                                  `cover` text COLLATE utf8mb4_0900_ai_ci COMMENT '封面图片URL或Base64',
                                  `author_id` bigint NOT NULL COMMENT '作者ID（门户用户ID）',
                                  `category_id` bigint DEFAULT NULL COMMENT '分类ID',
                                  `root_category_id` bigint DEFAULT NULL COMMENT '顶级分类ID',
                                  `status` varchar(20) COLLATE utf8mb4_0900_ai_ci DEFAULT 'draft' COMMENT '状态：draft=草稿 / pending=待审核 / published=已发布 / rejected=已拒绝 / archived=已归档',
                                  `is_featured` tinyint(1) DEFAULT '0' COMMENT '是否精选',
                                  `is_top` tinyint(1) DEFAULT '0' COMMENT '是否置顶',
                                  `is_carousel` tinyint(1) DEFAULT '0' COMMENT '是否轮播',
                                  `is_category_recommended` tinyint(1) DEFAULT '0' COMMENT '是否栏目推荐',
                                  `views` bigint DEFAULT '0' COMMENT '浏览量',
                                  `likes` bigint DEFAULT '0' COMMENT '点赞数',
                                  `comments` bigint DEFAULT '0' COMMENT '评论数',
                                  `share_count` bigint DEFAULT '0' COMMENT '分享数',
                                  `bookmark_count` bigint DEFAULT '0' COMMENT '收藏数',
                                  `published_at` datetime DEFAULT NULL COMMENT '发布时间',
                                  `link` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '外部链接',
                                  `editor_mode` varchar(20) COLLATE utf8mb4_0900_ai_ci DEFAULT 'richtext' COMMENT '编辑器模式：richtext/markdown',
                                  `session_token` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '编辑会话标识（一次编辑会话唯一，用于草稿/发布幂等去重）',
                                  `content_markdown` text COLLATE utf8mb4_0900_ai_ci COMMENT 'Markdown 原始内容',
                                  `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                  `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                  `category_path` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '分类路径，包含所有祖先ID，例如：1,3,5',
                                  `is_paid` tinyint NOT NULL DEFAULT '0' COMMENT '是否付费阅读 0=免费 1=付费',
                                  `paid_content` longtext COLLATE utf8mb4_0900_ai_ci COMMENT '付费内容（购买后可见）',
                                  `preview_length` int NOT NULL DEFAULT '0' COMMENT '试读字数（未购买可预览的字数）',
                                  `price` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '付费价格，0=免费',
                                  `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                  PRIMARY KEY (`id`),
                                  UNIQUE KEY `uk_slug` (`slug`),
                                  KEY `idx_author_id` (`author_id`),
                                  KEY `idx_category_id` (`category_id`),
                                  KEY `idx_status` (`status`),
                                  KEY `idx_is_featured` (`is_featured`),
                                  KEY `idx_is_top` (`is_top`),
                                  KEY `idx_published_at` (`published_at`),
                                  KEY `idx_views` (`views`),
                                  KEY `idx_likes` (`likes`),
                                  KEY `idx_is_category_recommended` (`is_category_recommended`),
                                  KEY `idx_root_category_id` (`root_category_id`),
                                  KEY `idx_category_path` (`category_path`(100)),
                                  KEY `idx_session_token` (`session_token`),
                                  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户文章表';


--
-- Table structure for table `portal_article_tag`
--

DROP TABLE IF EXISTS `portal_article_tag`;
CREATE TABLE `portal_article_tag` (
                                      `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
                                      `article_id` bigint NOT NULL COMMENT '文章ID',
                                      `tag_id` bigint NOT NULL COMMENT '标签ID',
                                      `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                      `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                      `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                      `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                      PRIMARY KEY (`id`),
                                      UNIQUE KEY `uk_article_tag` (`article_id`,`tag_id`),
                                      KEY `idx_article_id` (`article_id`),
                                      KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文章标签关联表';


--
-- Table structure for table `portal_article_version`
--

DROP TABLE IF EXISTS `portal_article_version`;
CREATE TABLE `portal_article_version` (
                                          `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                          `article_id` bigint NOT NULL COMMENT '文章ID',
                                          `version_no` int NOT NULL COMMENT '版本号（同一文章内自增）',
                                          `title` varchar(256) NOT NULL COMMENT '版本标题快照',
                                          `content` longtext COMMENT '版本内容快照（HTML）',
                                          `content_markdown` longtext COMMENT '版本 Markdown 原始内容快照',
                                          `excerpt` varchar(500) DEFAULT NULL COMMENT '版本摘要快照',
                                          `operator_id` bigint DEFAULT NULL COMMENT '操作人ID（保存/回滚的执行者）',
                                          `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '版本创建时间',
                                          PRIMARY KEY (`id`),
                                          KEY `idx_article_version` (`article_id`,`version_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文章版本快照';


--
-- Table structure for table `portal_article_view`
--

DROP TABLE IF EXISTS `portal_article_view`;
CREATE TABLE `portal_article_view` (
                                       `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
                                       `article_id` bigint NOT NULL COMMENT '文章ID',
                                       `user_id` bigint DEFAULT NULL COMMENT '用户ID（NULL表示游客）',
                                       `ip` varchar(50) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'IP地址',
                                       `view_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
                                       `user_agent` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '浏览器User-Agent',
                                       `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                       `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                       `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                       `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                       `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                       PRIMARY KEY (`id`),
                                       KEY `idx_article_id` (`article_id`),
                                       KEY `idx_user_id` (`user_id`),
                                       KEY `idx_ip` (`ip`),
                                       KEY `idx_view_time` (`view_time`),
                                       KEY `idx_article_user` (`article_id`,`user_id`),
                                       KEY `idx_article_ip` (`article_id`,`ip`),
                                       KEY `idx_article_viewtime` (`article_id`,`view_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文章浏览记录表';


--
-- Table structure for table `portal_book`
--

DROP TABLE IF EXISTS `portal_book`;
CREATE TABLE `portal_book` (
                               `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                               `title` varchar(500) NOT NULL COMMENT '书名',
                               `author` varchar(200) NOT NULL COMMENT '作者',
                               `cover` varchar(500) DEFAULT NULL COMMENT '封面URL',
                               `description` text COMMENT '简介',
                               `isbn` varchar(50) DEFAULT NULL COMMENT 'ISBN',
                               `publisher` varchar(200) DEFAULT NULL COMMENT '出版社',
                               `publish_date` date DEFAULT NULL COMMENT '出版日期',
                               `page_count` int DEFAULT '0' COMMENT '页数',
                               `category_id` bigint DEFAULT NULL COMMENT '分类ID',
                               `tags` varchar(500) DEFAULT NULL COMMENT '标签，逗号分隔',
                               `rating` decimal(3,2) DEFAULT '0.00' COMMENT '评分',
                               `reading_count` bigint DEFAULT '0' COMMENT '阅读人数',
                               `status` varchar(20) DEFAULT 'active' COMMENT '状态:active,inactive',
                               `type` varchar(20) DEFAULT 'published' COMMENT '书籍类型：published=出版物，novel=网络小说，longform=长文',
                               `serial_status` varchar(20) DEFAULT 'completed' COMMENT '连载状态：ongoing=连载中，completed=已完结，hiatus=暂停更新',
                               `word_count` bigint DEFAULT '0' COMMENT '总字数（章节字数之和）',
                               `chapter_count` int DEFAULT '0' COMMENT '总章节数',
                               `latest_chapter_id` bigint DEFAULT NULL COMMENT '最新章节ID（用于追更展示）',
                               `latest_chapter_title` varchar(500) DEFAULT NULL COMMENT '最新章节标题',
                               `last_update_time` datetime DEFAULT NULL COMMENT '最后更新时间（章节发布时同步）',
                               `is_finished` tinyint(1) DEFAULT '1' COMMENT '是否完结：1=完结，0=连载中（冗余字段，便于查询）',
                               `access_level` varchar(20) DEFAULT 'free' COMMENT '访问级别:free,vip,preview',
                               `preview_ratio` int DEFAULT '30' COMMENT '免费试读比例（0-100）',
                               `price` decimal(10,2) DEFAULT '0.00' COMMENT '书籍单价（元）',
                               `is_featured` tinyint(1) DEFAULT '0' COMMENT '是否精选',
                               `is_recommended` tinyint(1) DEFAULT '0' COMMENT '是否推荐',
                               `summary` text COMMENT '简介（纯文本）',
                               `author_bio` text COMMENT '作者简介',
                               `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                               `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                               `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                               `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                               `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                               PRIMARY KEY (`id`),
                               KEY `idx_category_id` (`category_id`),
                               KEY `idx_status` (`status`),
                               KEY `idx_title` (`title`),
                               KEY `idx_access_level` (`access_level`),
                               KEY `idx_is_featured` (`is_featured`),
                               KEY `idx_is_recommended` (`is_recommended`),
                               KEY `idx_type` (`type`),
                               KEY `idx_serial_status` (`serial_status`),
                               KEY `idx_is_finished` (`is_finished`),
                               KEY `idx_word_count` (`word_count`),
                               KEY `idx_last_update_time` (`last_update_time`),
                               KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='书籍表';

--
-- Table structure for table `portal_book_chapter`
--

DROP TABLE IF EXISTS `portal_book_chapter`;
CREATE TABLE `portal_book_chapter` (
                                       `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                       `book_id` bigint NOT NULL COMMENT '所属书籍ID',
                                       `title` varchar(500) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '章节标题',
                                       `content` longtext COLLATE utf8mb4_0900_ai_ci COMMENT '章节正文（HTML格式，上限4GB）',
                                       `content_markdown` text COLLATE utf8mb4_0900_ai_ci COMMENT 'Markdown原始内容（上限64KB，单章足够）',
                                       `editor_mode` varchar(20) COLLATE utf8mb4_0900_ai_ci DEFAULT 'richtext' COMMENT '编辑器模式：richtext/markdown',
                                       `word_count` int DEFAULT '0' COMMENT '字数统计',
                                       `chapter_no` int NOT NULL DEFAULT '0' COMMENT '章节序号（用于排序，从1开始）',
                                       `volume_id` bigint DEFAULT NULL COMMENT '所属分卷ID（可选，支持分卷管理）',
                                       `is_free` tinyint(1) DEFAULT '1' COMMENT '是否免费：1=免费，0=VIP章节',
                                       `price` decimal(10,2) DEFAULT '0.00' COMMENT '章节单价（元，VIP章节购买）',
                                       `is_published` tinyint(1) DEFAULT '0' COMMENT '是否已发布：0=草稿，1=已发布',
                                       `publish_time` datetime DEFAULT NULL COMMENT '发布时间（支持定时发布）',
                                       `view_count` bigint DEFAULT '0' COMMENT '章节浏览量',
                                       `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                       `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                       `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                       `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                       `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                       `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                       PRIMARY KEY (`id`),
                                       UNIQUE KEY `uk_book_chapter_no` (`book_id`,`chapter_no`),
                                       KEY `idx_book_id` (`book_id`),
                                       KEY `idx_publish_time` (`publish_time`),
                                       KEY `idx_is_published` (`is_published`),
                                       KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='书籍章节表';

--
-- Table structure for table `portal_book_chapter_view`
--

DROP TABLE IF EXISTS `portal_book_chapter_view`;
CREATE TABLE `portal_book_chapter_view` (
                                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                            `chapter_id` bigint NOT NULL COMMENT '章节ID',
                                            `book_id` bigint NOT NULL COMMENT '书籍ID',
                                            `user_id` bigint DEFAULT NULL COMMENT '用户ID（未登录为NULL）',
                                            `client_ip` varchar(50) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '客户端IP',
                                            `read_duration_ms` int DEFAULT '0' COMMENT '阅读时长（毫秒）',
                                            `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
                                            `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                            PRIMARY KEY (`id`),
                                            KEY `idx_chapter_id` (`chapter_id`),
                                            KEY `idx_user_id` (`user_id`),
                                            KEY `idx_create_time` (`create_time`),
                                            KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='章节浏览记录表';


--
-- Table structure for table `portal_book_list`
--

DROP TABLE IF EXISTS `portal_book_list`;
CREATE TABLE `portal_book_list` (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                    `title` varchar(500) NOT NULL COMMENT '书单标题',
                                    `description` text COMMENT '书单简介',
                                    `cover` varchar(500) DEFAULT NULL COMMENT '封面URL',
                                    `user_id` bigint NOT NULL COMMENT '创建者ID',
                                    `category_id` bigint DEFAULT NULL COMMENT '分类ID',
                                    `is_public` tinyint(1) DEFAULT '1' COMMENT '是否公开',
                                    `book_count` int DEFAULT '0' COMMENT '书籍数量',
                                    `view_count` bigint DEFAULT '0' COMMENT '浏览数',
                                    `like_count` bigint DEFAULT '0' COMMENT '点赞数',
                                    `status` varchar(20) DEFAULT 'active' COMMENT '状态:active,inactive',
                                    `is_featured` tinyint(1) DEFAULT '0' COMMENT '是否精选',
                                    `access_level` varchar(20) DEFAULT 'free' COMMENT '访问级别:free,vip',
                                    `tags` varchar(500) DEFAULT NULL COMMENT '标签（逗号分隔）',
                                    `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                    PRIMARY KEY (`id`),
                                    KEY `idx_user_id` (`user_id`),
                                    KEY `idx_category_id` (`category_id`),
                                    KEY `idx_status` (`status`),
                                    KEY `idx_is_featured` (`is_featured`),
                                    KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='书单表';


--
-- Table structure for table `portal_book_list_bookmark`
--

DROP TABLE IF EXISTS `portal_book_list_bookmark`;
CREATE TABLE `portal_book_list_bookmark` (
                                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                             `booklist_id` bigint NOT NULL COMMENT '书单ID',
                                             `user_id` bigint NOT NULL COMMENT '用户ID',
                                             `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                             `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
                                             `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                             `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                             `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                             PRIMARY KEY (`id`),
                                             UNIQUE KEY `uk_booklist_user` (`booklist_id`,`user_id`),
                                             KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='书单收藏表';


--
-- Table structure for table `portal_book_list_item`
--

DROP TABLE IF EXISTS `portal_book_list_item`;
CREATE TABLE `portal_book_list_item` (
                                         `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                         `book_list_id` bigint NOT NULL COMMENT '书单ID',
                                         `book_id` bigint NOT NULL COMMENT '书籍ID',
                                         `sort` int DEFAULT '0' COMMENT '排序',
                                         `note` text COMMENT '添加说明',
                                         `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                         `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
                                         `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                         `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                         `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                         PRIMARY KEY (`id`),
                                         KEY `idx_book_list_id` (`book_list_id`),
                                         KEY `idx_book_id` (`book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='书单-书籍关联表';


--
-- Table structure for table `portal_book_list_like`
--

DROP TABLE IF EXISTS `portal_book_list_like`;
CREATE TABLE `portal_book_list_like` (
                                         `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                         `book_list_id` bigint NOT NULL COMMENT '书单ID',
                                         `user_id` bigint NOT NULL COMMENT '用户ID',
                                         `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                         `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
                                         `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                         `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                         `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                         PRIMARY KEY (`id`),
                                         UNIQUE KEY `uk_list_user` (`book_list_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='书单点赞表';


--
-- Table structure for table `portal_book_quote`
--

DROP TABLE IF EXISTS `portal_book_quote`;
CREATE TABLE `portal_book_quote` (
                                     `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                     `user_id` bigint NOT NULL COMMENT '用户ID',
                                     `book_id` bigint NOT NULL COMMENT '书籍ID',
                                     `chapter_id` bigint DEFAULT NULL COMMENT '章节ID（关联 portal_book_chapter）',
                                     `content` text NOT NULL COMMENT '金句内容',
                                     `page` varchar(100) DEFAULT NULL COMMENT '页码',
                                     `chapter` varchar(200) DEFAULT NULL COMMENT '章节',
                                     `like_count` bigint DEFAULT '0' COMMENT '点赞数',
                                     `is_public` tinyint(1) DEFAULT '1' COMMENT '是否公开',
                                     `is_featured` tinyint(1) DEFAULT '0' COMMENT '是否精选',
                                     `location` varchar(200) DEFAULT NULL COMMENT '章节标题/位置描述',
                                     `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                     `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                     `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                     `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                     `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                     PRIMARY KEY (`id`),
                                     KEY `idx_user_id` (`user_id`),
                                     KEY `idx_book_id` (`book_id`),
                                     KEY `idx_is_public` (`is_public`),
                                     KEY `idx_is_featured` (`is_featured`),
                                     KEY `idx_chapter_id` (`chapter_id`),
                                     KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='金句摘录表';

--
-- Table structure for table `portal_book_quote_like`
--

DROP TABLE IF EXISTS `portal_book_quote_like`;
CREATE TABLE `portal_book_quote_like` (
                                          `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                          `quote_id` bigint NOT NULL COMMENT '金句ID',
                                          `user_id` bigint NOT NULL COMMENT '用户ID',
                                          `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                          `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
                                          `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                          `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                          `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                          PRIMARY KEY (`id`),
                                          UNIQUE KEY `uk_quote_user` (`quote_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='金句点赞表';


--
-- Table structure for table `portal_book_recommend`
--

DROP TABLE IF EXISTS `portal_book_recommend`;
CREATE TABLE `portal_book_recommend` (
                                         `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                         `book_id` bigint NOT NULL COMMENT '书籍ID',
                                         `position` varchar(50) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '推荐位置：home_banner=首页轮播 / home_hot=首页热门 / category_top=分类顶推 / limit_free=限免专区 / discover_banner=发现页轮播',
                                         `sort` int DEFAULT '0' COMMENT '排序（越小越靠前）',
                                         `start_time` datetime DEFAULT NULL COMMENT '推荐开始时间（NULL 表示立即生效）',
                                         `end_time` datetime DEFAULT NULL COMMENT '推荐结束时间（NULL 表示长期有效）',
                                         `is_active` tinyint(1) DEFAULT '1' COMMENT '是否生效：1=生效，0=下架',
                                         `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注（运营说明）',
                                         `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                         `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                         `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                         `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                         `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                         PRIMARY KEY (`id`),
                                         UNIQUE KEY `uk_book_position` (`book_id`,`position`),
                                         KEY `idx_position` (`position`),
                                         KEY `idx_is_active` (`is_active`),
                                         KEY `idx_time_window` (`start_time`,`end_time`),
                                         KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='书籍推荐位表';

--
-- Table structure for table `portal_bookmark`
--

DROP TABLE IF EXISTS `portal_bookmark`;
CREATE TABLE `portal_bookmark` (
                                   `id` bigint NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户收藏表';


--
-- Table structure for table `portal_bookshelf`
--

DROP TABLE IF EXISTS `portal_bookshelf`;
CREATE TABLE `portal_bookshelf` (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                    `user_id` bigint NOT NULL COMMENT '用户ID',
                                    `book_id` bigint NOT NULL COMMENT '书籍ID',
                                    `last_chapter_id` bigint DEFAULT NULL COMMENT '最后阅读章节ID（冗余，用于续读）',
                                    `last_chapter_no` int DEFAULT '0' COMMENT '最后阅读章节序号',
                                    `sort` int DEFAULT '0' COMMENT '排序（用户自定义书架顺序，越大越靠前）',
                                    `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
                                    `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                    `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                    PRIMARY KEY (`id`),
                                    UNIQUE KEY `uk_user_book` (`user_id`,`book_id`),
                                    KEY `idx_user_id` (`user_id`),
                                    KEY `idx_book_id` (`book_id`),
                                    KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户书架（收藏书籍）表';


--
-- Table structure for table `portal_category`
--

DROP TABLE IF EXISTS `portal_category`;
CREATE TABLE `portal_category` (
                                   `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
                                   `name` varchar(100) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类名称',
                                   `slug` varchar(100) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '分类别名',
                                   `description` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '分类描述',
                                   `icon` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '图标URL',
                                   `sort` int DEFAULT '0' COMMENT '排序',
                                   `parent_id` bigint DEFAULT '0' COMMENT '父分类ID',
                                   `status` char(1) COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
                                   `show_in_nav` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否在头部栏目展示（0否/1是）',
                                   `nav_route_type` varchar(20) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'category' COMMENT '路由类型（home/category/static/external）',
                                   `nav_route_path` varchar(200) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '静态/外链路由路径（仅 static/external 类型使用）',
                                   `category_type` varchar(20) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'article' COMMENT '栏目内容类型（article=文章栏目可发布文章 special=特殊页面不发布文章）',
                                   `requires_auth` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否需要登录（0否/1是）',
                                   `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                                   `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                                   `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                   `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                                   `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                                   PRIMARY KEY (`id`),
                                   KEY `idx_parent_id` (`parent_id`),
                                   KEY `idx_slug` (`slug`),
                                   KEY `idx_show_in_nav` (`show_in_nav`),
                                   KEY `idx_category_type` (`category_type`),
                                   KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=51 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户分类表';

--
-- Table structure for table `portal_tag`
--

DROP TABLE IF EXISTS `portal_tag`;
CREATE TABLE `portal_tag` (
                              `id` bigint NOT NULL AUTO_INCREMENT COMMENT '标签ID',
                              `name` varchar(100) COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标签名称',
                              `slug` varchar(100) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '标签别名',
                              `sort` int DEFAULT '0' COMMENT '排序',
                              `status` char(1) COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
                              `module` varchar(50) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '所属模块（article/interview_question/interview_experience/interview_resume_template 等，null 表示通用）',
                              `reference_count` bigint unsigned DEFAULT '0' COMMENT '被引用次数（冗余计数列，绑定/解绑时同步维护）',
                              `create_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
                              `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              `update_by` varchar(64) COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
                              `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                              `remark` varchar(500) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
                              `del_flag` char(1) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
                              PRIMARY KEY (`id`),
                              UNIQUE KEY `uk_name` (`name`),
                              KEY `idx_slug` (`slug`),
                              KEY `idx_module` (`module`),
                              KEY `idx_reference_count` (`reference_count` DESC),
                              KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户标签表';
