-- ============================================
-- 墨韵智库 - 社交模块建表 DDL（最终合并版）
-- ============================================
-- 合并来源脚本：
--   55_feed_init.sql          （动态 Feed 流建表：portal_feed_event/portal_feed_inbox）
--   56_message_init.sql       （私信建表：portal_message_session/portal_message）
--   57_column_init.sql        （专栏/连载建表：portal_column/portal_column_article/portal_column_subscribe）
--   70_circle_init.sql        （圈子/兴趣小组建表：portal_circle/portal_circle_member/portal_circle_post）
--   71_topic_init.sql         （话题/超话建表：portal_topic/portal_topic_follow）
--   87_message_user_type.sql  （扩展 portal_message_session：新增 user_a_type/user_b_type、唯一键升级为 uk_users_type；
--                              扩展 portal_message：新增 sender_type/receiver_type、索引升级为 idx_receiver_type_read）
-- 涉及表：
--   portal_feed_event（55 建表）
--   portal_feed_inbox（55 建表）
--   portal_message_session（56 建表 + 87 扩展字段/索引）
--   portal_message（56 建表 + 87 扩展字段/索引）
--   portal_column（57 建表）
--   portal_column_article（57 建表）
--   portal_column_subscribe（57 建表）
--   portal_circle（70 建表）
--   portal_circle_member（70 建表）
--   portal_circle_post（70 建表）
--   portal_topic（71 建表）
--   portal_topic_follow（71 建表）
-- 说明：
--   - 所有 ALTER TABLE 已合并进 CREATE TABLE IF NOT EXISTS
--   - 87 中"先 DROP 旧索引再建新索引"已合并为直接定义最终索引（uk_users_type / idx_receiver_type_read）
--   - 不包含 INSERT 数据语句
--   - 不包含后台菜单注入语句
-- @author moyun
-- ============================================

