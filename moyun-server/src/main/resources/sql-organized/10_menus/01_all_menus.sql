-- =====================================================================
-- 墨韵智库 - 菜单初始化合并脚本
-- 文件路径：sql-organized/10_menus/01_all_menus.sql
-- 生成说明：本文件由 SQL 整理工程师自动合并整理，所有 INSERT 均使用幂等写法
--           （INSERT IGNORE 或 INSERT ... WHERE NOT EXISTS），可重复执行。
--
-- 合并来源脚本（按编号）：
--   菜单注入类（sys_menu INSERT）：
--     04_cms_menu_init.sql                            CMS 内容管理菜单
--     17_portal_book_menu_init.sql                    读书空间菜单
--     18_interview_menu_init.sql                      面试空间菜单
--     23_growth_admin_menu.sql                        成长体系后台菜单
--     24_featured_note_menu.sql                       精选笔记菜单
--     37_init_dashboard_config.sql（仅菜单部分）       运营首页权限点
--     39_init_flowable_menu.sql（仅菜单部分）          流程管理菜单补充
--     43_portal_book_chapter_menu_init.sql            章节管理菜单
--     45_portal_bookshelf_menu_init.sql               书架管理菜单
--     47_portal_book_recommend_menu_init.sql          推荐位管理菜单
--     63_creator_certification_init.sql（仅菜单部分）  创作者认证菜单
--     64_learn_center_init.sql                        学习者中心菜单
--     67_job_init.sql（仅菜单部分）                    职位管理菜单
--     75_vip_center_init.sql                          VIP 会员中心菜单
--     77_book_club_admin_menu.sql                     共读活动菜单
--     78_column_admin_menu.sql                        专栏管理菜单
--     79_tip_admin_menu.sql                           打赏管理菜单（已下线隐藏）
--     80_order_admin_menu.sql                         付费订单菜单（已下线隐藏）
--     88_message_admin_menu.sql                       私信中心菜单
--
--   菜单修复类（sys_menu UPDATE，文件末尾执行）：
--     25_fix_operlog_path-new.sql                     操作日志路径修复
--     40_fix_bugs_v4.sql（仅菜单部分）                 评论菜单修复 + 权限统一
--     49_fix_cms_category_menu_path.sql               分类管理 path 修复
--     50_disable_tool_build_menu.sql                  表单构建菜单停用
--     82_deprecated_menus_offline.sql                 已下线菜单隐藏
--     83_fix_menu_path_prefix.sql                     子菜单 path 前缀修复
--
-- 执行说明：
--   1. 先执行 INSERT 段（菜单注入），再执行 UPDATE 段（菜单修复）
--   2. 所有 INSERT 已幂等化，可重复执行
--   3. 已包含 sys_role_menu 角色权限分配（role_id=1 超级管理员）
-- =====================================================================

SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS = 0;

SET @admin_role_id = 1;

-- =====================================================================
-- 一、CMS 内容管理菜单（来源：04_cms_menu_init.sql）
-- =====================================================================

-- 1. 一级菜单：内容管理
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '内容管理', 0, 10, 'cms', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'documentation', 'admin', NOW(), '', NULL, '内容管理目录'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '内容管理' AND `parent_id` = 0);
SET @cms_menu_id = (SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` = '内容管理' AND `parent_id` = 0 LIMIT 1);

-- 2. 二级菜单：门户用户
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '门户用户', @cms_menu_id, 1, 'user', 'cms/user/index', NULL, 1, 0, 'C', '0', '0', 'cms:user:list', 'user', 'admin', NOW(), '', NULL, '门户用户管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '门户用户' AND `parent_id` = @cms_menu_id);
SET @user_menu_id = (SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` = '门户用户' AND `parent_id` = @cms_menu_id LIMIT 1);

-- 3. 门户用户的按钮权限
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '用户查询', @user_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:user:query', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:user:query');
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '用户新增', @user_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:user:add', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:user:add');
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '用户修改', @user_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:user:edit', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:user:edit');
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '用户删除', @user_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:user:remove', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:user:remove');
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '用户状态', @user_menu_id, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:user:status', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:user:status');
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '重置密码', @user_menu_id, 6, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:user:resetPwd', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:user:resetPwd');

-- 4. 二级菜单：文章管理
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '文章管理', @cms_menu_id, 2, 'article', 'cms/article/index', NULL, 1, 0, 'C', '0', '0', 'cms:article:list', 'edit', 'admin', NOW(), '', NULL, '文章管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '文章管理' AND `parent_id` = @cms_menu_id);
SET @article_menu_id = (SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` = '文章管理' AND `parent_id` = @cms_menu_id LIMIT 1);

-- 5. 文章管理的按钮权限
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '文章查询', @article_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:article:query', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:article:query');
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '文章新增', @article_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:article:add', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:article:add');
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '文章修改', @article_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:article:edit', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:article:edit');
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '文章删除', @article_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:article:remove', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:article:remove');
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '文章审核', @article_menu_id, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:article:audit', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:article:audit');
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '文章上架', @article_menu_id, 6, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:article:publish', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:article:publish');
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '文章推荐', @article_menu_id, 7, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:article:featured', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:article:featured');

-- 6. 二级菜单：分类管理
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '分类管理', @cms_menu_id, 3, 'category', 'cms/category/index', NULL, 1, 0, 'C', '0', '0', 'cms:category:list', 'tree', 'admin', NOW(), '', NULL, '分类管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '分类管理' AND `parent_id` = @cms_menu_id);
SET @category_menu_id = (SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` = '分类管理' AND `parent_id` = @cms_menu_id LIMIT 1);

-- 7. 分类管理的按钮权限
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '分类查询', @category_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:category:query', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:category:query' AND `parent_id` = @category_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '分类新增', @category_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:category:add', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:category:add' AND `parent_id` = @category_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '分类修改', @category_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:category:edit', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:category:edit' AND `parent_id` = @category_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '分类删除', @category_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:category:remove', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:category:remove' AND `parent_id` = @category_menu_id);

