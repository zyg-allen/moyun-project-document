-- =============================================================================
-- portal_ai_task 增列 payload：承载「任务大文本输入」（如简历解析抽取文本）
-- -----------------------------------------------------------------------------
-- 背景（v13.38 简历解析架构修正）：
--   简历附件改为**纯内存解析、不落盘/不进对象存储**，上传阶段就地抽取纯文本，
--   再交给 resume_parse 异步任务做 LLM 结构化解析。
--   但原表唯一的参数列 biz_ref 仅 varchar(500)，装不下抽取文本（上限 6000 字符）；
--   而 result 是**输出**语义，不应被当输入复用。
--   → 新增 payload 列承载「大文本输入」（mediumtext，与 result 对齐），
--     小参数仍走 biz_ref，职责分明。
--
-- 幂等性：information_schema.COLUMNS 前置判断（ADD COLUMN 重复执行报 1060），可重复执行。
-- =============================================================================

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_ai_task' AND COLUMN_NAME = 'payload');
SET @ddl := IF(@has = 0,
  'ALTER TABLE `portal_ai_task` ADD COLUMN `payload` mediumtext COLLATE utf8mb4_0900_ai_ci COMMENT ''任务大文本输入（如 resume_parse 的简历抽取文本，上限 6000 字符）；小参数走 biz_ref'' AFTER `biz_ref`',
  'SELECT ''skip: payload 已存在'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 复核 SQL
-- ---------------------------------------------------------------------------
-- SELECT column_name, column_type, column_comment FROM information_schema.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_ai_task' AND column_name IN ('biz_ref','payload','result');
