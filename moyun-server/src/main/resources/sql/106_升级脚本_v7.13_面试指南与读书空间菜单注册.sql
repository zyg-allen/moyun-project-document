-- ====================================================================
-- v7.13 升级脚本：注册「面试指南」与「读书空间」菜单（历史遗漏补注册）
-- 适配 MySQL 8.x
-- 说明：本脚本幂等（FROM DUAL WHERE NOT EXISTS / IFNULL 兜底）
--
-- 背景：面试指南（cms/interview）与读书空间（portal/book*）后端 Controller、
--       前端页面均已完整实现，但 sys_menu 从未注册 → 后台管理员无法通过菜单
--       进入管理页面，属于功能可访问性阻塞问题。
--
-- 注册清单（按方案：保持两个独立一级目录，内部合并为 Tab 容器）：
--   ┌─ 面试指南（interview，一级目录，order=15）
--   │   ├─ 1.1 题库资源（Tab 容器：题目/分类/公司/简历）
--   │   ├─ 1.2 面经运营（Tab 容器：面经/评论，含审核）
--   │   └─ 1.3 精选笔记（采纳/取消）
--   │   配套按钮权限：cms:interview:query/add/edit/remove
--   │
--   └─ 读书空间（book，一级目录，order=16）
--       ├─ 2.1 书籍管理（主入口，章节管理为隐藏子路由）
--       ├─ 2.2 书单&推荐位（Tab 容器：书单/推荐位）
--       ├─ 2.3 用户内容（Tab 容器：金句摘录/书架）
--       └─ 2.4 学习辅助（Tab 容器：学习计划/错题本，已存在）
--       配套按钮权限：各子模块独立 *:query/add/edit/remove
--
-- 注：Tab 容器前端页面在后续低优任务中实现；现阶段先注册「C 菜单」
--     指向现有单模块页面，保证后台可进入。
-- ====================================================================

-- --------------------------------------------------------------
-- 0. 变量准备：获取顶级父目录
-- --------------------------------------------------------------
-- 面试指南、读书空间均为一级目录（parent_id=0）
SET @root_parent_id := 0;

-- --------------------------------------------------------------
-- 1. 面试指南（一级目录 M + 3 个 C 菜单 + 按钮权限）
-- --------------------------------------------------------------

-- 1.1 顶级目录：面试指南
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '面试指南', @root_parent_id, 15, 'interview', '', 1, 0, 'M', '0', '0', NULL, 'guide', 'admin', NOW(), '面试指南一级目录：题库/面经/简历/笔记'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = 'interview' AND parent_id = 0 AND menu_type = 'M');
-- 获取面试指南顶级目录 menu_id
SELECT @interview_mid := menu_id FROM sys_menu WHERE path = 'interview' AND parent_id = 0 AND menu_type = 'M' LIMIT 1;
SET @interview_mid := IFNULL(@interview_mid, 0);

-- 1.1.1 题库资源（C 菜单：当前指向 question/index，Tab 容器实现后改 component）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '题库资源', @interview_mid, 1, 'question', 'cms/interview/question/index', 1, 0, 'C', '0', '0', 'cms:interview:list', 'tree', 'admin', NOW(), '题目/分类/公司/简历 资源管理（Tab 容器待实现）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:interview:list' AND parent_id = @interview_mid);
UPDATE sys_menu SET parent_id = @interview_mid, path = 'question', component = 'cms/interview/question/index', update_by = 'admin', update_time = NOW()
 WHERE perms = 'cms:interview:list' AND parent_id IS NULL;
SELECT @interview_question_mid := menu_id FROM sys_menu WHERE perms = 'cms:interview:list' LIMIT 1;
SET @interview_question_mid := IFNULL(@interview_question_mid, @interview_mid);

-- 1.1.2 面经运营（C 菜单：指向 experience/index，含审核）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '面经运营', @interview_mid, 2, 'experience', 'cms/interview/experience/index', 1, 0, 'C', '0', '0', 'cms:interview:experience:list', 'edit', 'admin', NOW(), '面经审核与评论审核（Tab 容器待实现，复用 AuditWorkbench）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:interview:experience:list');
UPDATE sys_menu SET parent_id = @interview_mid, path = 'experience', component = 'cms/interview/experience/index', update_by = 'admin', update_time = NOW()
 WHERE perms = 'cms:interview:experience:list' AND parent_id IS NULL;
SELECT @interview_exp_mid := menu_id FROM sys_menu WHERE perms = 'cms:interview:experience:list' LIMIT 1;
SET @interview_exp_mid := IFNULL(@interview_exp_mid, @interview_mid);

