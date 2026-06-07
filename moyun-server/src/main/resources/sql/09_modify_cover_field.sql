-- 修改文章封面字段，支持 base64 图片
-- 原来只有 varchar(500)，现在改为 text 类型，支持更大的 base64 数据

ALTER TABLE `portal_article`
MODIFY COLUMN `cover` TEXT DEFAULT NULL COMMENT '封面图片URL或Base64';
