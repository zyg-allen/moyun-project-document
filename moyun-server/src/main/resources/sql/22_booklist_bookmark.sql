-- =============================================
-- V22: 书单收藏表（支持 booklist_bookmarked 成长事件）
-- @author moyun
-- =============================================

DROP TABLE IF EXISTS `portal_book_list_bookmark`;
CREATE TABLE `portal_book_list_bookmark` (
    `id`          BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `booklist_id` BIGINT NOT NULL COMMENT '书单ID',
    `user_id`     BIGINT NOT NULL COMMENT '用户ID',
    `create_by`   VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    `update_by`   VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`      VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_booklist_user` (`booklist_id`, `user_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='书单收藏表';
