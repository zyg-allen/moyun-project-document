-- =============================================
-- 读书模块 v1.0 第二阶段 - 书架管理菜单初始化脚本
-- 版本：1.0
-- 日期：2026-06-30
-- 说明：为读书模块新增"书架管理"二级菜单（只读，便于后台查看用户书架）
-- =============================================

-- 1. 查询"读书空间"一级菜单 ID（由 17_portal_book_menu_init.sql 创建）
SELECT @reading_menu_id := `menu_id` FROM `sys_menu`
WHERE `menu_name` = '读书空间' AND `parent_id` = 0 LIMIT 1;

-- 2. 插入二级菜单：书架管理
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '书架管理', @reading_menu_id, 5, 'bookshelf', 'portal/bookshelf/index', NULL, 1, 0, 'C', '0', '0', 'portal:bookshelf:list', 'shopping', 'admin', NOW(), '', NULL, '用户书架（收藏书籍）管理菜单'
);

SET @bookshelf_menu_id = LAST_INSERT_ID();

-- 3. 插入书架管理的按钮权限
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES
('书架查询', @bookshelf_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookshelf:query', '#', 'admin', NOW(), '', NULL, ''),
('书架移除', @bookshelf_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookshelf:remove', '#', 'admin', NOW(), '', NULL, '');

-- 4. 为管理员角色分配书架管理权限
SET @admin_role_id = 1;

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @admin_role_id, `menu_id` FROM `sys_menu`
WHERE `menu_id` = @bookshelf_menu_id OR `parent_id` = @bookshelf_menu_id;

-- =============================================
-- 脚本执行完成
-- =============================================

SELECT '书架管理菜单初始化完成！' AS message;
