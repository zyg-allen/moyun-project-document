-- =============================================================================
-- 岗位配置统一：删除 portal_interview_position，全部并入 portal_job_template
-- -----------------------------------------------------------------------------
-- 背景（用户裁决，v13.33）：
--   portal_interview_position（面试岗位字典）与 portal_job_template（岗位模板）
--   **职责重复**，且前者**没有任何后台管理入口**（无法配置、无法维护），
--   导致「两套岗位数据、使用混乱」。统一为 **portal_job_template 一处**。
--
-- 处置：
--   ① portal_job_template 并入 6 列（原字典独有，且为业务所必需）：
--        code            岗位编码（如 java_backend）——按编码反查
--        industry        所属行业
--        level           岗位级别 junior/mid/senior
--        required_skills 必备技能 JSON 数组 ← **驱动「简历岗位匹配评分」与「用户画像必备技能」，必须迁移**
--        hot_companies   热门公司 JSON 数组
--        sort            排序
--   ② 迁移数据：旧字典 3 行按 **name 精确匹配**并入同名模板行（补齐上述 6 列）；
--      未匹配到的（前端工程师 / 算法工程师）作为**新模板行**插入。
--   ③ 删除 portal_interview_position（数据已备份至 .archive/drops-20260928/）。
--
-- 消费方改造（同批已改代码）：
--   PortalInterviewPositionController → PortalJobTemplateController
--     （门户公开接口 GET /portal/interview/jobTemplate/list）
--   PortalInterviewPositionServiceImpl.findByName → PortalJobTemplateServiceImpl.findActiveByName
--   ResumeScoringService / UserProfileSnapshotServiceImpl 改注 IPortalJobTemplateService
--
-- 幂等性：
--   · 加列 —— information_schema.COLUMNS 前置判断（ADD COLUMN 重复执行报 1060）
--   · 回填 —— 带 `AND code IS NULL` 守卫，重跑 0 行受影响
--   · 插入 —— 带 `AND NOT EXISTS(...)` 守卫
--   · 删表 —— DROP TABLE IF EXISTS
-- 可重复执行。
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 一、portal_job_template 并入 6 列（逐列判断）
-- ---------------------------------------------------------------------------
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_job_template' AND COLUMN_NAME = 'code');
SET @ddl := IF(@has = 0,
  'ALTER TABLE `portal_job_template` ADD COLUMN `code` varchar(64) DEFAULT NULL COMMENT ''岗位编码（如 java_backend）——v13.33 由 portal_interview_position.code 并入，按编码反查用'' AFTER `position_code`',
  'SELECT ''skip: code 已存在'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_job_template' AND COLUMN_NAME = 'industry');
SET @ddl := IF(@has = 0,
  'ALTER TABLE `portal_job_template` ADD COLUMN `industry` varchar(50) DEFAULT NULL COMMENT ''所属行业（如 互联网/金融/制造）——v13.33 由 portal_interview_position.industry 并入'' AFTER `code`',
  'SELECT ''skip: industry 已存在'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_job_template' AND COLUMN_NAME = 'level');
SET @ddl := IF(@has = 0,
  'ALTER TABLE `portal_job_template` ADD COLUMN `level` varchar(32) DEFAULT NULL COMMENT ''岗位级别 junior/mid/senior——v13.33 由 portal_interview_position.level 并入'' AFTER `industry`',
  'SELECT ''skip: level 已存在'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_job_template' AND COLUMN_NAME = 'required_skills');
SET @ddl := IF(@has = 0,
  'ALTER TABLE `portal_job_template` ADD COLUMN `required_skills` text COMMENT ''必备技能 JSON 数组（如 ["Spring","MySQL"]，与 portal_tag.name 对齐）——v13.33 由 portal_interview_position.required_skills 并入，驱动简历岗位匹配评分与画像必备技能'' AFTER `level`',
  'SELECT ''skip: required_skills 已存在'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_job_template' AND COLUMN_NAME = 'hot_companies');
SET @ddl := IF(@has = 0,
  'ALTER TABLE `portal_job_template` ADD COLUMN `hot_companies` text COMMENT ''热门公司 JSON 数组（如 ["阿里","腾讯"]）——v13.33 由 portal_interview_position.hot_companies 并入'' AFTER `required_skills`',
  'SELECT ''skip: hot_companies 已存在'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_job_template' AND COLUMN_NAME = 'sort');
SET @ddl := IF(@has = 0,
  'ALTER TABLE `portal_job_template` ADD COLUMN `sort` int DEFAULT 0 COMMENT ''排序（升序）——v13.33 由 portal_interview_position.sort 并入'' AFTER `hot_companies`',
  'SELECT ''skip: sort 已存在'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 索引（idx_code / idx_sort）
SET @has := (SELECT COUNT(*) FROM information_schema.STATISTICS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_job_template' AND INDEX_NAME = 'idx_code');
SET @ddl := IF(@has = 0,
  'ALTER TABLE `portal_job_template` ADD KEY `idx_code` (`code`)',
  'SELECT ''skip: idx_code 已存在'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has := (SELECT COUNT(*) FROM information_schema.STATISTICS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_job_template' AND INDEX_NAME = 'idx_sort');
SET @ddl := IF(@has = 0,
  'ALTER TABLE `portal_job_template` ADD KEY `idx_sort` (`sort`)',
  'SELECT ''skip: idx_sort 已存在'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 二、数据迁移（旧字典存在时才有意义；已删则整段空跑）
--     策略：按 name 精确匹配并入同名模板行；未匹配的作为新模板行插入。
--     每条 UPDATE 都带 `AND code IS NULL` 守卫 → 重跑 0 行受影响（幂等）。
-- ---------------------------------------------------------------------------
SET @legacy := (SELECT COUNT(*) FROM information_schema.TABLES
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_interview_position');

-- 2.1 匹配项：把旧字典 6 列并入同名模板行
SET @ddl := IF(@legacy = 1,
  'UPDATE portal_job_template t JOIN portal_interview_position p ON p.name = t.name
      SET t.code = p.code, t.industry = p.industry, t.level = p.`level`,
          t.required_skills = p.required_skills, t.hot_companies = p.hot_companies, t.sort = p.sort
    WHERE t.code IS NULL',
  'SELECT ''skip: 旧字典已不存在，无需迁移'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.2 未匹配项：作为新模板行插入（保留旧字典的全部业务字段）
SET @ddl := IF(@legacy = 1,
  'INSERT INTO portal_job_template
     (name, category, position_code, code, industry, `level`, required_skills, hot_companies, sort,
      description, jd_text, keywords, difficulty, question_count, weights, status, create_by, create_time, update_by, update_time, remark, del_flag)
   SELECT p.name, ''技术'', NULL, p.code, p.industry, p.`level`, p.required_skills, p.hot_companies, p.sort,
          p.description, NULL, NULL, ''medium'', 8, ''{"job":40,"resume":30,"weak":20,"random":10}'', ''active'', ''admin'', NOW(), '''', NOW(),
          ''v13.33 由 portal_interview_position 迁入（需补配 JD 与难度/题量）'', ''0''
     FROM portal_interview_position p
    WHERE NOT EXISTS (SELECT 1 FROM portal_job_template t WHERE t.name = p.name)',
  'SELECT ''skip: 旧字典已不存在，无需迁入'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 三、删除旧表（数据已备份至 .archive/drops-20260928/portal_interview_position.sql）
-- ---------------------------------------------------------------------------
SET @ddl := IF(@legacy = 1,
  'DROP TABLE `portal_interview_position`',
  'SELECT ''skip: 旧字典已删除'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 四、复核 SQL（应返回：旧表不存在 / 新表 5 行且 code+required_skills 均已填充）
-- ---------------------------------------------------------------------------
-- SELECT COUNT(*) AS legacy_table_left FROM information_schema.TABLES
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_interview_position';   -- 期望 0
--
-- SELECT id, name, code, industry, `level`, sort,
--        LEFT(IFNULL(required_skills,''), 30) AS skills_head,
--        difficulty, question_count, status
--   FROM portal_job_template ORDER BY sort, id;
--
-- SELECT COUNT(*) AS missing_skills FROM portal_job_template
--  WHERE status = 'active' AND (required_skills IS NULL OR required_skills = '');   -- 期望 0（否则简历岗位匹配会失分）
