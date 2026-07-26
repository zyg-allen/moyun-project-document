-- =====================================================================
-- 墨韵智库 - 业务数据初始化合并脚本
-- 文件路径：sql-organized/11_data/01_seed_data.sql
-- 生成说明：本文件由 SQL 整理工程师自动合并整理，所有 INSERT 均使用幂等写法
--           （INSERT IGNORE 或 INSERT ... WHERE NOT EXISTS），可重复执行。
--
-- 合并来源脚本（按编号）：
--   05_moyun_v2_init.sql           分类体系（7 个一级栏目 + 二级分类）+ 28 个标签
--   19_growth_system_init.sql      成长规则（20 条）+ 成就定义（20 条）
--   89_tip_growth_rules.sql        打赏成长规则（2 条规则 + 2 条成就）
--   61_writing_prompt_init.sql     每日写作 prompt（最近 7 天）
--
-- 执行说明：
--   1. 本文件仅包含数据 INSERT 语句，不包含 CREATE TABLE DDL
--      （建表脚本位于 sql-organized/03_portal_base、06_portal_growth、
--        08_portal_commerce 等目录，请先执行建表脚本）
--   2. 所有 INSERT 已幂等化，可重复执行
--   3. 依赖：portal_category / portal_tag / portal_growth_rule /
--            portal_achievement / portal_writing_prompt 表已存在
--
-- 注意：
--   - 05 原脚本含 DELETE + ALTER TABLE AUTO_INCREMENT 重置动作，本合并版
--     改为 INSERT IGNORE 幂等写法，不再破坏既有数据，请确保首次执行环境为空
--     或通过唯一键（slug 等）避免重复
-- =====================================================================

SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================================
-- 一、一级栏目（来源：05_moyun_v2_init.sql）
--    网站头部主导航 7 个一级栏目
-- =====================================================================
INSERT IGNORE INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `create_by`) VALUES
('首页',     'home',      '精选推荐、双轨轮播',                     'fa-home',         1, 0, '0', 'admin'),
('散文天地', 'prose',     '人文书写与情感表达',                     'fa-pen-fancy',    2, 0, '0', 'admin'),
('技术笔记', 'tech-notes','开发记录、技术解析、AI编程实践',         'fa-code',         3, 0, '0', 'admin'),
('读书空间', 'reading',   '读书心得、精选好书、书单推荐',           'fa-book',         4, 0, '0', 'admin'),
('面试指南', 'interview', '真题整理、面经复盘、简历优化',           'fa-briefcase',    5, 0, '0', 'admin'),
('技能工坊', 'skills',    '写作技巧、代码技巧、学习方法论',         'fa-tools',        6, 0, '0', 'admin'),
('社区互动', 'community', '话题讨论、投稿征集、用户动态',           'fa-users',        7, 0, '0', 'admin');

-- =====================================================================
-- 二、二级分类（来源：05_moyun_v2_init.sql）
--    注意：parent_id 引用上方一级栏目的 id（假定首次执行时自增 id 为 1-7）
-- =====================================================================

-- 1. 散文天地的二级分类 (parent_id = 2)
INSERT IGNORE INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `create_by`) VALUES
('人间烟火', 'life-stories',    '饮食、市井、生活琐记',           'fa-utensils',   1, 2, '0', 'admin'),
('山河行吟', 'travel-nature',   '游记、自然书写、生态散文',       'fa-mountain',   2, 2, '0', 'admin'),
('心灵独白', 'inner-thoughts',  '孤独、成长、疗愈随笔',           'fa-heart',      3, 2, '0', 'admin'),
('城市笔记', 'city-notes',      '北上广深、小镇观察',             'fa-city',       4, 2, '0', 'admin'),
('四季专栏', 'seasons',         '春之思、夏之躁、秋之静、冬之藏', 'fa-leaf',       5, 2, '0', 'admin'),
('声音散文', 'audio-prose',     '作者自读、背景音效沉浸体验',     'fa-volume-up',  6, 2, '0', 'admin'),
('读者来信', 'reader-letters',  '短篇心声刊发与回声计划',         'fa-envelope',   7, 2, '0', 'admin');

