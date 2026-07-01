-- =============================================
-- 读书模块 v1.0 第三阶段 - 推荐位管理菜单初始化脚本
-- 版本：1.1（幂等化，可重复执行）
-- 日期：2026-07-01
-- 说明：为读书模块新增"推荐位管理"二级菜单（运营管理首页/发现页推荐位）
-- 执行前提：17_portal_book_menu_init.sql 已执行（存在"读书空间"一级菜单）
-- =============================================

-- 1. 查询"读书空间"一级菜单 ID（由 17_portal_book_menu_init.sql 创建）
SELECT @reading_menu_id := `menu_id` FROM `sys_menu`
WHERE `menu_name` = '读书空间' AND `parent_id` = 0 LIMIT 1;

-- 2. 幂等清理：先删除旧的推荐位菜单权限关联 + 菜单记录（保证可重复执行）
--    2.1 删除 sys_role_menu 中推荐位相关的权限关联（含二级菜单和按钮权限）
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (
    SELECT `menu_id` FROM `sys_menu`
    WHERE `menu_name` = '推荐位管理' AND `parent_id` = @reading_menu_id
       OR `parent_id` IN (
           SELECT `menu_id` FROM `sys_menu`
           WHERE `menu_name` = '推荐位管理' AND `parent_id` = @reading_menu_id
       )
);

--    2.2 删除旧的推荐位菜单记录（二级菜单 + 按钮权限）
DELETE FROM `sys_menu`
WHERE `menu_name` = '推荐位管理' AND `parent_id` = @reading_menu_id
   OR `parent_id` IN (
       SELECT `menu_id` FROM (
           SELECT `menu_id` FROM `sys_menu`
           WHERE `menu_name` = '推荐位管理' AND `parent_id` = @reading_menu_id
       ) AS tmp
   );

-- 3. 插入二级菜单：推荐位管理
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '推荐位管理', @reading_menu_id, 6, 'bookRecommend', 'portal/bookRecommend/index', NULL, 1, 0, 'C', '0', '0', 'portal:bookRecommend:list', 'star', 'admin', NOW(), '', NULL, '书籍推荐位管理菜单'
);

SET @recommend_menu_id = LAST_INSERT_ID();

-- 4. 插入推荐位管理的按钮权限
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES
('推荐位查询', @recommend_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookRecommend:query', '#', 'admin', NOW(), '', NULL, ''),
('推荐位新增', @recommend_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookRecommend:add', '#', 'admin', NOW(), '', NULL, ''),
('推荐位修改', @recommend_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookRecommend:edit', '#', 'admin', NOW(), '', NULL, ''),
('推荐位删除', @recommend_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookRecommend:remove', '#', 'admin', NOW(), '', NULL, '');

-- 5. 为管理员角色分配推荐位管理权限（role_id=1 通常为超级管理员）
SET @admin_role_id = 1;

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @admin_role_id, `menu_id` FROM `sys_menu`
WHERE `menu_id` = @recommend_menu_id OR `parent_id` = @recommend_menu_id;

-- =============================================
-- 脚本执行完成
-- =============================================

SELECT '推荐位管理菜单初始化完成（幂等，可重复执行）！' AS message;
SELECT CONCAT('二级菜单ID=', @recommend_menu_id, '，已分配管理员角色ID=', @admin_role_id) AS detail;
