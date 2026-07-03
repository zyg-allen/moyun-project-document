-- =====================================================
-- 错题本 DDL 脚本（任务 3.3）
-- 支持错题记录、状态机（wrong/reviewing/mastered）、艾宾浩斯复习
-- 数据源：可基于现有 portal_interview_submission 表的 is_success=0 数据聚合
-- 幂等设计：CREATE TABLE IF NOT EXISTS
-- 执行顺序：本脚本在 03_portal_init.sql 之后执行
-- =====================================================

-- 错题本
CREATE TABLE IF NOT EXISTS `portal_wrong_question` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`           BIGINT       NOT NULL                COMMENT '用户ID',
    `question_id`       BIGINT       NOT NULL                COMMENT '题目ID',
    `attempt_id`        BIGINT       DEFAULT NULL            COMMENT '最近一次答题ID',
    `status`            VARCHAR(16)  NOT NULL DEFAULT 'wrong' COMMENT '状态 wrong/reviewing/mastered',
    `wrong_count`       INT          NOT NULL DEFAULT 1      COMMENT '答错次数',
    `last_wrong_time`   DATETIME     DEFAULT NULL            COMMENT '最近答错时间',
    `next_review_time`  DATETIME     DEFAULT NULL            COMMENT '下次复习时间（艾宾浩斯）',
    `created_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_question` (`user_id`, `question_id`),
    KEY `idx_user_status` (`user_id`, `status`),
    KEY `idx_user_review` (`user_id`, `next_review_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题本';

SELECT '错题本初始化脚本执行完成！' AS message;