-- 2. 技术笔记的二级分类 (parent_id = 3)
INSERT IGNORE INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `create_by`) VALUES
('新手入门',   'beginner',     '环境配置、第一行代码实录',                       'fa-play-circle',    1, 3, '0', 'admin'),
('技术栈手册', 'tech-stack',   'Java/SpringBoot、React/Vue、Flutter/UniApp',     'fa-book-open',      2, 3, '0', 'admin'),
('架构札记',   'architecture', '微服务、缓存策略、分布式事务',                   'fa-project-diagram',3, 3, '0', 'admin'),
('性能日志',   'performance',  'SQL优化、前端加载、JVM调优',                     'fa-tachometer-alt', 4, 3, '0', 'admin'),
('AI编程',     'ai-coding',    'Cursor使用、ChatGPT提示工程、AI排错记录',        'fa-robot',          5, 3, '0', 'admin'),
('开源日志',   'open-source',  'PR提交、Issue解决、源码阅读',                    'fa-code-branch',    6, 3, '0', 'admin');

-- 3. 读书空间的二级分类 (parent_id = 4)
INSERT IGNORE INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `create_by`) VALUES
('人文经典', 'humanities',     '散文集、小说、哲学随笔',                 'fa-book-reader', 1, 4, '0', 'admin'),
('技术书籍', 'tech-books',     '《代码整洁之道》《Java并发编程》',       'fa-laptop-code', 2, 4, '0', 'admin'),
('书单推荐', 'book-lists',     '入门书单、进阶书单、冷门好书',           'fa-list',        3, 4, '0', 'admin'),
('金句摘录', 'quotes',         '高光语句+个人批注',                     'fa-quote-left',  4, 4, '0', 'admin');

-- 4. 面试指南的二级分类 (parent_id = 5)
INSERT IGNORE INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `create_by`) VALUES
('真题库',     'question-bank',    '算法题、系统设计、行为面试',         'fa-clipboard-list',1, 5, '0', 'admin'),
('面经复盘',   'interview-reviews','大厂面试全流程还原',                 'fa-chart-line',    2, 5, '0', 'admin'),
('简历优化',   'resume-tips',      '技术亮点提炼、项目描述技巧',         'fa-file-alt',      3, 5, '0', 'admin'),
('模拟面试',   'mock-interview',   '自测题集、答题思路拆解',             'fa-microphone',    4, 5, '0', 'admin'),
('职业规划',   'career-planning',  '3年成长路径、技能图谱构建',           'fa-chart-pie',     5, 5, '0', 'admin');

-- 5. 技能工坊的二级分类 (parent_id = 6)
INSERT IGNORE INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `create_by`) VALUES
('写作技巧', 'writing-skills',    '抒情结构、时间线设计、情感线索',     'fa-feather-alt', 1, 6, '0', 'admin'),
('代码技巧', 'coding-skills',     '命名规范、异常处理、日志实践',       'fa-code',        2, 6, '0', 'admin'),
('学习方法', 'learning-methods',  '费曼学习法、间隔复习、知识卡片',     'fa-brain',       3, 6, '0', 'admin'),
('工具指南', 'tools-guide',       'Markdown写作、Notion管理、Git协作',  'fa-wrench',      4, 6, '0', 'admin'),
('输出训练', 'output-practice',   '每日一写、代码日记、周总结模板',     'fa-edit',        5, 6, '0', 'admin');

-- 6. 社区互动的二级分类 (parent_id = 7)
INSERT IGNORE INTO `portal_category` (`name`, `slug`, `description`, `icon`, `sort`, `parent_id`, `status`, `create_by`) VALUES
('话题讨论', 'topics',        '每周主题帖（如"你最难忘的一顿饭"）', 'fa-comments',    1, 7, '0', 'admin'),
('投稿征集', 'contributions', '征文活动、技术挑战赛',               'fa-file-upload', 2, 7, '0', 'admin'),
('用户动态', 'user-activity', '关注流、点赞更新、新文提醒',         'fa-stream',      3, 7, '0', 'admin'),
('互评圈',   'peer-review',   '匿名互评、结对共改',                 'fa-users-cog',   4, 7, '0', 'admin'),
('成长打卡', 'check-in',      '写作打卡、刷题打卡、读书打卡',       'fa-calendar-check',5,7, '0', 'admin');

