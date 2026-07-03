-- =====================================================
-- 圈子/兴趣小组功能 DDL 脚本（任务 4.1，社交深化与商业化）
-- 支持圈子创建、加入/退出、发帖、成员管理、后台审核
-- 幂等设计：CREATE TABLE IF NOT EXISTS
-- 执行顺序：本脚本在 03_portal_init.sql 之后执行
-- =====================================================

-- 圈子
CREATE TABLE IF NOT EXISTS `portal_circle` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`          VARCHAR(64)   NOT NULL                COMMENT '圈子名称',
    `description`   TEXT                                  COMMENT '圈子简介',
    `cover`         VARCHAR(500)  DEFAULT NULL            COMMENT '封面URL',
    `owner_id`      BIGINT        NOT NULL                COMMENT '圈主用户ID',
    `member_count`  INT           NOT NULL DEFAULT 0     COMMENT '成员数',
    `post_count`    INT           NOT NULL DEFAULT 0     COMMENT '帖子数',
    `category`      VARCHAR(32)   DEFAULT NULL            COMMENT '分类 reading/writing/tech',
    `status`        VARCHAR(16)   NOT NULL DEFAULT 'active' COMMENT '状态 active/disabled/pending',
    `created_time`  DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_owner` (`owner_id`),
    KEY `idx_status` (`status`),
    KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='圈子';

-- 圈子成员
CREATE TABLE IF NOT EXISTS `portal_circle_member` (
    `id`           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `circle_id`    BIGINT   NOT NULL                COMMENT '圈子ID',
    `user_id`      BIGINT   NOT NULL                COMMENT '用户ID',
    `role`         VARCHAR(16) NOT NULL DEFAULT 'member' COMMENT '角色 owner/admin/member',
    `joined_time`  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_circle_user` (`circle_id`, `user_id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='圈子成员';

-- 圈子帖子
CREATE TABLE IF NOT EXISTS `portal_circle_post` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `circle_id`     BIGINT        NOT NULL                COMMENT '圈子ID',
    `user_id`       BIGINT        NOT NULL                COMMENT '发帖用户ID',
    `title`         VARCHAR(200)  NOT NULL                COMMENT '帖子标题',
    `content`       LONGTEXT                              COMMENT '帖子内容（HTML）',
    `view_count`    INT           NOT NULL DEFAULT 0     COMMENT '浏览数',
    `like_count`    INT           NOT NULL DEFAULT 0     COMMENT '点赞数',
    `reply_count`   INT           NOT NULL DEFAULT 0     COMMENT '回复数',
    `status`        VARCHAR(16)   NOT NULL DEFAULT 'active' COMMENT '状态 active/hidden/deleted',
    `created_time`  DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '发帖时间',
    PRIMARY KEY (`id`),
    KEY `idx_circle` (`circle_id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='圈子帖子';

-- =====================================================
-- 后台管理菜单初始化：圈子管理 + 帖子管理
-- 路径：/portal/circle，组件：portal/circle/index
-- 路径：/portal/circlePost，组件：portal/circlePost/index
-- 权限标识：portal:circle:list / portal:circle:audit / portal:circle:remove
--           portal:circlePost:list / portal:circlePost:remove
-- =====================================================

-- 0. 清理旧的菜单数据（避免重复执行时报错）
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (
    SELECT `menu_id` FROM (SELECT * FROM `sys_menu`) tmp WHERE `menu_name` IN ('圈子管理', '圈子列表', '帖子管理')
);
DELETE FROM `sys_menu` WHERE `menu_name` IN ('圈子管理', '圈子列表', '帖子管理');

-- 1. 插入一级菜单：圈子管理（一级目录，归入门户管理）
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '圈子管理', 0, 15, 'circle-mgr', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'people', 'admin', NOW(), '', NULL, '圈子管理目录'
);

SET @circle_dir_id = LAST_INSERT_ID();

-- 2. 二级菜单：圈子列表（审核/管理）
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '圈子列表', @circle_dir_id, 1, 'circle', 'portal/circle/index', NULL, 1, 0, 'C', '0', '0', 'portal:circle:list', 'list', 'admin', NOW(), '', NULL, '圈子审核与管理'
);

SET @circle_list_menu_id = LAST_INSERT_ID();

INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES
('圈子查询', @circle_list_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:circle:list', '#', 'admin', NOW(), '', NULL, ''),
('圈子审核', @circle_list_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:circle:audit', '#', 'admin', NOW(), '', NULL, ''),
('圈子删除', @circle_list_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:circle:remove', '#', 'admin', NOW(), '', NULL, '');

-- 3. 二级菜单：帖子管理
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '帖子管理', @circle_dir_id, 2, 'circlePost', 'portal/circlePost/index', NULL, 1, 0, 'C', '0', '0', 'portal:circlePost:list', 'edit', 'admin', NOW(), '', NULL, '圈子帖子管理'
);

SET @circle_post_menu_id = LAST_INSERT_ID();

INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES
('帖子查询', @circle_post_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:circlePost:list', '#', 'admin', NOW(), '', NULL, ''),
('帖子删除', @circle_post_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:circlePost:remove', '#', 'admin', NOW(), '', NULL, '');

-- 4. 为管理员角色分配圈子菜单权限
SET @admin_role_id = 1;

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @admin_role_id, `menu_id` FROM `sys_menu`
WHERE `menu_name` IN ('圈子管理', '圈子列表', '帖子管理')
   OR `parent_id` IN (SELECT `menu_id` FROM (SELECT * FROM `sys_menu`) tmp WHERE `menu_name` IN ('圈子列表', '帖子管理'));

SELECT '圈子/兴趣小组初始化脚本执行完成！' AS message;
