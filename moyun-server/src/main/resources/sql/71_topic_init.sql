-- =====================================================
-- 话题/超话功能 DDL 脚本（任务 4.2，社交深化与商业化）
-- 基于 portal_entity_tag 扩展：话题聚合页通过 portal_tag + portal_entity_tag 关联文章/面经/动态
-- 新增 portal_topic（话题元数据）+ portal_topic_follow（话题关注）
-- 幂等设计：CREATE TABLE IF NOT EXISTS
-- 执行顺序：本脚本在 03_portal_init.sql、31_create_portal_entity_tag.sql 之后执行
-- =====================================================

-- 话题
CREATE TABLE IF NOT EXISTS `portal_topic` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`          VARCHAR(64)   NOT NULL                COMMENT '话题名称',
    `slug`          VARCHAR(128)  NOT NULL                COMMENT '话题别名（URL 友好）',
    `description`   TEXT                                  COMMENT '话题描述',
    `cover`         VARCHAR(500)  DEFAULT NULL            COMMENT '话题封面',
    `post_count`    INT           NOT NULL DEFAULT 0     COMMENT '关联内容数',
    `follow_count`  INT           NOT NULL DEFAULT 0     COMMENT '关注数',
    `status`        VARCHAR(16)   NOT NULL DEFAULT 'active' COMMENT '状态 active/disabled',
    `created_time`  DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_slug` (`slug`),
    UNIQUE KEY `uk_name` (`name`),
    KEY `idx_status` (`status`),
    KEY `idx_follow_count` (`follow_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='话题/超话';

-- 话题关注（独立于 portal_follow，避免改动现有用户关注逻辑）
CREATE TABLE IF NOT EXISTS `portal_topic_follow` (
    `id`            BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `topic_id`      BIGINT   NOT NULL                COMMENT '话题ID',
    `user_id`       BIGINT   NOT NULL                COMMENT '关注用户ID',
    `created_time`  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_topic_user` (`topic_id`, `user_id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='话题关注';

-- =====================================================
-- 后台管理菜单初始化：话题管理
-- 路径：/portal/topic，组件：portal/topic/index
-- 权限标识：portal:topic:list / portal:topic:add / portal:topic:edit / portal:topic:remove
-- =====================================================

-- 0. 清理旧的菜单数据（避免重复执行时报错）
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (
    SELECT `menu_id` FROM (SELECT * FROM `sys_menu`) tmp WHERE `menu_name` IN ('话题管理', '话题列表')
);
DELETE FROM `sys_menu` WHERE `menu_name` IN ('话题管理', '话题列表');

-- 1. 插入一级菜单：话题管理
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '话题管理', 0, 16, 'topic-mgr', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'message', 'admin', NOW(), '', NULL, '话题管理目录'
);

SET @topic_dir_id = LAST_INSERT_ID();

-- 2. 二级菜单：话题列表（CRUD）
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '话题列表', @topic_dir_id, 1, 'topic', 'portal/topic/index', NULL, 1, 0, 'C', '0', '0', 'portal:topic:list', 'list', 'admin', NOW(), '', NULL, '话题CRUD'
);

SET @topic_list_menu_id = LAST_INSERT_ID();

INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES
('话题查询', @topic_list_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:topic:list', '#', 'admin', NOW(), '', NULL, ''),
('话题新增', @topic_list_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:topic:add', '#', 'admin', NOW(), '', NULL, ''),
('话题修改', @topic_list_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:topic:edit', '#', 'admin', NOW(), '', NULL, ''),
('话题删除', @topic_list_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:topic:remove', '#', 'admin', NOW(), '', NULL, '');

-- 3. 为管理员角色分配话题菜单权限
SET @admin_role_id = 1;

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @admin_role_id, `menu_id` FROM `sys_menu`
WHERE `menu_name` IN ('话题管理', '话题列表')
   OR `parent_id` IN (SELECT `menu_id` FROM (SELECT * FROM `sys_menu`) tmp WHERE `menu_name` = '话题列表');

SELECT '话题/超话初始化脚本执行完成！' AS message;