-- =====================================================================
-- 三、标签体系（来源：05_moyun_v2_init.sql，共 28 个标签）
--    说明：name 字段存储纯文本，不带 # 前缀（# 属于展示符号，由前端按需拼接）
-- =====================================================================

-- 人文类标签
INSERT IGNORE INTO `portal_tag` (`name`, `slug`, `sort`, `status`, `create_by`, `remark`) VALUES
('生活哲思', 'life-philosophy',    1, '0', 'admin', '人文类'),
('城市记忆', 'city-memory',        2, '0', 'admin', '人文类'),
('自然写作', 'nature-writing',     3, '0', 'admin', '人文类'),
('情感随笔', 'emotional-essay',    4, '0', 'admin', '人文类'),
('人间烟火', 'life-fireworks',     5, '0', 'admin', '人文类'),
('乡愁记忆', 'nostalgia',          6, '0', 'admin', '人文类'),
('孤独成长', 'loneliness-growth',  7, '0', 'admin', '人文类'),
('四季感悟', 'seasons-feeling',    8, '0', 'admin', '人文类');

-- 技术类标签
INSERT IGNORE INTO `portal_tag` (`name`, `slug`, `sort`, `status`, `create_by`, `remark`) VALUES
('SpringBoot实战', 'springboot-practice',   9,  '0', 'admin', '技术类'),
('React Hooks',    'react-hooks',           10, '0', 'admin', '技术类'),
('AI辅助开发',     'ai-assisted-dev',       11, '0', 'admin', '技术类'),
('算法突破',       'algorithm-breakthrough',12, '0', 'admin', '技术类'),
('Java并发',       'java-concurrency',      13, '0', 'admin', '技术类'),
('Vue3实践',       'vue3-practice',         14, '0', 'admin', '技术类'),
('微服务架构',     'microservices',         15, '0', 'admin', '技术类'),
('MySQL优化',      'mysql-optimization',    16, '0', 'admin', '技术类'),
('Git协作',        'git-collaboration',     17, '0', 'admin', '技术类'),
('前端性能',       'frontend-performance',  18, '0', 'admin', '技术类'),
('JVM调优',        'jvm-tuning',            19, '0', 'admin', '技术类'),
('系统设计',       'system-design',         20, '0', 'admin', '技术类');

-- 通用类标签
INSERT IGNORE INTO `portal_tag` (`name`, `slug`, `sort`, `status`, `create_by`, `remark`) VALUES
('新手入门', 'beginner-guide',            21, '0', 'admin', '通用类'),
('进阶提升', 'advanced-improvement',      22, '0', 'admin', '通用类'),
('面试备战', 'interview-prep',            23, '0', 'admin', '通用类'),
('读书心得', 'reading-notes',             24, '0', 'admin', '通用类'),
('写作技巧', 'writing-tips',              25, '0', 'admin', '通用类'),
('学习方法', 'learning-methods-tag',      26, '0', 'admin', '通用类'),
('职场经验', 'career-experience',         27, '0', 'admin', '通用类'),
('个人成长', 'personal-growth',           28, '0', 'admin', '通用类');

-- =====================================================================
-- 四、成长规则（来源：19_growth_system_init.sql，共 20 条）
--    幂等：依赖 portal_growth_rule.uk_module_action 唯一键去重
-- =====================================================================
INSERT IGNORE INTO `portal_growth_rule` (`module`, `action`, `growth_delta`, `daily_limit`, `description`, `status`, `sort`) VALUES
-- 文章模块
('article',    'publish_article',         50,  3,  '发布文章',         '0', 1),
('article',    'receive_like',            2,   0,  '文章被点赞',       '0', 2),
('article',    'receive_bookmark',        3,   0,  '文章被收藏',       '0', 3),
('article',    'receive_follow',          5,   0,  '被关注',           '0', 4),
('article',    'article_featured',        100, 0,  '文章被精选',       '0', 5),
('article',    'receive_comment',         2,   0,  '文章被评论',       '0', 6),
-- 读书空间
('reading',    'finish_book',             20,  1,  '完成阅读一本书',   '0', 10),
('reading',    'write_quote',             15,  0,  '发布金句',         '0', 11),
('reading',    'create_booklist',         20,  0,  '创建书单',         '0', 12),
('reading',    'quote_liked',             5,   0,  '金句被点赞',       '0', 13),
('reading',    'booklist_liked',          5,   0,  '书单被点赞',       '0', 14),
('reading',    'booklist_bookmarked',     10,  0,  '书单被收藏',       '0', 15),
-- 面试空间
('interview',  'solve_question',          10,  20, '解题',             '0', 20),
('interview',  'write_note',              15,  0,  '写笔记',           '0', 21),
('interview',  'note_adopted',            50,  0,  '笔记被精选',       '0', 22),
('interview',  'publish_experience',      30,  0,  '发布面经',         '0', 23),
('interview',  'experience_liked',        2,   0,  '面经被点赞',       '0', 24),
('interview',  'experience_bookmarked',   3,   0,  '面经被收藏',       '0', 25),
-- 通用
('all',        'daily_checkin',           1,   1,  '每日签到',         '0', 30),
('all',        'daily_login',             1,   1,  '每日登录',         '0', 31);

