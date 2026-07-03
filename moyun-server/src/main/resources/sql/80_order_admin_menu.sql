-- =====================================================
-- 付费阅读订单后台管理菜单初始化脚本（2.3 付费阅读订单）
-- 挂载在"财务"一级菜单下（如不存在则创建财务一级菜单）
-- 幂等设计：INSERT ... WHERE NOT EXISTS
-- 执行顺序：本脚本在 59_paid_read_init.sql 之后执行
-- =====================================================

-- =============================================
-- 一、创建"财务"一级菜单（如不存在）
-- =============================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '财务', 0, 20, 'finance', NULL, NULL, 1, 0,
       'M', '0', '0', NULL, 'money', 'admin', NOW(), '财务目录'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name = '财务' AND parent_id = 0);
SELECT @finance_menu_id := menu_id FROM sys_menu WHERE menu_name = '财务' AND parent_id = 0 LIMIT 1;
SET @finance_menu_id = IFNULL(@finance_menu_id, 0);

-- =============================================
-- 二、付费阅读订单管理（只读查询）
-- =============================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '付费订单', @finance_menu_id, 1, 'order', 'cms/order/index', NULL, 1, 0,
       'C', '0', '0', 'portal:order:list', 'shopping', 'admin', NOW(), '付费阅读订单后台查询菜单'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:order:list');
SELECT @order_menu_id := menu_id FROM sys_menu WHERE perms = 'portal:order:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '订单查询', @order_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:order:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:order:query');

-- =============================================
-- 三、为管理员角色分配订单管理菜单权限（含财务目录菜单）
-- =============================================
SET @admin_role_id = 1;

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @admin_role_id, `menu_id` FROM `sys_menu`
WHERE (`perms` IN ('portal:order:list', 'portal:order:query')
       OR (menu_name = '财务' AND parent_id = 0))
AND `menu_id` NOT IN (SELECT `menu_id` FROM `sys_role_menu` WHERE `role_id` = @admin_role_id);

-- =============================================
-- 校验结果
-- =============================================
SELECT '付费阅读订单后台管理菜单初始化脚本执行完成！' AS message;
