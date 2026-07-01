-- =============================================
-- 读书模块 v1.0 - 章节管理菜单初始化脚本
-- 版本：1.0
-- 日期：2026-06-30
-- 说明：为读书模块新增"章节管理"二级菜单及按钮权限
-- =============================================

-- 1. 查询"读书空间"一级菜单 ID（由 17_portal_book_menu_init.sql 创建）
SELECT @reading_menu_id := `menu_id` FROM `sys_menu`
WHERE `menu_name` = '读书空间' AND `parent_id` = 0 LIMIT 1;

-- 2. 插入二级菜单：章节管理
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '章节管理', @reading_menu_id, 4, 'bookChapter', 'portal/bookChapter/index', NULL, 1, 0, 'C', '0', '0', 'portal:bookChapter:list', 'edit', 'admin', NOW(), '', NULL, '书籍章节管理菜单'
);

SET @chapter_menu_id = LAST_INSERT_ID();

-- 3. 插入章节管理的按钮权限
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES
('章节查询', @chapter_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookChapter:query', '#', 'admin', NOW(), '', NULL, ''),
('章节新增', @chapter_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookChapter:add', '#', 'admin', NOW(), '', NULL, ''),
('章节修改', @chapter_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookChapter:edit', '#', 'admin', NOW(), '', NULL, ''),
('章节删除', @chapter_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookChapter:remove', '#', 'admin', NOW(), '', NULL, ''),
('章节发布', @chapter_menu_id, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookChapter:publish', '#', 'admin', NOW(), '', NULL, '');

-- 4. 为管理员角色分配章节管理权限
SET @admin_role_id = 1;

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @admin_role_id, `menu_id` FROM `sys_menu`
WHERE `menu_id` = @chapter_menu_id OR `parent_id` = @chapter_menu_id;

-- =============================================
-- 脚本执行完成
-- =============================================

SELECT '章节管理菜单初始化完成！' AS message;
