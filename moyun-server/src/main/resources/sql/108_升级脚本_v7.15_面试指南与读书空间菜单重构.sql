-- ====================================================================
-- v7.15 升级脚本：面试指南 & 读书空间 菜单重构（Tab 容器化 + 审核入口对齐）
-- 适配 MySQL 8.x
-- 性质：重构脚本（先删后建），建议仅执行一次；重复执行不会报错但 menu_id 会变化
-- 说明：
--   1. 删除 106 号脚本注册的旧菜单（C 菜单 + F 按钮 + 隐藏子菜单），重新注册
--   2. C 菜单组件路径指向 Tab 容器页面（questionTab / experienceTab / bookListTab / userContent）
--   3. Tab 容器页面嵌入原有单模块页面，实现「减少可见菜单数量 + 解决无入口页面」
--   4. 审核类 Tab（面经审核/评论审核）已整合到「内容审核中心」，此处仅注册管理菜单
--   5. 子模块级 F 按钮权限按 C 菜单分组注册，含 Tab 内嵌面板的 list 权限码
--   6. 角色关联自动补全到 role_id=1（管理员）
--
-- 前台栏目 → 后台菜单 对照：
--   【读书空间】
--     读书首页   → 书籍管理 + 书单&推荐位 + 用户内容（数据聚合）
--     发现好书   → 书籍管理（推荐位+书单支撑）
--     金句摘录   → 用户内容 → 金句摘录 Tab
--     我的书架   → 用户内容 → 书架管理 Tab
--   【面试指南】
--     面试题库   → 题库资源（题库+分类+公司+简历 Tab）
--     面试经验   → 面经运营（面经+评论 Tab）
--     简历模板   → 题库资源 → 简历模板 Tab
--     AI 模拟面试 → 前台用户功能，无后台管理页
--     学习中心   → 学习辅助（学习计划+错题本 Tab）
--     知识图谱   → 统计聚合数据，由内容管理支撑
--     刷题排行榜 → 统计聚合数据，由内容管理支撑
--     学习计划   → 学习辅助 → 学习计划 Tab
--     错题本     → 学习辅助 → 错题本 Tab
--     刷题日历   → 统计聚合数据，由内容管理支撑
-- ====================================================================

SET @db := DATABASE();

-- --------------------------------------------------------------
-- 0. 清理旧菜单（106 号脚本注册的所有面试指南/读书空间菜单）
--    按子树删除：先删子菜单，再删父菜单
-- --------------------------------------------------------------

-- 0.1 获取面试指南/读书空间顶级目录 ID
SELECT @old_interview_mid := menu_id FROM sys_menu WHERE path = 'interview' AND parent_id = 0 AND menu_type = 'M' LIMIT 1;
SELECT @old_book_mid := menu_id FROM sys_menu WHERE path = 'book' AND parent_id = 0 AND menu_type = 'M' LIMIT 1;

-- 0.2 删除角色关联
DELETE FROM sys_role_menu WHERE menu_id IN (
  SELECT menu_id FROM sys_menu WHERE parent_id IN (@old_interview_mid, @old_book_mid)
  AND menu_type IN ('C', 'F')
);
DELETE FROM sys_role_menu WHERE menu_id IN (@old_interview_mid, @old_book_mid);

-- 0.3 删除面试指南子菜单（C + F）
DELETE FROM sys_menu WHERE parent_id = @old_interview_mid AND menu_type IN ('C', 'F');

-- 0.4 删除读书空间子菜单（C + F）
DELETE FROM sys_menu WHERE parent_id = @old_book_mid AND menu_type IN ('C', 'F');

-- 0.5 删除顶级目录（如果存在）
DELETE FROM sys_menu WHERE menu_id = @old_interview_mid;
DELETE FROM sys_menu WHERE menu_id = @old_book_mid;

-- --------------------------------------------------------------
-- 1. 面试指南（重新注册：1 M + 3 C + 4 组 F 按钮）
-- --------------------------------------------------------------

-- 1.1 顶级目录
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES ('面试指南', 0, 15, 'interview', '', 1, 0, 'M', '0', '0', NULL, 'guide', 'admin', NOW(), '面试指南一级目录：题库/面经/简历/笔记');
SELECT @interview_mid := LAST_INSERT_ID();

-- 1.2.1 题库资源（C：Tab 容器 4 个 Tab，含简历模板/分类/公司/题库）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES ('题库资源', @interview_mid, 1, 'questionTab', 'cms/interview/questionTab/index', 1, 0, 'C', '0', '0', 'cms:interview:list', 'tree-table', 'admin', NOW(), '面试题库+分类+公司标签+简历模板 Tab 容器');
SELECT @question_tab_mid := LAST_INSERT_ID();