-- 8. 二级菜单：标签管理
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '标签管理', @cms_menu_id, 4, 'tag', 'cms/tag/index', NULL, 1, 0, 'C', '0', '0', 'cms:tag:list', 'tab', 'admin', NOW(), '', NULL, '标签管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '标签管理' AND `parent_id` = @cms_menu_id);
SET @tag_menu_id = (SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` = '标签管理' AND `parent_id` = @cms_menu_id LIMIT 1);

-- 9. 标签管理的按钮权限
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '标签查询', @tag_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:tag:query', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:tag:query' AND `parent_id` = @tag_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '标签新增', @tag_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:tag:add', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:tag:add' AND `parent_id` = @tag_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '标签修改', @tag_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:tag:edit', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:tag:edit' AND `parent_id` = @tag_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '标签删除', @tag_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:tag:remove', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:tag:remove' AND `parent_id` = @tag_menu_id);

-- 10. 二级菜单：评论管理
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '评论管理', @cms_menu_id, 5, 'comment', 'cms/comment/index', NULL, 1, 0, 'C', '0', '0', 'cms:comment:list', 'message', 'admin', NOW(), '', NULL, '评论管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '评论管理' AND `parent_id` = @cms_menu_id);
SET @comment_menu_id = (SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` = '评论管理' AND `parent_id` = @cms_menu_id LIMIT 1);

-- 11. 评论管理的按钮权限
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '评论查询', @comment_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:comment:query', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:comment:query' AND `parent_id` = @comment_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '评论审核', @comment_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:comment:audit', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:comment:audit' AND `parent_id` = @comment_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '评论删除', @comment_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:comment:remove', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:comment:remove' AND `parent_id` = @comment_menu_id);

-- 12. 二级菜单：通知管理
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '通知管理', @cms_menu_id, 6, 'notification', 'cms/notification/index', NULL, 1, 0, 'C', '0', '0', 'cms:notification:list', 'email', 'admin', NOW(), '', NULL, '通知管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '通知管理' AND `parent_id` = @cms_menu_id);
SET @notification_menu_id = (SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` = '通知管理' AND `parent_id` = @cms_menu_id LIMIT 1);

-- 13. 通知管理的按钮权限
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '通知查询', @notification_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:notification:query', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:notification:query' AND `parent_id` = @notification_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '通知新增', @notification_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:notification:add', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:notification:add' AND `parent_id` = @notification_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '通知修改', @notification_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:notification:edit', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:notification:edit' AND `parent_id` = @notification_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '通知删除', @notification_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:notification:remove', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:notification:remove' AND `parent_id` = @notification_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '发送系统通知', @notification_menu_id, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:notification:sendAll', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:notification:sendAll' AND `parent_id` = @notification_menu_id);

-- 14. 二级菜单：友情链接
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '友情链接', @cms_menu_id, 7, 'friend-link', 'cms/friend-link/index', NULL, 1, 0, 'C', '0', '0', 'cms:friend-link:list', 'link', 'admin', NOW(), '', NULL, '友情链接管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '友情链接' AND `parent_id` = @cms_menu_id);
SET @friend_link_menu_id = (SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` = '友情链接' AND `parent_id` = @cms_menu_id LIMIT 1);

-- 15. 友情链接管理的按钮权限
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '友情链接查询', @friend_link_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:friend-link:query', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:friend-link:query' AND `parent_id` = @friend_link_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '友情链接新增', @friend_link_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:friend-link:add', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:friend-link:add' AND `parent_id` = @friend_link_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '友情链接修改', @friend_link_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:friend-link:edit', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:friend-link:edit' AND `parent_id` = @friend_link_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '友情链接删除', @friend_link_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:friend-link:remove', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:friend-link:remove' AND `parent_id` = @friend_link_menu_id);

-- 为管理员角色分配 CMS 菜单权限（幂等）
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @admin_role_id, `menu_id` FROM `sys_menu`
WHERE (`menu_name` IN ('内容管理', '门户用户', '文章管理', '分类管理', '标签管理', '评论管理', '通知管理', '友情链接')
       OR `parent_id` IN (SELECT `menu_id` FROM (SELECT * FROM `sys_menu`) tmp WHERE `menu_name` IN (
            '内容管理', '门户用户', '文章管理', '分类管理', '标签管理', '评论管理', '通知管理', '友情链接'
       )))
AND `menu_id` NOT IN (SELECT `menu_id` FROM `sys_role_menu` WHERE `role_id` = @admin_role_id);

-- =====================================================================
-- 二、读书空间菜单（来源：17_portal_book_menu_init.sql）
-- =====================================================================

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '读书空间', 0, 11, 'portal', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'reading', 'admin', NOW(), '', NULL, '读书空间目录'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '读书空间' AND `parent_id` = 0);
SET @portal_menu_id = (SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` = '读书空间' AND `parent_id` = 0 LIMIT 1);

-- 书籍管理
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '书籍管理', @portal_menu_id, 1, 'book', 'portal/book/index', NULL, 1, 0, 'C', '0', '0', 'portal:book:list', 'book', 'admin', NOW(), '', NULL, '书籍管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '书籍管理' AND `parent_id` = @portal_menu_id);
SET @book_menu_id = (SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` = '书籍管理' AND `parent_id` = @portal_menu_id LIMIT 1);

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '书籍查询', @book_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:book:query', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'portal:book:query' AND `parent_id` = @book_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '书籍新增', @book_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:book:add', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'portal:book:add' AND `parent_id` = @book_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '书籍修改', @book_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:book:edit', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'portal:book:edit' AND `parent_id` = @book_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '书籍删除', @book_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:book:remove', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'portal:book:remove' AND `parent_id` = @book_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '书籍推荐', @book_menu_id, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:book:featured', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'portal:book:featured' AND `parent_id` = @book_menu_id);

-- 书单管理
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '书单管理', @portal_menu_id, 2, 'bookList', 'portal/bookList/index', NULL, 1, 0, 'C', '0', '0', 'portal:bookList:list', 'list', 'admin', NOW(), '', NULL, '书单管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '书单管理' AND `parent_id` = @portal_menu_id);
SET @booklist_menu_id = (SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` = '书单管理' AND `parent_id` = @portal_menu_id LIMIT 1);

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '书单查询', @booklist_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookList:query', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'portal:bookList:query' AND `parent_id` = @booklist_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '书单新增', @booklist_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookList:add', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'portal:bookList:add' AND `parent_id` = @booklist_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '书单修改', @booklist_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookList:edit', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'portal:bookList:edit' AND `parent_id` = @booklist_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '书单删除', @booklist_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookList:remove', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'portal:bookList:remove' AND `parent_id` = @booklist_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '书单推荐', @booklist_menu_id, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookList:featured', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'portal:bookList:featured' AND `parent_id` = @booklist_menu_id);

-- 金句管理
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '金句管理', @portal_menu_id, 3, 'bookQuote', 'portal/bookQuote/index', NULL, 1, 0, 'C', '0', '0', 'portal:bookQuote:list', 'edit', 'admin', NOW(), '', NULL, '金句管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '金句管理' AND `parent_id` = @portal_menu_id);
SET @quote_menu_id = (SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` = '金句管理' AND `parent_id` = @portal_menu_id LIMIT 1);

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '金句查询', @quote_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookQuote:query', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'portal:bookQuote:query' AND `parent_id` = @quote_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '金句新增', @quote_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookQuote:add', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'portal:bookQuote:add' AND `parent_id` = @quote_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '金句修改', @quote_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookQuote:edit', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'portal:bookQuote:edit' AND `parent_id` = @quote_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '金句删除', @quote_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookQuote:remove', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'portal:bookQuote:remove' AND `parent_id` = @quote_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '金句精选', @quote_menu_id, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookQuote:featured', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'portal:bookQuote:featured' AND `parent_id` = @quote_menu_id);

