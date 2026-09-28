-- =============================================================================
-- 系统监控菜单归并：服务监控 ×2、缓存 ×3、数据监控重复 → 各留一个分组入口
-- -----------------------------------------------------------------------------
-- 背景：项目对"多个子页面想看成一页"采用了 **TabContainer 分组页** 范式
--       （同类既有：cms/feedback-center、cms/help-center）。监控模块同时保留了
--       分组页与各子页面菜单，形成"同一功能两个入口"：
--
--         分组页（保留）                          被其包裹的子页面（冗余菜单）
--         --------------------------------------  ------------------------------------
--         5153 服务监控 monitor/server-panel       112 服务监控 monitor/server        ← 其 tab1
--              （tab: server + druid）             111 数据监控 monitor/druid         ← 其 tab2
--         5149 缓存管理 monitor/cache-manage       113 缓存监控 monitor/cache         ← 其 tab1
--              （tab: cache + list）               114 缓存列表 monitor/cache/list    ← 其 tab2
--
--       处置：**保留分组页，删除冗余子菜单行**（子页面 .vue 文件保留不动 —— 组件仍被
--             分组页引用，删文件会白屏）。删除的菜单行均**无子权限行**、且只绑定超管。
--
-- 重要：这些子菜单行删除后，其"生成路由"（/monitor/server、/monitor/druid、
--       /monitor/cache、/monitor/cacheList）随之消失。已核查全仓：
--         · `/monitor/cache*` 与 `/monitor/server` 的文本命中**全部是后端 API 前缀**
--           （api/monitor/cache.js、api/monitor/server.js 的 url 字段），不是前端路由；
--         · 唯一的真实路由引用是后台首页「系统运行概览」的 `goPath('/monitor/server')`，
--           本就指向已不存在的路由（分组页路由是 /monitor/server-panel），属既有缺陷，
--           已在本次一并修正为 /monitor/server-panel。
--
-- 幂等性：UPDATE + DELETE，可重复执行。
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. 修正后台首页指向的监控入口（原 /monitor/server 已无对应路由）
--    说明：这是后端下发的 routePath，改在 Java 侧（SysDashboardServiceImpl）；
--          此处仅保留说明，避免把路由修正误当作 SQL 职责。
-- ---------------------------------------------------------------------------
-- 见：SysDashboardServiceImpl#buildAdminPlatform → newModule("system_health", ..., "/monitor/server-panel")

-- ---------------------------------------------------------------------------
-- 2. 删除冗余子菜单行（分组页已覆盖其功能）
-- ---------------------------------------------------------------------------
DELETE rm FROM sys_role_menu rm WHERE rm.menu_id IN (111, 112, 113, 114);
DELETE FROM sys_menu WHERE menu_id IN (111, 112, 113, 114);

-- ---------------------------------------------------------------------------
-- 3. 分组页归位（名称/排序/图标统一，避免与已删项混淆）
-- ---------------------------------------------------------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (5153, '服务监控', 2, 3, 'server-panel', 'monitor/server-panel/index', 1, 0, 'C', '0', '0', 'monitor:server-panel:list', 'monitor', 'admin', NOW(), '服务器监控+数据监控 分组页（TabContainer）')
ON DUPLICATE KEY UPDATE menu_name='服务监控', parent_id=2, order_num=3, path='server-panel',
  component='monitor/server-panel/index', menu_type='C', visible='0', status='0',
  perms='monitor:server-panel:list', icon='monitor', remark='服务器监控+数据监控 分组页（TabContainer）';

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (5149, '缓存管理', 2, 4, 'cache-manage', 'monitor/cache-manage/index', 1, 0, 'C', '0', '0', 'monitor:cache-manage:list', 'redis', 'admin', NOW(), '缓存监控+缓存列表 分组页（TabContainer）')
ON DUPLICATE KEY UPDATE menu_name='缓存管理', parent_id=2, order_num=4, path='cache-manage',
  component='monitor/cache-manage/index', menu_type='C', visible='0', status='0',
  perms='monitor:cache-manage:list', icon='redis', remark='缓存监控+缓存列表 分组页（TabContainer）';

-- ---------------------------------------------------------------------------
-- 4. 角色绑定自愈
-- ---------------------------------------------------------------------------
DELETE rm FROM sys_role_menu rm LEFT JOIN sys_menu m ON m.menu_id = rm.menu_id WHERE m.menu_id IS NULL;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id, create_by, create_time, remark)
SELECT 1, m.menu_id, 'admin', NOW(), 'v13.26 监控菜单归并默认授权'
  FROM sys_menu m WHERE m.menu_id IN (5149, 5153);

-- ---------------------------------------------------------------------------
-- 5. 复核 SQL（系统监控下应为：在线用户/定时任务/服务监控/缓存管理）
-- ---------------------------------------------------------------------------
-- SELECT menu_id, menu_name, path, component FROM sys_menu WHERE parent_id = 2 ORDER BY order_num;
