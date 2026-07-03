-- =====================================================
-- 文章版本管理 DDL 脚本（草稿版本管理）
-- 保存文章时生成版本快照，支持版本列表、详情、回滚、对比
-- 幂等设计：CREATE TABLE IF NOT EXISTS
-- 执行顺序：本脚本在 57_column_init.sql 之后执行
-- =====================================================

CREATE TABLE IF NOT EXISTS `portal_article_version` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `article_id`       BIGINT        NOT NULL                COMMENT '文章ID',
    `version_no`       INT           NOT NULL                COMMENT '版本号（同一文章内自增）',
    `title`            VARCHAR(256)  NOT NULL                COMMENT '版本标题快照',
    `content`          LONGTEXT                               COMMENT '版本内容快照（HTML）',
    `content_markdown` LONGTEXT                               COMMENT '版本 Markdown 原始内容快照',
    `excerpt`          VARCHAR(500)  DEFAULT NULL             COMMENT '版本摘要快照',
    `operator_id`      BIGINT        DEFAULT NULL             COMMENT '操作人ID（保存/回滚的执行者）',
    `created_time`     DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '版本创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_article_version` (`article_id`, `version_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章版本快照';
