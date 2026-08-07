-- ====================================================================
-- v7.14 升级脚本：补面经审核字段 + 修复 Service 持久化
-- 适配 MySQL 8.x
-- 说明：本脚本幂等，可重复执行
--
-- 背景：
--   1. 面经表（portal_interview_experience）缺 auditor_id / audit_remark /
--      audit_time 字段；Service 层 auditExperience() 接收 remark 参数却未落库，
--      导致审核轨迹无法追溯（与文章/专栏/话题/评论的审核闭环不一致）。
--   2. 本脚本：
--      a) 补列：auditor_id / audit_remark / audit_time（对齐文章审核模式）
--      b) 数据回填：对存量已审核状态（published/rejected/archived）回填
--         auditor_id=1(admin)、audit_time=update_time，保证历史数据可查询
--      c) 加索引：idx_experience_auditor
-- ====================================================================

SET @db := DATABASE();

-- --------------------------------------------------------------
-- 1. 面经表：补审核字段
-- --------------------------------------------------------------

-- 1.1 auditor_id
SET @col := 'auditor_id';
SELECT COUNT(*) INTO @exists FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'portal_interview_experience' AND COLUMN_NAME = @col;
SET @sql := IF(@exists = 0,
  'ALTER TABLE portal_interview_experience ADD COLUMN auditor_id BIGINT NULL COMMENT ''审核人ID（系统用户ID，CMS审核时写入）'' AFTER status',
  'SELECT ''portal_interview_experience.auditor_id 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.2 audit_remark
SET @col := 'audit_remark';
SELECT COUNT(*) INTO @exists FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'portal_interview_experience' AND COLUMN_NAME = @col;
SET @sql := IF(@exists = 0,
  'ALTER TABLE portal_interview_experience ADD COLUMN audit_remark VARCHAR(500) NULL COMMENT ''审核意见/驳回原因'' AFTER auditor_id',
  'SELECT ''portal_interview_experience.audit_remark 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.3 audit_time
SET @col := 'audit_time';
SELECT COUNT(*) INTO @exists FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'portal_interview_experience' AND COLUMN_NAME = @col;
SET @sql := IF(@exists = 0,
  'ALTER TABLE portal_interview_experience ADD COLUMN audit_time DATETIME NULL COMMENT ''审核时间'' AFTER audit_remark',
  'SELECT ''portal_interview_experience.audit_time 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- --------------------------------------------------------------
-- 2. 存量数据回填：仅对已审核状态设置默认值
--    status in ('published', 'rejected', 'archived') 视为"已经被处理过"，
--    回填 auditor_id=1(admin)、audit_time=update_time，保证历史查询不乱
-- --------------------------------------------------------------
UPDATE portal_interview_experience
   SET auditor_id = IFNULL(auditor_id, 1),
       audit_time = IFNULL(audit_time, update_time)
 WHERE status IN ('published', 'rejected', 'archived')
   AND auditor_id IS NULL;

-- --------------------------------------------------------------
-- 3. 审核人索引（按审核员维度查询待办/历史）
-- --------------------------------------------------------------
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'portal_interview_experience' AND INDEX_NAME = 'idx_experience_auditor') = 0,
  'ALTER TABLE portal_interview_experience ADD INDEX idx_experience_auditor (auditor_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ====================================================================
-- 升级完成
-- ====================================================================