-- =====================================================================
-- 五、成就定义（来源：19_growth_system_init.sql，共 20 条）
--    幂等：依赖 portal_achievement.uk_code 唯一键去重
-- =====================================================================
INSERT IGNORE INTO `portal_achievement` (`code`, `name`, `description`, `icon`, `module`, `condition_json`, `growth_reward`, `sort`, `status`) VALUES
-- 文章模块成就
('first_article',     '初露锋芒',   '发布第一篇文章',   NULL, 'article',   '{"action":"publish_article","count":1}',     20,  1,  '0'),
('article_10',        '勤勉作者',   '发布10篇文章',     NULL, 'article',   '{"action":"publish_article","count":10}',    50,  2,  '0'),
('article_50',        '高产作者',   '发布50篇文章',     NULL, 'article',   '{"action":"publish_article","count":50}',    200, 3,  '0'),
('article_featured',  '精华创作者', '文章被精选',       NULL, 'article',   '{"action":"article_featured","count":1}',   100, 4,  '0'),
('article_100_likes', '人气作者',   '单篇文章获赞100',  NULL, 'article',   '{"action":"receive_like","count":100}',     50,  5,  '0'),
-- 读书空间成就
('first_book',        '开卷有益',   '完成阅读第一本书', NULL, 'reading',   '{"action":"finish_book","count":1}',        20,  10, '0'),
('book_worm_10',      '书虫',       '完成阅读10本书',   NULL, 'reading',   '{"action":"finish_book","count":10}',       100, 11, '0'),
('book_worm_50',      '阅读达人',   '完成阅读50本书',   NULL, 'reading',   '{"action":"finish_book","count":50}',       300, 12, '0'),
('first_booklist',    '书单策划',   '创建第一个书单',   NULL, 'reading',   '{"action":"create_booklist","count":1}',    20,  13, '0'),
('quote_master',      '金句达人',   '发布20条金句',     NULL, 'reading',   '{"action":"write_quote","count":20}',       50,  14, '0'),
-- 面试空间成就
('first_solve',       '初试身手',   '解答第一道面试题', NULL, 'interview', '{"action":"solve_question","count":1}',     10,  20, '0'),
('solve_50',          '刷题能手',   '解答50道面试题',   NULL, 'interview', '{"action":"solve_question","count":50}',    100, 21, '0'),
('solve_200',         '面试达人',   '解答200道面试题',  NULL, 'interview', '{"action":"solve_question","count":200}',   300, 22, '0'),
('first_note',        '笔记新手',   '撰写第一篇笔记',   NULL, 'interview', '{"action":"write_note","count":1}',         15,  23, '0'),
('note_adopted',      '知识贡献者', '笔记被精选',       NULL, 'interview', '{"action":"note_adopted","count":1}',       50,  24, '0'),
('first_experience',  '面经分享者', '发布第一篇面经',   NULL, 'interview', '{"action":"publish_experience","count":1}', 30,  25, '0'),
('experience_10',     '面经达人',   '发布10篇面经',     NULL, 'interview', '{"action":"publish_experience","count":10}',100, 26, '0'),
-- 通用成就
('checkin_7',         '坚持一周',   '连续签到7天',      NULL, 'all',       '{"action":"daily_checkin","count":7}',      10,  30, '0'),
('checkin_30',        '坚持一月',   '连续签到30天',     NULL, 'all',       '{"action":"daily_checkin","count":30}',     50,  31, '0'),
('level_5',           '渐入佳境',   '达到5级',          NULL, 'all',       '{"action":"level","count":5}',              0,   32, '0'),
('level_8',           '登峰造极',   '达到8级',          NULL, 'all',       '{"action":"level","count":8}',              0,   33, '0');