-- 1.2.2 面经运营（C：Tab 容器 2 个 Tab，含面经+评论；审核已整合到内容审核中心）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES ('面经运营', @interview_mid, 2, 'experienceTab', 'cms/interview/experienceTab/index', 1, 0, 'C', '0', '0', 'cms:interview:experience:list', 'edit', 'admin', NOW(), '面经管理+评论管理 Tab 容器；审核入口在内容审核中心');
SELECT @experience_tab_mid := LAST_INSERT_ID();

-- 1.2.3 精选笔记（C：独立页，采纳/取消精选）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES ('精选笔记', @interview_mid, 3, 'submission', 'cms/interview/submission/index', 1, 0, 'C', '0', '0', 'cms:interview:submission:list', 'star', 'admin', NOW(), '精选笔记采纳与取消');
SELECT @submission_mid := LAST_INSERT_ID();

-- 1.3 面试指南 F 按钮权限（按 C 菜单分组，保持 cms:interview:* 统一前缀）
-- 1.3.1 题库资源 按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES ('题库查询', @question_tab_mid, 1, '#', '', 1, 0, 'F', '0', '0', 'cms:interview:query', '#', 'admin', NOW(), NULL);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES ('题库新增', @question_tab_mid, 2, '#', '', 1, 0, 'F', '0', '0', 'cms:interview:add', '#', 'admin', NOW(), NULL);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES ('题库修改', @question_tab_mid, 3, '#', '', 1, 0, 'F', '0', '0', 'cms:interview:edit', '#', 'admin', NOW(), '含：审核/置顶/精选采纳等运营操作');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES ('题库删除', @question_tab_mid, 4, '#', '', 1, 0, 'F', '0', '0', 'cms:interview:remove', '#', 'admin', NOW(), NULL);

-- 1.3.2 面经运营 按钮（复用同一组权限码）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES ('面经查询', @experience_tab_mid, 1, '#', '', 1, 0, 'F', '0', '0', 'cms:interview:query', '#', 'admin', NOW(), NULL);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES ('面经修改', @experience_tab_mid, 2, '#', '', 1, 0, 'F', '0', '0', 'cms:interview:edit', '#', 'admin', NOW(), '含：审核/置顶/评论管理');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES ('面经删除', @experience_tab_mid, 3, '#', '', 1, 0, 'F', '0', '0', 'cms:interview:remove', '#', 'admin', NOW(), NULL);

-- 1.3.3 精选笔记 按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES ('笔记查询', @submission_mid, 1, '#', '', 1, 0, 'F', '0', '0', 'cms:interview:query', '#', 'admin', NOW(), NULL);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES ('笔记修改', @submission_mid, 2, '#', '', 1, 0, 'F', '0', '0', 'cms:interview:edit', '#', 'admin', NOW(), '含：采纳/取消精选');

-- --------------------------------------------------------------
-- 2. 读书空间（重新注册：1 M + 4 C + 多组 F 按钮）
-- --------------------------------------------------------------

-- 2.1 顶级目录
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES ('读书空间', 0, 16, 'book', '', 1, 0, 'M', '0', '0', NULL, 'book', 'admin', NOW(), '读书空间一级目录：书籍/书单/金句/学习');
SELECT @book_mid := LAST_INSERT_ID();

-- 2.2.1 书籍管理（C：独立页，含章节导入向导；章节管理由书籍详情跳转）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES ('书籍管理', @book_mid, 1, 'book-index', 'portal/book/index', 1, 0, 'C', '0', '0', 'portal:book:list', 'documentation', 'admin', NOW(), '书籍CRUD + 章节导入向导（章节管理为隐藏子路由）');
SELECT @book_mid2 := LAST_INSERT_ID();

-- 2.2.1.1 章节管理（隐藏 C 菜单，从书籍详情跳转进入，activeMenu 指向书籍管理）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES ('章节管理', @book_mid, 2, 'bookChapter', 'portal/bookChapter/index', 1, 0, 'C', '1', '0', 'portal:bookChapter:list', '#', 'admin', NOW(), '书籍章节CRUD + 发布/批量导入（隐藏菜单，从书籍详情跳转）');
SELECT @chapter_mid := LAST_INSERT_ID();

-- 2.2.2 书单&推荐位（C：Tab 容器 2 个 Tab，书单+推荐位）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES ('书单&推荐位', @book_mid, 3, 'bookListTab', 'portal/bookListTab/index', 1, 0, 'C', '0', '0', 'portal:bookList:list', 'list', 'admin', NOW(), '书单管理+推荐位管理 Tab 容器');
SELECT @booklist_tab_mid := LAST_INSERT_ID();