-- 为管理员角色分配读书空间菜单权限（幂等）
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @admin_role_id, `menu_id` FROM `sys_menu`
WHERE (`menu_name` IN ('读书空间', '书籍管理', '书单管理', '金句管理')
       OR `parent_id` IN (SELECT `menu_id` FROM (SELECT * FROM `sys_menu`) tmp WHERE `menu_name` IN (
            '读书空间', '书籍管理', '书单管理', '金句管理'
       )))
AND `menu_id` NOT IN (SELECT `menu_id` FROM `sys_role_menu` WHERE `role_id` = @admin_role_id);

-- =====================================================================
-- 三、面试空间菜单（来源：18_interview_menu_init.sql）
-- =====================================================================

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '面试空间', 0, 12, 'interview', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'education', 'admin', NOW(), '', NULL, '面试空间目录'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '面试空间' AND `parent_id` = 0);
SET @interview_menu_id = (SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` = '面试空间' AND `parent_id` = 0 LIMIT 1);

-- 题目管理
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '题目管理', @interview_menu_id, 1, 'question', 'cms/interview/question/index', NULL, 1, 0, 'C', '0', '0', 'cms:interview:list', 'list', 'admin', NOW(), '', NULL, '面试题目管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '题目管理' AND `parent_id` = @interview_menu_id);
SET @question_menu_id = (SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` = '题目管理' AND `parent_id` = @interview_menu_id LIMIT 1);

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '题目查询', @question_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:query', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:interview:query' AND `parent_id` = @question_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '题目新增', @question_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:add', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:interview:add' AND `parent_id` = @question_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '题目修改', @question_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:edit', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:interview:edit' AND `parent_id` = @question_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '题目删除', @question_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:remove', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:interview:remove' AND `parent_id` = @question_menu_id);

-- 面试分类
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '面试分类', @interview_menu_id, 2, 'category', 'cms/interview/category/index', NULL, 1, 0, 'C', '0', '0', 'cms:interview:list', 'tree-table', 'admin', NOW(), '', NULL, '面试分类管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '面试分类' AND `parent_id` = @interview_menu_id);
SET @interview_category_menu_id = (SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` = '面试分类' AND `parent_id` = @interview_menu_id LIMIT 1);

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '分类查询', @interview_category_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:query', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:interview:query' AND `parent_id` = @interview_category_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '分类新增', @interview_category_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:add', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:interview:add' AND `parent_id` = @interview_category_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '分类修改', @interview_category_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:edit', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:interview:edit' AND `parent_id` = @interview_category_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '分类删除', @interview_category_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:remove', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:interview:remove' AND `parent_id` = @interview_category_menu_id);

-- 面经管理
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '面经管理', @interview_menu_id, 3, 'experience', 'cms/interview/experience/index', NULL, 1, 0, 'C', '0', '0', 'cms:interview:list', 'documentation', 'admin', NOW(), '', NULL, '面试面经管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '面经管理' AND `parent_id` = @interview_menu_id);
SET @experience_menu_id = (SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` = '面经管理' AND `parent_id` = @interview_menu_id LIMIT 1);

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '面经查询', @experience_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:query', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:interview:query' AND `parent_id` = @experience_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '面经审核', @experience_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:edit', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '面经审核' AND `parent_id` = @experience_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '面经置顶', @experience_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:edit', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '面经置顶' AND `parent_id` = @experience_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '面经删除', @experience_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:remove', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '面经删除' AND `parent_id` = @experience_menu_id);

-- 面试评论
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '面试评论', @interview_menu_id, 4, 'comment', 'cms/interview/comment/index', NULL, 1, 0, 'C', '0', '0', 'cms:interview:list', 'message', 'admin', NOW(), '', NULL, '面试评论管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '面试评论' AND `parent_id` = @interview_menu_id);
SET @interview_comment_menu_id = (SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` = '面试评论' AND `parent_id` = @interview_menu_id LIMIT 1);

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '评论查询', @interview_comment_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:query', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:interview:query' AND `parent_id` = @interview_comment_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '评论审核', @interview_comment_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:edit', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '评论审核' AND `parent_id` = @interview_comment_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '评论删除', @interview_comment_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:remove', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '评论删除' AND `parent_id` = @interview_comment_menu_id);

-- 简历模板
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '简历模板', @interview_menu_id, 5, 'resume', 'cms/interview/resume/index', NULL, 1, 0, 'C', '0', '0', 'cms:interview:list', 'example', 'admin', NOW(), '', NULL, '简历模板管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '简历模板' AND `parent_id` = @interview_menu_id);
SET @resume_menu_id = (SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` = '简历模板' AND `parent_id` = @interview_menu_id LIMIT 1);

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '模板查询', @resume_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:query', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:interview:query' AND `parent_id` = @resume_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '模板新增', @resume_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:add', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:interview:add' AND `parent_id` = @resume_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '模板修改', @resume_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:edit', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:interview:edit' AND `parent_id` = @resume_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '模板删除', @resume_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:remove', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:interview:remove' AND `parent_id` = @resume_menu_id);

