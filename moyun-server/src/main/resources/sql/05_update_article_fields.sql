-- =============================================
-- 墨韵智库 - 文章表字段更新脚本
-- 用途：为 portal_article 表添加新字段
-- 创建时间：2026-05-27
-- =============================================

-- 为 portal_article 表添加外部链接字段
ALTER TABLE portal_article 
ADD COLUMN IF NOT EXISTS link VARCHAR(500) COMMENT '外部链接' AFTER published_at;

-- 为 portal_article 表添加编辑器模式字段
ALTER TABLE portal_article 
ADD COLUMN IF NOT EXISTS editor_mode VARCHAR(20) DEFAULT 'richtext' COMMENT '编辑器模式：richtext/markdown' AFTER link;

-- 为 portal_article 表添加 Markdown 内容字段
ALTER TABLE portal_article 
ADD COLUMN IF NOT EXISTS content_markdown TEXT COMMENT 'Markdown 原始内容' AFTER editor_mode;

-- 提示信息
SELECT '数据库字段更新完成！' AS message;