-- 1.1.3 精选笔记（C 菜单：submission）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '精选笔记', @interview_mid, 3, 'submission', 'cms/interview/submission/index', 1, 0, 'C', '0', '0', 'cms:interview:submission:list', 'star', 'admin', NOW(), '精选笔记采纳与取消'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:interview:submission:list');
UPDATE sys_menu SET parent_id = @interview_mid, path = 'submission', component = 'cms/interview/submission/index', update_by = 'admin', update_time = NOW()
 WHERE perms = 'cms:interview:submission:list' AND parent_id IS NULL;

-- 1.2 面试指南按钮权限（F 类型，统一挂在「题库资源」下，因该模块所有子页面共用权限码）
--     注意：原项目全模块共用 cms:interview:*，此处不拆分保持一致；
--           若后续需子模块级控制，可在此基础上新增 cms:interview:experience:audit 等。
-- 注意：先用 perms='cms:interview:list' 拿到正确的父菜单 ID
SELECT @btn_parent_id := menu_id FROM sys_menu WHERE perms = 'cms:interview:list' LIMIT 1;
SET @btn_parent_id := IFNULL(@btn_parent_id, @interview_mid);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '面试查询', @btn_parent_id, 1, '#', '', 1, 0, 'F', '0', '0', 'cms:interview:query', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:interview:query');
UPDATE sys_menu SET parent_id = @btn_parent_id WHERE perms = 'cms:interview:query' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '面试新增', @btn_parent_id, 2, '#', '', 1, 0, 'F', '0', '0', 'cms:interview:add', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:interview:add');
UPDATE sys_menu SET parent_id = @btn_parent_id WHERE perms = 'cms:interview:add' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '面试修改', @btn_parent_id, 3, '#', '', 1, 0, 'F', '0', '0', 'cms:interview:edit', '#', 'admin', NOW(), '含：审核/置顶/精选采纳等运营操作'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:interview:edit');
UPDATE sys_menu SET parent_id = @btn_parent_id WHERE perms = 'cms:interview:edit' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '面试删除', @btn_parent_id, 4, '#', '', 1, 0, 'F', '0', '0', 'cms:interview:remove', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:interview:remove');
UPDATE sys_menu SET parent_id = @btn_parent_id WHERE perms = 'cms:interview:remove' AND parent_id IS NULL;

-- --------------------------------------------------------------
-- 2. 读书空间（一级目录 M + 4 个 C 菜单 + 按钮权限）
-- --------------------------------------------------------------

-- 2.1 顶级目录：读书空间
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '读书空间', @root_parent_id, 16, 'book', '', 1, 0, 'M', '0', '0', NULL, 'book', 'admin', NOW(), '读书空间一级目录：书籍/章节/书单/摘录/书架'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = 'book' AND parent_id = 0 AND menu_type = 'M');
SELECT @book_mid := menu_id FROM sys_menu WHERE path = 'book' AND parent_id = 0 AND menu_type = 'M' LIMIT 1;
SET @book_mid := IFNULL(@book_mid, 0);

-- 2.1.1 书籍管理（C 菜单，主入口）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '书籍管理', @book_mid, 1, 'book-index', 'portal/book/index', 1, 0, 'C', '0', '0', 'portal:book:list', 'documentation', 'admin', NOW(), '书籍CRUD + 章节导入向导（章节管理为隐藏子路由）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:book:list');
UPDATE sys_menu SET parent_id = @book_mid, path = 'book-index', component = 'portal/book/index', update_by = 'admin', update_time = NOW()
 WHERE perms = 'portal:book:list' AND parent_id IS NULL;
SELECT @book_list_mid := menu_id FROM sys_menu WHERE perms = 'portal:book:list' LIMIT 1;
SET @book_list_mid := IFNULL(@book_list_mid, @book_mid);

-- 2.1.1.1 章节管理（C 菜单，隐藏 visible=1，作为隐藏子路由，activeMenu 指向书籍管理）
--           注意：前端 router 已静态注册隐藏路由，此处同步注册 sys_menu 供动态路由解析
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '章节管理', @book_mid, 2, 'bookChapter', 'portal/bookChapter/index', 1, 0, 'C', '1', '0', 'portal:bookChapter:list', '#', 'admin', NOW(), '书籍章节CRUD + 发布/批量导入（隐藏菜单，从书籍详情跳转）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookChapter:list');
UPDATE sys_menu SET parent_id = @book_mid, path = 'bookChapter', component = 'portal/bookChapter/index', visible = '1', update_by = 'admin', update_time = NOW()
 WHERE perms = 'portal:bookChapter:list' AND parent_id IS NULL;