-- 公司管理
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '公司管理', @interview_menu_id, 6, 'company', 'cms/interview/company/index', NULL, 1, 0, 'C', '0', '0', 'cms:interview:list', 'peoples', 'admin', NOW(), '', NULL, '面试公司管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '公司管理' AND `parent_id` = @interview_menu_id);
SET @company_menu_id = (SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` = '公司管理' AND `parent_id` = @interview_menu_id LIMIT 1);

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '公司查询', @company_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:query', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:interview:query' AND `parent_id` = @company_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '公司新增', @company_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:add', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:interview:add' AND `parent_id` = @company_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '公司修改', @company_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:edit', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:interview:edit' AND `parent_id` = @company_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '公司删除', @company_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:remove', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:interview:remove' AND `parent_id` = @company_menu_id);

-- 为管理员角色分配面试空间菜单权限（幂等）
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @admin_role_id, `menu_id` FROM `sys_menu`
WHERE (`menu_name` IN ('面试空间', '题目管理', '面试分类', '面经管理', '面试评论', '简历模板', '公司管理')
       OR `parent_id` IN (SELECT `menu_id` FROM (SELECT * FROM `sys_menu`) tmp WHERE `menu_name` IN (
            '面试空间', '题目管理', '面试分类', '面经管理', '面试评论', '简历模板', '公司管理'
       )))
AND `menu_id` NOT IN (SELECT `menu_id` FROM `sys_role_menu` WHERE `role_id` = @admin_role_id);

-- =====================================================================
-- 四、成长体系后台菜单（来源：23_growth_admin_menu.sql）
-- =====================================================================

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '成长体系', 0, 13, 'growth', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'chart', 'admin', NOW(), '', NULL, '成长体系目录'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '成长体系' AND `parent_id` = 0);
SET @growth_menu_id = (SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` = '成长体系' AND `parent_id` = 0 LIMIT 1);

-- 成长规则
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '成长规则', @growth_menu_id, 1, 'rule', 'cms/growth/rule/index', NULL, 1, 0, 'C', '0', '0', 'cms:growth:list', 'edit', 'admin', NOW(), '', NULL, '成长规则配置菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '成长规则' AND `parent_id` = @growth_menu_id);
SET @rule_menu_id = (SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` = '成长规则' AND `parent_id` = @growth_menu_id LIMIT 1);

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '规则查询', @rule_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:growth:query', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:growth:query' AND `parent_id` = @rule_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '规则新增', @rule_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:growth:add', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:growth:add' AND `parent_id` = @rule_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '规则修改', @rule_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:growth:edit', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:growth:edit' AND `parent_id` = @rule_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '规则删除', @rule_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:growth:remove', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:growth:remove' AND `parent_id` = @rule_menu_id);

-- 成就管理
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '成就管理', @growth_menu_id, 2, 'achievement', 'cms/growth/achievement/index', NULL, 1, 0, 'C', '0', '0', 'cms:growth:list', 'star', 'admin', NOW(), '', NULL, '成就定义管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '成就管理' AND `parent_id` = @growth_menu_id);
SET @achievement_menu_id = (SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` = '成就管理' AND `parent_id` = @growth_menu_id LIMIT 1);

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '成就查询', @achievement_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:growth:query', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:growth:query' AND `parent_id` = @achievement_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '成就新增', @achievement_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:growth:add', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:growth:add' AND `parent_id` = @achievement_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '成就修改', @achievement_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:growth:edit', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:growth:edit' AND `parent_id` = @achievement_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '成就删除', @achievement_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:growth:remove', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:growth:remove' AND `parent_id` = @achievement_menu_id);

-- 用户成长
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '用户成长', @growth_menu_id, 3, 'user-growth', 'cms/growth/user/index', NULL, 1, 0, 'C', '0', '0', 'cms:growth:list', 'people', 'admin', NOW(), '', NULL, '用户成长值查询菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '用户成长' AND `parent_id` = @growth_menu_id);
SET @user_growth_menu_id = (SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` = '用户成长' AND `parent_id` = @growth_menu_id LIMIT 1);

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '成长查询', @user_growth_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:growth:query', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:growth:query' AND `parent_id` = @user_growth_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '成长调整', @user_growth_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:growth:edit', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '成长调整' AND `parent_id` = @user_growth_menu_id);

-- 用户徽章
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '用户徽章', @growth_menu_id, 4, 'badge', 'cms/growth/badge/index', NULL, 1, 0, 'C', '0', '0', 'cms:growth:list', 'medal', 'admin', NOW(), '', NULL, '用户徽章记录查询菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '用户徽章' AND `parent_id` = @growth_menu_id);
SET @badge_menu_id = (SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` = '用户徽章' AND `parent_id` = @growth_menu_id LIMIT 1);

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '徽章查询', @badge_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:growth:query', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:growth:query' AND `parent_id` = @badge_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '徽章授予', @badge_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:growth:add', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '徽章授予' AND `parent_id` = @badge_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '徽章撤销', @badge_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:growth:remove', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '徽章撤销' AND `parent_id` = @badge_menu_id);

-- 成长流水
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '成长流水', @growth_menu_id, 5, 'log', 'cms/growth/log/index', NULL, 1, 0, 'C', '0', '0', 'cms:growth:list', 'log', 'admin', NOW(), '', NULL, '成长事件流水查询菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '成长流水' AND `parent_id` = @growth_menu_id);
SET @log_menu_id = (SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` = '成长流水' AND `parent_id` = @growth_menu_id LIMIT 1);

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '流水查询', @log_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:growth:query', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '流水查询' AND `parent_id` = @log_menu_id);

-- 为管理员角色分配成长体系菜单权限（幂等）
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @admin_role_id, `menu_id` FROM `sys_menu`
WHERE (`menu_name` IN ('成长体系', '成长规则', '成就管理', '用户成长', '用户徽章', '成长流水')
       OR `parent_id` IN (SELECT `menu_id` FROM (SELECT * FROM `sys_menu`) tmp WHERE `menu_name` IN (
            '成长体系', '成长规则', '成就管理', '用户成长', '用户徽章', '成长流水'
       )))
AND `menu_id` NOT IN (SELECT `menu_id` FROM `sys_role_menu` WHERE `role_id` = @admin_role_id);

-- =====================================================================
-- 五、精选笔记菜单（来源：24_featured_note_menu.sql）
-- =====================================================================

-- 精选笔记（挂在面试空间下，排序在公司管理之后）
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '精选笔记', @interview_menu_id, 7, 'submission', 'cms/interview/submission/index', NULL, 1, 0, 'C', '0', '0', 'cms:interview:list', 'documentation', 'admin', NOW(), '', NULL, '精选笔记管理菜单（采纳/取消采纳用户提交的笔记）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '精选笔记' AND `parent_id` = @interview_menu_id);
SET @submission_menu_id = (SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` = '精选笔记' AND `parent_id` = @interview_menu_id LIMIT 1);

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '笔记查询', @submission_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:query', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:interview:query' AND `parent_id` = @submission_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '采纳精选', @submission_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:edit', '#', 'admin', NOW(), '', NULL, '采纳为精选笔记（触发 note_adopted 成长事件）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '采纳精选' AND `parent_id` = @submission_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '取消精选', @submission_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:edit', '#', 'admin', NOW(), '', NULL, '取消精选笔记'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '取消精选' AND `parent_id` = @submission_menu_id);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @admin_role_id, `menu_id` FROM `sys_menu`
WHERE (`menu_name` = '精选笔记'
       OR `parent_id` IN (SELECT `menu_id` FROM (SELECT * FROM `sys_menu`) tmp WHERE `menu_name` = '精选笔记'))
