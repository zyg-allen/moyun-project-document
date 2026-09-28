-- =============================================================================
-- 审核中心菜单归并：两条入口 → 一条（修正 20260928-01 的误删与误判）
-- -----------------------------------------------------------------------------
-- 背景：sys_menu 里历史上存在**两条**指向同一个页面 cms/audit-center/index 的菜单，
--       路径不一致，构成"一条路由两个入口"：
--
--         menu_id  parent           parent.path  menu.path     实际路由                      perms
--         -------  ---------------  -----------  -------------  ---------------------------  -----------------------
--          5152    内容管理(5068)   cms          audit-center   /portal/cms/audit-center ❌   cms:audit-center:list
--          5242    门户管理(5241)   portal       audit-center   /portal/audit-center     ✅   system:auditTask:list
--
--       ✅ `/portal/audit-center` 是全项目认可的路径，证据：
--          · 后端 `AuditTaskType` 8 个审核类型的 defaultRoutePath 全部为 `/portal/audit-center`；
--          · 后端 `SysDashboardServiceImpl` 兜底与兼容替换逻辑均产出 `/portal/audit-center`；
--          · 前端 4 处跳转（首页待办/已办、文章列表、话题、专栏）均 push `/portal/audit-center`。
--       而 5152 生成的 `/portal/cms/audit-center` **没有任何代码引用** → 是个错误的重复入口。
--
-- 处置（按"审核中心不拆分"的要求）：**只保留 5242**，删除 5152。
--
-- 幂等性：DELETE + INSERT ... ON DUPLICATE KEY UPDATE，可重复执行。
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. 删除错误的重复入口 5152（生成 /portal/cms/audit-center，无任何代码引用）
-- ---------------------------------------------------------------------------
DELETE rm FROM sys_role_menu rm WHERE rm.menu_id = 5152;
DELETE FROM sys_menu WHERE menu_id = 5152;

-- ---------------------------------------------------------------------------
-- 2. 保留唯一入口 5242，路径与权限归位
-- ---------------------------------------------------------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (5242, '审核中心', 5241, 7, 'audit-center', 'cms/audit-center/index', 1, 0, 'C', '0', '0', 'system:auditTask:list', 'eye-open', 'admin', NOW(), '唯一审核中心入口；实际路由 /portal/audit-center')
ON DUPLICATE KEY UPDATE menu_name='审核中心', parent_id=5241, order_num=7, path='audit-center',
  component='cms/audit-center/index', menu_type='C', visible='0', status='0',
  perms='system:auditTask:list', icon='eye-open',
  remark='唯一审核中心入口；实际路由 /portal/audit-center';

-- ---------------------------------------------------------------------------
-- 3. 角色绑定自愈
-- ---------------------------------------------------------------------------
DELETE rm FROM sys_role_menu rm LEFT JOIN sys_menu m ON m.menu_id = rm.menu_id WHERE m.menu_id IS NULL;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id, create_by, create_time, remark)
SELECT 1, m.menu_id, 'admin', NOW(), 'v13.26 审核中心归并默认授权'
  FROM sys_menu m WHERE m.menu_id = 5242;

-- ---------------------------------------------------------------------------
-- 4. 复核 SQL（应恰好返回 1 行）
-- ---------------------------------------------------------------------------
-- SELECT m.menu_id, p.menu_name AS parent, p.path AS parent_path, m.path,
--        CONCAT('/', p.path, '/', m.path) AS actual_route, m.perms
--   FROM sys_menu m JOIN sys_menu p ON p.menu_id = m.parent_id
--  WHERE m.component = 'cms/audit-center/index';
