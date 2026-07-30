-- 来源：all-db-ddl.sql 行4582-4975
-- 用途：sys_menu 第二段——CMS 内容管理菜单完整初始化（INSERT 种子数据）
-- v6.1 修复：将 CMS 菜单 ID 起点设为 2000，与 RuoYi 菜单（1-1060）区分，便于维护和角色关联

ALTER TABLE sys_menu AUTO_INCREMENT = 2000;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '内容管理', 0, 10, 'cms', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'documentation', 'admin', NOW(), '内容管理目录'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name = '内容管理' AND parent_id = 0);
SELECT @cms_parent_id := menu_id FROM sys_menu WHERE menu_name = '内容管理' AND parent_id = 0 LIMIT 1;

-- =============================================================================
-- 二、门户用户管理（cms:user）
--    path=portal-user，避免与"系统用户管理"菜单 path=user 冲突
--    （两者子 path 相同会导致 RuoYi 前端动态路由 route name 撞车，门户用户菜单点击无法跳转）
-- =============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '门户用户', @cms_parent_id, 1, 'portal-user', 'cms/user/index', NULL, 1, 0, 'C', '0', '0', 'cms:user:list', 'user', 'admin', NOW(), '门户用户管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:user:list');
SELECT @user_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:user:list' LIMIT 1;
-- 修复历史 path（若已存在但 path 不是 portal-user，统一修正，避免与系统用户菜单 path=user 冲突）
UPDATE sys_menu SET path = 'portal-user', update_by = 'admin', update_time = NOW()
WHERE perms = 'cms:user:list' AND path != 'portal-user';

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '用户查询', @user_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:user:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:user:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '用户新增', @user_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:user:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:user:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '用户修改', @user_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:user:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:user:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '用户删除', @user_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:user:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:user:remove');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '用户状态', @user_menu_id, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:user:status', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:user:status');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '重置密码', @user_menu_id, 6, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:user:resetPwd', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:user:resetPwd');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '绑定系统用户', @user_menu_id, 7, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:user:bind', '#', 'admin', NOW(), '身份桥接：绑定/解绑后台系统用户'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:user:bind');

-- =============================================================================
-- 三、文章管理（cms:article）
-- =============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文章管理', @cms_parent_id, 2, 'article', 'cms/article/index', NULL, 1, 0, 'C', '0', '0', 'cms:article:list', 'edit', 'admin', NOW(), '文章管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:article:list');
SELECT @article_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:article:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文章查询', @article_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:article:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:article:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文章新增', @article_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:article:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:article:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文章修改', @article_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:article:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:article:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文章删除', @article_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:article:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:article:remove');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文章审核', @article_menu_id, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:article:audit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:article:audit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文章上架', @article_menu_id, 6, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:article:publish', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:article:publish');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文章推荐', @article_menu_id, 7, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:article:featured', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:article:featured');

-- =============================================================================
-- 四、分类管理（cms:category）
--    path=/category 绝对路径，避免前端拼接父 path 后 /cms/cms/category 404（来自 49 修复）
-- =============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '分类管理', @cms_parent_id, 3, '/category', 'cms/category/index', NULL, 1, 0, 'C', '0', '0', 'cms:category:list', 'tree', 'admin', NOW(), '分类管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:category:list');
SELECT @category_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:category:list' LIMIT 1;
-- 修复历史 path（若已存在但 path 不是 /category，统一修正）
UPDATE sys_menu SET path = '/category', update_by = 'admin', update_time = NOW()
WHERE menu_name = '分类管理' AND perms = 'cms:category:list' AND path != '/category';

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '分类查询', @category_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:category:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:category:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '分类新增', @category_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:category:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:category:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '分类修改', @category_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:category:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:category:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '分类删除', @category_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:category:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:category:remove');

-- =============================================================================
-- 五、标签管理（cms:tag）
-- =============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '标签管理', @cms_parent_id, 4, 'tag', 'cms/tag/index', NULL, 1, 0, 'C', '0', '0', 'cms:tag:list', 'tab', 'admin', NOW(), '标签管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:tag:list');
SELECT @tag_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:tag:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '标签查询', @tag_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:tag:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:tag:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '标签新增', @tag_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:tag:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:tag:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '标签修改', @tag_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:tag:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:tag:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '标签删除', @tag_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:tag:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:tag:remove');

-- =============================================================================
-- 六、评论管理（cms:comment）
-- =============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '评论管理', @cms_parent_id, 5, 'comment', 'cms/comment/index', NULL, 1, 0, 'C', '0', '0', 'cms:comment:list', 'message', 'admin', NOW(), '评论管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:comment:list');
SELECT @comment_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:comment:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '评论查询', @comment_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:comment:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:comment:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '评论审核', @comment_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:comment:audit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:comment:audit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '评论删除', @comment_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:comment:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:comment:remove');

-- v6.2 通知管理已移出 CMS，归属"系统管理"目录，权限码 system:notification:*，见 93_菜单权限_消息中心.sql
-- 原 cms:notification:* 菜单不再在此创建，避免与系统管理菜单重复

-- =============================================================================
-- 八、友情链接（cms:friend-link）
-- =============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '友情链接', @cms_parent_id, 7, 'friend-link', 'cms/friend-link/index', NULL, 1, 0, 'C', '0', '0', 'cms:friend-link:list', 'link', 'admin', NOW(), '友情链接管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:friend-link:list');
SELECT @friend_link_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:friend-link:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '友情链接查询', @friend_link_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:friend-link:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:friend-link:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '友情链接新增', @friend_link_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:friend-link:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:friend-link:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '友情链接修改', @friend_link_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:friend-link:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:friend-link:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '友情链接删除', @friend_link_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:friend-link:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:friend-link:remove');

