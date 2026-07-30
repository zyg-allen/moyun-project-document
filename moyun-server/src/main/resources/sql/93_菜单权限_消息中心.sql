-- =============================================================================
-- 消息中心 + 通知管理 菜单与权限初始化
-- =============================================================================
-- 用途：
--   1. 补齐 /system/message 动态路由所需菜单（修复 MessageBell 点击无反应）
--   2. 通知管理从 CMS 移出，归属"系统管理"目录，权限码 cms:notification:* → system:notification:*
--      （通知是全局系统级能力，不应归属 CMS 业务模块）
--   3. 通知收件箱权限（控制后台消息中心"我的通知"Tab 显示）
-- 归属：全部挂在"系统管理"目录（menu_name='系统管理' AND parent_id=0，RuoYi 默认 menu_id=1）
-- 关联：role_id=1（admin 超管）通过代码旁路已拥有 *:*:*，此处关联仅为后台菜单分配界面观感一致
-- 执行顺序：91 之后执行；自带 role_id=1 关联，NOT EXISTS 幂等，可重复执行
-- =============================================================================

-- 公共：系统管理目录 ID
SELECT @sys_parent_id := menu_id FROM sys_menu WHERE menu_name = '系统管理' AND parent_id = 0 LIMIT 1;

-- =============================================================================
-- 一、消息中心菜单（C 类型，私信+通知双 Tab，路由 /system/message）
--    与 MessageBell 的 router.push('/system/message') 一致
-- =============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '消息中心', @sys_parent_id, 20, 'message', 'system/message/index', NULL, 1, 0,
       'C', '0', '0', 'system:message:list', 'message', 'admin', NOW(), '消息中心菜单（私信+通知双Tab）'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:message:list' AND menu_type = 'C');
SELECT @msg_center_menu_id := menu_id FROM sys_menu WHERE perms = 'system:message:list' AND menu_type = 'C' LIMIT 1;

-- 消息中心-私信功能权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '私信查询', @msg_center_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:message:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:message:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '私信发送', @msg_center_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:message:send', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:message:send');

-- 消息中心-通知收件箱权限（控制"我的通知"Tab 显示）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '通知查询', @msg_center_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:notification:list', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:notification:list');

-- =============================================================================
-- 二、通知管理菜单（C 类型，台账：管理全部通知记录，路由 /system/notification）
--    原 cms/notification 已迁移至此，归属系统管理，权限码 system:notification:*
--    前端页面：system/notification/index（原 cms/notification/index）
-- =============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '通知管理', @sys_parent_id, 21, 'notification', 'system/notification/index', NULL, 1, 0,
       'C', '0', '0', 'system:notification:list', 'email', 'admin', NOW(), '通知管理菜单（台账）'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:notification:list' AND menu_type = 'C' AND component = 'system/notification/index');
SELECT @notification_admin_menu_id := menu_id FROM sys_menu WHERE perms = 'system:notification:list' AND menu_type = 'C' AND component = 'system/notification/index' LIMIT 1;

-- 通知管理-台账功能权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '通知查询', @notification_admin_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:notification:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:notification:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '通知新增', @notification_admin_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:notification:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:notification:add');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '通知修改', @notification_admin_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:notification:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:notification:edit');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '通知删除', @notification_admin_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:notification:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:notification:remove');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '发送广播通知', @notification_admin_menu_id, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:notification:sendAll', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:notification:sendAll');

-- =============================================================================
-- 三、role_id=1（admin 超管）关联消息中心+通知管理全部菜单（精确匹配 perms，NOT EXISTS 防重复）
--    说明：admin 走代码旁路（*:*:*）实际不依赖此关联，仅保证后台"角色菜单分配"界面观感一致
-- =============================================================================
INSERT INTO sys_role_menu (role_id, menu_id, create_by, create_time)
SELECT 1, menu_id, 'admin', NOW()
FROM sys_menu
WHERE perms IN (
    'system:message:list', 'system:message:query', 'system:message:send',
    'system:notification:list', 'system:notification:query', 'system:notification:add',
    'system:notification:edit', 'system:notification:remove', 'system:notification:sendAll'
  )
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu rm
      WHERE rm.role_id = 1 AND rm.menu_id = sys_menu.menu_id
  );

-- =============================================================================
-- 四、role_id=2（普通角色）关联消息中心+通知管理菜单（只读：list + query，不含 send/add/edit/remove）
-- =============================================================================
INSERT INTO sys_role_menu (role_id, menu_id, create_by, create_time)
SELECT 2, menu_id, 'admin', NOW()
FROM sys_menu
WHERE perms IN (
    'system:message:list', 'system:message:query',
    'system:notification:list', 'system:notification:query'
  )
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu rm
      WHERE rm.role_id = 2 AND rm.menu_id = sys_menu.menu_id
  );

SELECT '消息中心 + 通知管理菜单初始化完成！' AS message;
