-- =============================================================================
-- 题库分类扩展：支持「职业 / 行业考试类型」多维度分类
-- -----------------------------------------------------------------------------
-- 背景：portal_interview_category 原为扁平单层（仅 name/slug/description/icon/sort），
--       无法表达「面试题库 / 职业资格 / 公务员 / 考研 …」等考试类型，
--       也无法做「职业大类 → 细分方向」的两级分类。
-- 变更：新增 4 列（只加列、不改列类型、不动存量数据）
--       bank_type  考试类型（决定归类到哪个题库板块；默认 interview 保持存量语义）
--       parent_id  上级分类（0 = 顶级；支持两级）
--       job_family 职业族群（如 后端/前端/测试/产品，便于跨类型聚合）
--       visible    前台是否展示（'0' 展示 / '1' 隐藏；与 sys_menu.visible 同约定）
--
-- 幂等性：每列均有 information_schema 前置判断；可重复执行（连跑两次 exit 0）。
-- 依据：00-项目现状总结 开发铁律 4（DDL 追加 + 增量脚本幂等）。
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. bank_type：考试类型
-- ---------------------------------------------------------------------------
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE table_schema = DATABASE()
     AND table_name = 'portal_interview_category'
     AND column_name = 'bank_type'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE portal_interview_category ADD COLUMN bank_type varchar(32) NOT NULL DEFAULT ''interview'' COMMENT ''题库类型：interview=面试/certification=职业资格/civil-service=公务员/postgraduate=考研/other=其他'' AFTER slug',
  'SELECT ''skip: bank_type 已存在'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 2. parent_id：上级分类（两级分类支持）
-- ---------------------------------------------------------------------------
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE table_schema = DATABASE()
     AND table_name = 'portal_interview_category'
     AND column_name = 'parent_id'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE portal_interview_category ADD COLUMN parent_id bigint NOT NULL DEFAULT 0 COMMENT ''上级分类ID（0=顶级）'' AFTER bank_type',
  'SELECT ''skip: parent_id 已存在'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 3. job_family：职业族群
-- ---------------------------------------------------------------------------
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE table_schema = DATABASE()
     AND table_name = 'portal_interview_category'
     AND column_name = 'job_family'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE portal_interview_category ADD COLUMN job_family varchar(64) DEFAULT NULL COMMENT ''职业族群：后端/前端/测试/产品/运维…'' AFTER parent_id',
  'SELECT ''skip: job_family 已存在'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 4. visible：前台是否展示
-- ---------------------------------------------------------------------------
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE table_schema = DATABASE()
     AND table_name = 'portal_interview_category'
     AND column_name = 'visible'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE portal_interview_category ADD COLUMN visible char(1) NOT NULL DEFAULT ''0'' COMMENT ''前台展示：0=展示 1=隐藏'' AFTER status',
  'SELECT ''skip: visible 已存在'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 5. 索引：按类型 + 父级检索（前台按 bank_type 筛选、后台按父级建树）
--    1061 = Duplicate key name，用 information_schema 前置判断规避
-- ---------------------------------------------------------------------------
SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE table_schema = DATABASE()
     AND table_name = 'portal_interview_category'
     AND index_name = 'idx_bank_type'
);
SET @ddl := IF(@idx_exists = 0,
  'ALTER TABLE portal_interview_category ADD INDEX idx_bank_type (bank_type, visible, sort)',
  'SELECT ''skip: idx_bank_type 已存在'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE table_schema = DATABASE()
     AND table_name = 'portal_interview_category'
     AND index_name = 'idx_parent_id'
);
SET @ddl := IF(@idx_exists = 0,
  'ALTER TABLE portal_interview_category ADD INDEX idx_parent_id (parent_id, sort)',
  'SELECT ''skip: idx_parent_id 已存在'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 6. 存量数据归位：现有分类均为面试场景（算法与数据结构/系统设计/前端开发/…）
--    仅当 bank_type 仍为空或非预期值时才回填，保证可重复执行不覆盖人工调整
-- ---------------------------------------------------------------------------
UPDATE portal_interview_category
   SET bank_type = 'interview'
 WHERE bank_type IS NULL OR bank_type = '';

-- ---------------------------------------------------------------------------
-- 7. 复核 SQL（可人工执行验证）
-- ---------------------------------------------------------------------------
-- SELECT column_name, column_type, is_nullable, column_default, column_comment
--   FROM information_schema.COLUMNS
--  WHERE table_schema = DATABASE() AND table_name = 'portal_interview_category'
--  ORDER BY ordinal_position;
-- SELECT bank_type, COUNT(*) AS cnt FROM portal_interview_category GROUP BY bank_type;