-- =============================================================================
-- 九、帮助分类（cms:help-category）  来自 38
-- =============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '帮助分类', @cms_parent_id, 8, 'help-category', 'cms/help-category/index', NULL, 1, 0, 'C', '0', '0', 'cms:help-category:list', 'tree', 'admin', NOW(), '帮助中心分类管理'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:help-category:list');
SELECT @help_category_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:help-category:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '分类查询', @help_category_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:help-category:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:help-category:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '分类新增', @help_category_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:help-category:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:help-category:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '分类修改', @help_category_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:help-category:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:help-category:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '分类删除', @help_category_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:help-category:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:help-category:remove');

-- =============================================================================
-- 十、帮助文章（cms:help-article）  来自 38
-- =============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '帮助文章', @cms_parent_id, 9, 'help-article', 'cms/help-article/index', NULL, 1, 0, 'C', '0', '0', 'cms:help-article:list', 'documentation', 'admin', NOW(), '帮助中心文章管理'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:help-article:list');
SELECT @help_article_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:help-article:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文章查询', @help_article_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:help-article:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:help-article:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文章新增', @help_article_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:help-article:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:help-article:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文章修改', @help_article_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:help-article:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:help-article:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文章删除', @help_article_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:help-article:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:help-article:remove');

-- =============================================================================
-- 十一、举报管理（cms:report）  来自 38
-- =============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '举报管理', @cms_parent_id, 10, 'report', 'cms/report/index', NULL, 1, 0, 'C', '0', '0', 'cms:report:list', 'warning', 'admin', NOW(), '用户举报记录管理'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:report:list');
SELECT @report_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:report:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '举报查询', @report_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:report:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:report:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '处理举报', @report_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:report:handle', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:report:handle');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '删除举报', @report_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:report:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:report:remove');

-- =============================================================================
-- 十二、反馈管理（cms:feedback）  来自 38
-- =============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '反馈管理', @cms_parent_id, 11, 'feedback', 'cms/feedback/index', NULL, 1, 0, 'C', '0', '0', 'cms:feedback:list', 'message', 'admin', NOW(), '用户意见反馈管理'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:feedback:list');
SELECT @feedback_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:feedback:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '反馈查询', @feedback_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:feedback:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:feedback:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '处理反馈', @feedback_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:feedback:handle', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:feedback:handle');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '删除反馈', @feedback_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:feedback:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:feedback:remove');

-- =============================================================================
-- 十三、专栏管理（portal:column）  来自 78  挂内容管理下
-- =============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '专栏管理', @cms_parent_id, 12, 'column', 'cms/column/index', NULL, 1, 0, 'C', '0', '0', 'portal:column:list', 'documentation', 'admin', NOW(), '专栏后台管理菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:column:list');
SELECT @column_menu_id := menu_id FROM sys_menu WHERE perms = 'portal:column:list' LIMIT 1;

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

-- =============================================================================
-- 十四、打赏管理（portal:tip）  来自 79  已下线 visible=1 隐藏
-- =============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '打赏管理', @cms_parent_id, 13, 'tip', 'cms/tip/index', NULL, 1, 0, 'C', '1', '0', 'portal:tip:list', 'money', 'admin', NOW(), '【已下线】前台打赏功能移除，菜单隐藏保留以兼容历史数据'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:tip:list');
SELECT @tip_menu_id := menu_id FROM sys_menu WHERE perms = 'portal:tip:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '打赏查询', @tip_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '1', '0', 'portal:tip:query', '#', 'admin', NOW(), '【已下线】'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:tip:query');

-- =============================================================================
-- 十五、独立一级目录：创作者认证（certification）  来自 63
-- =============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '创作者认证', 0, 14, 'certification', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'user', 'admin', NOW(), '创作者认证目录'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name = '创作者认证' AND parent_id = 0);
SELECT @cert_menu_id := menu_id FROM sys_menu WHERE menu_name = '创作者认证' AND parent_id = 0 LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '认证审核', @cert_menu_id, 1, 'audit', 'cms/certification/index', NULL, 1, 0, 'C', '0', '0', 'cms:certification:audit', 'edit', 'admin', NOW(), '创作者认证审核菜单'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:certification:audit');
SELECT @cert_audit_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:certification:audit' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '认证查询', @cert_audit_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:certification:list', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:certification:list');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '认证审核', @cert_audit_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:certification:audit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:certification:audit' AND menu_type = 'F');

-- =============================================================================
-- 十六、独立一级目录：财务（finance）  来自 80  已下线 visible=1 隐藏
-- =============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '财务', 0, 20, 'finance', NULL, NULL, 1, 0, 'M', '1', '0', NULL, 'money', 'admin', NOW(), '【已下线】财务目录，前台消费记录入口已移除'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name = '财务' AND parent_id = 0);
SELECT @finance_menu_id := menu_id FROM sys_menu WHERE menu_name = '财务' AND parent_id = 0 LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '付费订单', @finance_menu_id, 1, 'order', 'cms/order/index', NULL, 1, 0, 'C', '1', '0', 'portal:order:list', 'shopping', 'admin', NOW(), '【已下线】前台消费记录入口移除，菜单隐藏保留以兼容历史数据'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:order:list');
SELECT @order_menu_id := menu_id FROM sys_menu WHERE perms = 'portal:order:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '订单查询', @order_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '1', '0', 'portal:order:query', '#', 'admin', NOW(), '【已下线】'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:order:query');

-- =============================================================================