-- 2.1.2 书单&推荐位（C 菜单：当前指向 bookList/index，Tab 容器待实现）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '书单&推荐位', @book_mid, 3, 'bookList', 'portal/bookList/index', 1, 0, 'C', '0', '0', 'portal:bookList:list', 'list', 'admin', NOW(), '书单管理 + 推荐位（Tab 容器待实现）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookList:list');
UPDATE sys_menu SET parent_id = @book_mid, path = 'bookList', component = 'portal/bookList/index', update_by = 'admin', update_time = NOW()
 WHERE perms = 'portal:bookList:list' AND parent_id IS NULL;
SELECT @booklist_mid := menu_id FROM sys_menu WHERE perms = 'portal:bookList:list' LIMIT 1;
SET @booklist_mid := IFNULL(@booklist_mid, @book_mid);

-- 2.1.2.1 推荐位管理（C 菜单，隐藏 visible=1，Tab 容器实现后从书单&推荐位进入）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '推荐位管理', @book_mid, 4, 'bookRecommend', 'portal/bookRecommend/index', 1, 0, 'C', '1', '0', 'portal:bookRecommend:list', '#', 'admin', NOW(), '书籍推荐位上下架/排序（隐藏，从书单&推荐位 Tab 进入）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookRecommend:list');
UPDATE sys_menu SET parent_id = @book_mid, path = 'bookRecommend', component = 'portal/bookRecommend/index', visible = '1', update_by = 'admin', update_time = NOW()
 WHERE perms = 'portal:bookRecommend:list' AND parent_id IS NULL;

-- 2.1.3 用户内容（C 菜单：当前指向 bookQuote/index，Tab 容器待实现）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '用户内容', @book_mid, 5, 'bookQuote', 'portal/bookQuote/index', 1, 0, 'C', '0', '0', 'portal:bookQuote:list', 'peoples', 'admin', NOW(), '金句摘录 + 书架（Tab 容器待实现）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookQuote:list');
UPDATE sys_menu SET parent_id = @book_mid, path = 'bookQuote', component = 'portal/bookQuote/index', update_by = 'admin', update_time = NOW()
 WHERE perms = 'portal:bookQuote:list' AND parent_id IS NULL;
SELECT @bookquote_mid := menu_id FROM sys_menu WHERE perms = 'portal:bookQuote:list' LIMIT 1;
SET @bookquote_mid := IFNULL(@bookquote_mid, @book_mid);

-- 2.1.3.1 书架管理（C 菜单，隐藏 visible=1，Tab 容器实现后从用户内容 Tab 进入）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '书架管理', @book_mid, 6, 'bookshelf', 'portal/bookshelf/index', 1, 0, 'C', '1', '0', 'portal:bookshelf:list', '#', 'admin', NOW(), '用户书架查看/移除（隐藏，从用户内容 Tab 进入）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookshelf:list');
UPDATE sys_menu SET parent_id = @book_mid, path = 'bookshelf', component = 'portal/bookshelf/index', visible = '1', update_by = 'admin', update_time = NOW()
 WHERE perms = 'portal:bookshelf:list' AND parent_id IS NULL;

-- 2.1.4 学习辅助（C 菜单：learn-aux Tab 容器已存在，直接指向）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '学习辅助', @book_mid, 7, 'learn-aux', 'portal/learn-aux/index', 1, 0, 'C', '0', '0', 'portal:learn:list', 'skill', 'admin', NOW(), '学习计划 + 错题本（Tab 容器已存在）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:learn:list');
UPDATE sys_menu SET parent_id = @book_mid, path = 'learn-aux', component = 'portal/learn-aux/index', update_by = 'admin', update_time = NOW()
 WHERE perms = 'portal:learn:list' AND parent_id IS NULL;

