-- 1. 增加字段
-- truncate table portal_comment;
ALTER TABLE `portal_comment` 
ADD COLUMN `root_id` bigint DEFAULT '0' COMMENT '根评论ID（一级评论ID）' AFTER `parent_id`,
ADD COLUMN `reply_to_content` varchar(200) DEFAULT '' COMMENT '被回复的内容摘要' AFTER `reply_to`;

-- 2. 增加索引
ALTER TABLE `portal_comment` 
ADD INDEX `idx_article_root` (`article_id`, `root_id`);

-- 3. 历史数据迁移
-- 一级评论 root_id = 0
UPDATE `portal_comment` SET `root_id` = 0 WHERE `parent_id` = 0 OR `parent_id` IS NULL;

-- 二级及以上评论：先更新 parent_id 字段，再找到所属的一级评论ID
-- 首先确保所有评论都有 parent_id
UPDATE `portal_comment` SET `parent_id` = 0 WHERE `parent_id` IS NULL;

-- 然后更新 root_id
UPDATE `portal_comment` c1
INNER JOIN `portal_comment` c2 ON c1.`parent_id` = c2.`id`
SET c1.`root_id` = IF(c2.`parent_id` = 0, c2.`id`, c2.`root_id`)
WHERE c1.`parent_id` != 0;
