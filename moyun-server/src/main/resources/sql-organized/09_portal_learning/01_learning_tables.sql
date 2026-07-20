-- ============================================
-- 墨韵智库 - 学习工具模块建表 DDL（最终合并版）
-- ============================================
-- 合并来源脚本：
--   65_study_plan_init.sql  （学习计划与目标建表：portal_study_plan/portal_study_plan_log）
--   66_wrong_question_init.sql （错题本建表：portal_wrong_question）
--   68_code_run_init.sql    （在线代码运行建表：portal_code_run）
--   81_pk_init.sql          （PK 对战建表：portal_pk_challenge）
-- 涉及表：
--   portal_study_plan（65 建表）
--   portal_study_plan_log（65 建表）
--   portal_wrong_question（66 建表）
--   portal_code_run（68 建表）
--   portal_pk_challenge（81 建表，原脚本为 DROP + CREATE，统一改造为 CREATE IF NOT EXISTS）
-- 说明：
--   - 所有表均无后续 ALTER 扩展，直接采用原始 CREATE TABLE IF NOT EXISTS
--   - 81 中 DROP TABLE IF EXISTS + CREATE TABLE 已改造为 CREATE TABLE IF NOT EXISTS，保持幂等
--   - 不包含 INSERT 数据语句
--   - 不包含后台菜单注入语句
-- @author moyun
-- ============================================

-- ----------------------------
-- 1. 学习计划
--    合并：65 建表
-- ----------------------------
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习计划';

-- ----------------------------
-- 2. 计划每日进度
--    合并：65 建表
-- ----------------------------
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='计划每日进度';

-- ----------------------------
-- 3. 错题本
--    合并：66 建表
-- ----------------------------
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='错题本';

-- ----------------------------
-- 4. 代码运行记录表
--    合并：68 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_code_run` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT       NOT NULL                COMMENT '运行者用户ID',
    `language`    VARCHAR(16)  NOT NULL                COMMENT '编程语言 java/python/javascript',
    `code`        MEDIUMTEXT   NOT NULL                COMMENT '用户提交的源代码',
    `stdin`       TEXT                                 COMMENT '标准输入内容',
    `output`      MEDIUMTEXT                           COMMENT '标准输出（截断至 1MB）',
    `error_msg`   MEDIUMTEXT                           COMMENT '错误输出 / 编译错误信息',
    `status`      VARCHAR(16)  NOT NULL DEFAULT 'running' COMMENT '运行状态 running/success/failed/timeout',
    `runtime_ms`  INT          DEFAULT NULL            COMMENT '运行耗时（毫秒）',
    `mem_kb`      INT          DEFAULT NULL            COMMENT '内存占用（KB，粗略估算）',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='代码运行记录';

-- ----------------------------
-- 5. PK 对战表（异步对战）
--    合并：81 建表
--    原脚本为 DROP TABLE IF EXISTS + CREATE TABLE，此处改造为 CREATE TABLE IF NOT EXISTS 以保持幂等且不破坏存量数据
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_pk_challenge` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `challenger_id`   BIGINT       NOT NULL                COMMENT '发起方用户ID',
    `opponent_id`     BIGINT       NOT NULL                COMMENT '应战方用户ID',
    `status`          VARCHAR(20)  NOT NULL DEFAULT 'pending' COMMENT '状态:pending/accepted/declined/ongoing/finished',
    `winner_id`       BIGINT       DEFAULT NULL            COMMENT '胜者用户ID（平局为NULL）',
    `challenger_score` INT         NOT NULL DEFAULT 0      COMMENT '发起方得分（通过题数）',
    `opponent_score`  INT          NOT NULL DEFAULT 0      COMMENT '应战方得分（通过题数）',
    `question_ids`    VARCHAR(500) NOT NULL                COMMENT '题目ID列表，逗号分隔',
    `scene`           VARCHAR(20)  NOT NULL DEFAULT '1v1'  COMMENT '场景:1v1=好友PK / company=公司题目挑战',
    `company_id`      BIGINT       DEFAULT NULL            COMMENT '公司ID（scene=company 时关联 portal_interview_company）',
    `created_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '发起时间',
    `finished_time`   DATETIME     DEFAULT NULL            COMMENT '结束时间',
    PRIMARY KEY (`id`),
    KEY `idx_challenger_id` (`challenger_id`),
    KEY `idx_opponent_id` (`opponent_id`),
    KEY `idx_status` (`status`),
    KEY `idx_company_id` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='PK 对战表（异步对战）';
