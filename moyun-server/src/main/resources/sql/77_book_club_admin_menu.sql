-- =====================================================
-- 共读活动后台管理菜单初始化脚本（1.8 共读活动）
-- 挂载在"读书空间"一级菜单下
-- 幂等设计：INSERT ... WHERE NOT EXISTS
-- 执行顺序：本脚本在 17_portal_book_menu_init.sql 之后执行
-- =====================================================

-- 取"读书空间"父菜单ID（由 17_portal_book_menu_init.sql 创建）
SELECT @reading_menu_id := menu_id FROM sys_menu WHERE menu_name = '读书空间' AND parent_id = 0 LIMIT 1;
SET @reading_menu_id = IFNULL(@reading_menu_id, 0);

-- =============================================
-- 共读活动管理
-- =============================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '共读活动', @reading_menu_id, 8, 'bookClub', 'portal/bookClub/index', NULL, 1, 0,
       'C', '0', '0', 'portal:bookClub:list', 'people', 'admin', NOW(), '共读活动后台管理菜单'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookClub:list');
SELECT @book_club_menu_id := menu_id FROM sys_menu WHERE perms = 'portal:bookClub:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '活动查询', @book_club_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookClub:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookClub:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '活动新增', @book_club_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookClub:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookClub:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '活动修改', @book_club_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookClub:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookClub:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '活动删除', @book_club_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookClub:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookClub:remove');

-- =============================================
-- 为管理员角色分配共读活动管理菜单权限
-- =============================================
SET @admin_role_id = 1;

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @admin_role_id, `menu_id` FROM `sys_menu`
WHERE `perms` IN (
    'portal:bookClub:list', 'portal:bookClub:query', 'portal:bookClub:add',
    'portal:bookClub:edit', 'portal:bookClub:remove'
)
AND `menu_id` NOT IN (SELECT `menu_id` FROM `sys_role_menu` WHERE `role_id` = @admin_role_id);

-- =============================================
-- 校验结果
-- =============================================
SELECT '共读活动后台管理菜单初始化脚本执行完成！' AS message;
