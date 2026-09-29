-- =============================================================================
-- 门户前台栏目初始化（portal_category 重做脚本）
-- -----------------------------------------------------------------------------
-- 执行时机：DDL 之后、业务数据（文章/书籍）之前或之后均可，天然幂等可重跑。
--
-- 为什么必须显式 id（v13.29 重做背景）：
--    旧版脚本依赖 AUTO_INCREMENT 顺序（id 从 51 起），parent_id 硬编码 52~71。
--    换库重导时自增起点/插入顺序不同，parent_id 全部错位 → 前台导航树（/portal/category/nav/tree）父子关系错乱。
--    故改为与 moyun-menu-redo.sql 相同模式：**父子关系锚定显式 id，不依赖自增**。
--
-- 重跑安全（三段式，v13.29）：
--    ① 先把旧表 id→slug 快照到 _bak_ 备份表（全新库为空快照，无副作用）；
--    ② TRUNCATE 后以**显式 id 1..49** 全量重插——任何库、任何次数重跑，落位恒定；
--    ③ 按 **slug（稳定键）** 回填下游引用（portal_article.category_id/root_category_id、
--       portal_book.category_id、portal_book_list.category_id），旧 id 经 slug 映射到新 id，
--       下游数据不悬空。二次重跑时快照已是新 id，回填为同值无操作（幂等）。
--
-- 编号规则：id 显式编号 1..49，按**语义树深度优先**（同一父节点下先父后子、按 sort 排序），
--    故 **父 id 恒小于子 id**；TRUNCATE 后 AUTO_INCREMENT 归 1，显式插完自增继续于 50。
--
-- 归属归纳（按前台路由实际上下文，v13.29）：
--    顶级五大主线：面试专区(/interview) / 学习中心(/learn) / 阅读空间(/reading) / 创作互动(/feed) / 我的(/user)
--    · 面试题库归学习中心（v11.13 路由反转，/learn/questions）
--    · 简历模板/面经/AI 面试官归面试专区（与 InterviewPage 板块一致）
--    · 读书三入口（发现好书/我的书架/金句摘录）挂读书空间目录下
--    · 话题/动态/专栏/征文/发布/成长排行归创作互动
-- 数据修正：
--    · 剔除游离行「面试指南」（slug='interview' 与顶级重复、sort=0 冗余；无下游引用）
--    · 剔除软删行「金句摘录」(del_flag='1')；存活行 slug 统一为 reading-quotes
--    · 死路径修正：创作互动 /creation → /feed（前台无 /creation 路由，页脚同口径）；
--      读书空间 /reading/space → /reading（前台无 /reading/space 路由，指阅读枢纽页）
-- =============================================================================

-- ① 旧表 id→slug 快照（重跑场景的映射依据；全新库为空，CREATE AS SELECT 无副作用）
DROP TABLE IF EXISTS _bak_portal_category;
CREATE TABLE _bak_portal_category AS
  SELECT id, slug, category_type FROM portal_category WHERE del_flag = '0';

-- ② TRUNCATE + 显式 id 全量重插（落位恒定，任何库重跑结果一致）
TRUNCATE TABLE portal_category;

