-- =====================================================
-- 学习计划与目标 DDL 脚本（任务 3.2）
-- 支持学习计划创建、每日进度记录
-- 幂等设计：CREATE TABLE IF NOT EXISTS
-- 执行顺序：本脚本在 03_portal_init.sql 之后执行
-- =====================================================

-- 学习计划
CREATE TABLE IF NOT EXISTS `portal_study_plan` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`          BIGINT       NOT NULL                COMMENT '用户ID',
    `title`            VARCHAR(128) NOT NULL                COMMENT '计划标题',
    `plan_type`        VARCHAR(32)  DEFAULT NULL            COMMENT '计划类型 daily_question/weekly_reading/custom',
    `target_count`     INT          DEFAULT NULL            COMMENT '目标数量',
    `target_category`  VARCHAR(64)  DEFAULT NULL            COMMENT '目标分类',
    `start_date`       DATE         DEFAULT NULL            COMMENT '开始日期',
    `end_date`         DATE         DEFAULT NULL            COMMENT '结束日期',
    `status`           VARCHAR(16)  NOT NULL DEFAULT 'active' COMMENT '状态 active/completed/abandoned',
    `created_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习计划';

-- 计划每日进度
CREATE TABLE IF NOT EXISTS `portal_study_plan_log` (
    `id`            BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `plan_id`       BIGINT   NOT NULL                COMMENT '计划ID',
    `user_id`       BIGINT   NOT NULL                COMMENT '用户ID',
    `log_date`      DATE     NOT NULL                COMMENT '日志日期',
    `done_count`    INT      NOT NULL DEFAULT 0      COMMENT '当日完成数量',
    `created_time`  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_plan_date` (`plan_id`, `log_date`),
    KEY `idx_user_date` (`user_id`, `log_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计划每日进度';

SELECT '学习计划初始化脚本执行完成！' AS message;
