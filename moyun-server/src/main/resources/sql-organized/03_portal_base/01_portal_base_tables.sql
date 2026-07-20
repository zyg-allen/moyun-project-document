-- ============================================
-- 墨韵智库 - 门户基础模块建表 DDL（最终合并版）
-- ============================================
-- 合并来源脚本：
--   03_portal_init.sql                              （门户基础建表：portal_user/portal_category/portal_tag/portal_article/portal_article_tag/portal_comment/portal_bookmark/portal_like/portal_comment_like/portal_follow/portal_friend_link）
--   08_add_category_recommended_field.sql           （portal_article 新增 is_category_recommended 字段及索引）
--   09_modify_cover_field.sql                       （portal_article.cover 改为 TEXT 类型）
--   10_add_category_path_field.sql                  （portal_article 新增 root_category_id、category_path 字段及索引）
--   12_add_comment_fields.sql                       （portal_comment 新增 root_id、reply_to_content 字段及 idx_article_root 索引）
--   13_create_article_view_table.sql                （portal_article_view 建表）
--   14_V1.7_portal_user_add_extend_fields.sql       （portal_user 新增性别/生日/地点/社交链接/通知/隐私等扩展字段）
--   15_add_article_slug_field.sql                  （portal_article 新增 slug 字段及 uk_slug 唯一索引）
--   28_alter_tables_add_base_fields.sql            （本批表中 portal_entity_tag 已在 31 建表时直接包含审计字段，无需变更）
--   30_alter_portal_tag_fields.sql                 （portal_tag 的 module/reference_count 字段已在 03 建表时直接包含）
--   31_create_portal_entity_tag.sql                （portal_entity_tag 建表）
--   32_cleanup_tag_name_prefix.sql                 （portal_tag 数据迁移，无表结构变更）
--   33_alter_article_status_enum.sql               （portal_article.status 注释更新为 draft/pending/published/rejected/archived）
--   38_init_report_feedback_menu.sql               （portal_report、portal_feedback 建表，菜单部分本文件不含）
--   40_fix_bugs_v4.sql                             （portal_friend_link.status 注释统一；portal_report.description 改为 varchar(2000)，菜单部分本文件不含）
--   41_init_comment_like.sql                       （portal_comment_like 重建为评论+用户维度唯一约束版本，覆盖 03 旧结构）
--   59_paid_read_init.sql                          （portal_article 新增 is_paid/paid_content/preview_length/price 字段，配置部分本文件不含）
--   62_article_version_init.sql                    （portal_article_version 建表）
--   72_mention_report_init.sql                     （portal_report 新增 target_type/target_id 字段及 idx_target 索引）
--   86_portal_article_session_token.sql            （portal_article 新增 session_token 字段及 idx_session_token 索引）
-- 涉及表：
--   portal_user, portal_category, portal_tag, portal_article, portal_article_tag,
--   portal_comment, portal_comment_like, portal_article_view, portal_article_version,
--   portal_friend_link, portal_follow, portal_like, portal_bookmark,
--   portal_entity_tag, portal_report, portal_feedback
-- 说明：
--   - 本文件仅包含建表 DDL（CREATE TABLE IF NOT EXISTS），不含 INSERT 数据、菜单 UPDATE、配置 INSERT 等
--   - 所有 ALTER TABLE 已合并入对应的 CREATE TABLE
--   - 菜单修复类（38 中的 sys_menu INSERT、40 中的 sys_menu UPDATE）见其他目录
-- ============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 门户用户表
-- 来源：03 建表 + 14 扩展字段
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `user_id` bigint DEFAULT NULL COMMENT '关联后台用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `password` varchar(200) DEFAULT NULL COMMENT '密码',
  `avatar` varchar(500) DEFAULT NULL COMMENT '头像URL',
  `bio` varchar(500) DEFAULT NULL COMMENT '个人简介',
  `position` varchar(100) DEFAULT NULL COMMENT '职位',
  `wechat` varchar(100) DEFAULT NULL COMMENT '微信号',
  `gender` varchar(20) DEFAULT NULL COMMENT '性别：male-男，female-女，other-其他',
  `birthday` varchar(20) DEFAULT NULL COMMENT '生日：YYYY-MM-DD格式',
  `location` varchar(100) DEFAULT NULL COMMENT '所在城市：如北京市',
  `website` varchar(200) DEFAULT NULL COMMENT '个人网站URL',
  `github` varchar(100) DEFAULT NULL COMMENT 'GitHub用户名或完整URL',
  `company` varchar(200) DEFAULT NULL COMMENT '公司名称',
  `school` varchar(200) DEFAULT NULL COMMENT '学校名称',
  `language` varchar(20) DEFAULT NULL COMMENT '语言偏好：zh-CN，en-US等',
  `timezone` varchar(50) DEFAULT NULL COMMENT '时区：如Asia/Shanghai',
  `notify_like` tinyint(1) DEFAULT 1 COMMENT '是否接收点赞通知',
  `notify_comment` tinyint(1) DEFAULT 1 COMMENT '是否接收评论通知',
  `notify_follow` tinyint(1) DEFAULT 1 COMMENT '是否接收关注通知',
  `notify_system` tinyint(1) DEFAULT 1 COMMENT '是否接收系统通知',
  `privacy_follow` tinyint(1) DEFAULT 1 COMMENT '是否允许被关注',
  `privacy_bookmark` tinyint(1) DEFAULT 1 COMMENT '是否公开收藏夹',
  `privacy_email` tinyint(1) DEFAULT 0 COMMENT '是否公开邮箱',
  `privacy_phone` tinyint(1) DEFAULT 0 COMMENT '是否公开手机号',
  `role` varchar(20) DEFAULT 'user' COMMENT '角色：user/admin',
  `vip_expire_at` datetime DEFAULT NULL COMMENT 'VIP过期时间',
  `is_phone_verified` tinyint(1) DEFAULT '0' COMMENT '是否已验证手机号',
  `is_wechat_verified` tinyint(1) DEFAULT '0' COMMENT '是否已验证微信',
  `two_factor_enabled` tinyint(1) DEFAULT '0' COMMENT '是否开启两步验证',
  `status` char(1) DEFAULT '0' COMMENT '帐号状态（0正常 1停用）',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `login_ip` varchar(128) DEFAULT '' COMMENT '最后登录IP',
  `login_date` datetime DEFAULT NULL COMMENT '最后登录时间',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_email` (`email`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户用户表';

-- ----------------------------
-- 门户分类表
-- 来源：03 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` varchar(100) NOT NULL COMMENT '分类名称',
  `slug` varchar(100) DEFAULT NULL COMMENT '分类别名',
  `description` varchar(500) DEFAULT NULL COMMENT '分类描述',
  `icon` varchar(500) DEFAULT NULL COMMENT '图标URL',
  `sort` int DEFAULT '0' COMMENT '排序',
  `parent_id` bigint DEFAULT '0' COMMENT '父分类ID',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_slug` (`slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户分类表';

