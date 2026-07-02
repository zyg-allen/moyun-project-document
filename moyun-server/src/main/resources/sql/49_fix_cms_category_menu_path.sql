-- =============================================
-- 修复"分类管理"菜单 path
-- 现象：访问 /cms/cms/category 404（或 /cms/category 404）
-- 原因：数据库 path 存为 'cms/category' 或 'category'，
--       前端 filterAsyncRouter 拼接父 path '/cms' 时路由匹配失败
-- 修复：path = '/category'（绝对路径，前端不再拼接父 path）
--       经实测验证：path 带 / 前缀可正常访问
-- 幂等：仅当 path 不等于 '/category' 时才修改，已正确不动
-- 作者：moyun  日期：2026-07-01
-- =============================================

UPDATE `sys_menu`
SET `path` = '/category',
    `update_by` = 'admin',
    `update_time` = NOW()
WHERE `menu_name` = '分类管理'
  AND `path` != '/category';

-- 验证（执行后可手动运行核对，应看到 path = '/category'）
-- SELECT menu_id, menu_name, parent_id, path, component, perms
-- FROM sys_menu
-- WHERE menu_name = '分类管理';