AND `menu_id` NOT IN (SELECT `menu_id` FROM `sys_role_menu` WHERE `role_id` = @admin_role_id);

-- =====================================================================
-- 六、运营首页权限点（来源：37_init_dashboard_config.sql，仅菜单部分）
-- =====================================================================

-- 取首页菜单 ID（path='index' 的 C 类型菜单）
SELECT @indexMenuId := menu_id FROM sys_menu WHERE path = 'index' AND menu_type = 'C' LIMIT 1;
SET @indexMenuId = IFNULL(@indexMenuId, 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '首页数据查询', @indexMenuId, 1, '#', '', 1, 0, 'F', '0', '0', 'system:dashboard:query', '#', 'admin', NOW(), '运营首页数据查询权限'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:dashboard:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '首页缓存刷新', @indexMenuId, 2, '#', '', 1, 0, 'F', '0', '0', 'system:dashboard:refresh', '#', 'admin', NOW(), '运营首页缓存刷新权限'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:dashboard:refresh');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu
WHERE perms LIKE 'system:dashboard:%'
  AND menu_id NOT IN (SELECT menu_id FROM sys_role_menu WHERE role_id = 1);

-- =====================================================================
-- 七、流程管理菜单补充（来源：39_init_flowable_menu.sql，仅菜单部分）
-- =====================================================================

SELECT @flowParentId := menu_id FROM sys_menu WHERE menu_name = '流程管理' AND parent_id = 0 LIMIT 1;
SET @flowParentId = IFNULL(@flowParentId, 4);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '待办任务', @flowParentId, 2, 'todo', 'flowable/task/todo/index', NULL, 1, 0, 'C', '0', '0', 'flowable:task:todoList', 'list', 'admin', NOW(), '我的待办流程任务'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'flowable:task:todoList');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '已办任务', @flowParentId, 3, 'finished', 'flowable/task/finished/index', NULL, 1, 0, 'C', '0', '0', 'flowable:task:finishedList', 'checkbox', 'admin', NOW(), '我已处理的流程任务'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'flowable:task:finishedList');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '我的流程', @flowParentId, 4, 'myProcess', 'flowable/task/myProcess/index', NULL, 1, 0, 'C', '0', '0', 'flowable:task:myProcess', 'people', 'admin', NOW(), '我发起的流程实例'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'flowable:task:myProcess');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '表单管理', @flowParentId, 5, 'form', 'flowable/task/form/index', NULL, 1, 0, 'C', '0', '0', 'flowable:form:list', 'form', 'admin', NOW(), '流程表单管理'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'flowable:form:list');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '流程表达式', @flowParentId, 6, 'expression', 'flowable/expression/index', NULL, 1, 0, 'C', '0', '0', 'flowable:expression:list', 'edit', 'admin', NOW(), '流程表达式管理'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'flowable:expression:list');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '流程监听器', @flowParentId, 7, 'listener', 'flowable/listener/index', NULL, 1, 0, 'C', '0', '0', 'flowable:listener:list', 'guide', 'admin', NOW(), '流程监听器管理'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'flowable:listener:list');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu
WHERE perms IN (
    'flowable:task:todoList', 'flowable:task:finishedList', 'flowable:task:myProcess',
    'flowable:form:list', 'flowable:expression:list', 'flowable:listener:list'
)
AND menu_id NOT IN (SELECT menu_id FROM sys_role_menu WHERE role_id = 1);

-- =====================================================================
-- 八、章节管理菜单（来源：43_portal_book_chapter_menu_init.sql）
-- =====================================================================

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '章节管理', @portal_menu_id, 4, 'bookChapter', 'portal/bookChapter/index', NULL, 1, 0, 'C', '0', '0', 'portal:bookChapter:list', 'edit', 'admin', NOW(), '', NULL, '书籍章节管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '章节管理' AND `parent_id` = @portal_menu_id);
SET @chapter_menu_id = (SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` = '章节管理' AND `parent_id` = @portal_menu_id LIMIT 1);

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '章节查询', @chapter_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookChapter:query', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'portal:bookChapter:query' AND `parent_id` = @chapter_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '章节新增', @chapter_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookChapter:add', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'portal:bookChapter:add' AND `parent_id` = @chapter_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '章节修改', @chapter_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookChapter:edit', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'portal:bookChapter:edit' AND `parent_id` = @chapter_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '章节删除', @chapter_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookChapter:remove', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'portal:bookChapter:remove' AND `parent_id` = @chapter_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '章节发布', @chapter_menu_id, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookChapter:publish', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'portal:bookChapter:publish' AND `parent_id` = @chapter_menu_id);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @admin_role_id, `menu_id` FROM `sys_menu`
WHERE (`menu_id` = @chapter_menu_id OR `parent_id` = @chapter_menu_id)
AND `menu_id` NOT IN (SELECT `menu_id` FROM `sys_role_menu` WHERE `role_id` = @admin_role_id);

-- =====================================================================
-- 九、书架管理菜单（来源：45_portal_bookshelf_menu_init.sql）
-- =====================================================================

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '书架管理', @portal_menu_id, 5, 'bookshelf', 'portal/bookshelf/index', NULL, 1, 0, 'C', '0', '0', 'portal:bookshelf:list', 'shopping', 'admin', NOW(), '', NULL, '用户书架（收藏书籍）管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '书架管理' AND `parent_id` = @portal_menu_id);
SET @bookshelf_menu_id = (SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` = '书架管理' AND `parent_id` = @portal_menu_id LIMIT 1);

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '书架查询', @bookshelf_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookshelf:query', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'portal:bookshelf:query' AND `parent_id` = @bookshelf_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '书架移除', @bookshelf_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookshelf:remove', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'portal:bookshelf:remove' AND `parent_id` = @bookshelf_menu_id);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @admin_role_id, `menu_id` FROM `sys_menu`
WHERE (`menu_id` = @bookshelf_menu_id OR `parent_id` = @bookshelf_menu_id)
AND `menu_id` NOT IN (SELECT `menu_id` FROM `sys_role_menu` WHERE `role_id` = @admin_role_id);