-- =====================================================================
-- 六、打赏成长规则与成就（来源：89_tip_growth_rules.sql）
--    将打赏行为接入成长体系闭环，幂等：INSERT ... WHERE NOT EXISTS
-- =====================================================================

-- 1. 成长规则：被打赏者获得成长值
INSERT INTO `portal_growth_rule` (`module`, `action`, `growth_delta`, `daily_limit`, `description`, `status`, `sort`)
SELECT 'article', 'receive_tip', 3, 0, '文章/专栏被打赏', '0', 7
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `portal_growth_rule` WHERE `module` = 'article' AND `action` = 'receive_tip');

-- 2. 成长规则：打赏者获得成长值（鼓励正向互动）
INSERT INTO `portal_growth_rule` (`module`, `action`, `growth_delta`, `daily_limit`, `description`, `status`, `sort`)
SELECT 'article', 'tip_others', 1, 3, '打赏他人', '0', 8
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `portal_growth_rule` WHERE `module` = 'article' AND `action` = 'tip_others');

-- 3. 成就：首次被打赏
INSERT INTO `portal_achievement` (`code`, `name`, `description`, `icon`, `module`, `condition_json`, `growth_reward`, `sort`, `status`)
SELECT 'first_tip_received', '初获鼓励', '首次收到打赏', NULL, 'article', '{"action":"receive_tip","count":1}', 10, 7, '0'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `portal_achievement` WHERE `code` = 'first_tip_received');

-- 4. 成就：累计打赏 10 次（慷慨鼓励者）
INSERT INTO `portal_achievement` (`code`, `name`, `description`, `icon`, `module`, `condition_json`, `growth_reward`, `sort`, `status`)
SELECT 'generous_tipper', '慷慨鼓励', '累计打赏他人 10 次', NULL, 'article', '{"action":"tip_others","count":10}', 30, 8, '0'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `portal_achievement` WHERE `code` = 'generous_tipper');

-- =====================================================================
-- 七、每日写作 prompt（来源：61_writing_prompt_init.sql，最近 7 天）
--    幂等：依赖 portal_writing_prompt.uk_prompt_date 唯一键去重
--    使用 CURDATE() 动态生成日期，避免硬编码绝对日期
-- =====================================================================
INSERT IGNORE INTO `portal_writing_prompt` (`prompt_date`, `title`, `description`, `category`) VALUES
(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '一封信',           '请以书信的形式，写一封给十年后自己的信。可以是忠告、可以是期许，也可以是当下的困惑。', '生活'),
(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '雨夜',             '描述一个雨夜的场景：一个未眠的人，一扇半开的窗，一段未说完的话。',                 '情感'),
(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '通勤路上',         '记录一次通勤路上的所见所闻。一个陌生人、一段广播、一闪而过的风景，都可能成为故事的开端。', '生活'),
(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '如果时间可以暂停', '假如你拥有让时间暂停 30 秒的能力，你会用它做什么？请写一个具体的场景。',           '虚构'),
(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '面试官的沉默',     '一场面试中，面试官在某个问题后沉默了 10 秒。请描写那 10 秒里应聘者的内心活动。',   '职场'),
(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '一件旧物',         '选择一件你保留多年的旧物，写它背后的故事。它从哪里来，又见证了什么？',             '情感'),
(CURDATE(),                          '此刻的光',         '观察此刻你所在空间里的光：它的颜色、强度、来源、投下的影子。用 300 字描绘它，并赋予它一种情绪。', '哲思');

-- =====================================================================
-- 收尾：恢复外键检查 + 完成提示
-- =====================================================================
SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS;

-- 以下统计 SELECT 已移除（避免自动化部署管道输出意外内容）
-- 如需验证初始化结果，可手动执行：
--   SELECT COUNT(*) FROM `portal_category`;
--   SELECT COUNT(*) FROM `portal_tag`;
--   SELECT COUNT(*) FROM `portal_growth_rule`;
--   SELECT COUNT(*) FROM `portal_achievement`;
--   SELECT COUNT(*) FROM `portal_writing_prompt`;
