-- =============================================================================
-- 20260927-04 话题模块软删列统一：is_deleted(tinyint 0/1) → del_flag(char '0'/'2')（v13.13）
-- =============================================================================
-- 背景（报告六 §6.5「三套逻辑删除」的最后一处真实偏差）：
--   `portal_topic_post` / `portal_topic_comment` 早期沿用 is_deleted(tinyint 0/1) +
--   实体级 @TableLogic 覆盖（PortalTopicPost/PortalTopicComment 各自声明），
--   与全库 del_flag(char '0'/'2' + BaseEntity + 全局 logic-delete-field=delFlag) 不一致。
--   两表实体在 v13.13 已回归 BaseEntity.delFlag，本脚本把库里列一并统一。
--
-- 数据映射：is_deleted = 0 → del_flag = '0'（存在）；is_deleted = 1 → del_flag = '2'（删除）
-- 影响面：仅上述 2 张表的 1 列（新增 + 数据映射 + 删除旧列），无索引变更（is_deleted 未被索引）。
-- 幂等：ADD / DROP 通过 information_schema 前置判断 + 预处理语句保证重复执行安全
--       （已存在 del_flag 或已无 is_deleted 时自动跳过）；UPDATE 天然幂等。
-- 执行记录：dev 库已执行并复核（两表当时为 0 行，映射为空操作；列结构已变更，见文件末尾复核 SQL）。
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 一、portal_topic_post
-- -----------------------------------------------------------------------------
SET @has_old := (SELECT COUNT(*) FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_topic_post' AND COLUMN_NAME = 'is_deleted');
SET @has_new := (SELECT COUNT(*) FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_topic_post' AND COLUMN_NAME = 'del_flag');

SET @sql := IF(@has_old = 1 AND @has_new = 0,
    'ALTER TABLE `portal_topic_post` ADD COLUMN `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'' AFTER `comment_count`',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(@has_old = 1,
    'UPDATE `portal_topic_post` SET `del_flag` = CASE WHEN `is_deleted` = 1 THEN ''2'' ELSE ''0'' END',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(@has_old = 1, 'ALTER TABLE `portal_topic_post` DROP COLUMN `is_deleted`', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 二、portal_topic_comment
-- -----------------------------------------------------------------------------
SET @has_old := (SELECT COUNT(*) FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_topic_comment' AND COLUMN_NAME = 'is_deleted');
SET @has_new := (SELECT COUNT(*) FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_topic_comment' AND COLUMN_NAME = 'del_flag');

SET @sql := IF(@has_old = 1 AND @has_new = 0,
    'ALTER TABLE `portal_topic_comment` ADD COLUMN `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT ''0'' COMMENT ''删除标记（0=存在 2=删除）'' AFTER `reply_count`',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(@has_old = 1,
    'UPDATE `portal_topic_comment` SET `del_flag` = CASE WHEN `is_deleted` = 1 THEN ''2'' ELSE ''0'' END',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(@has_old = 1, 'ALTER TABLE `portal_topic_comment` DROP COLUMN `is_deleted`', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =============================================================================
-- 三、复核 SQL（执行后应满足）
-- =============================================================================
-- SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT, COLLATION_NAME
--   FROM information_schema.COLUMNS
--   WHERE TABLE_SCHEMA = DATABASE()
--     AND TABLE_NAME IN ('portal_topic_post','portal_topic_comment')
--     AND COLUMN_NAME IN ('del_flag','is_deleted');
--   -- 期望：每表只有 del_flag（char(1)、默认 '0'、collation utf8mb4_0900_ai_ci），无 is_deleted
--
-- SELECT COUNT(*) FROM information_schema.COLUMNS
--   WHERE TABLE_SCHEMA = DATABASE() AND COLUMN_NAME = 'is_deleted';
--   -- 期望：0（全库已无 is_deleted）
