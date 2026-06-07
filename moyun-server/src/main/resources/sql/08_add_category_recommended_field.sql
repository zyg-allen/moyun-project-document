-- =============================================
-- 墨韵智库 - 增加栏目推荐字段
-- 版本: v1.1
-- 描述: 为portal_article表增加is_category_recommended字段
-- =============================================

-- 1. 为portal_article表增加is_category_recommended字段
ALTER TABLE `portal_article` 
ADD COLUMN `is_category_recommended` tinyint(1) DEFAULT '0' COMMENT '是否栏目推荐' 
AFTER `is_carousel`;

-- 2. 为新字段增加索引
ALTER TABLE `portal_article` 
ADD INDEX `idx_is_category_recommended` (`is_category_recommended`);
