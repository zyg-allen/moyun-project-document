-- ====================================================================
-- v7.11 升级脚本：监控与接口菜单整合
-- 适配 MySQL 8.x
-- 说明：本脚本幂等，可重复执行
-- 内容：
--   1. swagger 菜单改名：系统接口 → 接口文档（P3-5）
--   2. 服务监控合并：服务器监控 + 数据监控(Druid) → 「服务监控」Tab 容器（P3-6）
-- ====================================================================

SET @db := DATABASE();

-- ====================================================================
-- 1. swagger 菜单改名：系统接口 → 接口文档
-- 背景：原菜单名「系统接口」语义模糊，统一为「接口文档」更直观。
-- 按 perms 定位（幂等：已改名则跳过）。
-- ====================================================================
UPDATE sys_menu SET menu_name = '接口文档', update_by = 'admin', update_time = NOW()
  WHERE perms = 'tool:swagger:list' AND menu_name <> '接口文档';

-- ====================================================================
-- 2. 服务监控合并（服务器监控 + 数据监控 Druid）
-- 背景：原「系统监控」目录下「数据监控」(111, monitor:druid:list) 与
--      「服务监控」(112, monitor:server:list) 两个二级菜单高度相关，
--      合并为「服务监控」Tab 容器（monitor/server-panel/index），减少菜单数量。
-- 旧菜单设为隐藏（visible=1），保留权限项以兼容历史角色分配，支持回滚。
-- ====================================================================
SELECT @monitor_parent_id := menu_id FROM sys_menu WHERE menu_name = '系统监控' AND parent_id = 0 LIMIT 1;
SET @monitor_parent_id := IFNULL(@monitor_parent_id, 2);

-- 2.1 注册合并菜单（幂等，基于 perms 去重）
SET @sql := IF(
  (SELECT COUNT(*) FROM sys_menu WHERE perms = 'monitor:server-panel:list') = 0,
  CONCAT('INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES (''服务监控'', ', @monitor_parent_id, ', 3, ''server-panel'', ''monitor/server-panel/index'', NULL, 1, 0, ''C'', ''0'', ''0'', ''monitor:server-panel:list'', ''monitor'', ''admin'', NOW(), ''服务器监控与数据监控(Druid)合并查看（Tab）'')'),
  'SELECT ''monitor:server-panel:list 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.2 隐藏旧菜单（数据监控 111、服务监控 112）- visible=1 隐藏，status=0 正常，保留权限
UPDATE sys_menu SET visible = '1', update_by = 'admin', update_time = NOW()
  WHERE perms IN ('monitor:druid:list', 'monitor:server:list') AND visible = '0';

-- ====================================================================
-- 角色权限分配提示：
--   admin 超级管理员通过 role_key='admin' 通配权限自动放行，无需额外分配。
--   非 admin 角色需分配 monitor:server-panel:list 权限；容器内各 Tab 的
--   操作权限（monitor:server:* / monitor:druid:*）仍沿用各页原有权限项。
-- ====================================================================

-- ====================================================================
-- 升级完成
-- ====================================================================
-- 系统监控 (monitor, id=2)
--   ├─ 在线用户 (109)
--   ├─ 定时任务 (110)
--   ├─ 服务监控 (monitor:server-panel:list)  ← 服务器 + Druid（隐藏旧 111/112）
--   └─ 缓存管理 (monitor:cache-manage:list)  ← 缓存监控 + 列表（v7.7 已合并，隐藏旧 113/114）
--
-- 系统工具 (tool)
--   └─ 接口文档 (tool:swagger:list)          ← 原「系统接口」改名
-- ====================================================================
