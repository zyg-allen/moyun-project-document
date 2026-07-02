-- =====================================================
-- 用户简历表 DDL 脚本（面试空间 第2期）
-- 支持结构化简历录入、版本历史、PDF 导出 URL、规则评分
-- 幂等设计：CREATE TABLE IF NOT EXISTS
-- 执行顺序：本脚本必须在 07_reading_interview_init.sql / 51_interview_ddl_fix.sql 之后执行
-- =====================================================

-- 用户简历主表
CREATE TABLE IF NOT EXISTS `portal_user_resume` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`         BIGINT       NOT NULL                COMMENT '用户ID',
    `title`           VARCHAR(100) NOT NULL DEFAULT '我的简历' COMMENT '简历名称',
    `parent_id`       BIGINT       DEFAULT NULL            COMMENT '父简历ID（版本历史关联，首次创建为 NULL）',
    `version_no`      INT          NOT NULL DEFAULT 1      COMMENT '版本号',

    -- 基本信息
    `name`            VARCHAR(50)  DEFAULT NULL            COMMENT '姓名',
    `gender`          VARCHAR(10)  DEFAULT NULL            COMMENT '性别：男/女',
    `birth_date`      DATE         DEFAULT NULL            COMMENT '出生日期',
    `phone`           VARCHAR(20)  DEFAULT NULL            COMMENT '联系电话',
    `email`           VARCHAR(100) DEFAULT NULL            COMMENT '邮箱',
    `avatar`          VARCHAR(255) DEFAULT NULL            COMMENT '头像URL',

    -- 结构化内容（JSON 字符串）
    `job_intention`   TEXT         COMMENT '求职意向（JSON：期望职位/城市/薪资/类型）',
    `educations`      TEXT         COMMENT '教育经历（JSON 数组：学校/专业/学历/时间/描述）',
    `works`           TEXT         COMMENT '工作经历（JSON 数组：公司/职位/时间/描述）',
    `projects`        TEXT         COMMENT '项目经历（JSON 数组：名称/角色/时间/描述/链接）',
    `skills`          TEXT         COMMENT '技能列表（JSON 数组：名称/等级/分类）',
    `self_intro`      TEXT         COMMENT '自我介绍',

    -- 评分
    `score`           INT          DEFAULT NULL            COMMENT '评分（0-100）',
    `score_detail`    TEXT         COMMENT '评分明细（JSON 数组）',
    `scored_time`     DATETIME     DEFAULT NULL            COMMENT '评分时间',

    -- 导出
    `file_url`        VARCHAR(255) DEFAULT NULL            COMMENT 'PDF 导出文件URL',
    `export_time`     DATETIME     DEFAULT NULL            COMMENT '最后导出时间',

    -- 状态
    `status`          VARCHAR(20)  NOT NULL DEFAULT 'draft' COMMENT '状态：draft/published/archived',

    -- 公共字段
    `create_by`       VARCHAR(64)  DEFAULT ''              COMMENT '创建者',
    `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`       VARCHAR(64)  DEFAULT ''              COMMENT '更新者',
    `update_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`          VARCHAR(500) DEFAULT NULL            COMMENT '备注',

    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_parent_id` (`parent_id`),
    -- 复合索引：(parent_id, version_no) 用于按根简历查询版本历史；同时作为唯一约束防止并发复制产生重复版本号。
    -- 注意：parent_id 可为 NULL（根简历），MySQL 中 NULL 不参与唯一性约束，根简历的 versionNo=1 需应用层保证。
    UNIQUE KEY `uk_parent_version` (`parent_id`, `version_no`),
    KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户简历';
