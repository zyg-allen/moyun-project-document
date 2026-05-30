-- =============================================
-- CMS内容管理系统 - 菜单和权限初始化脚本
-- 版本：1.0
-- 日期：2026-05-27
-- 说明：为墨韵智库CMS模块添加后台管理菜单和权限
-- =============================================

-- 1. 首先插入一级菜单：内容管理
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '内容管理', 0, 10, 'cms', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'documentation', 'admin', NOW(), '', NULL, '内容管理目录'
);

SET @cms_menu_id = LAST_INSERT_ID();

-- 2. 插入二级菜单：门户用户管理
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '门户用户', @cms_menu_id, 1, 'user', 'cms/user/index', NULL, 1, 0, 'C', '0', '0', 'cms:user:list', 'user', 'admin', NOW(), '', NULL, '门户用户管理菜单'
);

SET @user_menu_id = LAST_INSERT_ID();

-- 3. 插入门户用户的按钮权限
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES
('用户查询', @user_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:user:query', '#', 'admin', NOW(), '', NULL, ''),
('用户新增', @user_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:user:add', '#', 'admin', NOW(), '', NULL, ''),
('用户修改', @user_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:user:edit', '#', 'admin', NOW(), '', NULL, ''),
('用户删除', @user_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:user:remove', '#', 'admin', NOW(), '', NULL, ''),
('用户状态', @user_menu_id, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:user:status', '#', 'admin', NOW(), '', NULL, ''),
('重置密码', @user_menu_id, 6, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:user:resetPwd', '#', 'admin', NOW(), '', NULL, '');

-- 4. 插入二级菜单：文章管理
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '文章管理', @cms_menu_id, 2, 'article', 'cms/article/index', NULL, 1, 0, 'C', '0', '0', 'cms:article:list', 'edit', 'admin', NOW(), '', NULL, '文章管理菜单'
);

SET @article_menu_id = LAST_INSERT_ID();

-- 5. 插入文章管理的按钮权限
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES
('文章查询', @article_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:article:query', '#', 'admin', NOW(), '', NULL, ''),
('文章新增', @article_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:article:add', '#', 'admin', NOW(), '', NULL, ''),
('文章修改', @article_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:article:edit', '#', 'admin', NOW(), '', NULL, ''),
('文章删除', @article_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:article:remove', '#', 'admin', NOW(), '', NULL, ''),
('文章审核', @article_menu_id, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:article:audit', '#', 'admin', NOW(), '', NULL, ''),
('文章上架', @article_menu_id, 6, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:article:publish', '#', 'admin', NOW(), '', NULL, ''),
('文章推荐', @article_menu_id, 7, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:article:featured', '#', 'admin', NOW(), '', NULL, '');

-- 6. 插入二级菜单：分类管理
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '分类管理', @cms_menu_id, 3, 'category', 'cms/category/index', NULL, 1, 0, 'C', '0', '0', 'cms:category:list', 'tree', 'admin', NOW(), '', NULL, '分类管理菜单'
);

SET @category_menu_id = LAST_INSERT_ID();

-- 7. 插入分类管理的按钮权限
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES
('分类查询', @category_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:category:query', '#', 'admin', NOW(), '', NULL, ''),
('分类新增', @category_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:category:add', '#', 'admin', NOW(), '', NULL, ''),
('分类修改', @category_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:category:edit', '#', 'admin', NOW(), '', NULL, ''),
('分类删除', @category_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:category:remove', '#', 'admin', NOW(), '', NULL, '');

-- 8. 插入二级菜单：标签管理
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '标签管理', @cms_menu_id, 4, 'tag', 'cms/tag/index', NULL, 1, 0, 'C', '0', '0', 'cms:tag:list', 'tab', 'admin', NOW(), '', NULL, '标签管理菜单'
);

SET @tag_menu_id = LAST_INSERT_ID();

-- 9. 插入标签管理的按钮权限
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES
('标签查询', @tag_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:tag:query', '#', 'admin', NOW(), '', NULL, ''),
('标签新增', @tag_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:tag:add', '#', 'admin', NOW(), '', NULL, ''),
('标签修改', @tag_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:tag:edit', '#', 'admin', NOW(), '', NULL, ''),
('标签删除', @tag_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:tag:remove', '#', 'admin', NOW(), '', NULL, '');

-- 10. 插入二级菜单：评论管理
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '评论管理', @cms_menu_id, 5, 'comment', 'cms/comment/index', NULL, 1, 0, 'C', '0', '0', 'cms:comment:list', 'message', 'admin', NOW(), '', NULL, '评论管理菜单'
);

SET @comment_menu_id = LAST_INSERT_ID();

-- 11. 插入评论管理的按钮权限
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES
('评论查询', @comment_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:comment:query', '#', 'admin', NOW(), '', NULL, ''),
('评论审核', @comment_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:comment:audit', '#', 'admin', NOW(), '', NULL, ''),
('评论删除', @comment_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:comment:remove', '#', 'admin', NOW(), '', NULL, '');

-- 12. 插入二级菜单：通知管理
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '通知管理', @cms_menu_id, 6, 'notification', 'cms/notification/index', NULL, 1, 0, 'C', '0', '0', 'cms:notification:list', 'email', 'admin', NOW(), '', NULL, '通知管理菜单'
);

SET @notification_menu_id = LAST_INSERT_ID();

-- 13. 插入通知管理的按钮权限
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES
('通知查询', @notification_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:notification:query', '#', 'admin', NOW(), '', NULL, ''),
('通知新增', @notification_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:notification:add', '#', 'admin', NOW(), '', NULL, ''),
('通知修改', @notification_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:notification:edit', '#', 'admin', NOW(), '', NULL, ''),
('通知删除', @notification_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:notification:remove', '#', 'admin', NOW(), '', NULL, ''),
('发送系统通知', @notification_menu_id, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:notification:sendAll', '#', 'admin', NOW(), '', NULL, '');

-- 14. 插入二级菜单：友情链接管理
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '友情链接', @cms_menu_id, 7, 'friend-link', 'cms/friend-link/index', NULL, 1, 0, 'C', '0', '0', 'cms:friend-link:list', 'link', 'admin', NOW(), '', NULL, '友情链接管理菜单'
);

SET @friend_link_menu_id = LAST_INSERT_ID();

-- 15. 插入友情链接管理的按钮权限
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES
('友情链接查询', @friend_link_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:friend-link:query', '#', 'admin', NOW(), '', NULL, ''),
('友情链接新增', @friend_link_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:friend-link:add', '#', 'admin', NOW(), '', NULL, ''),
('友情链接修改', @friend_link_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:friend-link:edit', '#', 'admin', NOW(), '', NULL, ''),
('友情链接删除', @friend_link_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:friend-link:remove', '#', 'admin', NOW(), '', NULL, '');

-- =============================================
-- 为管理员角色分配 CMS 菜单权限
-- =============================================

-- 首先获取管理员角色的ID（通常为1）
SET @admin_role_id = 1;

-- 删除旧的 CMS 权限（如果存在）
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (
    SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` IN (
        '内容管理', '门户用户', '文章管理', '分类管理', '标签管理', '评论管理', '通知管理', '友情链接'
    ) OR `parent_id` IN (
        SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` IN (
            '内容管理', '门户用户', '文章管理', '分类管理', '标签管理', '评论管理', '通知管理', '友情链接'
        )
    )
);

-- 重新为管理员角色分配 CMS 权限
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @admin_role_id, `menu_id` FROM `sys_menu`
WHERE `menu_name` IN (
    '内容管理', '门户用户', '文章管理', '分类管理', '标签管理', '评论管理', '通知管理', '友情链接'
) OR `parent_id` IN (
    SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` IN (
        '内容管理', '门户用户', '文章管理', '分类管理', '标签管理', '评论管理', '通知管理', '友情链接'
    )
);

-- =============================================
-- 脚本执行完成
-- =============================================

SELECT 'CMS菜单和权限初始化脚本执行完成！' AS message;
SELECT '请在数据库初始化完成后执行此脚本！' AS note;
