-- =============================================
-- 墨韵平台 - 分类导航字段初始化 v5.6
-- 执行顺序: 94
-- 描述:
--   1. 为 portal_category 表新增 4 个导航字段，支持后台动态配置头部栏目
--      - show_in_nav     : 是否在头部栏目展示（0否/1是）
--      - nav_route_type  : 路由类型（home/category/static/external）
--      - nav_route_path  : 静态/外链路径（仅 static/external 类型使用）
--      - requires_auth   : 是否需要登录（0否/1是）
--   2. 按最新栏目设置重建种子数据（8 个一级 + 42 个二级 = 50 条）
--   3. 配合前端 Navbar 改为读取 /portal/category/nav/tree 接口动态渲染
--
-- 路由类型说明（nav_route_type）:
--   home      : 首页，path 固定为 '/'
--   category  : 动态分类栏目，前端拼装为 /category/<encodeURIComponent(name)>
--   static    : 静态路由，path = nav_route_path（如 /reading/discover）
--   external  : 外部链接，path = nav_route_path，新窗口打开
--
-- 注意：此脚本会清空并重建 portal_category 表数据。
--       若 portal_article.category_id 引用了旧分类，执行后可能成为悬空引用，
--       请在执行前备份或在测试环境验证。
-- =============================================

-- ----------------------------
-- 1. 新增导航字段
-- ----------------------------
ALTER TABLE `portal_category`
    ADD COLUMN `show_in_nav` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否在头部栏目展示（0否/1是）' AFTER `status`,
    ADD COLUMN `nav_route_type` varchar(20) NOT NULL DEFAULT 'category' COMMENT '路由类型（home/category/static/external）' AFTER `show_in_nav`,
    ADD COLUMN `nav_route_path` varchar(200) DEFAULT NULL COMMENT '静态/外链路由路径（仅 static/external 类型使用）' AFTER `nav_route_type`,
    ADD COLUMN `requires_auth` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否需要登录（0否/1是）' AFTER `nav_route_path`;

-- 加索引：Navbar 查询只取 show_in_nav=1 的数据，索引加速过滤
ALTER TABLE `portal_category` ADD INDEX `idx_show_in_nav` (`show_in_nav`);

-- ----------------------------
-- 2. 清空旧分类数据并重置自增 ID
-- ----------------------------
truncate table `portal_category`;


-- =============================================
-- 3. 一级栏目（8 个）
-- =============================================
INSERT INTO `portal_category` (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by) VALUES
    -- 1. 首页：平台总入口，固定跳转 /
    ('首页', 'home', '精选推荐、双轨轮播', 'fa-home', 1, 0, '0', 1, 'home', '/', 0, 'admin'),
    -- 2. 散文天地：散文类动态栏目，二级走 /category/<名称>
    ('散文天地', 'prose', '人文书写与情感表达', 'fa-pen-fancy', 2, 0, '0', 1, 'category', NULL, 0, 'admin'),
    -- 3. 技术笔记：技术成长模块，二级走 /category/<名称>
    ('技术笔记', 'tech-notes', '开发记录、技术解析、AI编程实践', 'fa-code', 3, 0, '0', 1, 'category', NULL, 0, 'admin'),
    -- 4. 读书空间：读书模块（书籍），静态栏目，一级可点跳 /reading
    ('读书空间', 'reading', '读书心得、经典共读、书单推荐', 'fa-book', 4, 0, '0', 1, 'static', '/reading', 0, 'admin'),
    -- 5. 面试指南：求职面试模块，静态栏目，一级可点跳 /interview
    ('面试指南', 'interview', '真题整理、面经复盘、简历优化', 'fa-briefcase', 5, 0, '0', 1, 'static', '/interview', 0, 'admin'),
    -- 6. 社区互动：话题讨论、动态，静态栏目（一级仅展开子菜单，无直接跳转）
    ('社区互动', 'interaction', '话题讨论、动态广场', 'fa-users', 6, 0, '0', 1, 'category', NULL, 0, 'admin'),
    -- 7. 创作者中心：发布和认证，静态栏目（一级仅展开子菜单）
    ('创作者中心', 'creator', '发布文章、专栏、征文、认证', 'fa-feather', 7, 0, '0', 1, 'category', NULL, 0, 'admin'),
    -- 8. 个人空间：个人内容管理，静态栏目（一级仅展开子菜单）
    ('个人空间', 'mine', '个人中心、成长时间线、我的内容', 'fa-user', 8, 0, '0', 1, 'category', NULL, 0, 'admin');

