-- ============================================
-- 文章浏览记录表 - 用于防止刷阅读量
-- ============================================

-- 1. 创建文章浏览记录表
CREATE TABLE IF NOT EXISTS `portal_article_view` (
                                                     `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
                                                     `article_id` bigint NOT NULL COMMENT '文章ID',
                                                     `user_id` bigint DEFAULT NULL COMMENT '用户ID（NULL表示游客）',
                                                     `ip` varchar(50) DEFAULT NULL COMMENT 'IP地址',
                                                     `view_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
                                                     `user_agent` varchar(500) DEFAULT NULL COMMENT '浏览器User-Agent',
                                                     `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
                                                     `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                     `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
                                                     `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                     `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                                     PRIMARY KEY (`id`),
                                                     KEY `idx_article_id` (`article_id`),
                                                     KEY `idx_user_id` (`user_id`),
                                                     KEY `idx_ip` (`ip`),
                                                     KEY `idx_view_time` (`view_time`),
                                                     KEY `idx_article_user` (`article_id`, `user_id`),
                                                     KEY `idx_article_ip` (`article_id`, `ip`),
                                                     KEY `idx_article_viewtime` (`article_id`, `view_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='文章浏览记录表';

-- 2. 说明
-- - 同一用户对同一文章，5分钟内重复浏览不增加阅读量
-- - 游客（无user_id）按IP地址限制，5分钟内同一IP对同一文章只计一次
-- - 可以通过 view_time 字段定期清理历史数据，节省空间
