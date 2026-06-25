-- ===================================================================
-- 36_init_notification_center_menu.sql
-- 后台管理员通知中心菜单初始化
--
-- 新增菜单：系统管理 > 通知中心
-- 路由：/system/notification
-- 组件：system/notification/index
-- 权限：system:notification:list（所有管理员可见，无需额外授权）
-- ===================================================================

-- 1. 查询父菜单"系统管理"的 ID（RuoYi 默认系统管理菜单 ID 通常为 1）
-- 使用变量兼容不同环境
SELECT @parentId := menu_id FROM sys_menu WHERE menu_name = '系统管理' AND parent_id = 0 LIMIT 1;
SET @parentId = IFNULL(@parentId, 1);

-- 2. 插入通知中心菜单（如果不存在）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '通知中心', @parentId, 15, 'notification', 'system/notification/index', 1, 0,
       'C', '0', '0', 'system:notification:list', 'message', 'admin', NOW(), '后台管理员通知中心（个人+广播）'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE menu_name = '通知中心' AND parent_id = @parentId
);

-- 3. 查询刚插入的菜单 ID
SELECT @menuId := menu_id FROM sys_menu WHERE menu_name = '通知中心' AND parent_id = @parentId LIMIT 1;

-- 4. 插入按钮权限（查询、标记已读）—— 通知中心为只读+已读操作，无需增删改按钮权限
-- 查询权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '通知查询', @menuId, 1, '#', '', 1, 0,
       'F', '0', '0', 'system:notification:query', '#', 'admin', NOW(), ''
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE menu_name = '通知查询' AND parent_id = @menuId
);

-- 5. 为超级管理员角色（role_id=1）分配菜单权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu
WHERE perms LIKE 'system:notification:%'
  AND menu_id NOT IN (SELECT menu_id FROM sys_role_menu WHERE role_id = 1);

-- 6. 校验结果
SELECT '通知中心菜单及权限:' AS info;
SELECT menu_id, menu_name, parent_id, path, component, perms, menu_type
FROM sys_menu
WHERE perms LIKE 'system:notification:%' OR (menu_name = '通知中心' AND parent_id = @parentId)
ORDER BY menu_id;
