-- ============================================
-- 墨韵智库 - 逻辑删除改造迁移脚本（v5.9 P0）
-- ============================================
-- 目的：
--   给所有继承 BaseEntity 的业务表添加 del_flag 字段，配合 MyBatis-Plus @TableLogic 实现逻辑删除。
--   与 sys_user / sys_dept / sys_role / portal_user 已有的 del_flag 字段对齐：
--     - 类型：char(1)
--     - 默认值：'0'（存在）
--     - 删除标记：'2'（已删除）
--
-- 设计原则：
--   1. 主业务实体表（article / comment / circle / column / book 等）：加 del_flag，逻辑删除
--   2. 关联表（like / bookmark / follow / comment_like 等）：不加 del_flag，保持物理删除（toggle 语义）
--   3. 日志表（sys_oper_log / sys_logininfor）：不加 del_flag，按时间归档
--   4. 统计表（portal_user_stats）：不加 del_flag，跟随用户走
--
-- 幂等性：
--   使用 information_schema.columns 检查字段是否存在，已有 del_flag 的表自动跳过。
--   脚本可重复执行，不会报错。
--
-- 兼容性：
--   本脚本不使用存储过程和 DELIMITER 命令，兼容所有 MySQL 客户端
--   （mysql CLI / Navicat / DataGrip / DBeaver 等），可直接整段执行。
--   使用预处理语句（PREPARE/EXECUTE）实现幂等性，可重复执行。
--
-- 执行方式：
--   mysql -u root -p moyun-db < 96_add_del_flag_for_logical_delete.sql
--   或在任意 MySQL 客户端中直接整段执行（无需配置 DELIMITER）
--
-- 回滚方式：
--   ALTER TABLE <table_name> DROP COLUMN del_flag;
--   （仅在确信不再需要逻辑删除时执行）
-- ============================================

SET NAMES utf8mb4;

-- ============================================
-- 给所有需要 del_flag 的业务表添加字段
-- ============================================
-- 排除以下类型的表（保持物理删除或不加字段）：
--   - 关联表（toggle 语义）：portal_like, portal_bookmark, portal_comment_like, portal_topic_like, ...
--   - 浏览记录表：portal_article_view（日志性质，按时间归档）
--   - 多对多关联表：portal_article_tag, portal_entity_tag, portal_interview_question_company, ...
--   - Feed 推送表：portal_feed_inbox（日志性质）
--   - 已有 del_flag 的表：portal_user, sys_user, sys_dept, sys_role
--   - 已有 is_deleted 字段的话题表：portal_topic_post, portal_topic_comment
-- ============================================

-- ---------- portal_* 业务主表（48 张）----------

-- portal_ad_slot
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_ad_slot' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_ad_slot` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_article
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_article' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_article` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_book
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_book' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_book` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_book_chapter
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_book_chapter' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_book_chapter` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_book_list
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_book_list' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_book_list` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_book_quote
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_book_quote' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_book_quote` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_book_recommend
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_book_recommend' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_book_recommend` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_bookshelf
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_bookshelf' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_bookshelf` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_category
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_category' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_category` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_circle
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_circle' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_circle` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_circle_post
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_circle_post' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_circle_post` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_column
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_column' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_column` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_comment
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_comment' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_comment` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_contest_submission
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_contest_submission' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_contest_submission` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_creator_certification
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_creator_certification' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_creator_certification` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_creator_settlement
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_creator_settlement' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_creator_settlement` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_feedback
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_feedback' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_feedback` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_help_article
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_help_article' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_help_article` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_help_category
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_help_category' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_help_category` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_interview_attempt
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_interview_attempt' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_interview_attempt` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_interview_bookmark
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_interview_bookmark' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_interview_bookmark` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_interview_category
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_interview_category' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_interview_category` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_interview_comment
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_interview_comment' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_interview_comment` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_interview_company
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_interview_company' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_interview_company` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_interview_experience
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_interview_experience' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_interview_experience` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_interview_position
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_interview_position' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_interview_position` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_interview_question
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_interview_question' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_interview_question` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_interview_resume_template
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_interview_resume_template' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_interview_resume_template` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_interview_submission
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_interview_submission' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_interview_submission` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_mock_interview
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_mock_interview' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_mock_interview` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_mock_interview_qa
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_mock_interview_qa' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_mock_interview_qa` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_order
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_order' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_order` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_reading_preference
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_reading_preference' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_reading_preference` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_reading_progress
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_reading_progress' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_reading_progress` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_report
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_report' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_report` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_shop_exchange
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_shop_exchange' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_shop_exchange` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_shop_item
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_shop_item' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_shop_item` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_study_plan
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_study_plan' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_study_plan` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_tag
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_tag' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_tag` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_task
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_task' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_task` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_topic
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_topic' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_topic` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_user_resume
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_user_resume' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_user_resume` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_vip_package
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_vip_package' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_vip_package` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_wallet
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_wallet' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_wallet` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_writing_contest
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_writing_contest' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_writing_contest` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_writing_prompt
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_writing_prompt' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_writing_prompt` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_wrong_question
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_wrong_question' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_wrong_question` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_book_chapter_view
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'portal_book_chapter_view' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `portal_book_chapter_view` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- sys_* 系统表（13 张）----------

-- sys_config
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_config' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `sys_config` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_deploy_form
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_deploy_form' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `sys_deploy_form` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_dict_data
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_dict_data' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `sys_dict_data` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_dict_type
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_dict_type' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `sys_dict_type` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_expression
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_expression' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `sys_expression` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_file
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_file' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `sys_file` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_form
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_form' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `sys_form` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_job
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_job' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `sys_job` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_listener
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_listener' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `sys_listener` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_menu
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_menu' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `sys_menu` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_notice
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_notice' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `sys_notice` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_notification
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_notification' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `sys_notification` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_post
SET @c := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_post' AND column_name = 'del_flag');
SET @s := IF(@c = 0, 'ALTER TABLE `sys_post` ADD COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'', ADD INDEX `idx_del_flag` (`del_flag`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================
-- 注意：portal_topic_post / portal_topic_comment 已有 is_deleted 字段（应用层逻辑删除）
-- 这两张表的 is_deleted 字段不重命名为 del_flag，保持现状：
--   - 实体中覆盖 BaseEntity 的 delFlag 为 @TableField(exist = false)
--   - 继续用 isDeleted + 应用层 @Update SQL 管理删除
--   - 避免大面积修改话题模块 Mapper 注解 SQL 的风险
-- ============================================

-- ============================================
-- 验证：查询所有已添加 del_flag 字段的表
-- ============================================
SELECT table_name, column_name, column_type, column_default, column_comment
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND column_name = 'del_flag'
ORDER BY table_name;
