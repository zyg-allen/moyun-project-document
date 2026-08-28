-- =========================================================================================
-- 增量补丁：修复 /portal/prompt/today 接口 500：Unknown column 'festival_name' in 'field list'
-- 根因：PortalWritingPrompt 实体新增了 festival_name、source 两个字段，
--       但主 DDL 与线上库 portal_writing_prompt 表中尚未同步这两列（实体/DDL/现库三方漂移）。
-- 部署日期：2026-08-28
-- 执行：在已部署的 moyun-db 数据库中运行本 SQL；不需要重建库。
-- =========================================================================================
USE `moyun-db`;

ALTER TABLE `portal_writing_prompt`
    ADD COLUMN `festival_name` varchar(64) DEFAULT NULL COMMENT '关联特殊日期名称（节日/节气/纪念日，AI生成时自动识别）'
        AFTER `category`,
    ADD COLUMN `source` varchar(16) DEFAULT 'ai' COMMENT '来源：ai=AI生成 / manual=手动创建'
        AFTER `festival_name`;

-- （可选）给历史数据补默认值：source='ai'（AI 生成的旧数据）
UPDATE `portal_writing_prompt` SET `source` = 'ai' WHERE `source` IS NULL OR `source` = '';
