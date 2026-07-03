-- =====================================================
-- 打赏后台管理菜单初始化脚本（2.2 打赏）
-- 挂载在"内容管理"一级菜单下
-- 幂等设计：INSERT ... WHERE NOT EXISTS
-- 执行顺序：本脚本在 58_tip_init.sql 之后执行
-- =====================================================

-- 取"内容管理"父菜单ID
SELECT @cms_parent_id := menu_id FROM sys_menu WHERE menu_name = '内容管理' AND parent_id = 0 LIMIT 1;
SET @cms_parent_id = IFNULL(@cms_parent_id, 0);

-- =============================================
-- 打赏管理（只读查询）
-- =============================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '打赏管理', @cms_parent_id, 13, 'tip', 'cms/tip/index', NULL, 1, 0,
       'C', '0', '0', 'portal:tip:list', 'money', 'admin', NOW(), '打赏流水后台查询菜单'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:tip:list');
SELECT @tip_menu_id := menu_id FROM sys_menu WHERE perms = 'portal:tip:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '打赏查询', @tip_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:tip:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:tip:query');

-- =============================================
-- 为管理员角色分配打赏管理菜单权限
-- =============================================
SET @admin_role_id = 1;

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @admin_role_id, `menu_id` FROM `sys_menu`
WHERE `perms` IN (
    'portal:tip:list', 'portal:tip:query'
)
AND `menu_id` NOT IN (SELECT `menu_id` FROM `sys_role_menu` WHERE `role_id` = @admin_role_id);

-- =============================================
-- 校验结果
-- =============================================
SELECT '打赏后台管理菜单初始化脚本执行完成！' AS message;
