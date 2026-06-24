-- =============================================
-- V23: 成长体系后台管理菜单初始化
-- 版本：2.8
-- 日期：2026-06-18
-- 说明：为墨韵智库成长体系模块添加后台管理菜单和权限
-- 层级：独立一级菜单（与"面试空间"同级），二级菜单页面
-- 前端页面：moyun-admin-vue/src/views/cms/growth/{rule,achievement,user,badge,log}/index.vue
-- 后端接口：CmsGrowthController（/cms/growth/*）
-- =============================================

-- 0. 清理旧的成长体系菜单数据（如果存在，避免重复执行时报错或路径重复）
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (
    SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` IN (
        '成长体系', '成长规则', '成就管理', '用户成长', '用户徽章', '成长流水'
    ) OR `parent_id` IN (
        SELECT `menu_id` FROM (SELECT * FROM `sys_menu`) tmp WHERE `menu_name` IN (
            '成长体系', '成长规则', '成就管理', '用户成长', '用户徽章', '成长流水'
        )
    )
);
DELETE FROM `sys_menu` WHERE `menu_name` IN (
    '成长体系', '成长规则', '成就管理', '用户成长', '用户徽章', '成长流水'
) OR `parent_id` IN (
    SELECT `menu_id` FROM (SELECT * FROM `sys_menu`) tmp WHERE `menu_name` IN (
        '成长体系', '成长规则', '成就管理', '用户成长', '用户徽章', '成长流水'
    )
);

-- 1. 插入一级菜单：成长体系（与"面试空间"同级）
-- 注意：M类型（目录）的 component 为 NULL，RuoYi 后端 buildMenus 会自动设为 Layout
--       path 不带前导/，后端会自动加前导/ 变成 /growth
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '成长体系', 0, 13, 'growth', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'chart', 'admin', NOW(), '', NULL, '成长体系目录'
);

SET @growth_menu_id = LAST_INSERT_ID();

-- =============================================
-- 2. 成长规则管理
-- 注意：C类型（菜单）的 path 不带前导/，前端路由会自动拼接为 /growth/rule
--       component 为 views 目录下的相对路径（不含 .vue 后缀）
-- =============================================
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '成长规则', @growth_menu_id, 1, 'rule', 'cms/growth/rule/index', NULL, 1, 0, 'C', '0', '0', 'cms:growth:list', 'edit', 'admin', NOW(), '', NULL, '成长规则配置菜单'
);

SET @rule_menu_id = LAST_INSERT_ID();

INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES
('规则查询', @rule_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:growth:query', '#', 'admin', NOW(), '', NULL, ''),
('规则新增', @rule_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:growth:add', '#', 'admin', NOW(), '', NULL, ''),
('规则修改', @rule_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:growth:edit', '#', 'admin', NOW(), '', NULL, ''),
('规则删除', @rule_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:growth:remove', '#', 'admin', NOW(), '', NULL, '');

-- =============================================
-- 3. 成就定义管理
-- =============================================
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '成就管理', @growth_menu_id, 2, 'achievement', 'cms/growth/achievement/index', NULL, 1, 0, 'C', '0', '0', 'cms:growth:list', 'star', 'admin', NOW(), '', NULL, '成就定义管理菜单'
);

SET @achievement_menu_id = LAST_INSERT_ID();

INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES
('成就查询', @achievement_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:growth:query', '#', 'admin', NOW(), '', NULL, ''),
('成就新增', @achievement_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:growth:add', '#', 'admin', NOW(), '', NULL, ''),
('成就修改', @achievement_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:growth:edit', '#', 'admin', NOW(), '', NULL, ''),
('成就删除', @achievement_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:growth:remove', '#', 'admin', NOW(), '', NULL, '');

-- =============================================
-- 4. 用户成长查询
-- =============================================
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '用户成长', @growth_menu_id, 3, 'user-growth', 'cms/growth/user/index', NULL, 1, 0, 'C', '0', '0', 'cms:growth:list', 'people', 'admin', NOW(), '', NULL, '用户成长值查询菜单'
);

SET @user_growth_menu_id = LAST_INSERT_ID();

INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES
('成长查询', @user_growth_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:growth:query', '#', 'admin', NOW(), '', NULL, ''),
('成长调整', @user_growth_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:growth:edit', '#', 'admin', NOW(), '', NULL, '');

-- =============================================
-- 5. 用户徽章查询
-- =============================================
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '用户徽章', @growth_menu_id, 4, 'badge', 'cms/growth/badge/index', NULL, 1, 0, 'C', '0', '0', 'cms:growth:list', 'medal', 'admin', NOW(), '', NULL, '用户徽章记录查询菜单'
);

SET @badge_menu_id = LAST_INSERT_ID();

INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES
('徽章查询', @badge_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:growth:query', '#', 'admin', NOW(), '', NULL, ''),
('徽章授予', @badge_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:growth:add', '#', 'admin', NOW(), '', NULL, ''),
('徽章撤销', @badge_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:growth:remove', '#', 'admin', NOW(), '', NULL, '');

-- =============================================
-- 6. 成长流水查询
-- =============================================
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '成长流水', @growth_menu_id, 5, 'log', 'cms/growth/log/index', NULL, 1, 0, 'C', '0', '0', 'cms:growth:list', 'log', 'admin', NOW(), '', NULL, '成长事件流水查询菜单'
);

SET @log_menu_id = LAST_INSERT_ID();

INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES
('流水查询', @log_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:growth:query', '#', 'admin', NOW(), '', NULL, '');

-- =============================================
-- 7. 为管理员角色分配成长体系菜单权限
-- =============================================

SET @admin_role_id = 1;

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @admin_role_id, `menu_id` FROM `sys_menu`
WHERE `menu_name` IN (
    '成长体系', '成长规则', '成就管理', '用户成长', '用户徽章', '成长流水'
) OR `parent_id` IN (
    SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` IN (
        '成长体系', '成长规则', '成就管理', '用户成长', '用户徽章', '成长流水'
    )
);

-- =============================================
-- 脚本执行完成
-- =============================================

SELECT '成长体系后台管理菜单初始化脚本执行完成！' AS message;
