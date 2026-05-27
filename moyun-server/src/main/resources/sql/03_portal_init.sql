-- =============================================
-- 墨韵智库 - 门户系统数据库初始化脚本
-- 版本: v1.0
-- 执行顺序: 03 (在 02_workflow_init.sql 之后执行)
-- 描述: CMS和前台相关表结构
-- =============================================

-- ----------------------------
-- 门户用户表
-- ----------------------------
DROP TABLE IF EXISTS `portal_user`;
CREATE TABLE `portal_user` (
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
-- ----------------------------
DROP TABLE IF EXISTS `portal_category`;
CREATE TABLE `portal_category` (
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
-- ----------------------------
DROP TABLE IF EXISTS `portal_tag`;
CREATE TABLE `portal_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `name` varchar(100) NOT NULL COMMENT '标签名称',
  `slug` varchar(100) DEFAULT NULL COMMENT '标签别名',
  `sort` int DEFAULT '0' COMMENT '排序',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_slug` (`slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户标签表';

-- ----------------------------
-- 门户文章表
-- ----------------------------
DROP TABLE IF EXISTS `portal_article`;
CREATE TABLE `portal_article` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文章ID',
  `title` varchar(500) NOT NULL COMMENT '文章标题',
  `content` longtext COMMENT '文章内容（HTML格式）',
  `excerpt` varchar(1000) DEFAULT NULL COMMENT '文章摘要',
  `cover` varchar(500) DEFAULT NULL COMMENT '封面URL',
  `author_id` bigint NOT NULL COMMENT '作者ID（门户用户ID）',
  `category_id` bigint DEFAULT NULL COMMENT '分类ID',
  `status` varchar(20) DEFAULT 'draft' COMMENT '状态：draft/published/archived',
  `is_featured` tinyint(1) DEFAULT '0' COMMENT '是否精选',
  `is_top` tinyint(1) DEFAULT '0' COMMENT '是否置顶',
  `is_carousel` tinyint(1) DEFAULT '0' COMMENT '是否轮播',
  `views` bigint DEFAULT '0' COMMENT '浏览量',
  `likes` bigint DEFAULT '0' COMMENT '点赞数',
  `comments` bigint DEFAULT '0' COMMENT '评论数',
  `share_count` bigint DEFAULT '0' COMMENT '分享数',
  `bookmark_count` bigint DEFAULT '0' COMMENT '收藏数',
  `published_at` datetime DEFAULT NULL COMMENT '发布时间',
  `link` varchar(500) DEFAULT NULL COMMENT '外部链接',
  `editor_mode` varchar(20) DEFAULT 'richtext' COMMENT '编辑器模式：richtext/markdown',
  `content_markdown` text COMMENT 'Markdown 原始内容',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_author_id` (`author_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_status` (`status`),
  KEY `idx_is_featured` (`is_featured`),
  KEY `idx_is_top` (`is_top`),
  KEY `idx_published_at` (`published_at`),
  KEY `idx_views` (`views`),
  KEY `idx_likes` (`likes`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户文章表';

-- ----------------------------
-- 文章标签关联表
-- ----------------------------
DROP TABLE IF EXISTS `portal_article_tag`;
CREATE TABLE `portal_article_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `tag_id` bigint NOT NULL COMMENT '标签ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_tag` (`article_id`,`tag_id`),
  KEY `idx_article_id` (`article_id`),
  KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='文章标签关联表';

-- ----------------------------
-- 门户评论表
-- ----------------------------
DROP TABLE IF EXISTS `portal_comment`;
CREATE TABLE `portal_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `author_id` bigint NOT NULL COMMENT '评论者ID（门户用户ID）',
  `content` text NOT NULL COMMENT '评论内容',
  `parent_id` bigint DEFAULT '0' COMMENT '父评论ID',
  `reply_to` bigint DEFAULT NULL COMMENT '回复的用户ID',
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
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户评论表';

-- ----------------------------
-- 门户收藏表
-- ----------------------------
DROP TABLE IF EXISTS `portal_bookmark`;
CREATE TABLE `portal_bookmark` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `user_id` bigint NOT NULL COMMENT '用户ID（门户用户ID）',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_article` (`user_id`,`article_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_article_id` (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户收藏表';

-- ----------------------------
-- 门户点赞表（文章）
-- ----------------------------
DROP TABLE IF EXISTS `portal_like`;
CREATE TABLE `portal_like` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '点赞ID',
  `user_id` bigint NOT NULL COMMENT '用户ID（门户用户ID）',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_article` (`user_id`,`article_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_article_id` (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户点赞表（文章）';

-- ----------------------------
-- 门户评论点赞表
-- ----------------------------
DROP TABLE IF EXISTS `portal_comment_like`;
CREATE TABLE `portal_comment_like` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NOT NULL COMMENT '用户ID（门户用户ID）',
  `comment_id` bigint NOT NULL COMMENT '评论ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_comment` (`user_id`,`comment_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_comment_id` (`comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户评论点赞表';

-- ----------------------------
-- 门户关注表
-- ----------------------------
DROP TABLE IF EXISTS `portal_follow`;
CREATE TABLE `portal_follow` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关注ID',
  `follower_id` bigint NOT NULL COMMENT '关注者ID（门户用户ID）',
  `following_id` bigint NOT NULL COMMENT '被关注者ID（门户用户ID）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_follower_following` (`follower_id`,`following_id`),
  KEY `idx_follower_id` (`follower_id`),
  KEY `idx_following_id` (`following_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户关注表';

-- ----------------------------
-- 门户通知表
-- ----------------------------
DROP TABLE IF EXISTS `portal_notification`;
CREATE TABLE `portal_notification` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `user_id` bigint NOT NULL COMMENT '用户ID（门户用户ID）',
  `type` varchar(50) NOT NULL COMMENT '类型：comment/like/follow/system/order',
  `title` varchar(200) DEFAULT NULL COMMENT '通知标题',
  `content` text COMMENT '通知内容',
  `data` json DEFAULT NULL COMMENT '通知数据（JSON格式）',
  `is_read` tinyint(1) DEFAULT '0' COMMENT '是否已读',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_type` (`type`),
  KEY `idx_is_read` (`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户通知表';

-- ----------------------------
-- 门户友情链接表
-- ----------------------------
DROP TABLE IF EXISTS `portal_friend_link`;
CREATE TABLE `portal_friend_link` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '链接ID',
  `name` varchar(100) NOT NULL COMMENT '链接名称',
  `url` varchar(500) NOT NULL COMMENT '链接地址',
  `description` varchar(500) DEFAULT NULL COMMENT '链接描述',
  `logo` varchar(500) DEFAULT NULL COMMENT 'Logo URL',
  `sort` int DEFAULT '0' COMMENT '排序',
  `status` varchar(20) DEFAULT 'active' COMMENT '状态：active/inactive',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户友情链接表';

-- ----------------------------
-- 门户VIP套餐表
-- ----------------------------
DROP TABLE IF EXISTS `portal_vip_package`;
CREATE TABLE `portal_vip_package` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '套餐ID',
  `name` varchar(100) NOT NULL COMMENT '套餐名称',
  `price` decimal(10,2) NOT NULL COMMENT '价格',
  `original_price` decimal(10,2) DEFAULT NULL COMMENT '原价',
  `duration` int NOT NULL COMMENT '有效期（天）',
  `description` varchar(500) DEFAULT NULL COMMENT '套餐描述',
  `features` json DEFAULT NULL COMMENT '功能列表（JSON数组）',
  `popular` tinyint(1) DEFAULT '0' COMMENT '是否热门',
  `sort` int DEFAULT '0' COMMENT '排序',
  `status` varchar(20) DEFAULT 'active' COMMENT '状态：active/inactive',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户VIP套餐表';

-- ----------------------------
-- 门户订单表
-- ----------------------------
DROP TABLE IF EXISTS `portal_order`;
CREATE TABLE `portal_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` varchar(64) NOT NULL COMMENT '订单号',
  `user_id` bigint NOT NULL COMMENT '用户ID（门户用户ID）',
  `type` varchar(50) NOT NULL COMMENT '类型：vip/recharge/product',
  `product_id` bigint DEFAULT NULL COMMENT '商品ID',
  `amount` decimal(10,2) NOT NULL COMMENT '金额',
  `status` varchar(20) DEFAULT 'pending' COMMENT '状态：pending/paid/cancelled/refunded',
  `pay_method` varchar(50) DEFAULT NULL COMMENT '支付方式：wechat/alipay',
  `paid_at` datetime DEFAULT NULL COMMENT '支付时间',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_type` (`type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户订单表';

-- ----------------------------
-- 门户钱包表
-- ----------------------------
DROP TABLE IF EXISTS `portal_wallet`;
CREATE TABLE `portal_wallet` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '钱包ID',
  `user_id` bigint NOT NULL COMMENT '用户ID（门户用户ID）',
  `balance` decimal(10,2) DEFAULT '0.00' COMMENT '余额',
  `frozen_balance` decimal(10,2) DEFAULT '0.00' COMMENT '冻结余额',
  `total_recharge` decimal(10,2) DEFAULT '0.00' COMMENT '累计充值',
  `total_withdraw` decimal(10,2) DEFAULT '0.00' COMMENT '累计提现',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户钱包表';

-- ----------------------------
-- 门户钱包交易记录表
-- ----------------------------
DROP TABLE IF EXISTS `portal_wallet_transaction`;
CREATE TABLE `portal_wallet_transaction` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '交易ID',
  `user_id` bigint NOT NULL COMMENT '用户ID（门户用户ID）',
  `type` varchar(50) NOT NULL COMMENT '类型：recharge/consume/refund/withdraw',
  `amount` decimal(10,2) NOT NULL COMMENT '金额',
  `balance_before` decimal(10,2) NOT NULL COMMENT '交易前余额',
  `balance_after` decimal(10,2) NOT NULL COMMENT '交易后余额',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `order_id` bigint DEFAULT NULL COMMENT '关联订单ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户钱包交易记录表';

-- ----------------------------
-- 初始化数据 - VIP套餐
-- ----------------------------
INSERT INTO `portal_vip_package` (`name`, `price`, `original_price`, `duration`, `description`, `features`, `popular`, `sort`, `status`, `create_by`) VALUES
('月卡', 29.00, 39.00, 30, '一个月VIP会员', '["无限浏览", "专属标识", "优先推荐", "客服优先"]', 1, 1, 'active', 'admin'),
('季卡', 79.00, 99.00, 90, '三个月VIP会员', '["无限浏览", "专属标识", "优先推荐", "客服优先", "专属折扣"]', 0, 2, 'active', 'admin'),
('年卡', 259.00, 359.00, 365, '一年VIP会员', '["无限浏览", "专属标识", "优先推荐", "客服优先", "专属折扣", "专属课程"]', 1, 3, 'active', 'admin');

-- ----------------------------
-- 初始化数据 - 友情链接
-- ----------------------------
INSERT INTO `portal_friend_link` (`name`, `url`, `description`, `sort`, `status`, `create_by`) VALUES
('中国作家网', 'https://www.chinawriter.com.cn', '中国作家协会官方网站', 1, 'active', 'admin'),
('起点中文网', 'https://www.qidian.com', '阅文集团旗下网站', 2, 'active', 'admin'),
('掘金', 'https://juejin.cn', '帮助开发者成长的社区', 3, 'active', 'admin');
