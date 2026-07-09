-- =====================================================
-- 修复 sys_menu 子菜单 path 前缀不一致问题
-- RuoYi 框架要求：子菜单(menu_type='C') path 必须是相对路径(不带/)
-- vue-router 4 会自动拼接父级路径
-- 之前部分菜单 path 带了 / 前缀(如 /user、/job)，导致路由匹配异常
-- =====================================================

-- 1. 修复子菜单(menu_type='C') path：去掉前导 /
UPDATE sys_menu
SET path = TRIM(LEADING '/' FROM path)
WHERE menu_type = 'C'
  AND path LIKE '/%';

-- 2. 修复目录(menu_type='M')顶层菜单 path：确保不带 / 前缀
--    顶层菜单的 / 前缀由前端 normalizeTopLevelPath 统一补全
UPDATE sys_menu
SET path = TRIM(LEADING '/' FROM path)
WHERE menu_type = 'M'
  AND path LIKE '/%';

-- 验证：执行后确认无子菜单 path 带 / 前缀
-- SELECT path, component, menu_type FROM sys_menu WHERE menu_type = 'C' AND path LIKE '/%';
-- SELECT path, menu_type FROM sys_menu WHERE menu_type = 'M' AND path LIKE '/%';
