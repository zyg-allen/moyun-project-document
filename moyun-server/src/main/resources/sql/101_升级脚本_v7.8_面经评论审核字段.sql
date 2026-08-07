-- ====================================================================
-- v7.8 升级脚本：面经评论审核字段补齐 + 存量数据迁移
-- 适配 MySQL 8.x
-- 说明：本脚本幂等，可重复执行（使用 information_schema 判断列是否存在）
-- 背景：
--   1. portal_interview_comment 表仅有 status 字段，缺审核人/审核时间/审核意见，
--      无法追溯审核轨迹，与 portal_comment（v6.8 已补齐）不一致。
--   2. 本脚本：
--      a) 为 portal_interview_comment 增加 auditor_id / audit_remark / audit_time 字段，
--         对齐 portal_comment 审核字段设计；
--      b) 增加审核人索引；
--      c) 存量 pending 状态数据迁移为 published（评论类不审核策略，仅定时扫描兜底）。
--   3. 业务层（PortalInterviewServiceImpl.auditComment）已同步升级为：
--      状态白名单校验、乐观锁、审核人/时间/意见写入、事务化。
--   4. 发布侧（PortalInterviewServiceImpl.insertComment）已接入敏感词检查。
-- ====================================================================

SET @db := DATABASE();

-- 1.1 portal_interview_comment.auditor_id
SET @col := 'auditor_id';
SELECT COUNT(*) INTO @exists FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'portal_interview_comment' AND COLUMN_NAME = @col;
SET @sql := IF(@exists = 0,
  'ALTER TABLE portal_interview_comment ADD COLUMN auditor_id BIGINT NULL COMMENT ''审核人ID（系统用户ID，CMS审核或定时扫描命中时写入）'' AFTER status',
  'SELECT ''portal_interview_comment.auditor_id 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.2 portal_interview_comment.audit_remark
SET @col := 'audit_remark';
SELECT COUNT(*) INTO @exists FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'portal_interview_comment' AND COLUMN_NAME = @col;
SET @sql := IF(@exists = 0,
  'ALTER TABLE portal_interview_comment ADD COLUMN audit_remark VARCHAR(500) NULL COMMENT ''审核意见/驳回原因'' AFTER auditor_id',
  'SELECT ''portal_interview_comment.audit_remark 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.3 portal_interview_comment.audit_time
SET @col := 'audit_time';
SELECT COUNT(*) INTO @exists FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'portal_interview_comment' AND COLUMN_NAME = @col;
SET @sql := IF(@exists = 0,
  'ALTER TABLE portal_interview_comment ADD COLUMN audit_time DATETIME NULL COMMENT ''审核时间'' AFTER audit_remark',
  'SELECT ''portal_interview_comment.audit_time 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.4 审核人索引（CMS 审核员维度查询）
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'portal_interview_comment' AND INDEX_NAME = 'idx_auditor_id') = 0,
  'ALTER TABLE portal_interview_comment ADD INDEX idx_auditor_id (auditor_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ====================================================================
-- 2. 存量数据迁移：pending → published
-- 背景：评论类统一为"不审核 + 定时扫描兜底"策略，
--       历史遗留的 pending 状态评论需迁移为 published，避免被定时扫描误判或滞留待审。
-- 注意：deleted 状态保持不变（已删除），仅迁移 pending。
-- ====================================================================
UPDATE portal_interview_comment SET status = 'published', update_time = NOW()
  WHERE status = 'pending';

-- ====================================================================
-- 升级完成
-- ====================================================================