-- 2.2 读书空间按钮权限（F 类型，按子模块挂在对应 C 菜单下）
-- 2.2.1 书籍管理按钮权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '书籍查询', @book_list_mid, 1, '#', '', 1, 0, 'F', '0', '0', 'portal:book:query', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:book:query');
UPDATE sys_menu SET parent_id = @book_list_mid WHERE perms = 'portal:book:query' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '书籍新增', @book_list_mid, 2, '#', '', 1, 0, 'F', '0', '0', 'portal:book:add', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:book:add');
UPDATE sys_menu SET parent_id = @book_list_mid WHERE perms = 'portal:book:add' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '书籍修改', @book_list_mid, 3, '#', '', 1, 0, 'F', '0', '0', 'portal:book:edit', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:book:edit');
UPDATE sys_menu SET parent_id = @book_list_mid WHERE perms = 'portal:book:edit' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '书籍删除', @book_list_mid, 4, '#', '', 1, 0, 'F', '0', '0', 'portal:book:remove', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:book:remove');
UPDATE sys_menu SET parent_id = @book_list_mid WHERE perms = 'portal:book:remove' AND parent_id IS NULL;

-- 2.2.2 章节管理按钮权限（含 publish 发布）
SELECT @bookchapter_mid := menu_id FROM sys_menu WHERE perms = 'portal:bookChapter:list' LIMIT 1;
SET @bookchapter_mid := IFNULL(@bookchapter_mid, @book_mid);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '章节查询', @bookchapter_mid, 1, '#', '', 1, 0, 'F', '0', '0', 'portal:bookChapter:query', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookChapter:query');
UPDATE sys_menu SET parent_id = @bookchapter_mid WHERE perms = 'portal:bookChapter:query' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '章节新增', @bookchapter_mid, 2, '#', '', 1, 0, 'F', '0', '0', 'portal:bookChapter:add', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookChapter:add');
UPDATE sys_menu SET parent_id = @bookchapter_mid WHERE perms = 'portal:bookChapter:add' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '章节修改', @bookchapter_mid, 3, '#', '', 1, 0, 'F', '0', '0', 'portal:bookChapter:edit', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookChapter:edit');
UPDATE sys_menu SET parent_id = @bookchapter_mid WHERE perms = 'portal:bookChapter:edit' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '章节删除', @bookchapter_mid, 4, '#', '', 1, 0, 'F', '0', '0', 'portal:bookChapter:remove', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookChapter:remove');
UPDATE sys_menu SET parent_id = @bookchapter_mid WHERE perms = 'portal:bookChapter:remove' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '章节发布', @bookchapter_mid, 5, '#', '', 1, 0, 'F', '0', '0', 'portal:bookChapter:publish', '#', 'admin', NOW(), '章节发布/撤回'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookChapter:publish');
UPDATE sys_menu SET parent_id = @bookchapter_mid WHERE perms = 'portal:bookChapter:publish' AND parent_id IS NULL;

-- 2.2.3 书单管理按钮权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '书单查询', @booklist_mid, 1, '#', '', 1, 0, 'F', '0', '0', 'portal:bookList:query', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookList:query');
UPDATE sys_menu SET parent_id = @booklist_mid WHERE perms = 'portal:bookList:query' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '书单新增', @booklist_mid, 2, '#', '', 1, 0, 'F', '0', '0', 'portal:bookList:add', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookList:add');
UPDATE sys_menu SET parent_id = @booklist_mid WHERE perms = 'portal:bookList:add' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '书单修改', @booklist_mid, 3, '#', '', 1, 0, 'F', '0', '0', 'portal:bookList:edit', '#', 'admin', NOW(), '含：管理书籍（增删排序）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookList:edit');
UPDATE sys_menu SET parent_id = @booklist_mid WHERE perms = 'portal:bookList:edit' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '书单删除', @booklist_mid, 4, '#', '', 1, 0, 'F', '0', '0', 'portal:bookList:remove', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookList:remove');
UPDATE sys_menu SET parent_id = @booklist_mid WHERE perms = 'portal:bookList:remove' AND parent_id IS NULL;

-- 2.2.4 推荐位管理按钮权限
SELECT @bookrecommend_mid := menu_id FROM sys_menu WHERE perms = 'portal:bookRecommend:list' LIMIT 1;
SET @bookrecommend_mid := IFNULL(@bookrecommend_mid, @book_mid);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '推荐位查询', @bookrecommend_mid, 1, '#', '', 1, 0, 'F', '0', '0', 'portal:bookRecommend:query', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookRecommend:query');
UPDATE sys_menu SET parent_id = @bookrecommend_mid WHERE perms = 'portal:bookRecommend:query' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '推荐位新增', @bookrecommend_mid, 2, '#', '', 1, 0, 'F', '0', '0', 'portal:bookRecommend:add', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookRecommend:add');
UPDATE sys_menu SET parent_id = @bookrecommend_mid WHERE perms = 'portal:bookRecommend:add' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '推荐位修改', @bookrecommend_mid, 3, '#', '', 1, 0, 'F', '0', '0', 'portal:bookRecommend:edit', '#', 'admin', NOW(), '含：上下架/排序'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookRecommend:edit');
UPDATE sys_menu SET parent_id = @bookrecommend_mid WHERE perms = 'portal:bookRecommend:edit' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '推荐位删除', @bookrecommend_mid, 4, '#', '', 1, 0, 'F', '0', '0', 'portal:bookRecommend:remove', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookRecommend:remove');
UPDATE sys_menu SET parent_id = @bookrecommend_mid WHERE perms = 'portal:bookRecommend:remove' AND parent_id IS NULL;

