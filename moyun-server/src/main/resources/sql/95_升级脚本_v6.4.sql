-- =============================================================================
-- v6.4 升级脚本：首页运营指标修复
--   1. sys_logininfor 加 user_type 字段（区分后台/门户登录来源）
--   2. portal_category 加 category_type 字段（区分文章栏目/特殊页面）
--   3. portal_category 种子数据回填 category_type（散文天地/技术笔记=article，其余按 nav_route_type 判定）
-- 说明：使用 information_schema 判断列是否存在，幂等可重复执行
-- =============================================================================

-- ---------- 1. sys_logininfor.user_type ----------
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_logininfor'
      AND COLUMN_NAME = 'user_type'
);
SET @sql := IF(@col_exists = 0,
               'ALTER TABLE sys_logininfor ADD COLUMN user_type varchar(10) DEFAULT ''sys'' COMMENT ''登录来源类型（sys=后台用户 portal=门户用户）'' AFTER status',
               'SELECT ''sys_logininfor.user_type 已存在，跳过'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 补索引（幂等）
SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_logininfor' AND INDEX_NAME = 'idx_sys_logininfor_ut'
);
SET @sql := IF(@idx_exists = 0,
               'ALTER TABLE sys_logininfor ADD KEY idx_sys_logininfor_ut (user_type)',
               'SELECT ''idx_sys_logininfor_ut 已存在，跳过'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- ---------- 2. portal_category.category_type ----------
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'portal_category'
      AND COLUMN_NAME = 'category_type'
);
SET @sql := IF(@col_exists = 0,
               'ALTER TABLE portal_category ADD COLUMN category_type varchar(20) NOT NULL DEFAULT ''article'' COMMENT ''栏目内容类型（article=文章栏目可发布文章 special=特殊页面不发布文章）'' AFTER nav_route_path',
               'SELECT ''portal_category.category_type 已存在，跳过'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 补索引（幂等）
SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_category' AND INDEX_NAME = 'idx_category_type'
);
SET @sql := IF(@idx_exists = 0,
               'ALTER TABLE portal_category ADD KEY idx_category_type (category_type)',
               'SELECT ''idx_category_type 已存在，跳过'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- ---------- 3. 回填 portal_category.category_type ----------
-- 规则：nav_route_type='category' 视为文章栏目（article）；home/static/external 视为特殊页面（special）
-- 幂等：仅对 category_type 仍为默认值 'article' 但 nav_route_type<>'category' 的行做修正
UPDATE portal_category
SET category_type = 'special'
WHERE nav_route_type <> 'category'
  AND (category_type IS NULL OR category_type = 'article');

-- 二次兜底：nav_route_type='category' 但被误标为 special 的，修正回 article
UPDATE portal_category
SET category_type = 'article'
WHERE nav_route_type = 'category'
  AND (category_type IS NULL OR category_type = '' OR category_type = 'special');

-- 校验
SELECT '=== portal_category.category_type 分布 ===' AS info;
SELECT category_type, nav_route_type, COUNT(*) AS cnt
FROM portal_category
GROUP BY category_type, nav_route_type
ORDER BY category_type, nav_route_type;
