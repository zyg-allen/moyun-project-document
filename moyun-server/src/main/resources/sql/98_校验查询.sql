-- 来源：all-db-ddl.sql 行5004-5039
-- 用途：CMS 菜单完整性校验 SELECT 查询

-- =============================================================================
-- 十八、校验查询
-- =============================================================================
-- 1. 内容管理目录及子菜单（按 order_num 排序）
SELECT '===== 内容管理目录树 =====' AS info;
SELECT m1.menu_id, m1.menu_name, m1.order_num, m1.path, m1.component, m1.perms, m1.menu_type, m1.visible,
       m2.menu_name AS child_name, m2.perms AS child_perms, m2.menu_type AS child_type
FROM sys_menu m1
         LEFT JOIN sys_menu m2 ON m2.parent_id = m1.menu_id
WHERE m1.menu_name = '内容管理' AND m1.parent_id = 0
ORDER BY m1.order_num, m2.order_num;

-- 2. 创作者认证目录
SELECT '===== 创作者认证 =====' AS info;
SELECT menu_id, menu_name, parent_id, order_num, path, component, perms, menu_type
FROM sys_menu
WHERE menu_name = '创作者认证' OR perms LIKE 'cms:certification:%'
ORDER BY menu_id;

-- 3. 财务目录（已下线）
SELECT '===== 财务（已下线） =====' AS info;
SELECT menu_id, menu_name, parent_id, order_num, path, component, perms, menu_type, visible
FROM sys_menu
WHERE menu_name = '财务' OR perms LIKE 'portal:order:%'
ORDER BY menu_id;

-- 4. 统计总数
SELECT CONCAT('CMS 菜单总数: ', COUNT(*)) AS summary
FROM sys_menu
WHERE perms LIKE 'cms:%'
   OR perms LIKE 'portal:column:%'
   OR perms LIKE 'portal:tip:%'
   OR perms LIKE 'portal:order:%'
   OR (menu_name IN ('内容管理','创作者认证','财务') AND parent_id = 0);

SELECT 'CMS 菜单完整初始化脚本执行完成！' AS message;
