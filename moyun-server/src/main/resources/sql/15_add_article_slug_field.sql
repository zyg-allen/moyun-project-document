-- 给 portal_article 表增加 slug 字段，支持语义化 URL
-- 例如 /article/301/spring-boot-best-practice

ALTER TABLE `portal_article`
ADD COLUMN `slug` VARCHAR(500) DEFAULT NULL COMMENT '文章URL别名，用于SEO语义化路径' AFTER `title`;

-- 给 slug 字段加唯一索引，确保别名唯一
ALTER TABLE `portal_article`
ADD UNIQUE INDEX `uk_slug` (`slug`);

-- 给已有数据生成默认 slug（基于标题的拼音或简单替换）
-- 注意：实际项目中建议通过后端代码自动生成 slug
-- UPDATE portal_article SET slug = CONCAT('article-', id) WHERE slug IS NULL;
