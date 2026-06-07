-- 给 portal_article 表增加分类路径字段，方便按层级查询
-- 支持按顶级分类或任意层级分类进行查找

ALTER TABLE `portal_article`
ADD COLUMN `root_category_id` BIGINT DEFAULT NULL COMMENT '顶级分类ID' AFTER `category_id`,
ADD COLUMN `category_path` VARCHAR(500) DEFAULT NULL COMMENT '分类路径，包含所有祖先ID，例如：1,3,5';

-- 给新字段加索引，提升查询效率
ALTER TABLE `portal_article`
ADD INDEX `idx_root_category_id` (`root_category_id`),
ADD INDEX `idx_category_path` (`category_path`(100));
