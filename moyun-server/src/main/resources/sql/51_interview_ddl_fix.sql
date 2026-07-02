-- =====================================================
-- 面试空间 DDL 修复脚本
-- 补齐缺失的 4 张表 + 补齐实体已声明但 DDL 缺失的字段
-- 幂等设计：所有 ALTER 使用 IF NOT EXISTS 语义的存储过程
-- 执行顺序：本脚本必须在 07_reading_interview_init.sql 之后执行
-- =====================================================

-- 安全添加列的存储过程（与 28/29 脚本一致）
DROP PROCEDURE IF EXISTS AddColumnIfNotExists;
DELIMITER $$
CREATE PROCEDURE AddColumnIfNotExists(
    IN tableName VARCHAR(100),
    IN columnName VARCHAR(100),
    IN columnDefinition VARCHAR(500)
)
BEGIN
    DECLARE columnExists INT DEFAULT 0;
    SELECT COUNT(*) INTO columnExists
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = tableName
      AND COLUMN_NAME = columnName;
    IF columnExists = 0 THEN
        SET @sql = CONCAT('ALTER TABLE `', tableName, '` ADD COLUMN `', columnName, '` ', columnDefinition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- =====================================================
-- 一、补齐缺失的 4 张表 CREATE TABLE
-- =====================================================

-- 1. 公司标签表
CREATE TABLE IF NOT EXISTS `portal_interview_company` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name` VARCHAR(200) NOT NULL COMMENT '公司名称',
    `slug` VARCHAR(200) DEFAULT NULL COMMENT '公司标识',
    `logo` VARCHAR(500) DEFAULT NULL COMMENT '公司Logo URL',
    `description` TEXT COMMENT '公司描述',
    `industry` VARCHAR(100) DEFAULT NULL COMMENT '所属行业',
    `question_count` INT DEFAULT 0 COMMENT '相关题目数',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `status` VARCHAR(20) DEFAULT 'active' COMMENT '状态:active,inactive',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_slug` (`slug`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试公司标签表';

-- 2. 面经评论表
CREATE TABLE IF NOT EXISTS `portal_interview_comment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `experience_id` BIGINT NOT NULL COMMENT '面经ID',
    `user_id` BIGINT NOT NULL COMMENT '评论用户ID',
    `parent_id` BIGINT DEFAULT NULL COMMENT '父评论ID（支持两级回复）',
    `reply_to_user_id` BIGINT DEFAULT NULL COMMENT '回复目标用户ID',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `like_count` BIGINT DEFAULT 0 COMMENT '点赞数',
    `status` VARCHAR(20) DEFAULT 'published' COMMENT '状态:pending,published,rejected',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_experience_id` (`experience_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面经评论表';

-- 3. 面经评论点赞表
CREATE TABLE IF NOT EXISTS `portal_interview_comment_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `comment_id` BIGINT NOT NULL COMMENT '评论ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_comment_user` (`comment_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面经评论点赞表';

-- 4. 题目-公司关联表（多对多）
CREATE TABLE IF NOT EXISTS `portal_interview_question_company` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `question_id` BIGINT NOT NULL COMMENT '题目ID',
    `company_id` BIGINT NOT NULL COMMENT '公司ID',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_question_company` (`question_id`, `company_id`),
    KEY `idx_company_id` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目-公司关联表';

-- =====================================================
-- 二、补齐 portal_interview_submission 缺失字段
-- 实体 PortalInterviewSubmission.java 声明了 content/answer_type/is_success/note
-- 原始 DDL（07 脚本）只有 code/language/status/runtime/memory_usage
-- 21 脚本的 AFTER `note` 引用了不存在的列，这里先补齐 note
-- =====================================================

CALL AddColumnIfNotExists('portal_interview_submission', 'content', 'TEXT COMMENT \'提交的文字答案\' AFTER `code`');
CALL AddColumnIfNotExists('portal_interview_submission', 'answer_type', 'VARCHAR(20) DEFAULT \'code\' COMMENT \'答案类型：code/text/design\' AFTER `language`');
CALL AddColumnIfNotExists('portal_interview_submission', 'is_success', 'TINYINT(1) DEFAULT 0 COMMENT \'是否通过\' AFTER `status`');
CALL AddColumnIfNotExists('portal_interview_submission', 'note', 'TEXT COMMENT \'备注/笔记\' AFTER `memory_usage`');

-- 补齐 submission 的 create_by/update_by/update_time/remark（与 28 脚本保持一致，幂等）
CALL AddColumnIfNotExists('portal_interview_submission', 'create_by', 'VARCHAR(64) DEFAULT \'\' COMMENT \'创建者\'');
CALL AddColumnIfNotExists('portal_interview_submission', 'update_by', 'VARCHAR(64) DEFAULT \'\' COMMENT \'更新者\'');
CALL AddColumnIfNotExists('portal_interview_submission', 'update_time', 'DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT \'更新时间\'');
CALL AddColumnIfNotExists('portal_interview_submission', 'remark', 'VARCHAR(500) DEFAULT NULL COMMENT \'备注\'');

-- 补齐索引（user_id + question_id 复合索引，加速个人答题历史查询）
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_interview_submission' AND INDEX_NAME = 'idx_user_question');
SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE `portal_interview_submission` ADD INDEX `idx_user_question` (`user_id`, `question_id`)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =====================================================
-- 三、补齐 portal_interview_experience 缺失字段
-- 实体声明了 summary/cover_image/is_top
-- =====================================================

CALL AddColumnIfNotExists('portal_interview_experience', 'summary', 'VARCHAR(500) DEFAULT NULL COMMENT \'内容摘要\' AFTER `month`');
CALL AddColumnIfNotExists('portal_interview_experience', 'cover_image', 'VARCHAR(500) DEFAULT NULL COMMENT \'封面图URL\' AFTER `content`');
CALL AddColumnIfNotExists('portal_interview_experience', 'is_top', 'TINYINT(1) DEFAULT 0 COMMENT \'是否置顶\' AFTER `tags`');

-- 更新 status 字段注释，支持审核状态机
ALTER TABLE `portal_interview_experience` MODIFY COLUMN `status` VARCHAR(20) DEFAULT 'published' COMMENT '状态:draft,pending,published,rejected,archived';

-- =====================================================
-- 四、补齐 portal_interview_resume_template 缺失字段
-- 实体声明了 file_type/file_size/is_premium/usage_guide/tags
-- =====================================================

CALL AddColumnIfNotExists('portal_interview_resume_template', 'file_type', 'VARCHAR(20) DEFAULT NULL COMMENT \'文件类型：docx/pdf/psd\' AFTER `category`');
CALL AddColumnIfNotExists('portal_interview_resume_template', 'file_size', 'BIGINT DEFAULT NULL COMMENT \'文件大小（字节）\' AFTER `file_type`');
CALL AddColumnIfNotExists('portal_interview_resume_template', 'is_premium', 'TINYINT(1) DEFAULT 0 COMMENT \'是否付费模板\' AFTER `file_size`');
CALL AddColumnIfNotExists('portal_interview_resume_template', 'usage_guide', 'TEXT COMMENT \'使用指南\' AFTER `is_premium`');
CALL AddColumnIfNotExists('portal_interview_resume_template', 'tags', 'VARCHAR(500) DEFAULT NULL COMMENT \'标签，逗号分隔\' AFTER `usage_guide`');

-- =====================================================
-- 五、清理存储过程
-- =====================================================
DROP PROCEDURE IF EXISTS AddColumnIfNotExists;
