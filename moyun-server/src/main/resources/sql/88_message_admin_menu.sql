-- =====================================================
-- 88_message_admin_menu.sql
-- 后台私信中心菜单初始化
--
-- 内容：
--   1. 系统管理菜单下新增「私信中心」菜单项（component 指向 system/message/index）
--   2. 挂载 system:message:list / query / send 三个按钮权限
--   3. 为超级管理员（role_id=1）分配权限
--
-- 说明：
--   - 私信数据由 portal_message_session / portal_message 表承载（见 56/87 SQL）
--   - 后台走 /system/message 接口（SysMessageController），user_type='sys'
--   - 头部铃铛组件直接调 /system/message/unread-count，不走菜单权限
--   - 本脚本可重复执行（NOT EXISTS 守护）
-- =====================================================

SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS=0;

-- 取"系统管理"父菜单ID
SELECT @sysParentId := menu_id FROM sys_menu WHERE menu_name = '系统管理' AND parent_id = 0 LIMIT 1;
SET @sysParentId = IFNULL(@sysParentId, 1);

-- 1. 私信中心菜单（C=菜单，进入会话列表页）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '私信中心', @sysParentId, 12, 'message', 'system/message/index', NULL, 1, 0,
       'C', '0', '0', 'system:message:list', 'message', 'admin', NOW(), '管理员接收与回复门户用户私信'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:message:list');
SELECT @messageMenuId := menu_id FROM sys_menu WHERE perms = 'system:message:list' LIMIT 1;

-- 2. 按钮权限（F）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '会话查询', @messageMenuId, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:message:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:message:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '回复私信', @messageMenuId, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:message:send', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:message:send');

-- 3. 为超级管理员（role_id=1）分配权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu
WHERE perms IN ('system:message:list', 'system:message:query', 'system:message:send')
  AND menu_id NOT IN (SELECT menu_id FROM sys_role_menu WHERE role_id = 1);

SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;

-- 4. 校验
SELECT '私信中心菜单:' AS info;
SELECT menu_id, menu_name, parent_id, path, component, perms, menu_type
FROM sys_menu
WHERE perms LIKE 'system:message:%'
ORDER BY menu_id;
