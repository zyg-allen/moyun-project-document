-- =============================================================================
-- portal_article 增加 SEO 三列（清单 #4，v13.89）
-- -----------------------------------------------------------------------------
-- 背景：门户发布页「高级选项」一直在采集 SEO 标题/描述/关键词，但**后端没有这些列**
--       ⇒ 用户填了等于没填。本脚本补齐列，配合实体/DTO/Mapper/VO 的字段贯通。
--
-- 幂等性：每列均以 information_schema.COLUMNS 前置判断（重复执行安全），
--         满足 IncrementSqlIdempotencyGuardTest 对"结构性 DDL 必须可重跑"的要求。
-- 全新库：等价内容已写入 init-sql/moyun-db-ddl.sql（portal_article 建表语句）。
-- =============================================================================

SET NAMES utf8mb4;

-- seo_title
SET @ddl := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_article' AND COLUMN_NAME = 'seo_title') = 0,
  'ALTER TABLE `portal_article` ADD COLUMN `seo_title` varchar(200) DEFAULT NULL COMMENT ''SEO 自定义标题（留空回退文章标题）'' AFTER `preview_length`',
  'SELECT ''skip: portal_article.seo_title 已存在'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- seo_description
SET @ddl := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_article' AND COLUMN_NAME = 'seo_description') = 0,
  'ALTER TABLE `portal_article` ADD COLUMN `seo_description` varchar(500) DEFAULT NULL COMMENT ''SEO 自定义描述（留空回退摘要）'' AFTER `seo_title`',
  'SELECT ''skip: portal_article.seo_description 已存在'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- seo_keywords
SET @ddl := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_article' AND COLUMN_NAME = 'seo_keywords') = 0,
  'ALTER TABLE `portal_article` ADD COLUMN `seo_keywords` varchar(300) DEFAULT NULL COMMENT ''SEO 关键词（逗号分隔）'' AFTER `seo_description`',
  'SELECT ''skip: portal_article.seo_keywords 已存在'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 复核：应返回 3
SELECT COUNT(*) AS seo_columns
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_article'
   AND COLUMN_NAME IN ('seo_title','seo_description','seo_keywords');
