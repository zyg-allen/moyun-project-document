-- =============================================
-- V21: 面试提交笔记精选功能（支持 note_adopted 成长事件）
-- @author moyun
-- =============================================

ALTER TABLE `portal_interview_submission`
    ADD COLUMN `is_featured`    TINYINT(1)   DEFAULT 0          COMMENT '是否被精选（后台采纳为优质笔记）' AFTER `note`,
    ADD COLUMN `featured_time`  DATETIME     DEFAULT NULL       COMMENT '精选时间' AFTER `is_featured`;

-- 为精选笔记查询添加索引
ALTER TABLE `portal_interview_submission`
    ADD INDEX `idx_submission_featured` (`is_featured`, `featured_time`);
