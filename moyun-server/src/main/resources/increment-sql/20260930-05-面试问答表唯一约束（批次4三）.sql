-- =============================================================================
-- 语音面试问答表唯一约束（批次 4 三，v13.53）
-- -----------------------------------------------------------------------------
-- 目的：从**数据库层**杜绝「同一场面试的同一题号插入两条主问」。
--   批次 0 已从应用层加了两道防线（答题幂等分布式锁 + 建下题前判重），
--   本脚本补上最终一道：即使应用层守卫被绕过（并发、锁 TTL 到期、直连写库），
--   数据库也会拒绝产生重复主问。
--
-- ⚠️ 为什么不能简单加 UNIQUE(interview_id, question_idx)：
--   追问（follow-up）与主问**共享同一 question_idx**，仅靠 `parent_qa_id` 区分；
--   而 MySQL 唯一索引对 **NULL 不去重**（已实测：两条 parent_qa_id=NULL 可共存）
--   ⇒ 直接加唯一索引：① 拦不住重复主问；② 若换成含 parent_qa_id 的普通唯一键，
--   又因 NULL 语义导致主问完全不受约束。
--
-- ✅ 方案：新增 **STORED 生成列** `uk_qa` = `interview_id:question_idx:ifnull(parent_qa_id,0)`，
--   再对其建唯一索引。效果（已实测验证）：
--   · 同一 (会话, 题号, parent=NULL) 第二次插入 → **被拒**（重复主问被拦住）
--   · 同一 (会话, 题号) 下多个不同 parent 的追问 → **正常允许**
--   · 同一 (会话, 题号) 下同一 parent 的重复追问 → 被拒（额外收益）
--
-- 幂等：结构性 DDL 用 information_schema.COLUMNS / STATISTICS 预检 + PREPARE/EXECUTE，
--       可重复执行（连跑两次 exit 0）。
-- 可回滚：DROP INDEX uk_qa_main ON portal_voice_interview_qa;
--         ALTER TABLE portal_voice_interview_qa DROP COLUMN uk_qa;
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 0) 前置体检：若已有重复主问，本脚本会失败 —— 先暴露出来（只查不改）
--    期望：无输出（0 行）
-- ---------------------------------------------------------------------------
SELECT '前置体检：重复主问（期望 0 行）' AS stage, interview_id, question_idx, COUNT(*) AS cnt
  FROM portal_voice_interview_qa
 WHERE parent_qa_id IS NULL
 GROUP BY interview_id, question_idx
HAVING COUNT(*) > 1;

-- ---------------------------------------------------------------------------
-- 1) 新增生成列 uk_qa（幂等：列不存在才加）
-- ---------------------------------------------------------------------------
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'portal_voice_interview_qa'
     AND COLUMN_NAME = 'uk_qa'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE `portal_voice_interview_qa` ADD COLUMN `uk_qa` varchar(48) GENERATED ALWAYS AS (concat(`interview_id`,_utf8mb4'':'' ,`question_idx`,_utf8mb4'':'' ,ifnull(`parent_qa_id`,0))) STORED COMMENT ''v13.53 唯一键载体：会话:题号:父问答（NULL→0），绕开 MySQL 唯一索引对 NULL 不去重的语义'' AFTER `analysis_status`',
  'SELECT ''uk_qa 列已存在，跳过'' AS skip_msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 2) 新增唯一索引 uk_qa_main（幂等：索引不存在才建）
-- ---------------------------------------------------------------------------
SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'portal_voice_interview_qa'
     AND INDEX_NAME = 'uk_qa_main'
);
SET @sql2 := IF(@idx_exists = 0,
  'ALTER TABLE `portal_voice_interview_qa` ADD UNIQUE KEY `uk_qa_main` (`uk_qa`)',
  'SELECT ''uk_qa_main 索引已存在，跳过'' AS skip_msg');
PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- ---------------------------------------------------------------------------
-- 3) 复核（期望：col=1、idx=1、重复主问=0）
-- ---------------------------------------------------------------------------
SELECT '复核：生成列存在' AS stage, COUNT(*) AS cnt
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE()
   AND TABLE_NAME = 'portal_voice_interview_qa'
   AND COLUMN_NAME = 'uk_qa';

SELECT '复核：唯一索引存在' AS stage, COUNT(*) AS cnt
  FROM information_schema.STATISTICS
 WHERE TABLE_SCHEMA = DATABASE()
   AND TABLE_NAME = 'portal_voice_interview_qa'
   AND INDEX_NAME = 'uk_qa_main';

SELECT '复核：重复主问（期望 0）' AS stage, COUNT(*) AS cnt
  FROM (SELECT interview_id, question_idx
          FROM portal_voice_interview_qa
         WHERE parent_qa_id IS NULL
         GROUP BY interview_id, question_idx
        HAVING COUNT(*) > 1) dup;

-- ---------------------------------------------------------------------------
-- 备份建议（执行前）：
--   CREATE TABLE IF NOT EXISTS portal_voice_interview_qa_bak_v1353 AS
--     SELECT * FROM portal_voice_interview_qa;
-- ---------------------------------------------------------------------------