INSERT INTO portal_category (id,name,slug,description,icon,sort,parent_id,status,show_in_nav,nav_route_type,nav_route_path,nav_badge,category_type,requires_auth,create_by,create_time,update_by,update_time,remark,del_flag) VALUES
  -- ===== 顶级（parent_id=0）=====
  (1,'首页','home','精选推荐、双轨轮播','fa-home',1,0,'0',1,'home','/',NULL,'special',0,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  (2,'面试专区','interview','AI 语音面试、面经复盘、简历优化','fa-briefcase',2,0,'0',1,'static','/interview',NULL,'directory',0,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  (3,'学习中心','learn','题库、刷题、错题本、学习计划','fa-graduation-cap',3,0,'0',1,'static','/learn',NULL,'directory',0,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  (4,'阅读空间','reading','散文天地、技术笔记、读书空间','fa-book',4,0,'0',1,'static','/reading',NULL,'directory',0,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  (5,'创作互动','creation','话题、动态、专栏、征文、发布','fa-feather',5,0,'0',1,'static','/feed',NULL,'directory',0,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  (6,'我的','mine','个人中心、成长时间线、我的内容','fa-user',6,0,'0',1,'static','/user',NULL,'directory',1,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  -- ===== 面试专区（parent_id=2）=====
  (7,'AI 语音面试官','interview-voice','AI 语音面试官，真实面试场景模拟','fa-microphone',1,2,'0',1,'static','/interview/voice','NEW','special',1,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  (8,'面试经验','interview-experiences','大厂面试全流程还原','fa-chart-line',2,2,'0',1,'static','/interview/experiences',NULL,'special',0,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  (9,'简历模板','interview-resume-templates','技术亮点提炼、项目描述技巧','fa-file-alt',3,2,'0',1,'static','/interview/resume-templates',NULL,'special',0,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  -- ===== 学习中心（parent_id=3）=====
  (10,'面试题库','learn-questions','算法题、系统设计、行为面试','fa-clipboard-list',1,3,'0',1,'static','/learn/questions',NULL,'special',0,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  (11,'刷题中心','learn-practice','在线编程、选择题练习','fa-laptop-code',2,3,'0',1,'static','/learn/practice','HOT','directory',0,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  (12,'错题本','learn-wrong','错题归集与复习','fa-times-circle',3,3,'0',1,'static','/learn/wrong',NULL,'special',1,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  (13,'知识图谱','learn-knowledge','知识体系可视化','fa-project-diagram',4,3,'0',1,'static','/learn/knowledge',NULL,'special',0,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  (14,'刷题排行榜','learn-leaderboard','刷题榜、学习榜','fa-trophy',5,3,'0',1,'static','/learn/leaderboard',NULL,'special',0,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  (15,'刷题日历','learn-calendar','刷题打卡日历','fa-calendar-check',6,3,'0',1,'static','/learn/calendar',NULL,'special',1,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  (16,'学习计划','learn-plan','个人学习计划管理','fa-calendar-alt',7,3,'0',1,'static','/learn/plan',NULL,'special',1,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  -- ===== 刷题中心（parent_id=11）=====
  (17,'选择题','learn-practice-choice','选择题在线练习','fa-check-square',1,11,'0',1,'static','/learn/practice/choice',NULL,'special',0,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  (18,'编程题','learn-practice-coding','编程题在线练习','fa-code',2,11,'0',1,'static','/learn/practice/coding',NULL,'special',0,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  -- ===== 阅读空间（parent_id=4）：目录容器 =====
  (19,'散文天地','prose','人文书写与情感表达','fa-pen-fancy',1,4,'0',1,'category','/category/prose',NULL,'directory',0,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  (20,'技术笔记','tech-notes','开发记录、技术解析、AI编程实践','fa-code',2,4,'0',1,'category','/category/tech-notes',NULL,'directory',0,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  (21,'读书空间','reading-space','发现好书、我的书架、金句摘录','fa-book-reader',3,4,'0',1,'static','/reading',NULL,'directory',0,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  -- ===== 散文天地（parent_id=19）：文章分类 =====
  (22,'人间烟火','life-stories','饮食、市井、生活琐记','fa-utensils',1,19,'0',1,'category','/category/life-stories',NULL,'article',0,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  (23,'山河行吟','travel-nature','游记、自然书写、生态散文','fa-mountain',2,19,'0',1,'category','/category/travel-nature',NULL,'article',0,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  (24,'心灵独白','inner-thoughts','孤独、成长、疗愈随笔','fa-heart',3,19,'0',1,'category','/category/inner-thoughts',NULL,'article',0,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  (25,'城市笔记','city-notes','北上广深、小镇观察','fa-city',4,19,'0',1,'category','/category/city-notes',NULL,'article',0,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  (26,'四季专栏','seasons','春之思、夏之躁、秋之静、冬之藏','fa-leaf',5,19,'0',1,'category','/category/seasons',NULL,'article',0,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  (27,'声音散文','audio-prose','作者自读、背景音效沉浸体验','fa-volume-up',6,19,'0',1,'category','/category/audio-prose',NULL,'article',0,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  (28,'读者来信','reader-letters','短篇心声刊发与回声计划','fa-envelope',7,19,'0',1,'category','/category/reader-letters',NULL,'article',0,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  -- ===== 技术笔记（parent_id=20）：文章分类 =====
  (29,'技术栈手册','tech-stack','Java/SpringBoot、React/Vue、Flutter/UniApp','fa-book-open',1,20,'0',1,'category','/category/tech-stack',NULL,'article',0,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  (30,'架构札记','architecture','微服务、缓存策略、分布式事务','fa-project-diagram',2,20,'0',1,'category','/category/architecture',NULL,'article',0,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  (31,'性能日志','performance','SQL优化、前端加载、JVM调优','fa-tachometer-alt',3,20,'0',1,'category','/category/performance',NULL,'article',0,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  (32,'AI编程','ai-coding','Cursor使用、ChatGPT提示工程、AI排错记录','fa-robot',4,20,'0',1,'category','/category/ai-coding',NULL,'article',0,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  (33,'开源日志','open-source','PR提交、Issue解决、源码阅读','fa-code-branch',5,20,'0',1,'category','/category/open-source',NULL,'article',0,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  (34,'新手入门','beginner','环境配置、第一行代码实录','fa-play-circle',6,20,'0',1,'category','/category/beginner',NULL,'article',0,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  -- ===== 读书空间（parent_id=21）：功能入口 =====
  (35,'发现好书','reading-discover','发现好书、书单推荐','fa-list',1,21,'0',1,'static','/reading/discover',NULL,'special',0,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  (36,'我的书架','reading-bookshelf','个人书架管理','fa-bookmark',2,21,'0',1,'static','/reading/bookshelf',NULL,'special',1,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  (37,'金句摘录','reading-quotes','读书空间内的金句摘录','fa-quote-left',3,21,'0',1,'static','/reading/quotes',NULL,'special',0,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  -- ===== 创作互动（parent_id=5）=====
  (38,'话题广场','topics','话题讨论列表','fa-comments',1,5,'0',1,'static','/topics',NULL,'special',0,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  (39,'动态广场','feed','用户动态流','fa-stream',2,5,'0',1,'static','/feed',NULL,'special',0,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  (40,'专栏广场','columns','专栏列表与订阅','fa-columns',3,5,'0',1,'static','/columns',NULL,'special',0,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  (41,'征文活动','contests','征文活动、技术挑战赛','fa-file-upload',4,5,'0',1,'static','/contests',NULL,'special',0,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  (42,'发布文章','publish','发布新文章（快捷入口）','fa-edit',5,5,'0',1,'static','/publish',NULL,'special',1,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  (43,'成长排行榜','ranking','成长值排行榜','fa-trophy',6,5,'0',1,'static','/ranking',NULL,'special',0,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  -- ===== 我的（parent_id=6）=====
  (44,'个人中心','user','个人中心主页','fa-user-circle',1,6,'0',1,'static','/user',NULL,'special',1,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  (45,'成长时间线','growth-timeline','成长记录时间线','fa-chart-line',2,6,'0',1,'static','/growth/timeline',NULL,'special',1,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  (46,'我的文章','my-articles','我发布的文章','fa-file-alt',3,6,'0',1,'static','/my/articles',NULL,'special',1,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  (47,'我的专栏','column-my','我创建的专栏','fa-columns',4,6,'0',1,'static','/column/my',NULL,'special',1,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  (48,'我的成就','achievements','我的成就与徽章','fa-award',5,6,'0',1,'static','/achievements',NULL,'special',1,'admin','2026-08-19 18:01:44','',NULL,NULL,'0'),
  (49,'我的话题观点','topic-my','我发起的话题与观点','fa-comments',6,6,'0',1,'static','/topic/my',NULL,'special',1,'admin','2026-08-19 18:01:44','',NULL,NULL,'0');

-- ③ 下游引用按 slug 回填（旧 id → 新 id；全新库 0 行更新；二次重跑为同值无操作）
--    注意：root_category_id 沿用现有语义（与 category_id 同值，见 dev 库 2026-09-29 存量数据）
UPDATE portal_article pa
  JOIN _bak_portal_category old ON pa.category_id = old.id
  JOIN portal_category new ON new.slug = old.slug AND new.del_flag = '0'
SET pa.category_id = new.id,
    pa.root_category_id = new.id
WHERE pa.del_flag = '0';

UPDATE portal_book pb
  JOIN _bak_portal_category old ON pb.category_id = old.id
  JOIN portal_category new ON new.slug = old.slug AND new.del_flag = '0'
SET pb.category_id = new.id;

UPDATE portal_book_list pbl
  JOIN _bak_portal_category old ON pbl.category_id = old.id
  JOIN portal_category new ON new.slug = old.slug AND new.del_flag = '0'
SET pbl.category_id = new.id;

-- 复核①：下游悬空引用应为 0（文章的分类 id 必须存在于新表）
SELECT 'article_dangling' AS check_item, COUNT(*) AS violations
FROM portal_article pa
WHERE pa.del_flag = '0' AND pa.category_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM portal_category c WHERE c.id = pa.category_id AND c.del_flag = '0');

-- 复核②：父子关系完整性——孤儿（parent_id 不存在且非 0）应为 0，父应恒小于子
SELECT 'orphan_parent' AS check_item, COUNT(*) AS violations
FROM portal_category c
WHERE c.del_flag = '0' AND c.parent_id <> 0
  AND NOT EXISTS (SELECT 1 FROM portal_category p WHERE p.id = c.parent_id AND p.del_flag = '0');

SELECT 'parent_gt_child' AS check_item, COUNT(*) AS violations
FROM portal_category c
WHERE c.del_flag = '0' AND c.parent_id <> 0 AND c.parent_id >= c.id;

-- 复核③：slug 唯一性（本表无唯一键，靠脚本纪律；重复会导致回填映射歧义）
SELECT 'dup_slug' AS check_item, slug, COUNT(*) AS violations
FROM portal_category WHERE del_flag = '0'
GROUP BY slug HAVING COUNT(*) > 1;

select * from portal_category;
