-- ============================================================
-- 72_mention_report_init.sql
-- 阶段四 4.3：@提及通知 + 评论举报扩展
-- 1. 扩展 portal_report 表，增加 target_type / target_id 字段
--    用于支持评论/文章等具体内容的定向举报（原有 target_url 保留，向后兼容）
-- 说明：portal_report 表已由 38_init_report_feedback_menu.sql 创建，此处仅做增量扩展
-- @author moyun
-- ============================================================

-- 举报目标类型（comment=评论 / article=文章 / user=用户 / 其他自定义）
ALTER TABLE `portal_report`
    ADD COLUMN `target_type` VARCHAR(32) DEFAULT NULL COMMENT '举报目标类型：comment/article/user 等，为空表示通用举报（仅 target_url）' AFTER `target_url`;

-- 举报目标ID（评论ID/文章ID/用户ID 等，配合 target_type 使用）
ALTER TABLE `portal_report`
    ADD COLUMN `target_id` BIGINT DEFAULT NULL COMMENT '举报目标ID（评论/文章/用户ID，配合 target_type 使用）' AFTER `target_type`;

-- 目标类型 + 目标ID 联合索引，便于按内容维度统计/查重
ALTER TABLE `portal_report`
    ADD INDEX `idx_target` (`target_type`, `target_id`);