-- 2.2.5 金句摘录按钮权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '金句查询', @bookquote_mid, 1, '#', '', 1, 0, 'F', '0', '0', 'portal:bookQuote:query', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookQuote:query');
UPDATE sys_menu SET parent_id = @bookquote_mid WHERE perms = 'portal:bookQuote:query' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '金句新增', @bookquote_mid, 2, '#', '', 1, 0, 'F', '0', '0', 'portal:bookQuote:add', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookQuote:add');
UPDATE sys_menu SET parent_id = @bookquote_mid WHERE perms = 'portal:bookQuote:add' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '金句修改', @bookquote_mid, 3, '#', '', 1, 0, 'F', '0', '0', 'portal:bookQuote:edit', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookQuote:edit');
UPDATE sys_menu SET parent_id = @bookquote_mid WHERE perms = 'portal:bookQuote:edit' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '金句删除', @bookquote_mid, 4, '#', '', 1, 0, 'F', '0', '0', 'portal:bookQuote:remove', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookQuote:remove');
UPDATE sys_menu SET parent_id = @bookquote_mid WHERE perms = 'portal:bookQuote:remove' AND parent_id IS NULL;

-- 2.2.6 书架按钮权限（仅 remove，无 add/edit）
SELECT @bookshelf_mid := menu_id FROM sys_menu WHERE perms = 'portal:bookshelf:list' LIMIT 1;
SET @bookshelf_mid := IFNULL(@bookshelf_mid, @book_mid);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '书架移除', @bookshelf_mid, 1, '#', '', 1, 0, 'F', '0', '0', 'portal:bookshelf:remove', '#', 'admin', NOW(), '移出书架'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:bookshelf:remove');
UPDATE sys_menu SET parent_id = @bookshelf_mid WHERE perms = 'portal:bookshelf:remove' AND parent_id IS NULL;

-- --------------------------------------------------------------
-- 3. 角色菜单关联（为 role_id=1 管理员补全）
--    参照 93 / 96 / 99 范式，让角色分配界面观感一致，
--    以及让非 admin 角色可通过角色分配获得菜单权限。
-- --------------------------------------------------------------
INSERT INTO sys_role_menu (role_id, menu_id, create_by, create_time)
SELECT 1, m.menu_id, 'admin', NOW()
FROM sys_menu m
WHERE m.perms IN (
    -- 面试指南
    'cms:interview:list',
    'cms:interview:experience:list',
    'cms:interview:submission:list',
    -- 面试按钮
    'cms:interview:query', 'cms:interview:add', 'cms:interview:edit', 'cms:interview:remove',
    -- 读书空间
    'portal:book:list',
    'portal:bookChapter:list',
    'portal:bookList:list',
    'portal:bookRecommend:list',
    'portal:bookQuote:list',
    'portal:bookshelf:list',
    'portal:learn:list',
    -- 书籍按钮
    'portal:book:query', 'portal:book:add', 'portal:book:edit', 'portal:book:remove',
    -- 章节按钮
    'portal:bookChapter:query', 'portal:bookChapter:add', 'portal:bookChapter:edit', 'portal:bookChapter:remove', 'portal:bookChapter:publish',
    -- 书单按钮
    'portal:bookList:query', 'portal:bookList:add', 'portal:bookList:edit', 'portal:bookList:remove',
    -- 推荐位按钮
    'portal:bookRecommend:query', 'portal:bookRecommend:add', 'portal:bookRecommend:edit', 'portal:bookRecommend:remove',
    -- 金句按钮
    'portal:bookQuote:query', 'portal:bookQuote:add', 'portal:bookQuote:edit', 'portal:bookQuote:remove',
    -- 书架按钮
    'portal:bookshelf:remove'
)
AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id
);

-- ====================================================================
-- 升级完成
-- ====================================================================