-- =============================================
-- 4. 二级栏目 - 散文天地 (parent_id = 2，7 项，全部 category 类型)
-- 这些是真正的文章分类，文章发布时可归到这些分类下
-- =============================================
INSERT INTO `portal_category` (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by) VALUES
    ('人间烟火', 'life-stories', '饮食、市井、生活琐记', 'fa-utensils', 1, 2, '0', 1, 'category', NULL, 0, 'admin'),
    ('山河行吟', 'travel-nature', '游记、自然书写、生态散文', 'fa-mountain', 2, 2, '0', 1, 'category', NULL, 0, 'admin'),
    ('心灵独白', 'inner-thoughts', '孤独、成长、疗愈随笔', 'fa-heart', 3, 2, '0', 1, 'category', NULL, 0, 'admin'),
    ('城市笔记', 'city-notes', '北上广深、小镇观察', 'fa-city', 4, 2, '0', 1, 'category', NULL, 0, 'admin'),
    ('四季专栏', 'seasons', '春之思、夏之躁、秋之静、冬之藏', 'fa-leaf', 5, 2, '0', 1, 'category', NULL, 0, 'admin'),
    ('声音散文', 'audio-prose', '作者自读、背景音效沉浸体验', 'fa-volume-up', 6, 2, '0', 1, 'category', NULL, 0, 'admin'),
    ('读者来信', 'reader-letters', '短篇心声刊发与回声计划', 'fa-envelope', 7, 2, '0', 1, 'category', NULL, 0, 'admin');

-- =============================================
-- 5. 二级栏目 - 技术笔记 (parent_id = 3，6 项，全部 category 类型)
-- 这些是真正的文章分类，文章发布时可归到这些分类下
-- 顺序按用户要求：技术栈手册 → 架构札记 → 性能日志 → AI编程 → 开源日志 → 新手入门
-- =============================================
INSERT INTO `portal_category` (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by) VALUES
    ('技术栈手册', 'tech-stack', 'Java/SpringBoot、React/Vue、Flutter/UniApp', 'fa-book-open', 1, 3, '0', 1, 'category', NULL, 0, 'admin'),
    ('架构札记', 'architecture', '微服务、缓存策略、分布式事务', 'fa-project-diagram', 2, 3, '0', 1, 'category', NULL, 0, 'admin'),
    ('性能日志', 'performance', 'SQL优化、前端加载、JVM调优', 'fa-tachometer-alt', 3, 3, '0', 1, 'category', NULL, 0, 'admin'),
    ('AI编程', 'ai-coding', 'Cursor使用、ChatGPT提示工程、AI排错记录', 'fa-robot', 4, 3, '0', 1, 'category', NULL, 0, 'admin'),
    ('开源日志', 'open-source', 'PR提交、Issue解决、源码阅读', 'fa-code-branch', 5, 3, '0', 1, 'category', NULL, 0, 'admin'),
    ('新手入门', 'beginner', '环境配置、第一行代码实录', 'fa-play-circle', 6, 3, '0', 1, 'category', NULL, 0, 'admin');

-- =============================================
-- 6. 二级栏目 - 读书空间 (parent_id = 4，4 项，全部 static 类型)
-- 这些是导航占位项，文章不会归到这些"分类"下（读书内容由独立模块管理）
-- 已去掉"共读活动"
-- =============================================
INSERT INTO `portal_category` (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by) VALUES
    ('读书首页', 'reading-home', '读书空间总入口', 'fa-book-reader', 1, 4, '0', 1, 'static', '/reading', 0, 'admin'),
    ('发现好书', 'reading-discover', '发现好书、书单推荐', 'fa-list', 2, 4, '0', 1, 'static', '/reading/discover', 0, 'admin'),
    ('金句摘录', 'reading-quotes', '高光语句+个人批注', 'fa-quote-left', 3, 4, '0', 1, 'static', '/reading/quotes', 0, 'admin'),
    ('我的书架', 'reading-bookshelf', '个人书架管理', 'fa-bookmark', 4, 4, '0', 1, 'static', '/reading/bookshelf', 1, 'admin');

-- =============================================
-- 7. 二级栏目 - 面试指南 (parent_id = 5，10 项，全部 static 类型)
-- 含 4 项 requires_auth=1（AI模拟面试、学习计划、错题本、刷题日历）
-- =============================================
INSERT INTO `portal_category` (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by) VALUES
    ('面试题库', 'interview-questions', '算法题、系统设计、行为面试', 'fa-clipboard-list', 1, 5, '0', 1, 'static', '/interview/questions', 0, 'admin'),
    ('面试经验', 'interview-experiences', '大厂面试全流程还原', 'fa-chart-line', 2, 5, '0', 1, 'static', '/interview/experiences', 0, 'admin'),
    ('简历模板', 'interview-resume-templates', '技术亮点提炼、项目描述技巧', 'fa-file-alt', 3, 5, '0', 1, 'static', '/interview/resume-templates', 0, 'admin'),
    ('AI 模拟面试', 'interview-mock', '自测题集、答题思路拆解', 'fa-microphone', 4, 5, '0', 1, 'static', '/interview/mock', 1, 'admin'),
    ('学习中心', 'learn-center', '学习中心总入口', 'fa-graduation-cap', 5, 5, '0', 1, 'static', '/learn', 0, 'admin'),
    ('知识图谱', 'learn-knowledge', '知识体系可视化', 'fa-project-diagram', 6, 5, '0', 1, 'static', '/learn/knowledge', 0, 'admin'),
    ('刷题排行榜', 'learn-leaderboard', '刷题榜、学习榜', 'fa-trophy', 7, 5, '0', 1, 'static', '/learn/leaderboard', 0, 'admin'),
    ('学习计划', 'learn-plan', '个人学习计划管理', 'fa-calendar-alt', 8, 5, '0', 1, 'static', '/learn/plan', 1, 'admin'),
    ('错题本', 'learn-wrong', '错题归集与复习', 'fa-times-circle', 9, 5, '0', 1, 'static', '/learn/wrong', 1, 'admin'),
    ('刷题日历', 'learn-calendar', '刷题打卡日历', 'fa-calendar-check', 10, 5, '0', 1, 'static', '/learn/calendar', 1, 'admin');