-- ----------------------------
-- 1. 动态事件表
--    合并：55 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_feed_event` (
    `id`           BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id`      BIGINT       NOT NULL COMMENT '事件发布者',
    `event_type`   VARCHAR(32)  NOT NULL COMMENT 'publish_article/publish_experience/new_column/checkin等',
    `target_type`  VARCHAR(32)  NOT NULL COMMENT 'article/experience/column/book等',
    `target_id`    BIGINT       NOT NULL COMMENT '目标对象ID',
    `title`        VARCHAR(256)         COMMENT '目标标题',
    `summary`      VARCHAR(500)         COMMENT '动态摘要',
    `cover`        VARCHAR(500)         COMMENT '封面图',
    `created_time` DATETIME     NOT NULL,
    INDEX `idx_user_time` (`user_id`, `created_time`),
    INDEX `idx_type_time` (`event_type`, `created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动态事件流';

-- ----------------------------
-- 2. 动态收件箱（推模式，关注者收件箱）
--    合并：55 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_feed_inbox` (
    `id`           BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id`      BIGINT   NOT NULL COMMENT '接收者',
    `event_id`     BIGINT   NOT NULL COMMENT '动态事件ID',
    `created_time` DATETIME NOT NULL,
    INDEX `idx_user_time` (`user_id`, `created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动态收件箱';

-- ----------------------------
-- 3. 私信会话
--    合并：56 建表 + 87（user_a_type/user_b_type 字段；唯一键 uk_users 升级为 uk_users_type）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_message_session` (
    `id`                   BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_a`               BIGINT       NOT NULL COMMENT '用户A（较小ID）',
    `user_a_type`          VARCHAR(16)  NOT NULL DEFAULT 'portal' COMMENT 'A方用户类型 portal/sys',
    `user_b`               BIGINT       NOT NULL COMMENT '用户B（较大ID）',
    `user_b_type`          VARCHAR(16)  NOT NULL DEFAULT 'portal' COMMENT 'B方用户类型 portal/sys',
    `last_message_id`      BIGINT                COMMENT '最后一条消息ID',
    `last_message_content` VARCHAR(500)          COMMENT '最后消息内容预览',
    `last_message_time`    DATETIME              COMMENT '最后消息时间',
    `unread_a`             INT          DEFAULT 0 COMMENT 'A未读数',
    `unread_b`             INT          DEFAULT 0 COMMENT 'B未读数',
    `create_time`          DATETIME              COMMENT '创建时间',
    `update_time`          DATETIME              COMMENT '更新时间',
    UNIQUE KEY `uk_users_type` (`user_a`, `user_b`, `user_a_type`, `user_b_type`),
    INDEX `idx_user_a` (`user_a`),
    INDEX `idx_user_b` (`user_b`),
    INDEX `idx_last_time` (`last_message_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='私信会话';

-- ----------------------------
-- 4. 私信消息
--    合并：56 建表 + 87（sender_type/receiver_type 字段；索引 idx_receiver_read 升级为 idx_receiver_type_read）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_message` (
    `id`             BIGINT PRIMARY KEY AUTO_INCREMENT,
    `session_id`     BIGINT       NOT NULL COMMENT '会话ID',
    `sender_id`      BIGINT       NOT NULL COMMENT '发送者',
    `sender_type`    VARCHAR(16)  NOT NULL DEFAULT 'portal' COMMENT '发送者类型 portal/sys',
    `receiver_id`    BIGINT       NOT NULL COMMENT '接收者',
    `receiver_type`  VARCHAR(16)  NOT NULL DEFAULT 'portal' COMMENT '接收者类型 portal/sys',
    `content`        TEXT         NOT NULL COMMENT '消息内容',
    `msg_type`       VARCHAR(16)  DEFAULT 'text' COMMENT 'text/image/file',
    `is_read`        TINYINT      DEFAULT 0 COMMENT '是否已读',
    `create_time`    DATETIME              COMMENT '创建时间',
    INDEX `idx_session_time` (`session_id`, `create_time`),
    INDEX `idx_receiver_type_read` (`receiver_id`, `receiver_type`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='私信消息';

-- ----------------------------
-- 5. 专栏
--    合并：57 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_column` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`         BIGINT        NOT NULL                COMMENT '创作者',
    `title`           VARCHAR(128)  NOT NULL                COMMENT '专栏名',
    `subtitle`        VARCHAR(256)  DEFAULT NULL            COMMENT '副标题',
    `description`     TEXT                                  COMMENT '专栏简介',
    `cover`           VARCHAR(500)  DEFAULT NULL            COMMENT '封面',
    `category_id`     BIGINT         DEFAULT NULL            COMMENT '分类',
    `status`          VARCHAR(16)   NOT NULL DEFAULT 'draft' COMMENT 'draft/published/archived',
    `article_count`   INT           NOT NULL DEFAULT 0     COMMENT '文章数',
    `subscribe_count` INT           NOT NULL DEFAULT 0     COMMENT '订阅数',
    `view_count`      INT           NOT NULL DEFAULT 0     COMMENT '浏览数',
    `is_finished`     TINYINT        NOT NULL DEFAULT 0     COMMENT '是否完结',
    `price`           DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '专栏会员价，0=免费',
    `created_time`    DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time`    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专栏';

-- ----------------------------
-- 6. 专栏-文章关联
--    合并：57 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_column_article` (
    `id`           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `column_id`    BIGINT   NOT NULL                COMMENT '专栏ID',
    `article_id`   BIGINT   NOT NULL                COMMENT '文章ID',
    `sort_order`   INT      NOT NULL DEFAULT 0     COMMENT '专栏内顺序',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_column_article` (`column_id`, `article_id`),
    KEY `idx_column_sort` (`column_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专栏-文章关联';

-- ----------------------------
-- 7. 专栏订阅
--    合并：57 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_column_subscribe` (
    `id`           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `column_id`    BIGINT   NOT NULL                COMMENT '专栏ID',
    `user_id`      BIGINT   NOT NULL                COMMENT '订阅用户ID',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_column_user` (`column_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专栏订阅';

-- ----------------------------
-- 8. 圈子
--    合并：70 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_circle` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`          VARCHAR(64)   NOT NULL                COMMENT '圈子名称',
    `description`   TEXT                                  COMMENT '圈子简介',
    `cover`         VARCHAR(500)  DEFAULT NULL            COMMENT '封面URL',
    `owner_id`      BIGINT        NOT NULL                COMMENT '圈主用户ID',
    `member_count`  INT           NOT NULL DEFAULT 0     COMMENT '成员数',
    `post_count`    INT           NOT NULL DEFAULT 0     COMMENT '帖子数',
    `category`      VARCHAR(32)   DEFAULT NULL            COMMENT '分类 reading/writing/tech',
    `status`        VARCHAR(16)   NOT NULL DEFAULT 'active' COMMENT '状态 active/disabled/pending',
    `created_time`  DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_owner` (`owner_id`),
    KEY `idx_status` (`status`),
    KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='圈子';

-- ----------------------------
-- 9. 圈子成员
--    合并：70 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_circle_member` (
    `id`           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `circle_id`    BIGINT   NOT NULL                COMMENT '圈子ID',
    `user_id`      BIGINT   NOT NULL                COMMENT '用户ID',
    `role`         VARCHAR(16) NOT NULL DEFAULT 'member' COMMENT '角色 owner/admin/member',
    `joined_time`  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_circle_user` (`circle_id`, `user_id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='圈子成员';

-- ----------------------------
-- 10. 圈子帖子
--     合并：70 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_circle_post` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `circle_id`     BIGINT        NOT NULL                COMMENT '圈子ID',
    `user_id`       BIGINT        NOT NULL                COMMENT '发帖用户ID',
    `title`         VARCHAR(200)  NOT NULL                COMMENT '帖子标题',
    `content`       LONGTEXT                              COMMENT '帖子内容（HTML）',
    `view_count`    INT           NOT NULL DEFAULT 0     COMMENT '浏览数',
    `like_count`    INT           NOT NULL DEFAULT 0     COMMENT '点赞数',
    `reply_count`   INT           NOT NULL DEFAULT 0     COMMENT '回复数',
    `status`        VARCHAR(16)   NOT NULL DEFAULT 'active' COMMENT '状态 active/hidden/deleted',
    `created_time`  DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '发帖时间',
    PRIMARY KEY (`id`),
    KEY `idx_circle` (`circle_id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='圈子帖子';

-- ----------------------------
-- 11. 话题/超话
--     合并：71 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_topic` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`          VARCHAR(64)   NOT NULL                COMMENT '话题名称',
    `slug`          VARCHAR(128)  NOT NULL                COMMENT '话题别名（URL 友好）',
    `description`   TEXT                                  COMMENT '话题描述',
    `cover`         VARCHAR(500)  DEFAULT NULL            COMMENT '话题封面',
    `post_count`    INT           NOT NULL DEFAULT 0     COMMENT '关联内容数',
    `follow_count`  INT           NOT NULL DEFAULT 0     COMMENT '关注数',
    `status`        VARCHAR(16)   NOT NULL DEFAULT 'active' COMMENT '状态 active/disabled',
    `created_time`  DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_slug` (`slug`),
    UNIQUE KEY `uk_name` (`name`),
    KEY `idx_status` (`status`),
    KEY `idx_follow_count` (`follow_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='话题/超话';

-- ----------------------------
-- 12. 话题关注（独立于 portal_follow，避免改动现有用户关注逻辑）
--     合并：71 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_topic_follow` (
    `id`            BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `topic_id`      BIGINT   NOT NULL                COMMENT '话题ID',
    `user_id`       BIGINT   NOT NULL                COMMENT '关注用户ID',
    `created_time`  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_topic_user` (`topic_id`, `user_id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='话题关注';
