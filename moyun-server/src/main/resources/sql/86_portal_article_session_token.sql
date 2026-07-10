-- =============================================================
-- 86_portal_article_session_token.sql
-- 文章编辑会话标识：解决草稿/自动保存/发布生成多条数据的问题
--
-- 背景：前台发布页一次编辑会话应只产生一条文章记录。
--       原逻辑依赖「同作者+同标题+draft状态」复用草稿，但发布后状态
--       变 pending，再次保存草稿会因状态不匹配而新建记录，导致重复。
--
-- 方案：新增 session_token 字段作为编辑会话唯一标识。
--       前端进入发布页生成 token，保存草稿/发布都带上；
--       后端用 token 做幂等：同 token 已存在记录则更新，否则新建。
--       辅以 id 双重保障：有 id 优先按 id 更新，无 id 时按 token 查找。
-- =============================================================

ALTER TABLE `portal_article`
    ADD COLUMN `session_token` VARCHAR(64) DEFAULT NULL COMMENT '编辑会话标识（一次编辑会话唯一，用于草稿/发布幂等去重）' AFTER `editor_mode`;

-- 加索引加速按 token 查找（非唯一，草稿发布后 token 保留便于后续编辑沿用）
ALTER TABLE `portal_article` ADD INDEX `idx_session_token` (`session_token`);
