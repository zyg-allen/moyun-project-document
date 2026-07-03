-- ============================================================
-- 77_pk_init.sql
-- 阶段三 3.7 排行榜 / PK：异步对战（好友 PK / 公司题目挑战榜）
-- 说明：采用异步对战模式，双方不要求同时在线；题目复用 portal_interview_question，
--       答题提交复用 portal_interview_submission（通过 note 字段打标 pk:{challengeId} 关联到本对战）
-- @author moyun
-- ============================================================

DROP TABLE IF EXISTS `portal_pk_challenge`;
CREATE TABLE `portal_pk_challenge` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `challenger_id` BIGINT NOT NULL COMMENT '发起方用户ID',
    `opponent_id` BIGINT NOT NULL COMMENT '应战方用户ID',
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态:pending/accepted/declined/ongoing/finished',
    `winner_id` BIGINT DEFAULT NULL COMMENT '胜者用户ID（平局为NULL）',
    `challenger_score` INT NOT NULL DEFAULT 0 COMMENT '发起方得分（通过题数）',
    `opponent_score` INT NOT NULL DEFAULT 0 COMMENT '应战方得分（通过题数）',
    `question_ids` VARCHAR(500) NOT NULL COMMENT '题目ID列表，逗号分隔',
    `scene` VARCHAR(20) NOT NULL DEFAULT '1v1' COMMENT '场景:1v1=好友PK / company=公司题目挑战',
    `company_id` BIGINT DEFAULT NULL COMMENT '公司ID（scene=company 时关联 portal_interview_company）',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发起时间',
    `finished_time` DATETIME DEFAULT NULL COMMENT '结束时间',
    PRIMARY KEY (`id`),
    KEY `idx_challenger_id` (`challenger_id`),
    KEY `idx_opponent_id` (`opponent_id`),
    KEY `idx_status` (`status`),
    KEY `idx_company_id` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PK 对战表（异步对战）';