-- =============================================
-- 8. 二级栏目 - 社区互动 (parent_id = 6，2 项，全部 static 类型)
-- =============================================
INSERT INTO `portal_category` (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by) VALUES
    ('话题广场', 'topics', '话题讨论列表', 'fa-comments', 1, 6, '0', 1, 'static', '/topics', 0, 'admin'),
    ('动态广场', 'feed', '用户动态流', 'fa-stream', 2, 6, '0', 1, 'static', '/feed', 0, 'admin');

-- =============================================
-- 9. 二级栏目 - 创作者中心 (parent_id = 7，6 项，全部 static 类型)
-- 含 2 项 requires_auth=1（发布文章、创作者认证）
-- =============================================
INSERT INTO `portal_category` (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by) VALUES
    ('发布文章', 'publish', '发布新文章', 'fa-edit', 1, 7, '0', 1, 'static', '/publish', 1, 'admin'),
    ('专栏广场', 'columns', '专栏列表与订阅', 'fa-columns', 2, 7, '0', 1, 'static', '/columns', 0, 'admin'),
    ('征文活动', 'contests', '征文活动、技术挑战赛', 'fa-file-upload', 3, 7, '0', 1, 'static', '/contests', 0, 'admin'),
    ('创作者认证', 'creator-certification', '申请创作者认证', 'fa-certificate', 4, 7, '0', 1, 'static', '/creator/certification', 1, 'admin'),
    ('创作者列表', 'authors', '认证创作者列表', 'fa-users-cog', 5, 7, '0', 1, 'static', '/authors', 0, 'admin'),
    ('成长排行榜', 'ranking', '成长值排行榜', 'fa-trophy', 6, 7, '0', 1, 'static', '/ranking', 0, 'admin');

-- =============================================
-- 10. 二级栏目 - 个人空间 (parent_id = 8，7 项，全部 static 类型，全部 requires_auth=1)
-- =============================================
INSERT INTO `portal_category` (name, slug, description, icon, sort, parent_id, status, show_in_nav, nav_route_type, nav_route_path, requires_auth, create_by) VALUES
    ('个人中心', 'user', '个人中心主页', 'fa-user-circle', 1, 8, '0', 1, 'static', '/user', 1, 'admin'),
    ('成长时间线', 'growth-timeline', '成长记录时间线', 'fa-chart-line', 2, 8, '0', 1, 'static', '/growth/timeline', 1, 'admin'),
    ('我的专栏', 'column-my', '我创建的专栏', 'fa-columns', 3, 8, '0', 1, 'static', '/column/my', 1, 'admin'),
    ('我的文章', 'my-articles', '我发布的文章', 'fa-file-alt', 4, 8, '0', 1, 'static', '/my/articles', 1, 'admin'),
    ('我的话题', 'topic-my-topics', '我发起的话题', 'fa-comments', 5, 8, '0', 1, 'static', '/topic/my/topics', 1, 'admin'),
    ('我的观点', 'topic-my-posts', '我发表的观点', 'fa-comment', 6, 8, '0', 1, 'static', '/topic/my/posts', 1, 'admin'),
    ('我的成就', 'achievements', '我的成就与徽章', 'fa-award', 7, 8, '0', 1, 'static', '/achievements', 1, 'admin');

-- =============================================
-- 11. 完成校验
-- =============================================
SELECT '分类导航字段初始化完成！' AS message;
SELECT CONCAT('共创建 ', COUNT(*), ' 个分类') AS summary FROM portal_category;
SELECT
    CASE nav_route_type
        WHEN 'home' THEN '首页'
        WHEN 'category' THEN '动态分类栏目'
        WHEN 'static' THEN '静态路由'
        WHEN 'external' THEN '外部链接'
    END AS route_type_desc,
    COUNT(*) AS cnt,
    SUM(show_in_nav) AS show_in_nav_count
FROM portal_category
GROUP BY nav_route_type;
