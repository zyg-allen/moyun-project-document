-- =====================================================
-- 专栏/连载功能 DDL 脚本（创作者天堂核心）
-- 支持专栏创建、文章目录编排、订阅、完结控制、会员价
-- 幂等设计：CREATE TABLE IF NOT EXISTS
-- 执行顺序：本脚本在 03_portal_init.sql 之后执行
-- =====================================================

-- 专栏
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专栏';

-- 专栏-文章关联
CREATE TABLE IF NOT EXISTS `portal_column_article` (
    `id`           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `column_id`    BIGINT   NOT NULL                COMMENT '专栏ID',
    `article_id`   BIGINT   NOT NULL                COMMENT '文章ID',
    `sort_order`   INT      NOT NULL DEFAULT 0     COMMENT '专栏内顺序',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_column_article` (`column_id`, `article_id`),
    KEY `idx_column_sort` (`column_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专栏-文章关联';

-- 专栏订阅
CREATE TABLE IF NOT EXISTS `portal_column_subscribe` (
    `id`           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `column_id`    BIGINT   NOT NULL                COMMENT '专栏ID',
    `user_id`      BIGINT   NOT NULL                COMMENT '订阅用户ID',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_column_user` (`column_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专栏订阅';
