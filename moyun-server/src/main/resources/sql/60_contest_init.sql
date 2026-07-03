-- =====================================================
-- 创作挑战/征文活动 DDL 脚本（创作者天堂核心）
-- 支持征文活动发布、用户投稿、投票、展示
-- 幂等设计：CREATE TABLE IF NOT EXISTS
-- 执行顺序：本脚本在 57_column_init.sql 之后执行
-- =====================================================

-- 创作挑战/征文活动
CREATE TABLE IF NOT EXISTS `portal_writing_contest` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title`          VARCHAR(128)  NOT NULL                COMMENT '活动标题',
    `description`    TEXT                                  COMMENT '活动描述',
    `theme`          VARCHAR(128)  DEFAULT NULL            COMMENT '征文主题',
    `cover`          VARCHAR(500)  DEFAULT NULL            COMMENT '封面',
    `start_time`     DATETIME      DEFAULT NULL            COMMENT '活动开始时间',
    `end_time`       DATETIME      DEFAULT NULL            COMMENT '投稿截止时间',
    `vote_end_time`  DATETIME      DEFAULT NULL            COMMENT '投票截止时间',
    `prize`          VARCHAR(500)  DEFAULT NULL            COMMENT '奖品说明',
    `status`         VARCHAR(16)   NOT NULL DEFAULT 'draft' COMMENT 'draft/collecting/voting/ended',
    `created_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_start_time` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='创作挑战/征文活动';

-- 活动投稿
CREATE TABLE IF NOT EXISTS `portal_contest_submission` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `contest_id`   BIGINT        NOT NULL                COMMENT '活动ID',
    `user_id`      BIGINT        NOT NULL                COMMENT '投稿用户ID',
    `article_id`   BIGINT        NOT NULL                COMMENT '投稿文章ID',
    `status`       VARCHAR(16)   NOT NULL DEFAULT 'pending' COMMENT 'pending/shortlisted/eliminated/winner',
    `vote_count`   INT           NOT NULL DEFAULT 0     COMMENT '投票数',
    `rank`         INT           DEFAULT NULL            COMMENT '排名',
    `remark`       VARCHAR(500)  DEFAULT NULL            COMMENT '备注（评审意见等）',
    `created_time` DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_contest_user` (`contest_id`, `user_id`),
    UNIQUE KEY `uk_contest_article` (`contest_id`, `article_id`),
    KEY `idx_contest` (`contest_id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动投稿';

-- 投票记录（用于 toggle 去重：每用户对每条投稿仅能投一票）
CREATE TABLE IF NOT EXISTS `portal_contest_vote` (
    `id`             BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `submission_id`   BIGINT   NOT NULL                COMMENT '投稿ID',
    `user_id`        BIGINT   NOT NULL                COMMENT '投票用户ID',
    `contest_id`     BIGINT   NOT NULL                COMMENT '活动ID（冗余便于按活动统计）',
    `created_time`   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '投票时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_submission_user` (`submission_id`, `user_id`),
    KEY `idx_submission` (`submission_id`),
    KEY `idx_contest` (`contest_id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动投稿投票记录';
