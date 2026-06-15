-- =============================================
-- 读书空间模块 - 菜单和权限初始化脚本
-- 版本：1.0
-- 日期：2026-02-01
-- 说明：为墨韵智库读书空间模块添加后台管理菜单和权限
-- =============================================

-- 1. 首先插入一级菜单：读书空间
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '读书空间', 0, 11, 'portal', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'reading', 'admin', NOW(), '', NULL, '读书空间目录'
);

SET @portal_menu_id = LAST_INSERT_ID();

-- 2. 插入二级菜单：书籍管理
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '书籍管理', @portal_menu_id, 1, 'book', 'portal/book/index', NULL, 1, 0, 'C', '0', '0', 'portal:book:list', 'book', 'admin', NOW(), '', NULL, '书籍管理菜单'
);

SET @book_menu_id = LAST_INSERT_ID();

-- 3. 插入书籍管理的按钮权限
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES
('书籍查询', @book_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:book:query', '#', 'admin', NOW(), '', NULL, ''),
('书籍新增', @book_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:book:add', '#', 'admin', NOW(), '', NULL, ''),
('书籍修改', @book_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:book:edit', '#', 'admin', NOW(), '', NULL, ''),
('书籍删除', @book_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:book:remove', '#', 'admin', NOW(), '', NULL, ''),
('书籍推荐', @book_menu_id, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:book:featured', '#', 'admin', NOW(), '', NULL, '');

-- 4. 插入二级菜单：书单管理
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '书单管理', @portal_menu_id, 2, 'bookList', 'portal/bookList/index', NULL, 1, 0, 'C', '0', '0', 'portal:bookList:list', 'list', 'admin', NOW(), '', NULL, '书单管理菜单'
);

SET @booklist_menu_id = LAST_INSERT_ID();

-- 5. 插入书单管理的按钮权限
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES
('书单查询', @booklist_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookList:query', '#', 'admin', NOW(), '', NULL, ''),
('书单新增', @booklist_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookList:add', '#', 'admin', NOW(), '', NULL, ''),
('书单修改', @booklist_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookList:edit', '#', 'admin', NOW(), '', NULL, ''),
('书单删除', @booklist_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookList:remove', '#', 'admin', NOW(), '', NULL, ''),
('书单推荐', @booklist_menu_id, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookList:featured', '#', 'admin', NOW(), '', NULL, '');

-- 6. 插入二级菜单：金句管理
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '金句管理', @portal_menu_id, 3, 'bookQuote', 'portal/bookQuote/index', NULL, 1, 0, 'C', '0', '0', 'portal:bookQuote:list', 'edit', 'admin', NOW(), '', NULL, '金句管理菜单'
);

SET @quote_menu_id = LAST_INSERT_ID();

-- 7. 插入金句管理的按钮权限
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES
('金句查询', @quote_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookQuote:query', '#', 'admin', NOW(), '', NULL, ''),
('金句新增', @quote_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookQuote:add', '#', 'admin', NOW(), '', NULL, ''),
('金句修改', @quote_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookQuote:edit', '#', 'admin', NOW(), '', NULL, ''),
('金句删除', @quote_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookQuote:remove', '#', 'admin', NOW(), '', NULL, ''),
('金句精选', @quote_menu_id, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookQuote:featured', '#', 'admin', NOW(), '', NULL, '');

-- =============================================
-- 为管理员角色分配读书空间菜单权限
-- =============================================

-- 首先获取管理员角色的ID（通常为1）
SET @admin_role_id = 1;

-- 删除旧的读书空间权限（如果存在）
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (
    SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` IN (
        '读书空间', '书籍管理', '书单管理', '金句管理'
    ) OR `parent_id` IN (
        SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` IN (
            '读书空间', '书籍管理', '书单管理', '金句管理'
        )
    )
);

-- 重新为管理员角色分配读书空间权限
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @admin_role_id, `menu_id` FROM `sys_menu`
WHERE `menu_name` IN (
    '读书空间', '书籍管理', '书单管理', '金句管理'
) OR `parent_id` IN (
    SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` IN (
        '读书空间', '书籍管理', '书单管理', '金句管理'
    )
);

-- =============================================
-- 脚本执行完成
-- =============================================

SELECT '读书空间菜单和权限初始化脚本执行完成！' AS message;
SELECT '请在数据库初始化完成后执行此脚本！' AS note;