-- ----------------------------
-- 门户标签表
-- 来源：03 建表 + 30 字段补充（已在 03 中直接包含）+ 32 数据迁移（无结构变更）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `name` varchar(100) NOT NULL COMMENT '标签名称',
  `slug` varchar(100) DEFAULT NULL COMMENT '标签别名',
  `sort` int DEFAULT '0' COMMENT '排序',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `module` varchar(50) DEFAULT NULL COMMENT '所属模块（article/interview_question/interview_experience/interview_resume_template 等，null 表示通用）',
  `reference_count` bigint unsigned DEFAULT '0' COMMENT '被引用次数（冗余计数列，绑定/解绑时同步维护）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_slug` (`slug`),
  KEY `idx_module` (`module`),
  KEY `idx_reference_count` (`reference_count` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户标签表';

-- ----------------------------
-- 门户文章表
-- 来源：03 建表 + 08 + 09 + 10 + 15 + 28 + 33 + 59 + 86
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_article` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文章ID',
  `title` varchar(500) NOT NULL COMMENT '文章标题',
  `slug` varchar(500) DEFAULT NULL COMMENT '文章URL别名，用于SEO语义化路径',
  `content` longtext COMMENT '文章内容（HTML格式）',
  `excerpt` varchar(1000) DEFAULT NULL COMMENT '文章摘要',
  `cover` text DEFAULT NULL COMMENT '封面图片URL或Base64',
  `author_id` bigint NOT NULL COMMENT '作者ID（门户用户ID）',
  `category_id` bigint DEFAULT NULL COMMENT '分类ID',
  `root_category_id` bigint DEFAULT NULL COMMENT '顶级分类ID',
  `category_path` varchar(500) DEFAULT NULL COMMENT '分类路径，包含所有祖先ID，例如：1,3,5',
  `status` varchar(20) DEFAULT 'draft' COMMENT '状态：draft=草稿 / pending=待审核 / published=已发布 / rejected=已拒绝 / archived=已归档',
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
  `link` varchar(500) DEFAULT NULL COMMENT '外部链接',
  `editor_mode` varchar(20) DEFAULT 'richtext' COMMENT '编辑器模式：richtext/markdown',
  `session_token` varchar(64) DEFAULT NULL COMMENT '编辑会话标识（一次编辑会话唯一，用于草稿/发布幂等去重）',
  `content_markdown` text COMMENT 'Markdown 原始内容',
  `is_paid` tinyint NOT NULL DEFAULT 0 COMMENT '是否付费阅读 0=免费 1=付费',
  `paid_content` longtext COMMENT '付费内容（购买后可见）',
  `preview_length` int NOT NULL DEFAULT 0 COMMENT '试读字数（未购买可预览的字数）',
  `price` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '付费价格，0=免费',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_slug` (`slug`),
  KEY `idx_author_id` (`author_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_root_category_id` (`root_category_id`),
  KEY `idx_category_path` (`category_path`(100)),
  KEY `idx_status` (`status`),
  KEY `idx_is_featured` (`is_featured`),
  KEY `idx_is_top` (`is_top`),
  KEY `idx_is_category_recommended` (`is_category_recommended`),
  KEY `idx_published_at` (`published_at`),
  KEY `idx_views` (`views`),
  KEY `idx_likes` (`likes`),
  KEY `idx_session_token` (`session_token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户文章表';

-- ----------------------------
-- 文章标签关联表
-- 来源：03 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_article_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `tag_id` bigint NOT NULL COMMENT '标签ID',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_tag` (`article_id`,`tag_id`),
  KEY `idx_article_id` (`article_id`),
  KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='文章标签关联表';

-- ----------------------------
-- 门户评论表
-- 来源：03 建表 + 12 + 28（28 未涉及 portal_comment，已包含审计字段）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `author_id` bigint NOT NULL COMMENT '评论者ID（门户用户ID）',
  `content` text NOT NULL COMMENT '评论内容',
  `parent_id` bigint DEFAULT '0' COMMENT '父评论ID',
  `root_id` bigint DEFAULT '0' COMMENT '根评论ID（一级评论ID）',
  `reply_to` bigint DEFAULT NULL COMMENT '回复的用户ID',
  `reply_to_content` varchar(200) DEFAULT '' COMMENT '被回复的内容摘要',
  `like_count` bigint DEFAULT '0' COMMENT '点赞数',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_article_id` (`article_id`),
  KEY `idx_author_id` (`author_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_article_root` (`article_id`, `root_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户评论表';

-- ----------------------------
-- 门户评论点赞表
-- 来源：41 建表（覆盖 03 旧结构，使用 评论+用户 唯一约束）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_comment_like` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `comment_id` bigint NOT NULL COMMENT '评论ID',
  `user_id` bigint NOT NULL COMMENT '点赞用户ID',
  `create_time` datetime DEFAULT NULL COMMENT '点赞时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_comment_user` (`comment_id`, `user_id`) COMMENT '评论+用户唯一索引，防重复点赞',
  KEY `idx_user_id` (`user_id`) COMMENT '用户维度查询索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章评论点赞记录';

-- ----------------------------
-- 文章浏览记录表
-- 来源：13 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_article_view` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID（NULL表示游客）',
  `ip` varchar(50) DEFAULT NULL COMMENT 'IP地址',
  `view_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '浏览器User-Agent',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_article_id` (`article_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_ip` (`ip`),
  KEY `idx_view_time` (`view_time`),
  KEY `idx_article_user` (`article_id`, `user_id`),
  KEY `idx_article_ip` (`article_id`, `ip`),
  KEY `idx_article_viewtime` (`article_id`, `view_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='文章浏览记录表';

-- ----------------------------
-- 文章版本快照表
-- 来源：62 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_article_version` (
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
  KEY `idx_article_version` (`article_id`, `version_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章版本快照';

-- ----------------------------
-- 门户友情链接表
-- 来源：03 建表 + 40 status 注释修复（值已与 03 一致，仅注释统一）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_friend_link` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '链接ID',
  `name` varchar(100) NOT NULL COMMENT '链接名称',
  `url` varchar(500) NOT NULL COMMENT '链接地址',
  `description` varchar(500) DEFAULT NULL COMMENT '链接描述',
  `logo` varchar(500) DEFAULT NULL COMMENT 'Logo URL',
  `sort` int DEFAULT '0' COMMENT '排序',
  `status` varchar(20) DEFAULT '0' COMMENT '状态：0正常 1停用',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户友情链接表';

-- ----------------------------
-- 门户关注表
-- 来源：03 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_follow` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关注ID',
  `follower_id` bigint NOT NULL COMMENT '关注者ID（门户用户ID）',
  `following_id` bigint NOT NULL COMMENT '被关注者ID（门户用户ID）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_follower_following` (`follower_id`,`following_id`),
  KEY `idx_follower_id` (`follower_id`),
  KEY `idx_following_id` (`following_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户关注表';

-- ----------------------------
-- 门户点赞表（文章）
-- 来源：03 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_like` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '点赞ID',
  `user_id` bigint NOT NULL COMMENT '用户ID（门户用户ID）',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_article` (`user_id`,`article_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_article_id` (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户点赞表（文章）';

-- ----------------------------
-- 门户收藏表
-- 来源：03 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_bookmark` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `user_id` bigint NOT NULL COMMENT '用户ID（门户用户ID）',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_article` (`user_id`,`article_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_article_id` (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户收藏表';

-- ----------------------------
-- 通用实体标签关联表
-- 来源：31 建表（原脚本为 DROP TABLE + CREATE TABLE，整理为 CREATE TABLE IF NOT EXISTS）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_entity_tag` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tag_id` bigint unsigned NOT NULL COMMENT '标签ID（引用 portal_tag.id）',
  `entity_type` varchar(32) NOT NULL COMMENT '实体类型（article/interview_question/interview_experience/interview_resume_template/book 等）',
  `entity_id` bigint unsigned NOT NULL COMMENT '实体ID',
  `sort` int DEFAULT '0' COMMENT '排序',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag_entity` (`tag_id`, `entity_type`, `entity_id`),
  KEY `idx_entity` (`entity_type`, `entity_id`),
  KEY `idx_entity_create` (`entity_type`, `create_time`),
  KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通用实体标签关联表';

-- ----------------------------
-- 用户举报记录表
-- 来源：38 建表 + 40 description 改为 varchar(2000) + 72 target_type/target_id 字段及 idx_target 索引
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_report` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '举报ID',
  `report_type` varchar(32) NOT NULL COMMENT '举报类型：spam/inappropriate/infringement/fraud/other',
  `target_url` varchar(500) DEFAULT NULL COMMENT '举报目标URL',
  `target_type` varchar(32) DEFAULT NULL COMMENT '举报目标类型：comment/article/user 等，为空表示通用举报（仅 target_url）',
  `target_id` bigint DEFAULT NULL COMMENT '举报目标ID（评论/文章/用户ID，配合 target_type 使用）',
  `description` varchar(2000) NOT NULL COMMENT '问题描述',
  `contact` varchar(100) DEFAULT NULL COMMENT '联系方式（可选）',
  `images` varchar(1000) DEFAULT NULL COMMENT '图片证据（JSON数组，最多3张）',
  `user_id` bigint DEFAULT NULL COMMENT '举报人用户ID',
  `username` varchar(64) DEFAULT NULL COMMENT '举报人用户名（冗余）',
  `ip` varchar(128) DEFAULT NULL COMMENT '举报人IP',
  `status` varchar(20) DEFAULT 'pending' COMMENT '处理状态：pending/processing/resolved/rejected',
  `handler` varchar(64) DEFAULT NULL COMMENT '处理人',
  `handle_result` varchar(1000) DEFAULT NULL COMMENT '处理结果说明',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_report_type` (`report_type`),
  KEY `idx_status` (`status`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户举报记录表';

-- ----------------------------
-- 用户意见反馈表
-- 来源：38 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_feedback` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '反馈ID',
  `feedback_type` varchar(32) NOT NULL COMMENT '反馈类型：suggestion/bug/experience/other',
  `subject` varchar(200) DEFAULT NULL COMMENT '反馈主题',
  `description` varchar(2000) NOT NULL COMMENT '反馈详细描述',
  `contact` varchar(100) DEFAULT NULL COMMENT '联系方式（可选）',
  `user_id` bigint DEFAULT NULL COMMENT '反馈人用户ID',
  `username` varchar(64) DEFAULT NULL COMMENT '反馈人用户名（冗余）',
  `ip` varchar(128) DEFAULT NULL COMMENT '反馈人IP',
  `status` varchar(20) DEFAULT 'pending' COMMENT '处理状态：pending/processing/resolved/rejected',
  `handler` varchar(64) DEFAULT NULL COMMENT '处理人',
  `handle_result` varchar(1000) DEFAULT NULL COMMENT '处理结果说明',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_feedback_type` (`feedback_type`),
  KEY `idx_status` (`status`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户意见反馈表';

-- ============================================
-- 帮助中心表补齐（分类 + 文章）
--    合并来源：
--      27_help_center_init.sql （portal_help_category / portal_help_article 原始建表，已含全部 5 个审计字段）
--    说明：这两张表来自 27 号帮助中心脚本，之前整理时遗漏，现补齐
-- ============================================

-- ----------------------------
-- 帮助中心分类表
-- 来源：27 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_help_category` (
  `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name`         varchar(100) NOT NULL                COMMENT '分类名称',
  `icon`         varchar(100) DEFAULT NULL            COMMENT '图标（lucide 图标名）',
  `description`  varchar(500) DEFAULT NULL            COMMENT '分类描述',
  `sort`         int          DEFAULT 0               COMMENT '排序（升序）',
  `status`       varchar(20)  DEFAULT 'active'        COMMENT '状态：active/inactive',
  `create_by`    varchar(64)  DEFAULT ''              COMMENT '创建者',
  `create_time`  datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`    varchar(64)  DEFAULT ''              COMMENT '更新者',
  `update_time`  datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark`       varchar(500) DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='帮助中心分类表';

-- ----------------------------
-- 帮助中心文章表
-- 来源：27 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_help_article` (
  `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '文章ID',
  `category_id`  bigint       NOT NULL                COMMENT '分类ID',
  `title`        varchar(200) NOT NULL                COMMENT '问题标题',
  `content`      text         NOT NULL                COMMENT '答案内容（支持纯文本）',
  `view_count`   int          DEFAULT 0               COMMENT '查看次数',
  `like_count`   int          DEFAULT 0               COMMENT '点赞次数',
  `sort`         int          DEFAULT 0               COMMENT '排序（升序）',
  `is_featured`  tinyint      DEFAULT 0               COMMENT '是否精选：0=否 1=是',
  `status`       varchar(20)  DEFAULT 'published'     COMMENT '状态：published/draft',
  `create_by`    varchar(64)  DEFAULT ''              COMMENT '创建者',
  `create_time`  datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`    varchar(64)  DEFAULT ''              COMMENT '更新者',
  `update_time`  datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark`       varchar(500) DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_status` (`status`),
  KEY `idx_is_featured` (`is_featured`),
  KEY `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='帮助中心文章表';

SET FOREIGN_KEY_CHECKS = 1;
