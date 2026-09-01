-- =============================================================================
-- 文章+专栏管理菜单合并补丁（增量，对应 docs 待补）
-- 背景：
--   1. 需求：将「文章管理」(/portal/cms/article) 与「专栏管理」(/portal/cms/column)
--      合并为同一菜单下的 Tab 页（文章为主 Tab，专栏为次 Tab）。
--   2. 方案：复用文章管理路径，保留文章菜单 (5118) 入口不变；将原专栏管理菜单 (5122)
--      的 visible 改为 '1'（侧边栏隐藏，但路由仍保留以供 Tab 内部组件复用）。
--   3. 角色权限：原专栏管理菜单 (5122) 的权限点 portal:column:list / portal:column:query
--      / portal:column:add / portal:column:edit / portal:column:remove 已通过 role_menu
--      分配给 admin 角色；菜单隐藏不影响权限点生效，因此管理员在文章管理页的「专栏管理」
--      Tab 内仍可使用所有专栏操作按钮（v-hasPermi 校验依赖权限点而非菜单可见性）。
-- 幂等：可重复执行
-- =============================================================================

-- 1) 隐藏原专栏管理菜单（visible='1' 隐藏，但路由仍注册以供 Tab 内组件引用）
UPDATE `sys_menu`
SET `visible` = '1',
    `update_by` = 'admin',
    `update_time` = NOW(),
    `remark` = CONCAT(IFNULL(`remark`, ''), ' [v8.1 隐藏：合并到文章管理 Tab，路由保留供 Tab 组件复用]')
WHERE `menu_id` = 5122 AND `visible` = '0';

-- 2) 兜底：确保 admin 角色拥有专栏菜单 5122 的权限（避免某些环境 role_menu 漏配）
--    若已存在则跳过（INSERT IGNORE）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
  (1, 5122);