-- =====================================================================
-- 十、推荐位管理菜单（来源：47_portal_book_recommend_menu_init.sql）
-- =====================================================================

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '推荐位管理', @portal_menu_id, 6, 'bookRecommend', 'portal/bookRecommend/index', NULL, 1, 0, 'C', '0', '0', 'portal:bookRecommend:list', 'star', 'admin', NOW(), '', NULL, '书籍推荐位管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '推荐位管理' AND `parent_id` = @portal_menu_id);
SET @recommend_menu_id = (SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` = '推荐位管理' AND `parent_id` = @portal_menu_id LIMIT 1);

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '推荐位查询', @recommend_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookRecommend:query', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'portal:bookRecommend:query' AND `parent_id` = @recommend_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '推荐位新增', @recommend_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookRecommend:add', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'portal:bookRecommend:add' AND `parent_id` = @recommend_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '推荐位修改', @recommend_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookRecommend:edit', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'portal:bookRecommend:edit' AND `parent_id` = @recommend_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '推荐位删除', @recommend_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookRecommend:remove', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'portal:bookRecommend:remove' AND `parent_id` = @recommend_menu_id);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @admin_role_id, `menu_id` FROM `sys_menu`
WHERE (`menu_id` = @recommend_menu_id OR `parent_id` = @recommend_menu_id)
AND `menu_id` NOT IN (SELECT `menu_id` FROM `sys_role_menu` WHERE `role_id` = @admin_role_id);

-- =====================================================================
-- 十一、创作者认证菜单（来源：63_creator_certification_init.sql，仅菜单部分）
-- =====================================================================

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '创作者认证', 0, 14, 'certification', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'user', 'admin', NOW(), '', NULL, '创作者认证目录'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '创作者认证' AND `parent_id` = 0);
SET @cert_menu_id = (SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` = '创作者认证' AND `parent_id` = 0 LIMIT 1);

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '认证审核', @cert_menu_id, 1, 'audit', 'cms/certification/index', NULL, 1, 0, 'C', '0', '0', 'cms:certification:audit', 'edit', 'admin', NOW(), '', NULL, '创作者认证审核菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '认证审核' AND `parent_id` = @cert_menu_id);
SET @cert_audit_menu_id = (SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` = '认证审核' AND `parent_id` = @cert_menu_id LIMIT 1);

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '认证查询', @cert_audit_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:certification:list', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:certification:list' AND `parent_id` = @cert_audit_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '认证审核', @cert_audit_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:certification:audit', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:certification:audit' AND `parent_id` = @cert_audit_menu_id);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @admin_role_id, `menu_id` FROM `sys_menu`
WHERE (`menu_name` IN ('创作者认证', '认证审核')
   OR `parent_id` IN (SELECT `menu_id` FROM (SELECT * FROM `sys_menu`) tmp WHERE `menu_name` IN ('创作者认证', '认证审核')))
AND `menu_id` NOT IN (SELECT `menu_id` FROM `sys_role_menu` WHERE `role_id` = @admin_role_id);

-- =====================================================================
-- 十二、学习者中心菜单（来源：64_learn_center_init.sql）
-- =====================================================================

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '学习者中心', 0, 15, 'portal', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'education', 'admin', NOW(), '', NULL, '学习者中心目录'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '学习者中心' AND `parent_id` = 0);
SET @learn_menu_id = (SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` = '学习者中心' AND `parent_id` = 0 LIMIT 1);

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '学习计划', @learn_menu_id, 1, 'studyPlan', 'portal/studyPlan/index', NULL, 1, 0, 'C', '0', '0', 'portal:studyPlan:list', 'edit', 'admin', NOW(), '', NULL, '学习计划只读查看菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '学习计划' AND `parent_id` = @learn_menu_id);
SET @study_plan_menu_id = (SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` = '学习计划' AND `parent_id` = @learn_menu_id LIMIT 1);

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '计划查询', @study_plan_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:studyPlan:query', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'portal:studyPlan:query' AND `parent_id` = @study_plan_menu_id);

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '错题本', @learn_menu_id, 2, 'wrongQuestion', 'portal/wrongQuestion/index', NULL, 1, 0, 'C', '0', '0', 'portal:wrongQuestion:list', 'edit', 'admin', NOW(), '', NULL, '错题本只读查看菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = '错题本' AND `parent_id` = @learn_menu_id);
SET @wrong_question_menu_id = (SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` = '错题本' AND `parent_id` = @learn_menu_id LIMIT 1);

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '错题查询', @wrong_question_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:wrongQuestion:query', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'portal:wrongQuestion:query' AND `parent_id` = @wrong_question_menu_id);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @admin_role_id, `menu_id` FROM `sys_menu`
WHERE (`menu_name` IN ('学习者中心', '学习计划', '错题本')
   OR `parent_id` IN (SELECT `menu_id` FROM (SELECT * FROM `sys_menu`) tmp WHERE `menu_name` IN ('学习者中心', '学习计划', '错题本')))
AND `menu_id` NOT IN (SELECT `menu_id` FROM `sys_role_menu` WHERE `role_id` = @admin_role_id);

-- =====================================================================
-- 十三、职位管理菜单（来源：67_job_init.sql，仅菜单部分）
-- =====================================================================

