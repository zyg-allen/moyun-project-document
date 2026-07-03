-- =====================================================
-- 专栏后台管理菜单初始化脚本（2.1 专栏）
-- 挂载在"内容管理"一级菜单下
-- 幂等设计：INSERT ... WHERE NOT EXISTS
-- 执行顺序：本脚本在 57_column_init.sql 之后执行
-- =====================================================

-- 取"内容管理"父菜单ID
SELECT @cms_parent_id := menu_id FROM sys_menu WHERE menu_name = '内容管理' AND parent_id = 0 LIMIT 1;
SET @cms_parent_id = IFNULL(@cms_parent_id, 0);

-- =============================================
-- 专栏管理
-- =============================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '专栏管理', @cms_parent_id, 12, 'column', 'cms/column/index', NULL, 1, 0,
       'C', '0', '0', 'portal:column:list', 'documentation', 'admin', NOW(), '专栏后台管理菜单'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:column:list');
SELECT @column_menu_id := menu_id FROM sys_menu WHERE perms = 'portal:column:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '专栏查询', @column_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:column:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:column:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '专栏新增', @column_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:column:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:column:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '专栏修改', @column_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:column:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:column:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '专栏删除', @column_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:column:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:column:remove');

-- =============================================
-- 为管理员角色分配专栏管理菜单权限
-- =============================================
SET @admin_role_id = 1;

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @admin_role_id, `menu_id` FROM `sys_menu`
WHERE `perms` IN (
    'portal:column:list', 'portal:column:query', 'portal:column:add',
    'portal:column:edit', 'portal:column:remove'
)
AND `menu_id` NOT IN (SELECT `menu_id` FROM `sys_role_menu` WHERE `role_id` = @admin_role_id);

-- =============================================
-- 校验结果
-- =============================================
SELECT '专栏后台管理菜单初始化脚本执行完成！' AS message;