-- 2.2.3 用户内容（C：Tab 容器 2 个 Tab，金句+书架）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES ('用户内容', @book_mid, 4, 'userContent', 'portal/userContent/index', 1, 0, 'C', '0', '0', 'portal:bookQuote:list', 'peoples', 'admin', NOW(), '金句摘录+书架管理 Tab 容器');
SELECT @usercontent_mid := LAST_INSERT_ID();

-- 2.2.4 学习辅助（C：Tab 容器已存在，学习计划+错题本）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES ('学习辅助', @book_mid, 5, 'learn-aux', 'portal/learn-aux/index', 1, 0, 'C', '0', '0', 'portal:learn:list', 'skill', 'admin', NOW(), '学习计划+错题本 Tab 容器');
SELECT @learn_aux_mid := LAST_INSERT_ID();

-- 2.3 读书空间 F 按钮权限
-- 2.3.1 书籍管理
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES ('书籍查询', @book_mid2, 1, '#', '', 1, 0, 'F', '0', '0', 'portal:book:query', '#', 'admin', NOW(), NULL);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES ('书籍新增', @book_mid2, 2, '#', '', 1, 0, 'F', '0', '0', 'portal:book:add', '#', 'admin', NOW(), NULL);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES ('书籍修改', @book_mid2, 3, '#', '', 1, 0, 'F', '0', '0', 'portal:book:edit', '#', 'admin', NOW(), NULL);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES ('书籍删除', @book_mid2, 4, '#', '', 1, 0, 'F', '0', '0', 'portal:book:remove', '#', 'admin', NOW(), NULL);

-- 2.3.2 章节管理（含 publish）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES ('章节查询', @chapter_mid, 1, '#', '', 1, 0, 'F', '0', '0', 'portal:bookChapter:query', '#', 'admin', NOW(), NULL);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES ('章节新增', @chapter_mid, 2, '#', '', 1, 0, 'F', '0', '0', 'portal:bookChapter:add', '#', 'admin', NOW(), NULL);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES ('章节修改', @chapter_mid, 3, '#', '', 1, 0, 'F', '0', '0', 'portal:bookChapter:edit', '#', 'admin', NOW(), NULL);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES ('章节删除', @chapter_mid, 4, '#', '', 1, 0, 'F', '0', '0', 'portal:bookChapter:remove', '#', 'admin', NOW(), NULL);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES ('章节发布', @chapter_mid, 5, '#', '', 1, 0, 'F', '0', '0', 'portal:bookChapter:publish', '#', 'admin', NOW(), '章节发布/撤回');

-- 2.3.3 书单&推荐位（书单按钮 + 推荐位按钮，挂在同一个 C 菜单下）
--   注意：推荐位的 list 权限码必须注册，否则 Tab 内推荐位面板调 GET /list 接口会 403
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES ('书单查询', @booklist_tab_mid, 1, '#', '', 1, 0, 'F', '0', '0', 'portal:bookList:query', '#', 'admin', NOW(), NULL);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES ('书单新增', @booklist_tab_mid, 2, '#', '', 1, 0, 'F', '0', '0', 'portal:bookList:add', '#', 'admin', NOW(), NULL);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES ('书单修改', @booklist_tab_mid, 3, '#', '', 1, 0, 'F', '0', '0', 'portal:bookList:edit', '#', 'admin', NOW(), '含：管理书籍（增删排序）');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES ('书单删除', @booklist_tab_mid, 4, '#', '', 1, 0, 'F', '0', '0', 'portal:bookList:remove', '#', 'admin', NOW(), NULL);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES ('推荐位列表', @booklist_tab_mid, 5, '#', '', 1, 0, 'F', '0', '0', 'portal:bookRecommend:list', '#', 'admin', NOW(), 'Tab 内推荐位面板列表权限');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES ('推荐位查询', @booklist_tab_mid, 6, '#', '', 1, 0, 'F', '0', '0', 'portal:bookRecommend:query', '#', 'admin', NOW(), NULL);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES ('推荐位新增', @booklist_tab_mid, 7, '#', '', 1, 0, 'F', '0', '0', 'portal:bookRecommend:add', '#', 'admin', NOW(), NULL);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES ('推荐位修改', @booklist_tab_mid, 8, '#', '', 1, 0, 'F', '0', '0', 'portal:bookRecommend:edit', '#', 'admin', NOW(), '含：上下架/排序');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES ('推荐位删除', @booklist_tab_mid, 9, '#', '', 1, 0, 'F', '0', '0', 'portal:bookRecommend:remove', '#', 'admin', NOW(), NULL);