-- 取"面试空间"父菜单ID（已在第三段插入）
SET @interview_menu_id_for_job = IFNULL(@interview_menu_id, 0);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '职位管理', @interview_menu_id_for_job, 7, 'job', 'portal/job/index', NULL, 1, 0, 'C', '0', '0', 'portal:job:list', 'post', 'admin', NOW(), '职位管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:job:list');
SET @job_menu_id = (SELECT menu_id FROM sys_menu WHERE perms = 'portal:job:list' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '职位查询', @job_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:job:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:job:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '职位新增', @job_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:job:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:job:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '职位修改', @job_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:job:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:job:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '职位删除', @job_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:job:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:job:remove');

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @admin_role_id, `menu_id` FROM `sys_menu`
WHERE `perms` IN ('portal:job:list', 'portal:job:query', 'portal:job:add', 'portal:job:edit', 'portal:job:remove')
AND `menu_id` NOT IN (SELECT `menu_id` FROM `sys_role_menu` WHERE `role_id` = @admin_role_id);

-- =====================================================================
-- 十四、VIP 会员中心菜单（来源：75_vip_center_init.sql）
-- =====================================================================

-- 一级目录：商业化（若已存在则复用）
SET @biz_menu_id = (SELECT `menu_id` FROM (SELECT * FROM `sys_menu`) tmp WHERE `menu_name` = '商业化' LIMIT 1);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '商业化', 0, 15, 'biz', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'money', 'admin', NOW(), '', NULL, '商业化目录'
FROM DUAL WHERE @biz_menu_id IS NULL;
SET @biz_menu_id = (SELECT `menu_id` FROM (SELECT * FROM `sys_menu`) tmp WHERE `menu_name` = '商业化' LIMIT 1);

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 'VIP套餐管理', @biz_menu_id, 1, 'vipPackage', 'portal/vipPackage/index', NULL, 1, 0, 'C', '0', '0', 'portal:vipPackage:list', 'vip', 'admin', NOW(), '', NULL, 'VIP套餐管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_name` = 'VIP套餐管理' AND `parent_id` = @biz_menu_id);
SET @vip_menu_id = (SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` = 'VIP套餐管理' AND `parent_id` = @biz_menu_id LIMIT 1);

INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '套餐查询', @vip_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:vipPackage:query', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'portal:vipPackage:query' AND `parent_id` = @vip_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '套餐新增', @vip_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:vipPackage:add', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'portal:vipPackage:add' AND `parent_id` = @vip_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '套餐修改', @vip_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:vipPackage:edit', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'portal:vipPackage:edit' AND `parent_id` = @vip_menu_id);
INSERT INTO `sys_menu`(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '套餐删除', @vip_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:vipPackage:remove', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'portal:vipPackage:remove' AND `parent_id` = @vip_menu_id);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @admin_role_id, `menu_id` FROM `sys_menu`
WHERE (`menu_name` IN ('商业化', 'VIP套餐管理')
   OR `parent_id` IN (SELECT `menu_id` FROM (SELECT * FROM `sys_menu`) tmp WHERE `menu_name` = 'VIP套餐管理'))
AND `menu_id` NOT IN (SELECT `menu_id` FROM `sys_role_menu` WHERE `role_id` = @admin_role_id);

-- =====================================================================
-- 十五、共读活动菜单（来源：77_book_club_admin_menu.sql）
-- =====================================================================

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '共读活动', @portal_menu_id, 8, 'bookClub', 'portal/bookClub/index', NULL, 1, 0, 'C', '0', '0', 'portal:bookClub:list', 'people', 'admin', NOW(), '共读活动后台管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookClub:list');
SET @book_club_menu_id = (SELECT menu_id FROM sys_menu WHERE perms = 'portal:bookClub:list' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '活动查询', @book_club_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookClub:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookClub:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '活动新增', @book_club_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookClub:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookClub:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '活动修改', @book_club_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookClub:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookClub:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '活动删除', @book_club_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:bookClub:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookClub:remove');

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @admin_role_id, `menu_id` FROM `sys_menu`
WHERE `perms` IN ('portal:bookClub:list', 'portal:bookClub:query', 'portal:bookClub:add', 'portal:bookClub:edit', 'portal:bookClub:remove')
AND `menu_id` NOT IN (SELECT `menu_id` FROM `sys_role_menu` WHERE `role_id` = @admin_role_id);

-- =====================================================================
-- 十六、专栏管理菜单（来源：78_column_admin_menu.sql）
-- =====================================================================

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '专栏管理', @cms_menu_id, 12, 'column', 'cms/column/index', NULL, 1, 0, 'C', '0', '0', 'portal:column:list', 'documentation', 'admin', NOW(), '专栏后台管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:column:list');
SET @column_menu_id = (SELECT menu_id FROM sys_menu WHERE perms = 'portal:column:list' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '专栏查询', @column_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:column:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:column:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '专栏新增', @column_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:column:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:column:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '专栏修改', @column_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:column:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:column:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '专栏删除', @column_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:column:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:column:remove');

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @admin_role_id, `menu_id` FROM `sys_menu`
WHERE `perms` IN ('portal:column:list', 'portal:column:query', 'portal:column:add', 'portal:column:edit', 'portal:column:remove')
AND `menu_id` NOT IN (SELECT `menu_id` FROM `sys_role_menu` WHERE `role_id` = @admin_role_id);

-- =====================================================================
-- 十七、打赏管理菜单（来源：79_tip_admin_menu.sql，已下线 visible=1 隐藏）
-- =====================================================================

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '打赏管理', @cms_menu_id, 13, 'tip', 'cms/tip/index', NULL, 1, 0, 'C', '1', '0', 'portal:tip:list', 'money', 'admin', NOW(), '【已下线】前台打赏功能移除，菜单隐藏保留以兼容历史数据'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:tip:list');
SET @tip_menu_id = (SELECT menu_id FROM sys_menu WHERE perms = 'portal:tip:list' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '打赏查询', @tip_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '1', '0', 'portal:tip:query', '#', 'admin', NOW(), '【已下线】'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:tip:query');

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @admin_role_id, `menu_id` FROM `sys_menu`
WHERE `perms` IN ('portal:tip:list', 'portal:tip:query')
AND `menu_id` NOT IN (SELECT `menu_id` FROM `sys_role_menu` WHERE `role_id` = @admin_role_id);

-- =====================================================================
-- 十八、付费订单菜单（来源：80_order_admin_menu.sql，已下线 visible=1 隐藏）
-- =====================================================================

-- 财务一级目录（已下线 visible=1 隐藏）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '财务', 0, 20, 'finance', NULL, NULL, 1, 0, 'M', '1', '0', NULL, 'money', 'admin', NOW(), '【已下线】财务目录，前台消费记录入口已移除'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name = '财务' AND parent_id = 0);
SET @finance_menu_id = (SELECT menu_id FROM sys_menu WHERE menu_name = '财务' AND parent_id = 0 LIMIT 1);
SET @finance_menu_id = IFNULL(@finance_menu_id, 0);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '付费订单', @finance_menu_id, 1, 'order', 'cms/order/index', NULL, 1, 0, 'C', '1', '0', 'portal:order:list', 'shopping', 'admin', NOW(), '【已下线】前台消费记录入口移除，菜单隐藏保留以兼容历史数据'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:order:list');
SET @order_menu_id = (SELECT menu_id FROM sys_menu WHERE perms = 'portal:order:list' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '订单查询', @order_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '1', '0', 'portal:order:query', '#', 'admin', NOW(), '【已下线】'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:order:query');

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @admin_role_id, `menu_id` FROM `sys_menu`
WHERE (`perms` IN ('portal:order:list', 'portal:order:query')
       OR (menu_name = '财务' AND parent_id = 0))
AND `menu_id` NOT IN (SELECT `menu_id` FROM `sys_role_menu` WHERE `role_id` = @admin_role_id);

-- =====================================================================
-- 十九、私信中心菜单（来源：88_message_admin_menu.sql）
-- =====================================================================

-- 取"系统管理"父菜单ID
SELECT @sysParentId := menu_id FROM sys_menu WHERE menu_name = '系统管理' AND parent_id = 0 LIMIT 1;
SET @sysParentId = IFNULL(@sysParentId, 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '私信中心', @sysParentId, 12, 'message', 'system/message/index', NULL, 1, 0, 'C', '0', '0', 'system:message:list', 'message', 'admin', NOW(), '管理员接收与回复门户用户私信'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:message:list');
SET @messageMenuId = (SELECT menu_id FROM sys_menu WHERE perms = 'system:message:list' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '会话查询', @messageMenuId, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:message:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:message:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '回复私信', @messageMenuId, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:message:send', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:message:send');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu
WHERE perms IN ('system:message:list', 'system:message:query', 'system:message:send')
  AND menu_id NOT IN (SELECT menu_id FROM sys_role_menu WHERE role_id = 1);

-- =====================================================================
-- 菜单修复段（UPDATE 语句，文件末尾执行）
-- 来源：25, 40, 49, 50, 82, 83
-- =====================================================================

-- =====================================================================
-- 修复1：操作日志/登录日志菜单 path 异常（来源：25_fix_operlog_path-new.sql）
-- =====================================================================
UPDATE `sys_menu` SET `path` = 'system'    WHERE `menu_id` = 1;
UPDATE `sys_menu` SET `path` = 'log'       WHERE `menu_id` = 108;
UPDATE `sys_menu` SET `path` = 'operlog'   WHERE `menu_id` = 500;
UPDATE `sys_menu` SET `path` = 'logininfor' WHERE `menu_id` = 501;

-- =====================================================================
-- 修复2：评论管理菜单修复 + 权限标识统一（来源：40_fix_bugs_v4.sql，仅菜单部分）
-- =====================================================================
-- 删除多余通知菜单（保留 cms/notification 通知管理）
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (107, 1035, 1036, 1037, 1038);
DELETE FROM `sys_menu` WHERE `menu_id` = 107 OR `parent_id` = 107;
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (
    SELECT menu_id FROM (
        SELECT `menu_id` FROM `sys_menu`
        WHERE `perms` LIKE 'system:notification:%'
           OR (`menu_name` = '通知中心' AND `parent_id` = 1)
    ) t
);
DELETE FROM `sys_menu`
WHERE `perms` LIKE 'system:notification:%'
   OR (`menu_name` = '通知中心' AND `parent_id` = 1);

-- 修正评论管理菜单 path（如被错误改为 'cms/comment'，恢复为 'comment'）
UPDATE `sys_menu`
SET `path` = 'comment', `update_by` = 'admin', `update_time` = NOW()
WHERE `menu_name` = '评论管理'
  AND `path` != 'comment'
  AND `parent_id` IN (
      SELECT menu_id FROM (
          SELECT `menu_id` FROM `sys_menu`
          WHERE `menu_name` = '内容管理' AND `parent_id` = 0
      ) t
  );

-- 权限标识统一：cms:comment:audit → cms:comment:edit
UPDATE `sys_menu`
SET `perms` = 'cms:comment:edit', `update_by` = 'admin', `update_time` = NOW()
WHERE `perms` = 'cms:comment:audit';

-- =====================================================================
-- 修复3：分类管理 path 修复（来源：49_fix_cms_category_menu_path.sql）
-- path = '/category'（绝对路径，前端不再拼接父 path）
-- =====================================================================
UPDATE `sys_menu`
SET `path` = '/category', `update_by` = 'admin', `update_time` = NOW()
WHERE `menu_name` = '分类管理'
  AND `path` != '/category';

-- =====================================================================
-- 修复4：停用"表单构建"菜单（来源：50_disable_tool_build_menu.sql）
-- =====================================================================
UPDATE `sys_menu`
SET `status` = '1', `path` = 'build', `update_by` = 'admin', `update_time` = NOW()
WHERE `menu_name` = '表单构建'
  AND `status` != '1';

-- =====================================================================
-- 修复5：已下线菜单隐藏（来源：82_deprecated_menus_offline.sql）
-- =====================================================================
-- 5.1 打赏管理 + 打赏查询按钮
UPDATE `sys_menu` SET `visible` = '1', `remark` = '【已下线】前台打赏功能移除，菜单隐藏保留以兼容历史数据'
WHERE `perms` IN ('portal:tip:list', 'portal:tip:query');

-- 5.2 财务一级目录 + 付费订单 + 订单查询按钮
UPDATE `sys_menu` SET `visible` = '1', `remark` = '【已下线】财务目录，前台消费记录入口已移除'
WHERE `menu_name` = '财务' AND `parent_id` = 0;
UPDATE `sys_menu` SET `visible` = '1', `remark` = '【已下线】前台消费记录入口移除，菜单隐藏保留以兼容历史数据'
WHERE `perms` IN ('portal:order:list', 'portal:order:query');

-- 5.3 创作者结算一级目录 + 分成结算二级菜单
UPDATE `sys_menu` SET `visible` = '1', `remark` = '【已下线】前台打赏/消费记录移除，结算依赖的收入来源不存在'
WHERE `menu_name` IN ('创作者结算', '分成结算')
   OR `parent_id` IN (SELECT `menu_id` FROM (SELECT * FROM `sys_menu`) tmp WHERE `menu_name` = '分成结算');

-- 5.4 PK 对战兜底
UPDATE `sys_menu` SET `visible` = '1', `remark` = '【已下线】前台 PK 对战功能移除'
WHERE `perms` LIKE 'portal:pk%' OR `menu_name` LIKE '%PK%对战%';

-- =====================================================================
-- 修复6：子菜单 path 前缀修复（来源：83_fix_menu_path_prefix.sql）
-- 去掉 C/M 类型菜单 path 的前导 /
-- =====================================================================
UPDATE sys_menu
SET path = TRIM(LEADING '/' FROM path)
WHERE menu_type = 'C'
  AND path LIKE '/%';

UPDATE sys_menu
SET path = TRIM(LEADING '/' FROM path)
WHERE menu_type = 'M'
  AND path LIKE '/%';

SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS;

-- =====================================================================
-- 菜单初始化合并脚本执行完成
-- =====================================================================
SELECT '菜单初始化与修复合并脚本执行完成！' AS message;
