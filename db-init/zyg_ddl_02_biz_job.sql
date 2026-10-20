-- ==============================================================================
-- 墨云项目 · 数据库初始化 DDL（第 2 / 2 部分）
-- 覆盖模块：门户内容 portal_* → 记账 ledger_* → 已下线归档 vip_/pay_ → 定时任务 sys_job + qrtz_*
-- 说明：定时任务（sys_job 与 Quartz 11 张表）之间存在外键，必须排在最后创建
-- 幂等重建：每张表 = DROP TABLE IF EXISTS + CREATE TABLE，可反复执行
-- 执行命令：mysql -uroot -p --default-character-set=utf8mb4 < zyg_ddl_02_biz_job.sql
-- 风险提示：DROP TABLE 会清空同名表；仅用于全新库初始化或整库重建，务必先备份
-- ==============================================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
SET UNIQUE_CHECKS = 0;
SET SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO';
SET @OLD_SQL_NOTES = @@SQL_NOTES, SQL_NOTES = 0;


-- ==============================================================================
-- 四、门户内容模块（portal_*）：内容 / 社区 / 面试 / 简历 / 成长 / 消息
-- 模块优先级：业务主模块。
-- ==============================================================================

-- ----------------------------------------------------------------------------
-- 表：portal_category    [门户分类表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_category`;
CREATE TABLE `portal_category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类名称',
  `slug` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '分类别名',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '分类描述',
  `icon` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '图标URL',
  `sort` int DEFAULT '0' COMMENT '排序',
  `parent_id` bigint DEFAULT '0' COMMENT '父分类ID',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `show_in_nav` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否在头部栏目展示（0否/1是）',
  `nav_route_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'category' COMMENT '路由类型（home/category/static/external）',
  `nav_route_path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '静态/外链路由路径（仅 static/external 类型使用）',
  `nav_badge` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '导航徽章（NEW/HOT，仅 Mega Menu 展示）',
  `category_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'article' COMMENT '栏目内容类型（article=文章分类可发布文章 directory=目录容器仅组织子栏目不发布文章 special=静态页面不发布文章）',
  `requires_auth` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否需要登录（0否/1是）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_slug` (`slug`),
  KEY `idx_show_in_nav` (`show_in_nav`),
  KEY `idx_category_type` (`category_type`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户分类表';


-- ----------------------------------------------------------------------------
-- 表：portal_article    [门户文章表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_article`;
CREATE TABLE `portal_article` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文章标题',
  `slug` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '文章URL别名，用于SEO语义化路径',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '文章内容（HTML格式）',
  `excerpt` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '文章摘要',
  `cover` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '封面图片URL或Base64',
  `author_id` bigint NOT NULL COMMENT '作者ID（门户用户ID）',
  `category_id` bigint DEFAULT NULL COMMENT '分类ID',
  `root_category_id` bigint DEFAULT NULL COMMENT '顶级分类ID',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'draft' COMMENT '状态：draft=草稿 / pending=待审核 / published=已发布 / rejected=已拒绝 / archived=已归档',
  `auditor_id` bigint DEFAULT NULL COMMENT '审核人ID（系统用户ID）',
  `audit_remark` varchar(500) DEFAULT NULL COMMENT '审核意见/驳回原因',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
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
  `link` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '外部链接',
  `editor_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'richtext' COMMENT '编辑器模式：richtext/markdown',
  `session_token` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '编辑会话标识（一次编辑会话唯一，用于草稿/发布幂等去重）',
  `content_markdown` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT 'Markdown 原始内容',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `category_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '分类路径，包含所有祖先ID，例如：1,3,5',
  `is_paid` tinyint NOT NULL DEFAULT '0' COMMENT '是否付费阅读 0=免费 1=付费',
  `paid_content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '付费内容（购买后可见）',
  `preview_length` int NOT NULL DEFAULT '0' COMMENT '试读字数（未购买可预览的字数）',
  `seo_title` varchar(200) DEFAULT NULL COMMENT 'SEO 自定义标题（留空回退文章标题）',
  `seo_description` varchar(500) DEFAULT NULL COMMENT 'SEO 自定义描述（留空回退摘要）',
  `seo_keywords` varchar(300) DEFAULT NULL COMMENT 'SEO 关键词（逗号分隔）',
  `price` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '付费价格，0=免费',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
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
  KEY `idx_del_flag` (`del_flag`),
  KEY `idx_auditor_id` (`auditor_id`),
  KEY `idx_status_published_at` (`status`,`published_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户文章表';


-- ----------------------------------------------------------------------------
-- 表：portal_article_version    [文章版本快照]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_article_version`;
CREATE TABLE `portal_article_version` (
  `id` bigint NOT NULL AUTO_INCREMENT,
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


-- ----------------------------------------------------------------------------
-- 表：portal_article_view    [文章浏览记录表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_article_view`;
CREATE TABLE `portal_article_view` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID（NULL表示游客）',
  `ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'IP地址',
  `view_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
  `user_agent` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '浏览器User-Agent',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_article_id` (`article_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_ip` (`ip`),
  KEY `idx_view_time` (`view_time`),
  KEY `idx_article_user` (`article_id`,`user_id`),
  KEY `idx_article_ip` (`article_id`,`ip`),
  KEY `idx_article_viewtime` (`article_id`,`view_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文章浏览记录表';


-- ----------------------------------------------------------------------------
-- 表：portal_tag    [门户标签表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_tag`;
CREATE TABLE `portal_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标签名称',
  `slug` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '标签别名',
  `sort` int DEFAULT '0' COMMENT '排序',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `module` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '所属模块（article/interview_question/interview_experience/interview_resume_template 等，null 表示通用）',
  `reference_count` bigint unsigned DEFAULT '0' COMMENT '被引用次数（冗余计数列，绑定/解绑时同步维护）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_slug` (`slug`),
  KEY `idx_module` (`module`),
  KEY `idx_reference_count` (`reference_count` DESC),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户标签表';


-- ----------------------------------------------------------------------------
-- 表：portal_entity_tag    [通用实体标签关联表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_entity_tag`;
CREATE TABLE `portal_entity_tag` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `tag_id` bigint unsigned NOT NULL COMMENT '标签ID（引用 portal_tag.id）',
  `entity_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '实体类型（article/interview_question/interview_experience/interview_resume_template/book 等）',
  `entity_id` bigint unsigned NOT NULL COMMENT '实体ID',
  `sort` int DEFAULT '0' COMMENT '排序',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag_entity` (`tag_id`,`entity_type`,`entity_id`),
  KEY `idx_entity` (`entity_type`,`entity_id`),
  KEY `idx_entity_create` (`entity_type`,`create_time`),
  KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通用实体标签关联表';


-- ----------------------------------------------------------------------------
-- 表：portal_column    [专栏]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_column`;
CREATE TABLE `portal_column` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '创作者',
  `title` varchar(128) NOT NULL COMMENT '专栏名',
  `subtitle` varchar(256) DEFAULT NULL COMMENT '副标题',
  `description` text COMMENT '专栏简介',
  `cover` varchar(500) DEFAULT NULL COMMENT '封面',
  `category_id` bigint DEFAULT NULL COMMENT '分类',
  `status` varchar(16) NOT NULL DEFAULT 'draft' COMMENT '状态：draft 草稿/pending 待审核/published 已发布/archived 归档/rejected 审核驳回',
  `auditor_id` bigint DEFAULT NULL COMMENT '审核人ID（系统用户ID）',
  `audit_remark` varchar(500) DEFAULT NULL COMMENT '审核意见/驳回原因',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `article_count` int NOT NULL DEFAULT '0' COMMENT '文章数',
  `subscribe_count` int NOT NULL DEFAULT '0' COMMENT '订阅数',
  `view_count` int NOT NULL DEFAULT '0' COMMENT '浏览数',
  `is_finished` tinyint NOT NULL DEFAULT '0' COMMENT '是否完结',
  `price` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '专栏会员价，0=免费',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_del_flag` (`del_flag`),
  KEY `idx_auditor_id` (`auditor_id`),
  KEY `idx_status_created_time` (`status`,`created_time`),
  KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='专栏';


-- ----------------------------------------------------------------------------
-- 表：portal_column_article    [专栏-文章关联]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_column_article`;
CREATE TABLE `portal_column_article` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `column_id` bigint NOT NULL COMMENT '专栏ID',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '专栏内顺序',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_column_article` (`column_id`,`article_id`),
  KEY `idx_column_sort` (`column_id`,`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='专栏-文章关联';


-- ----------------------------------------------------------------------------
-- 表：portal_column_subscribe    [专栏订阅]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_column_subscribe`;
CREATE TABLE `portal_column_subscribe` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `column_id` bigint NOT NULL COMMENT '专栏ID',
  `user_id` bigint NOT NULL COMMENT '订阅用户ID',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_column_user` (`column_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='专栏订阅';


-- ----------------------------------------------------------------------------
-- 表：portal_comment    [门户评论表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_comment`;
CREATE TABLE `portal_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `author_id` bigint NOT NULL COMMENT '评论者ID（门户用户ID）',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '评论内容',
  `parent_id` bigint DEFAULT '0' COMMENT '父评论ID',
  `root_id` bigint DEFAULT '0' COMMENT '根评论ID（一级评论ID）',
  `reply_to` bigint DEFAULT NULL COMMENT '回复的用户ID',
  `reply_to_content` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '被回复的内容摘要',
  `like_count` bigint DEFAULT '0' COMMENT '点赞数',
  `status` char(1) DEFAULT '1' COMMENT '状态：0=待审核 1=已发布 2=审核驳回',
  `auditor_id` bigint DEFAULT NULL COMMENT '审核人ID（系统用户ID，CMS审核时写入）',
  `audit_remark` varchar(500) DEFAULT NULL COMMENT '审核意见/驳回原因（独立字段）',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  KEY `idx_article_id` (`article_id`),
  KEY `idx_author_id` (`author_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_article_root` (`article_id`,`root_id`),
  KEY `idx_del_flag` (`del_flag`),
  KEY `idx_auditor_id` (`auditor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户评论表';


-- ----------------------------------------------------------------------------
-- 表：portal_comment_like    [门户评论点赞表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_comment_like`;
CREATE TABLE `portal_comment_like` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID（门户用户ID）',
  `comment_id` bigint NOT NULL COMMENT '评论ID',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_comment` (`user_id`,`comment_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_comment_id` (`comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户评论点赞表';


-- ----------------------------------------------------------------------------
-- 表：portal_like    [门户点赞表（文章）]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_like`;
CREATE TABLE `portal_like` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID（门户用户ID）',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_article` (`user_id`,`article_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_article_id` (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户点赞表（文章）';


-- ----------------------------------------------------------------------------
-- 表：portal_bookmark    [门户收藏表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_bookmark`;
CREATE TABLE `portal_bookmark` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID（门户用户ID）',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_article` (`user_id`,`article_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_article_id` (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户收藏表';


-- ----------------------------------------------------------------------------
-- 表：portal_book    [书籍表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_book`;
CREATE TABLE `portal_book` (
  `id` bigint NOT NULL AUTO_INCREMENT,
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
  `price` decimal(18,2) DEFAULT '0.00' COMMENT '书籍单价（元）',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='书籍表';


-- ----------------------------------------------------------------------------
-- 表：portal_book_chapter    [书籍章节表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_book_chapter`;
CREATE TABLE `portal_book_chapter` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `book_id` bigint NOT NULL COMMENT '所属书籍ID',
  `title` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '章节标题',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '章节正文（HTML格式，上限4GB）',
  `content_markdown` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT 'Markdown原始内容（上限64KB，单章足够）',
  `editor_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'richtext' COMMENT '编辑器模式：richtext/markdown',
  `word_count` int DEFAULT '0' COMMENT '字数统计',
  `chapter_no` int NOT NULL DEFAULT '0' COMMENT '章节序号（用于排序，从1开始）',
  `volume_id` bigint DEFAULT NULL COMMENT '所属分卷ID（可选，支持分卷管理）',
  `is_free` tinyint(1) DEFAULT '1' COMMENT '是否免费：1=免费，0=VIP章节',
  `price` decimal(18,2) DEFAULT '0.00' COMMENT '章节单价（元，VIP章节购买）',
  `is_published` tinyint(1) DEFAULT '0' COMMENT '是否已发布：0=草稿，1=已发布',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间（支持定时发布）',
  `view_count` bigint DEFAULT '0' COMMENT '章节浏览量',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_book_chapter_no` (`book_id`,`chapter_no`),
  KEY `idx_book_id` (`book_id`),
  KEY `idx_publish_time` (`publish_time`),
  KEY `idx_is_published` (`is_published`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='书籍章节表';


-- ----------------------------------------------------------------------------
-- 表：portal_book_quote    [金句摘录表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_book_quote`;
CREATE TABLE `portal_book_quote` (
  `id` bigint NOT NULL AUTO_INCREMENT,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='金句摘录表';


-- ----------------------------------------------------------------------------
-- 表：portal_book_quote_like    [金句点赞表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_book_quote_like`;
CREATE TABLE `portal_book_quote_like` (
  `id` bigint NOT NULL AUTO_INCREMENT,
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


-- ----------------------------------------------------------------------------
-- 表：portal_book_recommend    [书籍推荐位表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_book_recommend`;
CREATE TABLE `portal_book_recommend` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `book_id` bigint NOT NULL COMMENT '书籍ID',
  `position` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '推荐位置：home_banner=首页轮播 / home_hot=首页热门 / category_top=分类顶推 / limit_free=限免专区 / discover_banner=发现页轮播',
  `sort` int DEFAULT '0' COMMENT '排序（越小越靠前）',
  `start_time` datetime DEFAULT NULL COMMENT '推荐开始时间（NULL 表示立即生效）',
  `end_time` datetime DEFAULT NULL COMMENT '推荐结束时间（NULL 表示长期有效）',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否生效：1=生效，0=下架',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注（运营说明）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_book_position` (`book_id`,`position`),
  KEY `idx_position` (`position`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_time_window` (`start_time`,`end_time`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='书籍推荐位表';


-- ----------------------------------------------------------------------------
-- 表：portal_bookshelf    [用户书架（收藏书籍）表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_bookshelf`;
CREATE TABLE `portal_bookshelf` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `book_id` bigint NOT NULL COMMENT '书籍ID',
  `last_chapter_id` bigint DEFAULT NULL COMMENT '最后阅读章节ID（冗余，用于续读）',
  `last_chapter_no` int DEFAULT '0' COMMENT '最后阅读章节序号',
  `sort` int DEFAULT '0' COMMENT '排序（用户自定义书架顺序，越大越靠前）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_book` (`user_id`,`book_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_book_id` (`book_id`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户书架（收藏书籍）表';


-- ----------------------------------------------------------------------------
-- 表：portal_book_list    [书单表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_book_list`;
CREATE TABLE `portal_book_list` (
  `id` bigint NOT NULL AUTO_INCREMENT,
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


-- ----------------------------------------------------------------------------
-- 表：portal_book_list_item    [书单-书籍关联表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_book_list_item`;
CREATE TABLE `portal_book_list_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
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


-- ----------------------------------------------------------------------------
-- 表：portal_book_list_bookmark    [书单收藏表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_book_list_bookmark`;
CREATE TABLE `portal_book_list_bookmark` (
  `id` bigint NOT NULL AUTO_INCREMENT,
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


-- ----------------------------------------------------------------------------
-- 表：portal_book_list_like    [书单点赞表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_book_list_like`;
CREATE TABLE `portal_book_list_like` (
  `id` bigint NOT NULL AUTO_INCREMENT,
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


-- ----------------------------------------------------------------------------
-- 表：portal_reading_preference    [用户阅读偏好表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_reading_preference`;
CREATE TABLE `portal_reading_preference` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `font_size` int DEFAULT '18' COMMENT '正文字号（px，12-32）',
  `line_height` decimal(3,1) DEFAULT '1.8' COMMENT '行距（倍，1.2-3.0）',
  `theme` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'default' COMMENT '阅读主题：default=跟随 / light=亮色 / dark=暗色 / sepia=护眼黄',
  `font_family` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'system' COMMENT '字体：system=系统默认 / serif=衬线 / song=宋体 / hei=黑体',
  `letter_spacing` decimal(3,1) DEFAULT '0.0' COMMENT '字间距（px，-1.0-5.0）',
  `paragraph_spacing` decimal(4,1) DEFAULT '1.2' COMMENT '段间距（em，0.5-5.0）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户阅读偏好表';


-- ----------------------------------------------------------------------------
-- 表：portal_reading_progress    [阅读进度表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_reading_progress`;
CREATE TABLE `portal_reading_progress` (
  `id` bigint NOT NULL AUTO_INCREMENT,
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


-- ----------------------------------------------------------------------------
-- 表：portal_topic    [话题主表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_topic`;
CREATE TABLE `portal_topic` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(128) NOT NULL COMMENT '话题标题',
  `description` varchar(500) DEFAULT NULL COMMENT '话题描述/导语',
  `cover` varchar(500) DEFAULT NULL COMMENT '封面图 URL',
  `creator_id` bigint NOT NULL COMMENT '发起人 portal_user.id（必须是认证创作者）',
  `status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending 待审核/active 活跃/archived 归档/deleted 删除/rejected 审核驳回',
  `auditor_id` bigint DEFAULT NULL COMMENT '审核人ID（系统用户ID）',
  `audit_remark` varchar(500) DEFAULT NULL COMMENT '审核意见/驳回原因',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
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
  KEY `idx_del_flag` (`del_flag`),
  KEY `idx_auditor_id` (`auditor_id`),
  KEY `idx_status_created_time` (`status`,`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='话题主表';


-- ----------------------------------------------------------------------------
-- 表：portal_topic_post    [话题观点（楼层）]
-- ----------------------------------------------------------------------------
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
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_topic_floor` (`topic_id`,`floor`),
  KEY `idx_topic_time` (`topic_id`,`created_time`),
  KEY `idx_user_time` (`user_id`,`created_time`),
  KEY `idx_parent` (`parent_post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='话题观点（楼层）';


-- ----------------------------------------------------------------------------
-- 表：portal_topic_post_like    [话题观点点赞]
-- ----------------------------------------------------------------------------
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


-- ----------------------------------------------------------------------------
-- 表：portal_topic_comment    [话题评论（多态）]
-- ----------------------------------------------------------------------------
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
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_target_type_id_parent` (`target_type`,`target_id`,`parent_id`,`created_time`),
  KEY `idx_root` (`root_id`,`created_time`),
  KEY `idx_author_time` (`author_id`,`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='话题评论（多态）';


-- ----------------------------------------------------------------------------
-- 表：portal_topic_comment_like    [话题评论点赞]
-- ----------------------------------------------------------------------------
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


-- ----------------------------------------------------------------------------
-- 表：portal_topic_like    [话题点赞]
-- ----------------------------------------------------------------------------
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


-- ----------------------------------------------------------------------------
-- 表：portal_feed_event    [动态事件流]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_feed_event`;
CREATE TABLE `portal_feed_event` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '事件发布者',
  `event_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'publish_article/publish_experience/new_column/checkin等',
  `target_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'article/experience/column/book等',
  `target_id` bigint NOT NULL COMMENT '目标对象ID',
  `title` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '目标标题',
  `summary` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '动态摘要',
  `cover` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '封面图',
  `created_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`,`created_time`),
  KEY `idx_type_time` (`event_type`,`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='动态事件流';


-- ----------------------------------------------------------------------------
-- 表：portal_feed_inbox    [动态收件箱]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_feed_inbox`;
CREATE TABLE `portal_feed_inbox` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '接收者',
  `event_id` bigint NOT NULL COMMENT '动态事件ID',
  `created_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`,`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='动态收件箱';


-- ----------------------------------------------------------------------------
-- 表：portal_follow    [门户关注表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_follow`;
CREATE TABLE `portal_follow` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `follower_id` bigint NOT NULL COMMENT '关注者ID（门户用户ID）',
  `following_id` bigint NOT NULL COMMENT '被关注者ID（门户用户ID）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_follower_following` (`follower_id`,`following_id`),
  KEY `idx_follower_id` (`follower_id`),
  KEY `idx_following_id` (`following_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户关注表';


-- ----------------------------------------------------------------------------
-- 表：portal_friend_link    [门户友情链接表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_friend_link`;
CREATE TABLE `portal_friend_link` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '链接名称',
  `url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '链接地址',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '链接描述',
  `logo` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'Logo URL',
  `sort` int DEFAULT '0' COMMENT '排序',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '状态：0正常 1停用',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标记（0=存在 2=删除，与全局逻辑删除配置一致）',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户友情链接表';


-- ----------------------------------------------------------------------------
-- 表：portal_achievement    [成就定义表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_achievement`;
CREATE TABLE `portal_achievement` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='成就定义表';


-- ----------------------------------------------------------------------------
-- 表：portal_user_badge    [用户徽章记录表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_user_badge`;
CREATE TABLE `portal_user_badge` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
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


-- ----------------------------------------------------------------------------
-- 表：portal_growth_rule    [成长规则配置表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_growth_rule`;
CREATE TABLE `portal_growth_rule` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='成长规则配置表';


-- ----------------------------------------------------------------------------
-- 表：portal_growth_log    [成长事件流水表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_growth_log`;
CREATE TABLE `portal_growth_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
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


-- ----------------------------------------------------------------------------
-- 表：portal_user_growth    [用户成长值总表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_user_growth`;
CREATE TABLE `portal_user_growth` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
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


-- ----------------------------------------------------------------------------
-- 表：portal_help_category    [帮助中心分类表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_help_category`;
CREATE TABLE `portal_help_category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类名称',
  `icon` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '图标（lucide 图标名）',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '分类描述',
  `sort` int DEFAULT '0' COMMENT '排序（升序）',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'active' COMMENT '状态：active/inactive',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_sort` (`sort`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='帮助中心分类表';


-- ----------------------------------------------------------------------------
-- 表：portal_help_article    [帮助中心文章表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_help_article`;
CREATE TABLE `portal_help_article` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category_id` bigint NOT NULL COMMENT '分类ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '问题标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '答案内容（支持纯文本）',
  `view_count` int DEFAULT '0' COMMENT '查看次数',
  `like_count` int DEFAULT '0' COMMENT '点赞次数',
  `sort` int DEFAULT '0' COMMENT '排序（升序）',
  `is_featured` tinyint DEFAULT '0' COMMENT '是否精选：0=否 1=是',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'published' COMMENT '状态：published/draft',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_status` (`status`),
  KEY `idx_is_featured` (`is_featured`),
  KEY `idx_sort` (`sort`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='帮助中心文章表';


-- ----------------------------------------------------------------------------
-- 表：portal_import_template_config    [导入模板字段配置（动态模板）]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_import_template_config`;
CREATE TABLE `portal_import_template_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `business_key` varchar(64) NOT NULL COMMENT '业务标识（interview_question/interview_experience/article/tag/note）',
  `field_name` varchar(64) NOT NULL COMMENT '实体字段名（Java 属性名）',
  `column_name` varchar(64) NOT NULL COMMENT 'Excel 列名（中文表头）',
  `description` varchar(255) DEFAULT NULL COMMENT '字段说明',
  `example_value` varchar(255) DEFAULT NULL COMMENT '示例值',
  `required` tinyint DEFAULT '0' COMMENT '是否必填：1=必填 0=可选',
  `field_type` varchar(20) DEFAULT 'string' COMMENT '字段类型：string/number/date/dict',
  `dict_type` varchar(64) DEFAULT NULL COMMENT '字典 type（field_type=dict 时生效）',
  `combo_values` varchar(500) DEFAULT NULL COMMENT '下拉可选值（逗号分隔）',
  `column_width` int DEFAULT '20' COMMENT 'Excel 列宽',
  `sort` int DEFAULT '0' COMMENT '列排序号',
  `status` char(1) DEFAULT '0' COMMENT '状态：0=启用 1=停用',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_business_key_status_sort` (`business_key`,`status`,`sort`),
  KEY `idx_business_key_field` (`business_key`,`field_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='导入模板字段配置（动态模板）';


-- ----------------------------------------------------------------------------
-- 表：portal_interview_category    [面试题目分类表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_interview_category`;
CREATE TABLE `portal_interview_category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(200) NOT NULL COMMENT '分类名称',
  `slug` varchar(200) DEFAULT NULL COMMENT '分类标识',
  `bank_type` varchar(32) NOT NULL DEFAULT 'interview' COMMENT '题库类型：interview=面试/certification=职业资格/civil-service=公务员/postgraduate=考研/other=其他',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '上级分类ID（0=顶级）',
  `job_family` varchar(64) DEFAULT NULL COMMENT '职业族群：后端/前端/测试/产品/运维…',
  `description` text COMMENT '分类描述',
  `icon` varchar(500) DEFAULT NULL COMMENT '图标URL',
  `sort` int DEFAULT '0' COMMENT '排序',
  `question_count` int DEFAULT '0' COMMENT '题目数量',
  `status` varchar(20) DEFAULT 'active' COMMENT '状态:active,inactive',
  `visible` char(1) NOT NULL DEFAULT '0' COMMENT '前台展示：0=展示 1=隐藏',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_bank_type` (`bank_type`,`visible`,`sort`),
  KEY `idx_parent_id` (`parent_id`,`sort`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面试题目分类表';


-- ----------------------------------------------------------------------------
-- 表：portal_interview_company    [面试公司标签表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_interview_company`;
CREATE TABLE `portal_interview_company` (
  `id` bigint NOT NULL AUTO_INCREMENT,
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


-- ----------------------------------------------------------------------------
-- 表：portal_interview_config    [面试配置表（人设/提示词/评分权重/追问策略/自我介绍）]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_interview_config`;
CREATE TABLE `portal_interview_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `config_name` varchar(100) NOT NULL COMMENT '配置名称（如：标准技术面）',
  `persona_type` varchar(20) NOT NULL DEFAULT 'professional' COMMENT '面试官人设:professional/friendly/strict（对齐 voice_interview_style）',
  `prompt_template` text COMMENT '面试官提示词模板（支持 {{position}}/{{resumeDigest}} 等占位符，空则用 Agent 人设）',
  `scoring_weights` varchar(500) DEFAULT '{"relevance":20,"depth":25,"fluency":20,"logic":20,"confidence":15,"fusion":0.7,"selfIntro":{"structure":30,"selfAwareness":25,"jobMatch":25,"fluency":20}}' COMMENT '评分权重 JSON（5技术维度+LLM融合比例+自我介绍4维度）',
  `question_weights` varchar(100) DEFAULT '{"job":40,"resume":30,"weak":20,"random":10}' COMMENT '出题权重 JSON',
  `max_followups` int NOT NULL DEFAULT '2' COMMENT '每题最大追问次数',
  `followup_triggers` varchar(200) DEFAULT '["vague_answer","contradiction","depth_needed"]' COMMENT '追问触发条件 JSON（模糊回答/前后矛盾/需深挖）',
  `enable_self_intro` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否启用自我介绍环节（0=旧流程兼容默认）',
  `self_intro_duration` int NOT NULL DEFAULT '180' COMMENT '自我介绍建议时长（秒）',
  `is_default` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否默认配置（互斥，全局唯一）',
  `status` varchar(20) NOT NULL DEFAULT 'active' COMMENT '状态:active 启用/inactive 停用',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  KEY `idx_is_default` (`is_default`),
  KEY `idx_status` (`status`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面试配置表（人设/提示词/评分权重/追问策略/自我介绍）';


-- ----------------------------------------------------------------------------
-- 表：portal_interview_question    [面试题目表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_interview_question`;
CREATE TABLE `portal_interview_question` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(500) NOT NULL COMMENT '题目标题',
  `description` text COMMENT '题目描述',
  `difficulty` varchar(20) DEFAULT 'medium' COMMENT '难度:easy,medium,hard',
  `category_id` bigint DEFAULT NULL COMMENT '分类ID',
  `job_template_id` bigint DEFAULT NULL COMMENT '所属岗位模板ID（portal_job_template.id）',
  `tags` varchar(500) DEFAULT NULL COMMENT '标签，逗号分隔',
  `companies` varchar(500) DEFAULT NULL COMMENT '公司，逗号分隔',
  `acceptance_rate` decimal(5,2) DEFAULT '0.00' COMMENT '通过率',
  `submission_count` bigint DEFAULT '0' COMMENT '提交次数',
  `like_count` bigint DEFAULT '0' COMMENT '点赞数',
  `hint` text COMMENT '提示',
  `solution` text COMMENT '参考答案（代码题参考代码片段）',
  `sort` int DEFAULT '0' COMMENT '排序',
  `status` varchar(20) DEFAULT 'active' COMMENT '状态:active,inactive',
  `question_type` varchar(50) DEFAULT NULL COMMENT '题目类型:bagwen八股/algorithm算法/system_design系统设计/project项目/hr',
  `examine_points` text COMMENT '考察点列表JSON数组',
  `answer_outline` text COMMENT '答题大纲Markdown',
  `scoring_criteria` text COMMENT '评分标准JSON数组',
  `reference_answer` text COMMENT '官方参考答案Markdown（八股/设计/项目/HR类完整答案）',
  `prerequisite_ids` varchar(500) DEFAULT NULL COMMENT '前置题目ID，逗号分隔',
  `practice_mode` varchar(20) DEFAULT 'reading' COMMENT '练习模式：reading=展示阅读/choice=选择题/coding=编程题',
  `options` json DEFAULT NULL COMMENT '选择题选项JSON数组：[{label,text,is_correct}]',
  `correct_answer` varchar(50) DEFAULT NULL COMMENT '正确答案（选择题：选项label如B；编程题：null靠测试用例判定）',
  `analysis` text COMMENT '题目解析（做题后展示，区别于 reference_answer 参考答案）',
  `knowledge_tags` varchar(500) DEFAULT NULL COMMENT '知识点标签，逗号分隔（用于错题本/学习路径聚类）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_difficulty` (`difficulty`),
  KEY `idx_question_type` (`question_type`),
  KEY `idx_status` (`status`),
  KEY `idx_del_flag` (`del_flag`),
  KEY `idx_practice_mode` (`practice_mode`),
  KEY `idx_job_template_id` (`job_template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面试题目表';


-- ----------------------------------------------------------------------------
-- 表：portal_interview_question_company    [题目-公司关联表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_interview_question_company`;
CREATE TABLE `portal_interview_question_company` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `question_id` bigint NOT NULL COMMENT '题目ID',
  `company_id` bigint NOT NULL COMMENT '公司ID',
  `sort` int DEFAULT '0' COMMENT '排序',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_question_company` (`question_id`,`company_id`),
  KEY `idx_company_id` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目-公司关联表';


-- ----------------------------------------------------------------------------
-- 表：portal_interview_question_like    [题目点赞表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_interview_question_like`;
CREATE TABLE `portal_interview_question_like` (
  `id` bigint NOT NULL AUTO_INCREMENT,
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


-- ----------------------------------------------------------------------------
-- 表：portal_interview_question_test_case    [面试题目测试用例表（OJ判题）]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_interview_question_test_case`;
CREATE TABLE `portal_interview_question_test_case` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `question_id` bigint NOT NULL COMMENT '题目ID',
  `input` text COMMENT '标准输入（运行时通过stdin传入）',
  `expected_output` text NOT NULL COMMENT '期望输出',
  `is_sample` tinyint(1) DEFAULT '0' COMMENT '是否样例:1=样例（前端展示）/0=隐藏',
  `order_num` int DEFAULT '0' COMMENT '用例排序',
  `explanation` varchar(1000) DEFAULT NULL COMMENT '用例说明',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_question_id` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面试题目测试用例表（OJ判题）';


-- ----------------------------------------------------------------------------
-- 表：portal_interview_experience    [面经表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_interview_experience`;
CREATE TABLE `portal_interview_experience` (
  `id` bigint NOT NULL AUTO_INCREMENT,
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
  `auditor_id` bigint DEFAULT NULL COMMENT '审核人ID（系统用户ID，CMS审核时写入）',
  `audit_remark` varchar(500) DEFAULT NULL COMMENT '审核意见/驳回原因',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
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
  KEY `idx_del_flag` (`del_flag`),
  KEY `idx_experience_auditor` (`auditor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面经表';


-- ----------------------------------------------------------------------------
-- 表：portal_interview_experience_like    [面经点赞表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_interview_experience_like`;
CREATE TABLE `portal_interview_experience_like` (
  `id` bigint NOT NULL AUTO_INCREMENT,
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


-- ----------------------------------------------------------------------------
-- 表：portal_interview_comment    [面经评论表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_interview_comment`;
CREATE TABLE `portal_interview_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `experience_id` bigint NOT NULL COMMENT '面经ID',
  `user_id` bigint NOT NULL COMMENT '评论用户ID',
  `parent_id` bigint DEFAULT NULL COMMENT '父评论ID（支持两级回复）',
  `reply_to_user_id` bigint DEFAULT NULL COMMENT '回复目标用户ID',
  `content` text NOT NULL COMMENT '评论内容',
  `like_count` bigint DEFAULT '0' COMMENT '点赞数',
  `status` varchar(20) DEFAULT 'published' COMMENT '状态:pending,published,rejected',
  `auditor_id` bigint DEFAULT NULL COMMENT '审核人ID（系统用户ID，CMS审核或定时扫描命中时写入）',
  `audit_remark` varchar(500) DEFAULT NULL COMMENT '审核意见/驳回原因',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
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
  KEY `idx_del_flag` (`del_flag`),
  KEY `idx_auditor_id` (`auditor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面经评论表';


-- ----------------------------------------------------------------------------
-- 表：portal_interview_comment_like    [面经评论点赞表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_interview_comment_like`;
CREATE TABLE `portal_interview_comment_like` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `comment_id` bigint NOT NULL COMMENT '评论ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_comment_user` (`comment_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面经评论点赞表';


-- ----------------------------------------------------------------------------
-- 表：portal_interview_attempt    [做题记录表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_interview_attempt`;
CREATE TABLE `portal_interview_attempt` (
  `id` bigint NOT NULL AUTO_INCREMENT,
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


-- ----------------------------------------------------------------------------
-- 表：portal_interview_bookmark    [题目收藏表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_interview_bookmark`;
CREATE TABLE `portal_interview_bookmark` (
  `id` bigint NOT NULL AUTO_INCREMENT,
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


-- ----------------------------------------------------------------------------
-- 表：portal_interview_submission    [题目提交记录表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_interview_submission`;
CREATE TABLE `portal_interview_submission` (
  `id` bigint NOT NULL AUTO_INCREMENT,
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
  `is_featured` tinyint(1) DEFAULT '0' COMMENT '是否精选（后台采纳为优质笔记）：0=否 1=是',
  `featured_time` datetime DEFAULT NULL COMMENT '精选时间',
  `passed_case_count` int DEFAULT NULL COMMENT '通过用例数（v6.3 OJ判题）',
  `total_case_count` int DEFAULT NULL COMMENT '总用例数（v6.3 OJ判题）',
  `failed_case_id` bigint DEFAULT NULL COMMENT '首个失败用例ID（v6.3 OJ判题）',
  `failed_case_input` text COMMENT '首个失败用例输入（v6.3 OJ判题，仅样例可见）',
  `failed_case_expected` text COMMENT '首个失败用例期望输出（v6.3 OJ判题）',
  `failed_case_actual` text COMMENT '首个失败用例实际输出（v6.3 OJ判题）',
  `error_message` text COMMENT '编译/运行错误信息（v6.3 OJ判题）',
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
  KEY `idx_del_flag` (`del_flag`),
  KEY `idx_is_featured` (`is_featured`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目提交记录表';


-- ----------------------------------------------------------------------------
-- 表：portal_interview_resume_template    [简历模板表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_interview_resume_template`;
CREATE TABLE `portal_interview_resume_template` (
  `id` bigint NOT NULL AUTO_INCREMENT,
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
  `sample_data` json DEFAULT NULL COMMENT '模板结构化示例数据（JSON：name/phone/email/jobIntention/educations/works/projects/skills/selfIntro，用于一键套用填充编辑页）',
  `like_count` bigint DEFAULT '0' COMMENT '点赞数',
  `download_count` bigint DEFAULT '0' COMMENT '下载次数',
  `sort` int DEFAULT '0' COMMENT '排序',
  `status` varchar(20) DEFAULT 'active' COMMENT '状态:active,inactive',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `preview_images` text COMMENT '模板预览图 JSON 数组（多图，V10.2 新增）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='简历模板表';


-- ----------------------------------------------------------------------------
-- 表：portal_interview_resume_template_like    [简历模板点赞表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_interview_resume_template_like`;
CREATE TABLE `portal_interview_resume_template_like` (
  `id` bigint NOT NULL AUTO_INCREMENT,
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


-- ----------------------------------------------------------------------------
-- 表：portal_job_template    [岗位模板表（JD/关键词/出题权重，支撑智能出题）]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_job_template`;
CREATE TABLE `portal_job_template` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL COMMENT '模板名称（如：Java后端工程师）',
  `category` varchar(50) DEFAULT NULL COMMENT '岗位类别（技术/产品/运营/设计等）',
  `position_code` varchar(50) DEFAULT NULL COMMENT '岗位编码（对齐 portal_voice_interview.position）',
  `code` varchar(64) DEFAULT NULL COMMENT '岗位编码（如 java_backend）——v13.33 由 portal_interview_position.code 并入，按编码反查用',
  `industry` varchar(50) DEFAULT NULL COMMENT '所属行业（如 互联网/金融/制造）——v13.33 由 portal_interview_position.industry 并入',
  `level` varchar(32) DEFAULT NULL COMMENT '岗位级别 junior/mid/senior——v13.33 由 portal_interview_position.level 并入',
  `required_skills` text COMMENT '必备技能 JSON 数组（如 ["Spring","MySQL"]，与 portal_tag.name 对齐）——v13.33 由 portal_interview_position.required_skills 并入，驱动简历岗位匹配评分与画像必备技能',
  `hot_companies` text COMMENT '热门公司 JSON 数组（如 ["阿里","腾讯"]）——v13.33 由 portal_interview_position.hot_companies 并入',
  `sort` int DEFAULT '0' COMMENT '排序（升序）——v13.33 由 portal_interview_position.sort 并入',
  `description` varchar(500) DEFAULT NULL COMMENT '模板描述',
  `jd_text` text COMMENT '岗位 JD 原文（用于 LLM 关键词提取与出题上下文）',
  `keywords` varchar(500) DEFAULT NULL COMMENT '岗位关键词，逗号分隔（LLM 提取 + 人工维护）',
  `difficulty` varchar(20) NOT NULL DEFAULT 'medium' COMMENT '难度:easy,medium,hard（对齐 portal_interview_question.difficulty）',
  `question_count` int NOT NULL DEFAULT '5' COMMENT '默认出题数量',
  `weights` varchar(100) DEFAULT '{"job":40,"resume":30,"weak":20,"random":10}' COMMENT '出题权重 JSON（job岗位核心/resume简历深挖/weak薄弱点/random随机兜底）',
  `status` varchar(20) NOT NULL DEFAULT 'active' COMMENT '状态:active 启用/inactive 停用',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`),
  KEY `idx_del_flag` (`del_flag`),
  KEY `idx_code` (`code`),
  KEY `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='岗位模板表（JD/关键词/出题权重，支撑智能出题）';


-- ----------------------------------------------------------------------------
-- 表：portal_resume_parse_config    [简历解析配置（章节词典/技能词域/学历词/岗位词，规则解析用）]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_resume_parse_config`;
CREATE TABLE `portal_resume_parse_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `config_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置类型：section 章节词 / skill 技能词 / degree 学历词 / position 岗位词',
  `item_key` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置键：section 时为目标大类 edu/work/project/skill/self/intention/basic/other；其余为词条本身',
  `item_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '显示名称（后台列表展示，如「教育背景」）',
  `keywords` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '关键词（多个用英文逗号分隔；section 用于匹配章节标题，其余用于全文匹配）',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序（升序）',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'active' COMMENT '状态：active 启用 / inactive 停用',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  KEY `idx_type_status` (`config_type`,`status`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='简历解析配置（章节词典/技能词域/学历词/岗位词，规则解析用）';


-- ----------------------------------------------------------------------------
-- 表：portal_resume_job_target    [简历优化-岗位目标表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_resume_job_target`;
CREATE TABLE `portal_resume_job_target` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID（门户用户ID）',
  `position` varchar(100) NOT NULL COMMENT '目标岗位名称（如 Java开发工程师）',
  `company` varchar(100) DEFAULT NULL COMMENT '目标公司（选填）',
  `city` varchar(50) DEFAULT NULL COMMENT '期望城市（选填）',
  `job_type` varchar(20) DEFAULT NULL COMMENT '岗位类型（全职/兼职/实习）',
  `jd_text` text NOT NULL COMMENT '岗位描述/JD原文（匹配分析核心输入）',
  `jd_keywords` varchar(1000) DEFAULT NULL COMMENT 'AI提取的JD核心关键词（逗号分隔，冗余加速展示）',
  `is_default` tinyint(1) DEFAULT '0' COMMENT '是否默认岗位：0=否 1=是',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='简历优化-岗位目标表';


-- ----------------------------------------------------------------------------
-- 表：portal_resume_job_match    [简历优化-岗位匹配报告表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_resume_job_match`;
CREATE TABLE `portal_resume_job_match` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `resume_id` bigint NOT NULL COMMENT '简历ID（portal_user_resume.id）',
  `job_target_id` bigint NOT NULL COMMENT '岗位目标ID',
  `match_score` int NOT NULL COMMENT '综合匹配度 0-100',
  `grade` varchar(20) DEFAULT NULL COMMENT '评级：excellent/good/medium/poor',
  `matched_keywords` varchar(1000) DEFAULT NULL COMMENT '已匹配关键词（逗号分隔）',
  `missing_keywords` varchar(1000) DEFAULT NULL COMMENT '缺失关键词（逗号分隔）',
  `dimensions` json DEFAULT NULL COMMENT '各维度评分明细（关键词/经验/技能/结构匹配）',
  `summary` text COMMENT 'AI分析总结',
  `ai_powered` tinyint(1) DEFAULT '0' COMMENT '是否LLM生成：0=规则 1=LLM',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_resume_id` (`resume_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='简历优化-岗位匹配报告表';


-- ----------------------------------------------------------------------------
-- 表：portal_resume_score_report    [简历优化-评分报告存档表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_resume_score_report`;
CREATE TABLE `portal_resume_score_report` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID（门户用户ID）',
  `resume_id` bigint NOT NULL COMMENT '简历ID（portal_user_resume.id）',
  `job_target_id` bigint DEFAULT NULL COMMENT '关联岗位目标ID（可选，纯规则评分时为空）',
  `position_snapshot` varchar(100) DEFAULT NULL COMMENT '评分时的目标岗位快照（便于报告独立解读）',
  `score` int NOT NULL COMMENT '综合评分 0-100',
  `score_detail` text COMMENT '各维度评分明细 JSON（基本信息/求职意向/教育/工作/项目/技能/自我评价/岗位匹配度）',
  `source` varchar(20) DEFAULT 'manual' COMMENT '评分来源：manual 单独评分 / optimize 优化后重新评分 / template 模板套用评分',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '评分时间',
  PRIMARY KEY (`id`),
  KEY `idx_resume_id` (`resume_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='简历优化-评分报告存档表';


-- ----------------------------------------------------------------------------
-- 表：portal_resume_optimize_history    [简历优化-优化历史表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_resume_optimize_history`;
CREATE TABLE `portal_resume_optimize_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `resume_id` bigint NOT NULL COMMENT '被优化的简历ID',
  `from_resume_id` bigint NOT NULL COMMENT '优化前简历ID（同一简历冗余记录，便于追溯）',
  `job_target_id` bigint DEFAULT NULL COMMENT '关联岗位目标ID',
  `score_before` int DEFAULT NULL COMMENT '优化前评分',
  `score_after` int DEFAULT NULL COMMENT '优化后评分',
  `match_score_before` int DEFAULT NULL COMMENT '优化前匹配度',
  `match_score_after` int DEFAULT NULL COMMENT '优化后匹配度',
  `adopted_count` int DEFAULT '0' COMMENT '采纳建议数',
  `total_count` int DEFAULT '0' COMMENT '生成建议总数',
  `optimize_data` json DEFAULT NULL COMMENT '优化明细快照（逐项 original/optimized/status）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_resume_id` (`resume_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='简历优化-优化历史表';


-- ----------------------------------------------------------------------------
-- 表：portal_user_resume    [用户简历]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_user_resume`;
CREATE TABLE `portal_user_resume` (
  `id` bigint NOT NULL AUTO_INCREMENT,
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
  `full_text` mediumtext COMMENT '简历全文纯文本（保存时自动拼接结构化字段，供 AI 分析使用）',
  `score` int DEFAULT NULL COMMENT '评分（0-100）',
  `score_detail` text COMMENT '评分明细（JSON 数组）',
  `parse_confidence` tinyint DEFAULT NULL COMMENT '解析置信度（0-100：LLM 结构化=85，规则兜底=60，NULL=未解析）',
  `scored_time` datetime DEFAULT NULL COMMENT '评分时间',
  `file_url` varchar(255) DEFAULT NULL COMMENT 'PDF 导出文件URL',
  `export_time` datetime DEFAULT NULL COMMENT '最后导出时间',
  `status` varchar(20) NOT NULL DEFAULT 'draft' COMMENT '状态：draft/published/archived',
  `source_type` varchar(20) NOT NULL DEFAULT 'online' COMMENT '来源类型：online（在线创建）/ attachment（附件解析）',
  `source_file_url` varchar(500) DEFAULT NULL COMMENT '附件源文件URL（source_type=attachment 时有值，支持下载）',
  `source_file_name` varchar(255) DEFAULT NULL COMMENT '附件原始文件名（上传时的文件名，用于下载时还原文件名）',
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
  KEY `idx_del_flag` (`del_flag`),
  KEY `idx_user_source` (`user_id`,`source_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户简历';


-- ----------------------------------------------------------------------------
-- 表：portal_code_run    [代码运行记录]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_code_run`;
CREATE TABLE `portal_code_run` (
  `id` bigint NOT NULL AUTO_INCREMENT,
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


-- ----------------------------------------------------------------------------
-- 表：portal_contest_submission    [活动投稿]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_contest_submission`;
CREATE TABLE `portal_contest_submission` (
  `id` bigint NOT NULL AUTO_INCREMENT,
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


-- ----------------------------------------------------------------------------
-- 表：portal_contest_vote    [活动投稿投票记录]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_contest_vote`;
CREATE TABLE `portal_contest_vote` (
  `id` bigint NOT NULL AUTO_INCREMENT,
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


-- ----------------------------------------------------------------------------
-- 表：portal_writing_contest    [创作挑战/征文活动]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_writing_contest`;
CREATE TABLE `portal_writing_contest` (
  `id` bigint NOT NULL AUTO_INCREMENT,
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


-- ----------------------------------------------------------------------------
-- 表：portal_writing_prompt    [每日写作 prompt]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_writing_prompt`;
CREATE TABLE `portal_writing_prompt` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `prompt_date` date NOT NULL COMMENT 'prompt 日期（唯一）',
  `title` varchar(128) NOT NULL COMMENT 'prompt 标题',
  `description` text COMMENT 'prompt 描述',
  `category` varchar(32) DEFAULT NULL COMMENT '分类（如：生活/职场/情感/虚构/哲思）',
  `festival_name` varchar(64) DEFAULT NULL COMMENT '关联特殊日期名称（节日/节气/纪念日）',
  `source` varchar(16) DEFAULT 'ai' COMMENT '来源：ai=AI生成 / manual=手动创建',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_prompt_date` (`prompt_date`),
  KEY `idx_category` (`category`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='每日写作 prompt';


-- ----------------------------------------------------------------------------
-- 表：portal_creator_certification    [创作者认证]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_creator_certification`;
CREATE TABLE `portal_creator_certification` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '申请用户ID',
  `real_name` varchar(64) NOT NULL COMMENT '真实姓名',
  `cert_type` varchar(32) NOT NULL COMMENT '认证类型 identity/creator/expert',
  `cert_no` varchar(64) DEFAULT NULL COMMENT '证件号',
  `cert_no_enc` varchar(512) DEFAULT NULL COMMENT '证件号密文（AES-GCM，格式 enc:v1:iv:cipher，base64）',
  `cert_no_mask` varchar(32) DEFAULT NULL COMMENT '证件号脱敏展示值（如 110***********1234）',
  `derived_gender` varchar(8) DEFAULT NULL COMMENT '由证件号推导的性别（男/女），仅身份认证类型',
  `derived_birth` date DEFAULT NULL COMMENT '由证件号推导的出生日期',
  `verify_channel` varchar(32) DEFAULT 'manual' COMMENT '实名核验渠道：manual=人工审核（默认），后期可扩展 aliyun/tencent 等',
  `verify_serial` varchar(64) DEFAULT NULL COMMENT '第三方实名核验流水号（预留，接入核验API后填充）',
  `cert_image` varchar(500) DEFAULT NULL COMMENT '证件照URL（兼容字段：旧单图或新流程中的「人像面」URL 别名）',
  `cert_image_front` varchar(500) DEFAULT NULL COMMENT '身份证正面（人像面）URL',
  `cert_image_back` varchar(500) DEFAULT NULL COMMENT '身份证背面（国徽面）URL',
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


-- ----------------------------------------------------------------------------
-- 表：portal_shop_item    [积分商城商品表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_shop_item`;
CREATE TABLE `portal_shop_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='积分商城商品表';


-- ----------------------------------------------------------------------------
-- 表：portal_shop_exchange    [积分兑换记录表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_shop_exchange`;
CREATE TABLE `portal_shop_exchange` (
  `id` bigint NOT NULL AUTO_INCREMENT,
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


-- ----------------------------------------------------------------------------
-- 表：portal_study_plan    [学习计划]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_study_plan`;
CREATE TABLE `portal_study_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT,
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


-- ----------------------------------------------------------------------------
-- 表：portal_study_plan_log    [计划每日进度]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_study_plan_log`;
CREATE TABLE `portal_study_plan_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `plan_id` bigint NOT NULL COMMENT '计划ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `log_date` date NOT NULL COMMENT '日志日期',
  `done_count` int NOT NULL DEFAULT '0' COMMENT '当日完成数量',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_date` (`plan_id`,`log_date`),
  KEY `idx_user_date` (`user_id`,`log_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='计划每日进度';


-- ----------------------------------------------------------------------------
-- 表：portal_wrong_question    [错题本]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_wrong_question`;
CREATE TABLE `portal_wrong_question` (
  `id` bigint NOT NULL AUTO_INCREMENT,
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


-- ----------------------------------------------------------------------------
-- 表：portal_ad_slot    [门户自研广告位表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_ad_slot`;
CREATE TABLE `portal_ad_slot` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `slot_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '广告位标识，如 article_detail_bottom',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '广告标题',
  `image` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '广告图片URL',
  `link` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '点击跳转链接',
  `open_target` varchar(10) DEFAULT '_blank' COMMENT '链接打开方式：_blank=新窗口（默认），_self=当前页',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '广告文案',
  `sort` int DEFAULT '0' COMMENT '排序',
  `status` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '状态：0=启用 1=停用',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  KEY `idx_slot_key` (`slot_key`),
  KEY `idx_status` (`status`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户自研广告位表';


-- ----------------------------------------------------------------------------
-- 表：portal_feedback    [用户意见反馈表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_feedback`;
CREATE TABLE `portal_feedback` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `feedback_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '反馈类型：suggestion/bug/experience/other',
  `subject` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '反馈主题',
  `description` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '反馈详细描述',
  `contact` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '联系方式（可选）',
  `user_id` bigint DEFAULT NULL COMMENT '反馈人用户ID',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '反馈人用户名（冗余）',
  `ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '反馈人IP',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'pending' COMMENT '处理状态：pending/processing/resolved/rejected',
  `handler` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '处理人',
  `handle_result` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '处理结果说明',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  KEY `idx_feedback_type` (`feedback_type`),
  KEY `idx_status` (`status`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户意见反馈表';


-- ----------------------------------------------------------------------------
-- 表：portal_report    [用户举报记录表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_report`;
CREATE TABLE `portal_report` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `report_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '举报类型：spam/inappropriate/infringement/fraud/other',
  `target_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '举报目标URL',
  `target_type` varchar(32) DEFAULT NULL COMMENT '举报目标类型：article=文章/comment=评论/user=用户/topic=话题/topic_post=话题观点/topic_comment=话题评论/column=专栏（为空表示通用举报，仅 target_url）',
  `target_id` bigint DEFAULT NULL COMMENT '举报目标ID（评论/文章/用户ID，配合 target_type 使用）',
  `description` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '问题描述',
  `contact` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '联系方式（可选）',
  `images` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '图片证据（JSON数组，最多3张）',
  `user_id` bigint DEFAULT NULL COMMENT '举报人用户ID',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '举报人用户名（冗余）',
  `ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '举报人IP',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'pending' COMMENT '处理状态：pending/processing/resolved/rejected',
  `handler` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '处理人',
  `handle_result` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '处理结果说明',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`id`),
  KEY `idx_report_type` (`report_type`),
  KEY `idx_status` (`status`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_target` (`target_type`,`target_id`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户举报记录表';


-- ----------------------------------------------------------------------------
-- 表：portal_message    [私信消息]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_message`;
CREATE TABLE `portal_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` bigint NOT NULL COMMENT '会话ID',
  `sender_id` bigint NOT NULL COMMENT '发送者',
  `sender_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'portal' COMMENT '发送者类型 portal/sys',
  `receiver_id` bigint NOT NULL COMMENT '接收者',
  `receiver_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'portal' COMMENT '接收者类型 portal/sys',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息内容',
  `msg_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'text' COMMENT 'text/image/file',
  `is_read` tinyint DEFAULT '0' COMMENT '是否已读',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_time` (`session_id`,`create_time`),
  KEY `idx_receiver_type_read` (`receiver_id`,`receiver_type`,`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='私信消息';


-- ----------------------------------------------------------------------------
-- 表：portal_message_session    [私信会话]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_message_session`;
CREATE TABLE `portal_message_session` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_a` bigint NOT NULL COMMENT '用户A（较小ID）',
  `user_a_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'portal' COMMENT 'A方用户类型 portal/sys',
  `user_b` bigint NOT NULL COMMENT '用户B（较大ID）',
  `user_b_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'portal' COMMENT 'B方用户类型 portal/sys',
  `last_message_id` bigint DEFAULT NULL COMMENT '最后一条消息ID',
  `last_message_content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '最后消息内容预览',
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


-- ----------------------------------------------------------------------------
-- 表：portal_ai_task    [通用AI异步任务表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_ai_task`;
CREATE TABLE `portal_ai_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '所属用户',
  `task_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务类型：resume_parse（简历解析）/ job_match（岗位匹配）/ ai_draft（空字段草稿）/ deep_optimize（深度优化）',
  `biz_ref` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '业务参数JSON，如 {"resumeId":1,"jobTargetId":2}',
  `payload` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '任务大文本输入（如 resume_parse 的简历抽取文本，上限 6000 字符）；小参数走 biz_ref',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'pending' COMMENT '状态：pending（排队）/ running（执行中）/ success（成功）/ failed（失败）',
  `progress_msg` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '进度提示文案（轮询时返回给前端展示）',
  `result` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '任务结果JSON（success 时有值）',
  `error` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '失败原因（failed 时有值）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `finish_time` datetime DEFAULT NULL COMMENT '完成时间（成功或失败）',
  PRIMARY KEY (`id`),
  KEY `idx_user_type` (`user_id`,`task_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通用AI异步任务表';


-- ----------------------------------------------------------------------------
-- 表：portal_user    [门户用户表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_user`;
CREATE TABLE `portal_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL COMMENT '关联后台用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '昵称',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '手机号',
  `password` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '密码',
  `avatar` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '头像URL',
  `bio` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '个人简介',
  `position` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '职位',
  `identity_tag` varchar(32) DEFAULT NULL COMMENT '身份标签（字典 ledger_identity_tag，AI 财务分析画像维度）',
  `wechat` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '微信号',
  `gender` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '性别：male-男，female-女，other-其他',
  `birthday` date DEFAULT NULL COMMENT '生日（v13.23 由 varchar(20) 改为 date：可索引比较、可直接 TIMESTAMPDIFF 算年龄）',
  `location` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '所在城市：如北京市',
  `website` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '个人网站URL',
  `github` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'GitHub用户名或完整URL',
  `company` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '公司名称',
  `school` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '学校名称',
  `industry` varchar(100) DEFAULT NULL COMMENT '行业（AI财务分析画像：行业分析）',
  `marital_status` varchar(20) DEFAULT NULL COMMENT '婚姻状况（AI财务分析画像：家庭负担；single/married/other）',
  `has_mortgage` tinyint DEFAULT NULL COMMENT '是否有房贷（AI财务分析画像：负债分析；1=是 0=否）',
  `has_side_income` tinyint DEFAULT NULL COMMENT '是否有副业收入（AI财务分析画像：收入结构；1=是 0=否）',
  `income_types` varchar(200) DEFAULT NULL COMMENT '收入类型（AI财务分析画像：逗号分隔，如 salary,investment,rent）',
  `language` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '语言偏好：zh-CN，en-US等',
  `timezone` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '时区：如Asia/Shanghai',
  `notify_like` tinyint(1) DEFAULT '1' COMMENT '是否接收点赞通知',
  `notify_comment` tinyint(1) DEFAULT '1' COMMENT '是否接收评论通知',
  `notify_follow` tinyint(1) DEFAULT '1' COMMENT '是否接收关注通知',
  `notify_system` tinyint(1) DEFAULT '1' COMMENT '是否接收系统通知',
  `notify_schedule` tinyint(1) DEFAULT '0' COMMENT '是否接收定时记账执行结果邮件（默认关，避免未经同意发信）',
  `warehouse_enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否开启仓库管理功能',
  `privacy_follow` tinyint(1) DEFAULT '1' COMMENT '是否允许被关注',
  `privacy_bookmark` tinyint(1) DEFAULT '1' COMMENT '是否公开收藏夹',
  `privacy_email` tinyint(1) DEFAULT '0' COMMENT '是否公开邮箱',
  `privacy_phone` tinyint(1) DEFAULT '0' COMMENT '是否公开手机号',
  `privacy_profile` tinyint(1) DEFAULT '1' COMMENT '是否公开主页（是否在名家录/作者列表展示）：1=公开，0=不公开',
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'user' COMMENT '角色：user/admin',
  `is_certified_creator` tinyint NOT NULL DEFAULT '0' COMMENT '是否认证创作者：0 否/1 是',
  `vip_expire_at` datetime DEFAULT NULL COMMENT 'VIP过期时间',
  `is_phone_verified` tinyint(1) DEFAULT '0' COMMENT '是否已验证手机号',
  `is_wechat_verified` tinyint(1) DEFAULT '0' COMMENT '是否已验证微信',
  `two_factor_enabled` tinyint(1) DEFAULT '0' COMMENT '是否开启两步验证',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '帐号状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  `login_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '最后登录IP',
  `login_date` datetime DEFAULT NULL COMMENT '最后登录时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `platform_code` varchar(50) DEFAULT 'portal' COMMENT '注册来源端',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`),
  UNIQUE KEY `uk_phone` (`phone`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_portal_user_platform` (`platform_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户用户表';


-- ----------------------------------------------------------------------------
-- 表：portal_user_stats    [门户用户统计聚合表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_user_stats`;
CREATE TABLE `portal_user_stats` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
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
  `weak_tags` text COMMENT '薄弱知识点 JSON 数组（如 [{"tagId":1,"tagName":"Spring","failRate":0.6}]）',
  `weak_tags_updated_time` datetime DEFAULT NULL COMMENT '薄弱点最后计算时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户用户统计聚合表';


-- ----------------------------------------------------------------------------
-- 表：portal_voice_interview    [语音面试会话主表（V10.1）]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_voice_interview`;
CREATE TABLE `portal_voice_interview` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '面试用户ID',
  `position` varchar(64) DEFAULT NULL COMMENT '面试岗位',
  `scene` varchar(64) DEFAULT NULL COMMENT '面试场景',
  `resume_id` bigint DEFAULT NULL COMMENT '简历ID（有简历时启用项目深挖题源）',
  `agent_id` bigint DEFAULT NULL COMMENT '面试官智能体ID（ai_agent.id，NULL=未绑定走默认逻辑）',
  `phase` varchar(30) DEFAULT NULL COMMENT '当前阶段:INTRO_WAITING/INTRO_RECEIVED/INTRO_FOLLOWUP/TECH_QUESTION/PROJECT_DEEP/SYSTEM_DESIGN/CANDIDATE_ASK/FINISHED（NULL=旧流程）',
  `intro_score_json` text COMMENT '自我介绍评分 JSON（4维度+总分+评语，ScoringEngine 产出）',
  `status` varchar(16) NOT NULL DEFAULT 'in_progress' COMMENT '状态 in_progress/finished',
  `style` varchar(20) DEFAULT 'professional' COMMENT '面试官风格（字典 voice_interview_style）',
  `difficulty` varchar(20) DEFAULT 'medium' COMMENT '难度 easy/medium/hard',
  `total_qa` int NOT NULL DEFAULT '0' COMMENT '主问题目总数（不含追问）',
  `current_idx` int NOT NULL DEFAULT '0' COMMENT '当前主问题目序号',
  `score` int DEFAULT NULL COMMENT '面试总分（0-100）',
  `summary` text COMMENT 'AI 生成的面试总结',
  `report` text COMMENT '报告 JSON（含维度分/亮点/薄弱点/逐题点评）',
  `share_token` varchar(64) DEFAULT NULL COMMENT '报告分享令牌（NULL=未分享）',
  `share_expire_time` datetime DEFAULT NULL COMMENT '分享过期时间',
  `share_count` int NOT NULL DEFAULT '0' COMMENT '分享访问次数',
  `config_json` text COMMENT '配置 JSON（hintsEnabled/stuckThreshold/style/difficulty）',
  `is_personalized` tinyint(1) DEFAULT '0' COMMENT '是否基于画像抽题',
  `profile_snapshot` text COMMENT '抽题时的画像快照 JSON',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  `context_snapshot` json DEFAULT NULL COMMENT '会话上下文快照（准备时生成：简历摘要+画像+配置）',
  `question_paper` json DEFAULT NULL COMMENT '题单快照（preset 模式：题目ID序列+题目快照）',
  `analysis_status` tinyint NOT NULL DEFAULT '0' COMMENT '报告分析状态：0未分析 1分析中 2已完成',
  `analysis_progress` int NOT NULL DEFAULT '0' COMMENT '报告分析进度 0-100（前端进度条轮询）',
  `closed_reason` varchar(50) DEFAULT NULL COMMENT '结束原因：user主动/auto题单耗尽/timeout超时/skip连续跳过',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_share_token` (`share_token`),
  KEY `idx_user_time` (`user_id`,`create_time`),
  KEY `idx_status` (`status`),
  KEY `idx_del_flag` (`del_flag`),
  KEY `idx_agent` (`agent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='语音面试会话主表（V10.1）';


-- ----------------------------------------------------------------------------
-- 表：portal_voice_interview_event    [AI语音面试会话事件日志（v11.88）]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_voice_interview_event`;
CREATE TABLE `portal_voice_interview_event` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `interview_id` bigint NOT NULL COMMENT '面试会话ID（portal_voice_interview.id）',
  `event_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '事件类型：start/answer/next/finish/close/error',
  `event_data` json DEFAULT NULL COMMENT '事件数据（题目索引/动作/原因等）',
  `create_time` datetime DEFAULT NULL COMMENT '事件时间',
  PRIMARY KEY (`id`),
  KEY `idx_interview` (`interview_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI语音面试会话事件日志（v11.88）';


-- ----------------------------------------------------------------------------
-- 表：portal_voice_interview_qa    [语音面试问答表（V10.1，含追问链）]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_voice_interview_qa`;
CREATE TABLE `portal_voice_interview_qa` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `interview_id` bigint NOT NULL COMMENT '面试会话ID',
  `question_id` bigint DEFAULT NULL COMMENT '关联题目ID（portal_interview_question.id）',
  `question_source` varchar(20) DEFAULT NULL COMMENT '问题来源 bank=题库/resume_project=简历锚定/llm=智能体生成',
  `question_idx` int NOT NULL COMMENT '主问题目序号（从0开始）',
  `parent_qa_id` bigint DEFAULT NULL COMMENT '追问父问答ID（NULL=主问）',
  `question` varchar(1000) NOT NULL COMMENT '面试问题',
  `user_answer` text COMMENT '用户回答（ASR 转写后可编辑）',
  `transcription_edited` tinyint(1) DEFAULT '0' COMMENT '转写是否被用户编辑',
  `ai_feedback` text COMMENT 'AI 反馈',
  `speak_text` text COMMENT 'AI 面试官话术（TTS 播报内容）',
  `score` int DEFAULT NULL COMMENT '本题评分（0-100）',
  `rule_dimensions_json` text COMMENT '规则维度分 JSON（6维对齐雷达图）',
  `llm_score_json` text COMMENT 'LLM 结构化评分 JSON（scores/total/strengths/weaknesses/comment）',
  `llm_analysis_json` text COMMENT 'LLM深度分析JSON（sentiment/fluency/redFlags/completeness）',
  `hint_used` int DEFAULT '0' COMMENT '已使用提示次数（0~3）',
  `latency_ms` int DEFAULT NULL COMMENT '答题耗时（毫秒）',
  `next_action` varchar(20) DEFAULT NULL COMMENT '下一步动作 followup/hint/next/report',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记',
  `answer_raw` text COMMENT '用户原始回答（提交即落库，未分析，防丢失）',
  `answer_time` datetime DEFAULT NULL COMMENT '回答时间（提交时刻）',
  `score_draft` int DEFAULT NULL COMMENT '草稿评分（异步 LLM 分析写回，报告批量分析融合）',
  `analysis_status` tinyint NOT NULL DEFAULT '0' COMMENT '单题分析状态：0未分析 1分析中 2已分析',
  `uk_qa` varchar(48) GENERATED ALWAYS AS (concat(`interview_id`,_utf8mb4':',`question_idx`,_utf8mb4':',ifnull(`parent_qa_id`,0))) STORED COMMENT 'v13.53 唯一键载体：会话:题号:父问答（NULL→0），绕开 MySQL 唯一索引对 NULL 不去重的语义',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_qa_main` (`uk_qa`),
  KEY `idx_interview` (`interview_id`),
  KEY `idx_question_idx` (`interview_id`,`question_idx`),
  KEY `idx_parent` (`parent_qa_id`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='语音面试问答表（V10.1，含追问链）';


-- ==============================================================================
-- 五、记账模块（ledger_*）：流水 / 账户 / 预算 / 储蓄 / 库存 / 经营
-- 模块优先级：业务主模块。
-- ==============================================================================

-- ----------------------------------------------------------------------------
-- 表：ledger_category    [记账-分类（系统预设+用户自定义）]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ledger_category`;
CREATE TABLE `ledger_category` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL DEFAULT '0' COMMENT '0=系统预设，>0=用户自定义（portal_user.id）',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类名称',
  `type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '类型：income/expense',
  `group_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '语义分组（前端展示分组用）',
  `parent_id` bigint DEFAULT NULL COMMENT '父分类ID（支持二级分类）',
  `icon` varchar(255) DEFAULT NULL COMMENT '图标：emoji key 或图片URL',
  `color` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '颜色',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `is_system` tinyint NOT NULL DEFAULT '0' COMMENT '系统预设：1=是（仅后台可维护） 0=自定义',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1=启用 0=停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_type` (`user_id`,`type`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='记账-分类（系统预设+用户自定义）';


-- ----------------------------------------------------------------------------
-- 表：ledger_transaction    [记账-流水]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ledger_transaction`;
CREATE TABLE `ledger_transaction` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL COMMENT '门户用户ID（portal_user.id）',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '类型：income/expense/transfer/repayment/borrow/adjust',
  `amount` decimal(18,2) NOT NULL COMMENT '金额（元；adjust可为负表示调减，其余恒为正，方向由type决定）',
  `category_id` bigint DEFAULT NULL COMMENT '分类ID（ledger_category）',
  `sub_category_id` bigint DEFAULT NULL COMMENT '二级分类ID（ledger_category.id，未选二级为 null）',
  `account_id` bigint DEFAULT NULL COMMENT '关联资产账户（支出/收入/转出方/还款扣款方/校准账户）',
  `liability_id` bigint DEFAULT NULL COMMENT '关联负债账户（还款/借款）',
  `receivable_id` bigint DEFAULT NULL COMMENT '关联应收款ID（借出/收款时填写）',
  `payable_id` bigint DEFAULT NULL COMMENT '关联应付款ID（借款/还款时填写）',
  `target_account_id` bigint DEFAULT NULL COMMENT '转账目标资产账户',
  `balance_after` decimal(18,2) DEFAULT NULL COMMENT '主账户交易后余额快照（元）',
  `target_balance_after` decimal(18,2) DEFAULT NULL COMMENT '转账目标账户交易后余额快照（元）',
  `liability_balance_after` decimal(18,2) DEFAULT NULL COMMENT '关联负债交易后欠款快照（元）',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `transaction_date` date NOT NULL COMMENT '交易日期（默认当天）',
  `transaction_time` time DEFAULT NULL COMMENT '交易时间',
  `merchant` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '商户名称',
  `voucher_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '凭证截图URL（门户文件服务地址）',
  `is_budget` tinyint NOT NULL DEFAULT '1' COMMENT '是否计入预算：1=是 0=否（adjust 默认0）',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1=正常 0=已删除（冲正后归档）',
  `client_uuid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '客户端幂等键（Phase 4 离线同步防重复提交）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '创建人（门户用户名/admin 代改标识）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `stock_in` tinyint NOT NULL DEFAULT '0' COMMENT '1=采购入库支出，消费统计时排除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_client` (`user_id`,`client_uuid`),
  KEY `idx_user_status_id` (`user_id`,`status`,`id`),
  KEY `idx_user_date` (`user_id`,`transaction_date`),
  KEY `idx_account` (`account_id`),
  KEY `idx_liability` (`liability_id`),
  KEY `idx_receivable` (`receivable_id`),
  KEY `idx_payable` (`payable_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='记账-流水';


-- ----------------------------------------------------------------------------
-- 表：ledger_memo    [记账-备忘录（待办事项）]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ledger_memo`;
CREATE TABLE `ledger_memo` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL COMMENT '门户用户ID（portal_user.id）',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '事项标题',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '待办内容',
  `done` tinyint NOT NULL DEFAULT '0' COMMENT '完成状态：1=已完成 0=未完成',
  `event_time` datetime DEFAULT NULL COMMENT '事项时间（提醒基准时间）',
  `remind_enabled` tinyint NOT NULL DEFAULT '0' COMMENT '是否提醒：1=是 0=否',
  `remind_rule` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '提醒方式：on_time=准时 advance_30m=提前30分钟 advance_1h=提前1小时 advance_2h=提前2小时 advance_1d=提前1天 advance_1d_9am=提前一天上午9点',
  `importance` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'normal' COMMENT '重要程度：low=不重要 normal=一般 high=重要 urgent=紧急',
  `reminded` tinyint NOT NULL DEFAULT '0' COMMENT '提醒是否已发送：1=已发 0=未发（防重复）',
  `todo_date` date DEFAULT NULL COMMENT '创建日期',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_done` (`user_id`,`done`),
  KEY `idx_user_remind` (`remind_enabled`,`done`,`reminded`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='记账-备忘录（待办事项）';


-- ----------------------------------------------------------------------------
-- 表：ledger_budget    [记账-预算]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ledger_budget`;
CREATE TABLE `ledger_budget` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL COMMENT '门户用户ID（portal_user.id）',
  `category_id` bigint DEFAULT NULL COMMENT '分类ID（NULL=月度总预算）',
  `year` int NOT NULL COMMENT '年份',
  `month` int NOT NULL COMMENT '月份（1-12）',
  `amount` decimal(18,2) NOT NULL COMMENT '预算金额（元）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_period` (`user_id`,`year`,`month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='记账-预算';


-- ----------------------------------------------------------------------------
-- 表：ledger_saving_plan    [记账-存钱计划]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ledger_saving_plan`;
CREATE TABLE `ledger_saving_plan` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL COMMENT '门户用户ID（portal_user.id）',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '计划名称，如"买房基金"',
  `method` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '存钱方式：52week/fixed/monthly/custom',
  `target_amount` decimal(18,2) NOT NULL COMMENT '目标金额（元）',
  `current_amount` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '当前已存金额（元）',
  `period_amount` decimal(18,2) DEFAULT NULL COMMENT '每期金额（元；fixed/monthly 用）',
  `period_count` int DEFAULT NULL COMMENT '总期数（custom 自定义规则期数）',
  `increase_step` decimal(18,2) DEFAULT NULL COMMENT '每期递增金额（元；custom 自定义递增规则用）',
  `start_date` date NOT NULL COMMENT '开始日期',
  `end_date` date DEFAULT NULL COMMENT '结束日期（可选）',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1=进行中 2=成功 3=失败 0=已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_status` (`user_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='记账-存钱计划';


-- ----------------------------------------------------------------------------
-- 表：ledger_saving_record    [记账-存钱流水]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ledger_saving_record`;
CREATE TABLE `ledger_saving_record` (
  `id` bigint NOT NULL,
  `plan_id` bigint NOT NULL COMMENT '计划ID（ledger_saving_plan.id）',
  `user_id` bigint NOT NULL COMMENT '门户用户ID（冗余，数据隔离校验用）',
  `period_index` int NOT NULL COMMENT '期次序号（第几期/第几周）',
  `target_amount` decimal(18,2) NOT NULL COMMENT '本期应存金额（元）',
  `amount` decimal(18,2) DEFAULT NULL COMMENT '实际存入金额（元；成功时记录）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0=待存 1=成功 2=失败',
  `fail_reason` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '失败原因（如余额不足）',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `record_date` date DEFAULT NULL COMMENT '存入/失败日期',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_plan` (`plan_id`,`period_index`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='记账-存钱流水';


-- ----------------------------------------------------------------------------
-- 表：ledger_asset_account    [记账-资产账户]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ledger_asset_account`;
CREATE TABLE `ledger_asset_account` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL COMMENT '门户用户ID（portal_user.id）',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '账户名称，如"招商银行储蓄卡"',
  `account_type` varchar(20) NOT NULL DEFAULT 'fund' COMMENT '账户类型：fund资金/receivable应收/investment投资/fixed固定',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `balance` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '当前余额（元）',
  `total_amount` decimal(18,2) DEFAULT NULL COMMENT '总额（应收/投资/固定类用）',
  `paid_amount` decimal(18,2) DEFAULT NULL COMMENT '已收（应收类用）',
  `settle_status` varchar(20) DEFAULT NULL COMMENT '结算状态：pending/partial/settled',
  `due_date` date DEFAULT NULL COMMENT '到期日（应收类用）',
  `risk_level` varchar(20) NOT NULL DEFAULT 'normal' COMMENT '风险标记：normal/attention/bad_debt',
  `valuation` decimal(18,2) DEFAULT NULL COMMENT '估值（元；投资/固定资产用，可≠balance）',
  `include_in_total` tinyint NOT NULL DEFAULT '1' COMMENT '是否计入总资产：1=是 0=否',
  `icon` varchar(255) DEFAULT NULL COMMENT '图标：emoji key 或图片URL',
  `hide_balance` tinyint NOT NULL DEFAULT '0' COMMENT '是否隐藏余额（隐私模式）：1=是 0=否',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1=启用 0=停用归档（删除即归档，流水永久保留）',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='记账-资产账户';


-- ----------------------------------------------------------------------------
-- 表：ledger_liability_account    [记账-负债账户]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ledger_liability_account`;
CREATE TABLE `ledger_liability_account` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL COMMENT '门户用户ID（portal_user.id）',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '负债名称，如"招行信用卡"',
  `account_type` varchar(20) NOT NULL DEFAULT 'debt' COMMENT '账户类型：debt普通负债/credit赊账应付',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `balance` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '当前欠款（元）',
  `total_amount` decimal(18,2) DEFAULT NULL COMMENT '总额（赊账类用）',
  `paid_amount` decimal(18,2) DEFAULT NULL COMMENT '已还（赊账类用）',
  `settle_status` varchar(20) DEFAULT NULL COMMENT '结算状态',
  `principal` decimal(18,2) DEFAULT NULL COMMENT '初始本金（元）',
  `annual_rate` decimal(10,4) DEFAULT NULL COMMENT '年利率（%）',
  `total_terms` int DEFAULT NULL COMMENT '总期数（月）',
  `paid_terms` int DEFAULT NULL COMMENT '已还期数（月）',
  `monthly_payment` decimal(18,2) DEFAULT NULL COMMENT '每期还款额（元）',
  `repayment_day` tinyint DEFAULT NULL COMMENT '还款日（每月几号，1-28）',
  `due_date` date DEFAULT NULL COMMENT '到期日',
  `risk_level` varchar(20) NOT NULL DEFAULT 'normal' COMMENT '风险标记',
  `include_in_total` tinyint NOT NULL DEFAULT '1' COMMENT '是否计入总负债：1=是 0=否',
  `icon` varchar(255) DEFAULT NULL COMMENT '图标：emoji key 或图片URL',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1=启用 0=停用归档（手动删除）',
  `settle_flag` tinyint NOT NULL DEFAULT '0' COMMENT '已结清：1=是 0=否（还款至0自动置位；归档展示、不计入当前总负债）',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='记账-负债账户';


-- ----------------------------------------------------------------------------
-- 表：ledger_net_worth_snapshot    [记账-净资产每日快照]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ledger_net_worth_snapshot`;
CREATE TABLE `ledger_net_worth_snapshot` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL COMMENT '门户用户ID（portal_user.id）',
  `snap_date` date NOT NULL COMMENT '快照日期（每日定时任务生成；当日有记账实时upsert）',
  `total_asset` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '总资产（元）',
  `total_liability` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '总负债（元）',
  `net_worth` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '净资产（元）= total_asset - total_liability',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_date` (`user_id`,`snap_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='记账-净资产每日快照';


-- ----------------------------------------------------------------------------
-- 表：ledger_schedule_task    [记账-定时记账任务]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ledger_schedule_task`;
CREATE TABLE `ledger_schedule_task` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL COMMENT '门户用户ID（portal_user.id）',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务名称',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '记账类型：income/expense',
  `amount` decimal(18,2) NOT NULL COMMENT '金额（元）',
  `category_id` bigint DEFAULT NULL COMMENT '分类ID（ledger_category）',
  `account_id` bigint DEFAULT NULL COMMENT '关联资产账户ID（支出扣款/收入入账）',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `cycle` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '执行周期：daily/weekly/monthly/interval',
  `day_of_week` tinyint DEFAULT NULL COMMENT '每周几（1-7，weekly 用）',
  `day_of_month` tinyint DEFAULT NULL COMMENT '每月几号（1-28，monthly 用）',
  `interval_days` int DEFAULT NULL COMMENT '间隔天数（interval 用）',
  `exec_time` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '08:00' COMMENT '执行时间 HH:mm',
  `start_date` date NOT NULL COMMENT '开始日期',
  `end_date` date DEFAULT NULL COMMENT '结束日期（可选，到期自动停用）',
  `next_exec_date` date DEFAULT NULL COMMENT '下次执行日期',
  `enabled` tinyint NOT NULL DEFAULT '1' COMMENT '启用状态：1=启用 0=停用',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1=正常 0=已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `notify_enabled` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否对该任务发送邮件提醒（默认关，避免每日轰炸）',
  `last_notify_at` datetime DEFAULT NULL COMMENT '上次发送提醒邮件的时间（节流用）',
  PRIMARY KEY (`id`),
  KEY `idx_user_status` (`user_id`,`status`),
  KEY `idx_next_exec` (`enabled`,`status`,`next_exec_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='记账-定时记账任务';


-- ----------------------------------------------------------------------------
-- 表：ledger_schedule_log    [记账-定时记账执行日志]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ledger_schedule_log`;
CREATE TABLE `ledger_schedule_log` (
  `id` bigint NOT NULL,
  `task_id` bigint NOT NULL COMMENT '任务ID（ledger_schedule_task.id）',
  `user_id` bigint NOT NULL COMMENT '门户用户ID（冗余）',
  `exec_date` date NOT NULL COMMENT '执行日期',
  `amount` decimal(18,2) NOT NULL COMMENT '金额（元）',
  `status` tinyint NOT NULL COMMENT '状态：1=成功 2=失败',
  `fail_reason` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '失败原因（如余额不足）',
  `transaction_id` bigint DEFAULT NULL COMMENT '生成的流水ID（ledger_transaction.id）',
  `retry_count` int NOT NULL DEFAULT '0' COMMENT '重试次数',
  `trigger_type` tinyint NOT NULL DEFAULT '1' COMMENT '触发来源：1=自动调度 2=页面主动执行',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_date` (`task_id`,`exec_date`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='记账-定时记账执行日志';


-- ----------------------------------------------------------------------------
-- 表：ledger_app_feature_config    [记账小程序功能入口配置（可视化运营）]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ledger_app_feature_config`;
CREATE TABLE `ledger_app_feature_config` (
  `id` bigint NOT NULL,
  `feature_key` varchar(50) NOT NULL COMMENT '功能标识（前端路由映射键）',
  `feature_name` varchar(50) NOT NULL COMMENT '功能名称',
  `icon` varchar(50) DEFAULT NULL COMMENT '图标（emoji/符号）',
  `icon_color` varchar(20) DEFAULT NULL COMMENT '图标颜色',
  `group_type` varchar(20) NOT NULL DEFAULT 'main' COMMENT '分组: main-主功能宫格/recommend-推荐小功能',
  `sort_num` int NOT NULL DEFAULT '0' COMMENT '组内排序（升序）',
  `visible` tinyint NOT NULL DEFAULT '1' COMMENT '是否展示: 1-展示 0-隐藏',
  `status` varchar(10) NOT NULL DEFAULT 'done' COMMENT '状态: done-已上线/dev-开发中',
  `badge` varchar(20) DEFAULT NULL COMMENT '角标文案（NEW 等）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_feature_key` (`feature_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='记账小程序功能入口配置（可视化运营）';


-- ----------------------------------------------------------------------------
-- 表：ledger_ai_analysis_report    [AI 财务分析报告月度快照]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ledger_ai_analysis_report`;
CREATE TABLE `ledger_ai_analysis_report` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL COMMENT '门户用户ID（portal_user.id）',
  `period` varchar(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '报告月份 yyyy-MM（每月一条，刷新覆盖）',
  `analysis_range` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT 'month' COMMENT '分析范围: month-本月/3m-近3个月/6m-近6个月/year-近12个月',
  `health_score` int NOT NULL DEFAULT '0' COMMENT '财务健康分 0-100',
  `metrics_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '核心指标 JSON（资产负债率/月均收支/储蓄率等）',
  `income_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '收入来源占比 JSON',
  `risk_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '债务风险列表 JSON',
  `advice_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '建议列表 JSON',
  `ai_summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT 'LLM 综述（失败时为模板文案）',
  `ai_enabled` tinyint NOT NULL DEFAULT '0' COMMENT '综述是否为 LLM 生成：1=是 0=模板降级',
  `profile_snapshot` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '当次画像快照（身份/职位/公司，用于历史回看）',
  `data_fingerprint` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '数据指纹（流水/资产/负债/画像变更痕迹，变化自动失效快照）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_period_range` (`user_id`,`period`,`analysis_range`),
  KEY `idx_user_period` (`user_id`,`period`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='AI 财务分析报告月度快照';


-- ----------------------------------------------------------------------------
-- 表：ledger_product_category    [商品类目（两级树）]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ledger_product_category`;
CREATE TABLE `ledger_product_category` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL DEFAULT '0' COMMENT '归属用户，0=系统预设',
  `name` varchar(50) NOT NULL COMMENT '类目名称',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父类目ID，0=一级',
  `icon` varchar(255) DEFAULT NULL COMMENT '图标：emoji key 或图片URL',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `is_system` tinyint NOT NULL DEFAULT '0' COMMENT '1=系统预设 0=用户自建',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1=启用 0=停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_parent_name` (`user_id`,`parent_id`,`name`),
  KEY `idx_user` (`user_id`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品类目（两级树）';


-- ----------------------------------------------------------------------------
-- 表：ledger_product    [记账-商品]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ledger_product`;
CREATE TABLE `ledger_product` (
  `id` bigint NOT NULL,
  `warehouse_id` bigint NOT NULL COMMENT '所属仓库ID',
  `shelf_id` bigint NOT NULL DEFAULT '0' COMMENT '所在货架（0=未上架）',
  `quantity` int NOT NULL DEFAULT '0' COMMENT '当前库存数量（原 ledger_stock.quantity）',
  `cost_value` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '库存成本价值（成本价×数量，原 ledger_stock.cost_value）',
  `user_id` bigint NOT NULL COMMENT '创建者用户ID（冗余，便于按人查）',
  `provider_user_id` bigint DEFAULT NULL COMMENT '提供者：谁把这批货放进仓库（NULL=仓库主）',
  `name` varchar(200) NOT NULL COMMENT '商品名称',
  `category_id` bigint DEFAULT NULL COMMENT '类目ID（ledger_product_category.id）',
  `barcode` varchar(50) DEFAULT NULL COMMENT '条码',
  `unit` varchar(20) NOT NULL DEFAULT '个' COMMENT '计量单位',
  `base_unit` varchar(20) DEFAULT NULL COMMENT '基础单位（库存与成本按它记）',
  `pack_unit` varchar(20) DEFAULT NULL COMMENT '包装单位（如 箱）',
  `pack_size` int DEFAULT NULL COMMENT '换算系数：1 包装单位 = N 基础单位',
  `cost_price` decimal(18,2) DEFAULT NULL COMMENT '成本价（元）',
  `cost_total` decimal(18,2) DEFAULT NULL COMMENT '成本总额（权威值）',
  `sell_price` decimal(18,2) DEFAULT NULL COMMENT '售价（元）',
  `production_date` date DEFAULT NULL COMMENT '生产日期',
  `shelf_life_days` int DEFAULT NULL COMMENT '保质期天数',
  `expiry_date` date DEFAULT NULL COMMENT '到期日期（生产日期+保质期，可手改）',
  `packaging_type` varchar(20) NOT NULL DEFAULT 'none' COMMENT '包装类型：none无需 standard标准 fragile防摔 custom特殊',
  `tags` varchar(500) DEFAULT NULL COMMENT '标签，逗号分隔',
  `description` varchar(1000) DEFAULT NULL COMMENT '商品描述',
  `image_urls` text COMMENT '图片地址（JSON 数组字符串）',
  `condition_grade` varchar(20) DEFAULT NULL COMMENT '成色（二手用）',
  `source_channel` varchar(50) DEFAULT NULL COMMENT '来源渠道',
  `source_type` varchar(20) NOT NULL DEFAULT 'purchase' COMMENT '来源：purchase采购/self_made自产/used_resale自用转卖/gift赠品',
  `source_party` varchar(100) DEFAULT NULL COMMENT '供应商/赠送方（自由文本）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '行状态：1=正常 0=已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `last_moved_at` datetime DEFAULT NULL COMMENT '最后动销时间（库存变动时刷新，用于滞销判定）',
  PRIMARY KEY (`id`),
  KEY `idx_warehouse` (`warehouse_id`,`status`),
  KEY `idx_user` (`user_id`),
  KEY `idx_category` (`category_id`),
  KEY `idx_product_shelf` (`shelf_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='记账-商品';


-- ----------------------------------------------------------------------------
-- 表：ledger_product_channel    [商品可售渠道]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ledger_product_channel`;
CREATE TABLE `ledger_product_channel` (
  `id` bigint NOT NULL,
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `channel_id` bigint NOT NULL COMMENT '渠道ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_channel` (`product_id`,`channel_id`),
  KEY `idx_channel` (`channel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品可售渠道';


-- ----------------------------------------------------------------------------
-- 表：ledger_warehouse    [记账-仓库（个人/家庭/合伙）]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ledger_warehouse`;
CREATE TABLE `ledger_warehouse` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL COMMENT '创建者用户ID（主体）',
  `name` varchar(100) NOT NULL COMMENT '仓库名称',
  `warehouse_type` varchar(20) NOT NULL DEFAULT 'personal' COMMENT '仓库模式：personal=个人仓 family=家庭仓 partner=合伙仓',
  `is_active` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用：1=启用 0=停用',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '行状态：1=正常 0=已删除',
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_status` (`user_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='记账-仓库（个人/家庭/合伙）';


-- ----------------------------------------------------------------------------
-- 表：ledger_warehouse_config    [仓库常用配置]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ledger_warehouse_config`;
CREATE TABLE `ledger_warehouse_config` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL COMMENT '归属用户',
  `warehouse_id` bigint NOT NULL DEFAULT '0' COMMENT '仓库ID，0=用户全局配置',
  `cfg_key` varchar(50) NOT NULL COMMENT '配置键：lowStock/showValue/defaultReason/pageSize/showTags/showExpiry',
  `cfg_value` varchar(500) DEFAULT NULL COMMENT '配置值（字符串/JSON）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_wh_key` (`user_id`,`warehouse_id`,`cfg_key`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='仓库常用配置';


-- ----------------------------------------------------------------------------
-- 表：ledger_warehouse_member    [记账-仓库成员]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ledger_warehouse_member`;
CREATE TABLE `ledger_warehouse_member` (
  `id` bigint NOT NULL,
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `user_id` bigint NOT NULL COMMENT '成员用户ID',
  `role` varchar(20) NOT NULL DEFAULT 'member' COMMENT '角色：owner=创建者 manager=管理 member=成员',
  `invited_by` bigint DEFAULT NULL COMMENT '邀请人用户ID',
  `joined_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '行状态：1=正常 0=已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_warehouse_user` (`warehouse_id`,`user_id`),
  KEY `idx_user` (`user_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='记账-仓库成员';


-- ----------------------------------------------------------------------------
-- 表：ledger_shelf    [仓库货架（存放位置）]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ledger_shelf`;
CREATE TABLE `ledger_shelf` (
  `id` bigint NOT NULL,
  `warehouse_id` bigint NOT NULL COMMENT '归属：所属仓库',
  `user_id` bigint NOT NULL COMMENT '归属：归属人',
  `code` varchar(50) NOT NULL COMMENT '货架编号（仓库内唯一，如 A-01）',
  `name` varchar(100) NOT NULL COMMENT '货架名称',
  `zone` varchar(50) DEFAULT NULL COMMENT '区域/分区（如 A区、冷藏区）',
  `row_no` int DEFAULT NULL COMMENT '排',
  `layer_no` int DEFAULT NULL COMMENT '层',
  `position_no` int DEFAULT NULL COMMENT '位',
  `location_text` varchar(200) DEFAULT NULL COMMENT '位置描述（后端按区/排/层/位自动拼装）',
  `shelf_type` varchar(20) NOT NULL DEFAULT 'normal' COMMENT '类型：normal/refrigerated/locked/temp',
  `capacity` int DEFAULT NULL COMMENT '容量（件，超容量仅提示不阻断）',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1=启用 0=停用',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_warehouse_code` (`warehouse_id`,`code`),
  KEY `idx_warehouse` (`warehouse_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='仓库货架（存放位置）';


-- ----------------------------------------------------------------------------
-- 表：ledger_stock_flow    [记账-库存流水]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ledger_stock_flow`;
CREATE TABLE `ledger_stock_flow` (
  `id` bigint NOT NULL,
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `shelf_id` bigint NOT NULL DEFAULT '0' COMMENT '本次操作货架（移库时为源货架），0=未上架',
  `to_shelf_id` bigint NOT NULL DEFAULT '0' COMMENT '移库目标货架（非移库为0）',
  `flow_type` varchar(30) NOT NULL COMMENT '变动类型：in=入库 out=出库 adjust=盘点调整',
  `reason` varchar(20) DEFAULT NULL COMMENT '原因：purchase/sale/self_use/loss/transfer/gift/stocktake/other',
  `quantity_before` int DEFAULT NULL COMMENT '变动前数量',
  `quantity_change` int NOT NULL COMMENT '变动量（正入负出）',
  `quantity_after` int NOT NULL COMMENT '变动后数量',
  `cost_before` decimal(18,2) DEFAULT NULL COMMENT '变动前成本价值',
  `cost_after` decimal(18,2) DEFAULT NULL COMMENT '变动后成本价值',
  `sale_price` decimal(18,2) DEFAULT NULL COMMENT '销售成交单价（仅出库原因为销售时填写）',
  `extra_cost` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '额外成本（运费/包装费），只影响本笔毛利',
  `channel_id` bigint DEFAULT NULL COMMENT '售卖渠道ID（ledger_sale_channel.id，卖出时填）',
  `operator_id` bigint NOT NULL COMMENT '操作人用户ID',
  `related_order` varchar(50) DEFAULT NULL COMMENT '关联单据号（销售/采购/盘点）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '行状态：1=正常 0=已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_warehouse_time` (`warehouse_id`,`create_time`),
  KEY `idx_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='记账-库存流水';


-- ----------------------------------------------------------------------------
-- 表：ledger_purchase_order    [记账-采购单]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ledger_purchase_order`;
CREATE TABLE `ledger_purchase_order` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL COMMENT '归属用户ID',
  `warehouse_id` bigint NOT NULL COMMENT '入库仓库ID',
  `order_no` varchar(50) DEFAULT NULL COMMENT '单号',
  `supplier_name` varchar(100) NOT NULL COMMENT '供应商名称（赊账时=应付对方）',
  `supplier_id` bigint DEFAULT NULL,
  `payment_type` varchar(20) NOT NULL DEFAULT 'cash' COMMENT '付款方式：cash=现结 credit=赊账',
  `total_amount` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '应付总额',
  `linked_account_id` bigint DEFAULT NULL COMMENT '赊账时关联的应收/应付账户ID（ledger_asset_account/ledger_liability_account）',
  `transaction_id` bigint DEFAULT NULL COMMENT '现结时关联的资金流水ID',
  `operator_id` bigint NOT NULL COMMENT '操作人用户ID',
  `remark` varchar(500) DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '行状态：1=正常 0=已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_wh` (`user_id`,`warehouse_id`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_linked_account` (`linked_account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='记账-采购单';


-- ----------------------------------------------------------------------------
-- 表：ledger_purchase_order_item    [记账-采购明细]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ledger_purchase_order_item`;
CREATE TABLE `ledger_purchase_order_item` (
  `id` bigint NOT NULL,
  `purchase_id` bigint NOT NULL COMMENT '采购单ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `quantity` int NOT NULL DEFAULT '1' COMMENT '数量',
  `unit_price` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '单价',
  `subtotal` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '小计',
  `status` tinyint NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `idx_purchase` (`purchase_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='记账-采购明细';


-- ----------------------------------------------------------------------------
-- 表：ledger_sale_order    [记账-销售单]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ledger_sale_order`;
CREATE TABLE `ledger_sale_order` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL COMMENT '归属用户ID',
  `warehouse_id` bigint NOT NULL COMMENT '出库仓库ID',
  `order_no` varchar(50) DEFAULT NULL COMMENT '单号',
  `customer_name` varchar(100) NOT NULL COMMENT '客户名称（赊账时=应收对方）',
  `customer_id` bigint DEFAULT NULL COMMENT '客户用户ID（对方是系统用户时）',
  `payment_type` varchar(20) NOT NULL DEFAULT 'cash' COMMENT '收款方式：cash=现结 credit=赊账',
  `total_amount` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '应收总额（按售价）',
  `cost_amount` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '成本总额',
  `profit_amount` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '利润（售价-成本）',
  `linked_account_id` bigint DEFAULT NULL COMMENT '赊账时关联的应收/应付账户ID（ledger_asset_account/ledger_liability_account）',
  `transaction_id` bigint DEFAULT NULL COMMENT '现结时关联的资金流水ID',
  `operator_id` bigint NOT NULL COMMENT '操作人用户ID',
  `remark` varchar(500) DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '行状态：1=正常 0=已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_wh` (`user_id`,`warehouse_id`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_linked_account` (`linked_account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='记账-销售单';


-- ----------------------------------------------------------------------------
-- 表：ledger_sale_order_item    [记账-销售明细]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ledger_sale_order_item`;
CREATE TABLE `ledger_sale_order_item` (
  `id` bigint NOT NULL,
  `sale_id` bigint NOT NULL COMMENT '销售单ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `quantity` int NOT NULL DEFAULT '1' COMMENT '数量',
  `unit_price` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '售价',
  `cost_price` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '成本价（结转成本）',
  `subtotal` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '小计（售价×数量）',
  `cost_subtotal` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '成本小计',
  `profit` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '利润',
  `status` tinyint NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `idx_sale` (`sale_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='记账-销售明细';


-- ----------------------------------------------------------------------------
-- 表：ledger_sale_channel    [售卖渠道]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ledger_sale_channel`;
CREATE TABLE `ledger_sale_channel` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL DEFAULT '0' COMMENT '归属用户，0=系统预设',
  `name` varchar(50) NOT NULL COMMENT '渠道名称（抖音/淘宝/闲鱼…）',
  `icon` varchar(255) DEFAULT NULL COMMENT '图标：emoji key 或图片URL',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `is_system` tinyint NOT NULL DEFAULT '0' COMMENT '1=系统预设 0=用户自建',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1=启用 0=停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_name` (`user_id`,`name`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='售卖渠道';


-- ----------------------------------------------------------------------------
-- 表：ledger_source_channel    [进货渠道]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ledger_source_channel`;
CREATE TABLE `ledger_source_channel` (
  `id` bigint NOT NULL COMMENT '主键（雪花ID）',
  `user_id` bigint NOT NULL COMMENT '归属用户（0=系统预设）',
  `name` varchar(50) NOT NULL COMMENT '渠道名称',
  `icon` varchar(255) DEFAULT NULL COMMENT '图标（emoji 或图片地址）',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `is_system` tinyint NOT NULL DEFAULT '0' COMMENT '是否系统预设：1=是 0=用户自建',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1=启用 0=停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_src_channel_user` (`user_id`),
  KEY `idx_src_channel_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='进货渠道';


-- ----------------------------------------------------------------------------
-- 表：ledger_invite    [邀请码]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ledger_invite`;
CREATE TABLE `ledger_invite` (
  `id` bigint NOT NULL,
  `code` varchar(16) NOT NULL COMMENT '邀请码（6 位，去易混字符）',
  `type` varchar(20) NOT NULL DEFAULT 'warehouse' COMMENT 'warehouse=加入仓库',
  `warehouse_id` bigint NOT NULL DEFAULT '0' COMMENT '目标仓库',
  `role` varchar(20) NOT NULL DEFAULT 'member' COMMENT '加入后角色',
  `created_by` bigint NOT NULL COMMENT '邀请人',
  `expire_at` datetime DEFAULT NULL COMMENT '过期时间',
  `max_use` int NOT NULL DEFAULT '10' COMMENT '可用次数上限',
  `used_count` int NOT NULL DEFAULT '0' COMMENT '已使用次数',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1 有效 0 已撤销',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_warehouse` (`warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='邀请码';


-- ==============================================================================
-- 六、已下线模块归档（vip_* / pay_* / *_tip_order / portal_creator_settlement）
-- 2026-10 全站免费化后已无代码引用；保留结构仅为兼容历史数据与后续恢复，可整段跳过。
-- ==============================================================================

-- ----------------------------------------------------------------------------
-- 表：vip_tier    [VIP等级（一端一套，类比 sys_role）]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `vip_tier`;
CREATE TABLE `vip_tier` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '等级ID',
  `platform_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '端代码（sys_platform.platform_code）',
  `tier_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '等级代码',
  `tier_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '等级名称',
  `duration_days` int DEFAULT NULL COMMENT '有效天数（-1=永久，0=免费tier）',
  `price` decimal(18,2) DEFAULT NULL COMMENT '价格（元）',
  `original_price` decimal(18,2) DEFAULT NULL COMMENT '划线原价（元，可空）',
  `popular` tinyint DEFAULT '0' COMMENT '是否推荐（1=售卖页推荐展示）',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '等级说明',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `status` tinyint DEFAULT '1' COMMENT '状态（1上架 0下架）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_platform_tier` (`platform_code`,`tier_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='VIP等级（一端一套，类比 sys_role）';


-- ----------------------------------------------------------------------------
-- 表：vip_tier_benefit    [等级权益关联（类比 sys_role_menu）]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `vip_tier_benefit`;
CREATE TABLE `vip_tier_benefit` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `platform_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '端代码',
  `tier_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '等级代码',
  `benefit_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '权益代码',
  `benefit_value` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '额度（unlimited 或数字）',
  `period` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'month' COMMENT '统计周期（day/month/year/unlimited）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tier_benefit` (`platform_code`,`tier_code`,`benefit_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='等级权益关联（类比 sys_role_menu）';


-- ----------------------------------------------------------------------------
-- 表：vip_benefit    [VIP权益定义（类比 sys_menu）]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `vip_benefit`;
CREATE TABLE `vip_benefit` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '权益ID',
  `platform_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '端代码',
  `benefit_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '权益代码（统一 {action} 命名，端级隔离）',
  `benefit_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '权益名称',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '描述',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_platform_benefit` (`platform_code`,`benefit_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='VIP权益定义（类比 sys_menu）';


-- ----------------------------------------------------------------------------
-- 表：vip_benefit_usage    [权益使用记录（consume=true 的次数统计）]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `vip_benefit_usage`;
CREATE TABLE `vip_benefit_usage` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `platform_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '端代码',
  `benefit_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '权益代码',
  `usage_count` int DEFAULT '1' COMMENT '当日累计使用次数',
  `usage_date` date DEFAULT NULL COMMENT '使用日期',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_benefit_date` (`user_id`,`platform_code`,`benefit_code`,`usage_date`),
  KEY `idx_user_date` (`user_id`,`usage_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='权益使用记录（consume=true 的次数统计）';


-- ----------------------------------------------------------------------------
-- 表：vip_user_card    [用户会员卡（类比 sys_user_role，一端一卡续费顺延）]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `vip_user_card`;
CREATE TABLE `vip_user_card` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会员卡ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `platform_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '端代码',
  `tier_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '当前等级代码',
  `order_id` bigint DEFAULT NULL COMMENT '最近一次支付订单（pay_order.id）',
  `start_time` datetime DEFAULT NULL COMMENT '生效时间',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间（永久为 NULL）',
  `status` tinyint DEFAULT '1' COMMENT '状态（1有效 0过期）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_platform` (`user_id`,`platform_code`),
  KEY `idx_expire` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户会员卡（类比 sys_user_role，一端一卡续费顺延）';


-- ----------------------------------------------------------------------------
-- 表：vip_api_registry    [VIP接口注册表（@VipOnly 启动扫描生成）]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `vip_api_registry`;
CREATE TABLE `vip_api_registry` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `api_path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '接口路径',
  `http_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'HTTP方法',
  `controller_class` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'Controller类全名',
  `method_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '方法名',
  `platform_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '端代码',
  `benefit_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '权益代码',
  `consume` tinyint DEFAULT '1' COMMENT '是否消耗次数（1消耗 0仅校验）',
  `message` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '校验失败提示',
  `api_desc` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '接口描述（运营填写）',
  `enabled` tinyint DEFAULT '1' COMMENT '是否启用校验（0=后台禁用该接口校验）',
  `scan_time` datetime DEFAULT NULL COMMENT '最近扫描时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_api` (`api_path`,`http_method`),
  KEY `idx_platform` (`platform_code`),
  KEY `idx_benefit` (`benefit_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='VIP接口注册表（@VipOnly 启动扫描生成）';


-- ----------------------------------------------------------------------------
-- 表：pay_order    [支付订单（公共支付通道）]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `pay_order`;
CREATE TABLE `pay_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `pay_no` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '支付单号（全局唯一，如 PAY20260902xxxx）',
  `biz_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务类型：tip=打赏 / member=会员 / course=课程（后续扩展）',
  `biz_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务单号（如打赏单ID）',
  `user_id` bigint DEFAULT NULL COMMENT '下单用户（portal_user.id，v11.79 对账维度）',
  `platform_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '归属端代码（sys_platform.platform_code）',
  `channel` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '支付渠道：wechat / alipay（预留）',
  `amount` decimal(18,2) NOT NULL COMMENT '支付金额（元）',
  `subject` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '商品描述',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'CREATED' COMMENT '状态机：CREATED→PAID→SETTLED / CREATED→CLOSED',
  `code_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '微信 native 支付二维码链接（code_url）',
  `channel_order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '三方交易单号（微信 transaction_id）',
  `trade_state` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '三方交易状态（微信 trade_state：SUCCESS/NOTPAY/CLOSED等）',
  `expire_time` datetime DEFAULT NULL COMMENT '订单过期时间（超时未支付自动关单依据）',
  `pay_success_time` datetime DEFAULT NULL COMMENT '支付成功时间',
  `settle_time` datetime DEFAULT NULL COMMENT '分账完成时间',
  `closed_time` datetime DEFAULT NULL COMMENT '关单时间',
  `close_reason` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '关单原因：TIMEOUT / ADMIN_MANUAL_CLOSE',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `pay_no` (`pay_no`),
  KEY `idx_biz` (`biz_type`,`biz_no`),
  KEY `idx_status_expire` (`status`,`expire_time`),
  KEY `idx_channel_order` (`channel_order_no`),
  KEY `idx_pay_order_user` (`user_id`),
  KEY `idx_pay_order_platform` (`platform_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付订单（公共支付通道）';


-- ----------------------------------------------------------------------------
-- 表：pay_notify_log    [渠道回调日志]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `pay_notify_log`;
CREATE TABLE `pay_notify_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `channel` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '渠道：wechat',
  `pay_no` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '关联支付单（解析成功后回填）',
  `raw_body` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '回调报文（截断留存）',
  `verify_ok` tinyint NOT NULL DEFAULT '0' COMMENT '验签结果：1=通过 0=失败',
  `handled` tinyint NOT NULL DEFAULT '0' COMMENT '业务处理：1=成功 0=失败',
  `error_msg` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '失败原因',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `platform_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '归属端代码',
  PRIMARY KEY (`id`),
  KEY `idx_pay_no` (`pay_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='渠道回调日志';


-- ----------------------------------------------------------------------------
-- 表：pay_notification    [支付站内通知]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `pay_notification`;
CREATE TABLE `pay_notification` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `user_id` bigint NOT NULL COMMENT '接收用户',
  `notify_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '通知类型：pay=支付结果 / withdraw=提现 / account=账户',
  `ref_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '关联单号（支付单/提现单）',
  `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '通知标题',
  `content` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '通知内容',
  `read_flag` tinyint NOT NULL DEFAULT '0' COMMENT '已读：0=未读 1=已读',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `platform_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '归属端代码',
  PRIMARY KEY (`id`),
  KEY `idx_user_read` (`user_id`,`read_flag`,`create_time`),
  KEY `idx_pay_notification_platform` (`platform_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付站内通知';


-- ----------------------------------------------------------------------------
-- 表：pay_user_account    [用户资金账户（钱包）]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `pay_user_account`;
CREATE TABLE `pay_user_account` (
  `user_id` bigint NOT NULL COMMENT '用户ID（sys_user.user_id）',
  `balance` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '可用余额（元）',
  `total_income` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '累计收入（元，含打赏所得）',
  `total_withdraw` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '累计提现（元）',
  `frozen_amount` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '冻结金额（元，提现审核中占用）',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户资金账户（钱包）';


-- ----------------------------------------------------------------------------
-- 表：pay_user_bank_card    [用户银行卡（密文落库）]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `pay_user_bank_card`;
CREATE TABLE `pay_user_bank_card` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '银行卡ID',
  `user_id` bigint NOT NULL COMMENT '所属用户',
  `holder_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '持卡人姓名',
  `card_no_encrypted` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '卡号密文（AES-GCM）',
  `card_no_masked` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '卡号脱敏（6217 **** **** 1234）',
  `phone_encrypted` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '预留手机号密文（AES-GCM）',
  `bank_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '银行编码（如 ICBC）',
  `bank_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '银行名称（如 中国工商银行）',
  `is_default` tinyint NOT NULL DEFAULT '0' COMMENT '是否默认卡：1=是 0=否',
  `verify_status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING' COMMENT '验证状态：PENDING=待验证 / VERIFIED=已验证 / REJECTED=已驳回（v13.18 默认值改 fail-closed：直插 SQL 不得绕过四要素核验）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_card` (`user_id`,`card_no_masked`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户银行卡（密文落库）';


-- ----------------------------------------------------------------------------
-- 表：pay_withdraw_order    [提现订单（预留）]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `pay_withdraw_order`;
CREATE TABLE `pay_withdraw_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '提现单ID',
  `withdraw_no` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '提现单号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `bank_card_id` bigint NOT NULL COMMENT '收款银行卡ID',
  `amount` decimal(18,2) NOT NULL COMMENT '提现金额（元）',
  `fee` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '手续费（元）',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'auditing' COMMENT '状态：auditing=审核中 paid=已打款 rejected=已驳回（v11.79 统一小写）',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `reject_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '驳回原因',
  `paid_time` datetime DEFAULT NULL COMMENT '打款完成时间（真实出金到账）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `platform_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '归属端代码',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_withdraw_no` (`withdraw_no`),
  KEY `idx_user` (`user_id`,`create_time`),
  KEY `idx_pay_withdraw_platform` (`platform_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='提现订单（预留）';


-- ----------------------------------------------------------------------------
-- 表：pay_ledger_entry    [分账流水（复式记账）]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `pay_ledger_entry`;
CREATE TABLE `pay_ledger_entry` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '流水ID',
  `pay_no` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '关联支付单号',
  `biz_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务类型：tip / withdraw',
  `biz_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务单号',
  `account_role` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '账户角色：USER=用户 / PLATFORM=平台',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID（PLATFORM 分录为 NULL）',
  `platform_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '归属端代码',
  `direction` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '方向：credit=收入 / debit=支出',
  `amount` decimal(18,2) NOT NULL COMMENT '金额（元）',
  `balance_after` decimal(18,2) DEFAULT NULL COMMENT '交易后余额（元；PLATFORM 分录不追踪余额，为 NULL）',
  `summary` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务摘要，如"打赏收入-作者所得" / "平台服务费"',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_pay_no` (`pay_no`),
  KEY `idx_user` (`user_id`,`create_time`),
  KEY `idx_role` (`account_role`,`direction`),
  KEY `idx_pay_ledger_platform` (`platform_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='分账流水（复式记账）';


-- ----------------------------------------------------------------------------
-- 表：portal_tip_order    [打赏订单（复用为付费阅读购买记录，target_type=article_paid）]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_tip_order`;
CREATE TABLE `portal_tip_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '打赏者用户ID',
  `author_id` bigint NOT NULL COMMENT '被打赏者用户ID',
  `target_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '打赏对象类型 article/column/article_paid',
  `target_id` bigint NOT NULL COMMENT '打赏对象ID',
  `amount` decimal(18,2) NOT NULL COMMENT '打赏金额',
  `message` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '打赏留言',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'pending' COMMENT '状态 pending/paid/refunded',
  `pay_method` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '支付方式',
  `trade_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '第三方交易号（支付宝/微信返回的交易号）',
  `pay_channel` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'points' COMMENT '支付渠道：points-积分/alipay-支付宝/wechat-微信支付',
  `notify_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '支付回调ID（用于回调验签与幂等去重）',
  `notify_time` datetime DEFAULT NULL COMMENT '支付回调时间',
  `refund_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '退款单号',
  `refund_amount` decimal(18,2) DEFAULT NULL COMMENT '退款金额',
  `refund_time` datetime DEFAULT NULL COMMENT '退款时间',
  `refund_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '退款原因',
  `paid_time` datetime DEFAULT NULL COMMENT '支付时间',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_author` (`author_id`),
  KEY `idx_target` (`target_type`,`target_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_trade_no` (`trade_no`),
  KEY `idx_pay_channel_status` (`pay_channel`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='打赏订单（复用为付费阅读购买记录，target_type=article_paid）';


-- ----------------------------------------------------------------------------
-- 表：ledger_tip_order    [记账-打赏记录]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ledger_tip_order`;
CREATE TABLE `ledger_tip_order` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL COMMENT '门户用户ID（portal_user.id）',
  `amount` decimal(18,2) NOT NULL COMMENT '打赏金额（元）',
  `target` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'developer' COMMENT '打赏对象：developer=开发者 platform=平台',
  `reason` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '打赏理由（可选）',
  `pay_channel` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'wechat' COMMENT '支付渠道：wechat/alipay（v11.79 与 portal_tip_order 统一命名）',
  `pay_no` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '关联公共通道单据号（pay_order.pay_no；演示模式为空）',
  `client_uuid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '客户端幂等号（防重复提交，v11.80 对齐记一笔机制）',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'pending' COMMENT '状态：pending=待支付 paid=已支付（网关回调推进） refunded=已退款 closed=已关闭（v11.80 接公共通道）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '打赏时间',
  `paid_time` datetime DEFAULT NULL COMMENT '支付完成时间（网关回调置 paid 时写入，v11.80）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_client` (`user_id`,`client_uuid`),
  KEY `idx_user` (`user_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='记账-打赏记录';


-- ----------------------------------------------------------------------------
-- 表：portal_creator_settlement    [创作者分成结算]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `portal_creator_settlement`;
CREATE TABLE `portal_creator_settlement` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `creator_id` bigint NOT NULL COMMENT '创作者用户ID',
  `period` varchar(16) NOT NULL COMMENT '结算周期，格式 yyyy-MM，如 2026-07',
  `tip_income` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '打赏收入（当月已支付打赏总额）',
  `paid_read_income` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '付费阅读收入（当月已支付购买总额）',
  `column_income` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '专栏订阅收入（当月已支付订阅总额）',
  `total_income` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '总收入（三项之和）',
  `platform_fee` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '平台抽成（total_income * platform_fee_rate）',
  `creator_income` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '创作者实得（total_income - platform_fee）',
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


-- ==============================================================================
-- 七、定时任务模块（sys_job + qrtz_*）：必须最后创建
-- qrtz_triggers → qrtz_job_details 及其子表存在外键，建表顺序为 主表 → 从表。
-- ==============================================================================

-- ----------------------------------------------------------------------------
-- 表：sys_job    [定时任务调度表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_job`;
CREATE TABLE `sys_job` (
  `job_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `job_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '任务名称',
  `job_group` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'DEFAULT' COMMENT '任务组名',
  `invoke_target` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调用目标字符串',
  `cron_expression` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT 'cron执行表达式',
  `misfire_policy` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '3' COMMENT '计划执行错误策略（1立即执行 2执行一次 3放弃执行）',
  `concurrent` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '1' COMMENT '是否并发执行（0允许 1禁止）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '状态（0正常 1暂停）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '备注信息',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '删除标记（0=存在 2=删除）',
  PRIMARY KEY (`job_id`,`job_name`,`job_group`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='定时任务调度表';


-- ----------------------------------------------------------------------------
-- 表：qrtz_job_details    [任务详细信息表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `qrtz_job_details`;
CREATE TABLE `qrtz_job_details` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `job_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务名称',
  `job_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务组名',
  `description` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '相关介绍',
  `job_class_name` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '执行任务类名称',
  `is_durable` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '是否持久化',
  `is_nonconcurrent` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '是否并发',
  `is_update_data` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '是否更新数据',
  `requests_recovery` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '是否接受恢复执行',
  `job_data` blob COMMENT '存放持久化job对象',
  PRIMARY KEY (`sched_name`,`job_name`,`job_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务详细信息表';


-- ----------------------------------------------------------------------------
-- 表：qrtz_triggers    [触发器详细信息表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `qrtz_triggers`;
CREATE TABLE `qrtz_triggers` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '触发器的名字',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '触发器所属组的名字',
  `job_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_job_details表job_name的外键',
  `job_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_job_details表job_group的外键',
  `description` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '相关介绍',
  `next_fire_time` bigint DEFAULT NULL COMMENT '上一次触发时间（毫秒）',
  `prev_fire_time` bigint DEFAULT NULL COMMENT '下一次触发时间（默认为-1表示不触发）',
  `priority` int DEFAULT NULL COMMENT '优先级',
  `trigger_state` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '触发器状态',
  `trigger_type` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '触发器的类型',
  `start_time` bigint NOT NULL COMMENT '开始时间',
  `end_time` bigint DEFAULT NULL COMMENT '结束时间',
  `calendar_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '日程表名称',
  `misfire_instr` smallint DEFAULT NULL COMMENT '补偿执行的策略',
  `job_data` blob COMMENT '存放持久化job对象',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
  KEY `sched_name` (`sched_name`,`job_name`,`job_group`),
  CONSTRAINT `qrtz_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `job_name`, `job_group`) REFERENCES `qrtz_job_details` (`sched_name`, `job_name`, `job_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='触发器详细信息表';


-- ----------------------------------------------------------------------------
-- 表：qrtz_blob_triggers    [Blob类型的触发器表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `qrtz_blob_triggers`;
CREATE TABLE `qrtz_blob_triggers` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `blob_data` blob COMMENT '存放持久化Trigger对象',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
  CONSTRAINT `qrtz_blob_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Blob类型的触发器表';


-- ----------------------------------------------------------------------------
-- 表：qrtz_simple_triggers    [简单触发器的信息表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `qrtz_simple_triggers`;
CREATE TABLE `qrtz_simple_triggers` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `repeat_count` bigint NOT NULL COMMENT '重复的次数统计',
  `repeat_interval` bigint NOT NULL COMMENT '重复的间隔时间',
  `times_triggered` bigint NOT NULL COMMENT '已经触发的次数',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
  CONSTRAINT `qrtz_simple_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='简单触发器的信息表';


-- ----------------------------------------------------------------------------
-- 表：qrtz_cron_triggers    [Cron类型的触发器表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `qrtz_cron_triggers`;
CREATE TABLE `qrtz_cron_triggers` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `cron_expression` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'cron表达式',
  `time_zone_id` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '时区',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
  CONSTRAINT `qrtz_cron_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Cron类型的触发器表';


-- ----------------------------------------------------------------------------
-- 表：qrtz_simprop_triggers    [同步机制的行锁表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `qrtz_simprop_triggers`;
CREATE TABLE `qrtz_simprop_triggers` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `str_prop_1` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'String类型的trigger的第一个参数',
  `str_prop_2` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'String类型的trigger的第二个参数',
  `str_prop_3` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'String类型的trigger的第三个参数',
  `int_prop_1` int DEFAULT NULL COMMENT 'int类型的trigger的第一个参数',
  `int_prop_2` int DEFAULT NULL COMMENT 'int类型的trigger的第二个参数',
  `long_prop_1` bigint DEFAULT NULL COMMENT 'long类型的trigger的第一个参数',
  `long_prop_2` bigint DEFAULT NULL COMMENT 'long类型的trigger的第二个参数',
  `dec_prop_1` decimal(13,4) DEFAULT NULL COMMENT 'decimal类型的trigger的第一个参数',
  `dec_prop_2` decimal(13,4) DEFAULT NULL COMMENT 'decimal类型的trigger的第二个参数',
  `bool_prop_1` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'Boolean类型的trigger的第一个参数',
  `bool_prop_2` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'Boolean类型的trigger的第二个参数',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
  CONSTRAINT `qrtz_simprop_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='同步机制的行锁表';


-- ----------------------------------------------------------------------------
-- 表：qrtz_calendars    [日历信息表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `qrtz_calendars`;
CREATE TABLE `qrtz_calendars` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `calendar_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '日历名称',
  `calendar` blob NOT NULL COMMENT '存放持久化calendar对象',
  PRIMARY KEY (`sched_name`,`calendar_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='日历信息表';


-- ----------------------------------------------------------------------------
-- 表：qrtz_fired_triggers    [已触发的触发器表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `qrtz_fired_triggers`;
CREATE TABLE `qrtz_fired_triggers` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `entry_id` varchar(95) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度器实例id',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `instance_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度器实例名',
  `fired_time` bigint NOT NULL COMMENT '触发的时间',
  `sched_time` bigint NOT NULL COMMENT '定时器制定的时间',
  `priority` int NOT NULL COMMENT '优先级',
  `state` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '状态',
  `job_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '任务名称',
  `job_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '任务组名',
  `is_nonconcurrent` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否并发',
  `requests_recovery` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否接受恢复执行',
  PRIMARY KEY (`sched_name`,`entry_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='已触发的触发器表';


-- ----------------------------------------------------------------------------
-- 表：qrtz_locks    [存储的悲观锁信息表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `qrtz_locks`;
CREATE TABLE `qrtz_locks` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `lock_name` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '悲观锁名称',
  PRIMARY KEY (`sched_name`,`lock_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='存储的悲观锁信息表';


-- ----------------------------------------------------------------------------
-- 表：qrtz_paused_trigger_grps    [暂停的触发器表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `qrtz_paused_trigger_grps`;
CREATE TABLE `qrtz_paused_trigger_grps` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  PRIMARY KEY (`sched_name`,`trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='暂停的触发器表';


-- ----------------------------------------------------------------------------
-- 表：qrtz_scheduler_state    [调度器状态表]
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `qrtz_scheduler_state`;
CREATE TABLE `qrtz_scheduler_state` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `instance_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '实例名称',
  `last_checkin_time` bigint NOT NULL COMMENT '上次检查时间',
  `checkin_interval` bigint NOT NULL COMMENT '检查间隔时间',
  PRIMARY KEY (`sched_name`,`instance_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='调度器状态表';


-- ==============================================================================
-- 收尾：恢复会话变量并做外键自检
-- ==============================================================================
SET FOREIGN_KEY_CHECKS = 1;
SET UNIQUE_CHECKS = 1;
SET SQL_NOTES = @OLD_SQL_NOTES;

-- 本部分结束。DDL 全部执行完毕，接下来按顺序执行 zyg_dml_*.sql