-- 2.3.4 用户内容（金句 + 书架按钮，挂在同一个 C 菜单下）
--   注意：书架的 list 权限码必须注册，否则 Tab 内书架面板调 GET /list 接口会 403
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES ('金句查询', @usercontent_mid, 1, '#', '', 1, 0, 'F', '0', '0', 'portal:bookQuote:query', '#', 'admin', NOW(), NULL);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES ('金句新增', @usercontent_mid, 2, '#', '', 1, 0, 'F', '0', '0', 'portal:bookQuote:add', '#', 'admin', NOW(), NULL);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES ('金句修改', @usercontent_mid, 3, '#', '', 1, 0, 'F', '0', '0', 'portal:bookQuote:edit', '#', 'admin', NOW(), NULL);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES ('金句删除', @usercontent_mid, 4, '#', '', 1, 0, 'F', '0', '0', 'portal:bookQuote:remove', '#', 'admin', NOW(), NULL);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES ('书架列表', @usercontent_mid, 5, '#', '', 1, 0, 'F', '0', '0', 'portal:bookshelf:list', '#', 'admin', NOW(), 'Tab 内书架面板列表权限');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES ('书架移除', @usercontent_mid, 6, '#', '', 1, 0, 'F', '0', '0', 'portal:bookshelf:remove', '#', 'admin', NOW(), '移出书架');

-- 2.3.5 学习辅助（Tab 容器内嵌入 studyPlan/wrongQuestion 面板，需额外权限）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES ('学习计划查询', @learn_aux_mid, 1, '#', '', 1, 0, 'F', '0', '0', 'portal:studyPlan:list', '#', 'admin', NOW(), '学习计划只读列表');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES ('错题本查询', @learn_aux_mid, 2, '#', '', 1, 0, 'F', '0', '0', 'portal:wrongQuestion:list', '#', 'admin', NOW(), '错题本只读列表');

-- --------------------------------------------------------------
-- 3. 角色菜单关联（为 role_id=1 管理员补全）
-- --------------------------------------------------------------
INSERT INTO sys_role_menu (role_id, menu_id, create_by, create_time)
SELECT 1, m.menu_id, 'admin', NOW()
FROM sys_menu m
WHERE m.perms IN (
    -- 面试指南 C 菜单
    'cms:interview:list',
    'cms:interview:experience:list',
    'cms:interview:submission:list',
    -- 面试指南 F 按钮
    'cms:interview:query', 'cms:interview:add', 'cms:interview:edit', 'cms:interview:remove',
    -- 读书空间 C 菜单
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
    -- 书单&推荐位按钮
    'portal:bookList:query', 'portal:bookList:add', 'portal:bookList:edit', 'portal:bookList:remove',
    'portal:bookRecommend:query', 'portal:bookRecommend:add', 'portal:bookRecommend:edit', 'portal:bookRecommend:remove',
    -- 用户内容按钮
    'portal:bookQuote:query', 'portal:bookQuote:add', 'portal:bookQuote:edit', 'portal:bookQuote:remove',
    'portal:bookshelf:remove',
    -- 学习辅助内嵌面板权限
    'portal:studyPlan:list',
    'portal:wrongQuestion:list'
)
AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id
);

-- 为 role_id=1 管理员补全顶级目录的关联
INSERT INTO sys_role_menu (role_id, menu_id, create_by, create_time)
SELECT 1, m.menu_id, 'admin', NOW()
FROM sys_menu m
WHERE m.menu_id IN (@interview_mid, @book_mid)
AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id
);

-- ====================================================================
-- 升级完成
-- 变更清单：
--   1. 面试指南：3 个可见菜单（题库资源/面经运营/精选笔记），
--      题库资源 = 题库+分类+公司+简历（4 Tab）
--      面经运营 = 面经+评论（2 Tab）
--   2. 读书空间：4 个可见菜单（书籍管理/书单&推荐位/用户内容/学习辅助），
--      书单&推荐位 = 书单+推荐位（2 Tab，名实一致）
--      用户内容 = 金句+书架（2 Tab，消除隐藏菜单）
--      章节管理保留为隐藏子路由（从书籍详情跳转）
--   3. 审核类 Tab 已在内容审核中心统一管理（面经审核/评论审核）
--      审核中心菜单由 103 号脚本管理，本脚本不重复注册
-- ====================================================================
