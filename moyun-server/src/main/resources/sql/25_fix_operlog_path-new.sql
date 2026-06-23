-- =============================================
-- V25: 修复操作日志/登录日志菜单 path 异常问题
-- 版本：2.9
-- 日期：2026-06-19
-- 问题现象：点击操作日志菜单，URL 拼接为
--   http://localhost/system/system/log/system/log/operlog （404）
-- 根因分析：
--   前端 filterAsyncRouter 递归拼接路径：lastRouter.path + '/' + route.path
--   正常应拼接为 /system/log/operlog，但数据库中 path 字段被写成了
--   "完整路径"而非"相对路径"，导致每层拼接都重复了父路径：
--     menu_id=108 path 被写成 "system/log"（应为 "log"）
--     menu_id=500 path 被写成 "system/log/operlog"（应为 "operlog"）
--   拼接过程：/system + / + system/log + / + system/log/operlog
--           = /system/system/log/system/log/operlog  ← 404
--   部分环境下 path 还可能带前导 "/"，同样会导致 // 双斜杠问题。
-- 修复方案：强制重置日志管理整条菜单链的 path 为正确的相对路径。
-- =============================================

-- ---------- 1. 诊断：修复前查看当前异常值 ----------
SELECT '====== 修复前 ======' AS info;
SELECT `menu_id`, `menu_name`, `parent_id`, `path`, `component`
FROM `sys_menu`
WHERE `menu_id` IN (1, 108, 500, 501)
ORDER BY `menu_id`;

-- ---------- 2. 修复：强制重置为正确的相对路径 ----------
-- 一级目录：系统管理（path 不带 /，后端 getRouterPath 会自动加前导 /）
UPDATE `sys_menu` SET `path` = 'system'    WHERE `menu_id` = 1;
-- 二级目录：日志管理（相对父级，只填 log）
UPDATE `sys_menu` SET `path` = 'log'       WHERE `menu_id` = 108;
-- 三级菜单：操作日志（相对父级，只填 operlog）
UPDATE `sys_menu` SET `path` = 'operlog'   WHERE `menu_id` = 500;
-- 三级菜单：登录日志（相对父级，只填 logininfor）
UPDATE `sys_menu` SET `path` = 'logininfor' WHERE `menu_id` = 501;

-- ---------- 3. 诊断：修复后验证 ----------
SELECT '====== 修复后 ======' AS info;
SELECT `menu_id`, `menu_name`, `parent_id`, `path`, `component`
FROM `sys_menu`
WHERE `menu_id` IN (1, 108, 500, 501)
ORDER BY `menu_id`;

-- ---------- 4. 排查：是否存在其他 path 异常的菜单 ----------
-- 规则：非外链、非一级目录的菜单，path 不应以 / 开头，
--       也不应包含 / （三级菜单的相对 path 不含斜杠）。
SELECT '====== 其他可疑 path ======' AS info;
SELECT `menu_id`, `menu_name`, `parent_id`, `path`, `menu_type`
FROM `sys_menu`
WHERE `parent_id` <> 0
  AND `menu_type` IN ('M', 'C')
  AND `path` IS NOT NULL
  AND `path` <> ''
  AND ( `path` LIKE '/%' OR `path` LIKE '%/%' )
ORDER BY `menu_id`;

SELECT '操作日志/登录日志 path 修复完成！请重新登录后台刷新路由。' AS message;
