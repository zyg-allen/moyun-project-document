-- =============================================
-- 墨韵智库 - 完整分类体系初始化脚本 v2.0
-- 执行顺序: 05 (在 04_cms_menu_init.sql 之后执行)
-- 描述: 完整的一级栏目、二级分类、标签体系
-- =============================================

-- ----------------------------
-- 清空现有分类和标签数据
-- ----------------------------
DELETE FROM portal_article_tag WHERE 1=1;
DELETE FROM portal_tag WHERE 1=1;
DELETE FROM portal_category WHERE 1=1;

-- ----------------------------
-- 重置自增ID
-- ----------------------------
ALTER TABLE portal_category AUTO_INCREMENT = 1;
ALTER TABLE portal_tag AUTO_INCREMENT = 1;

-- =============================================
-- 一、一级栏目（网站头部主导航）
-- =============================================

-- 1. 首页
INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, create_by) VALUES
    ('首页', 'home', '精选推荐、双轨轮播', 'fa-home', 1, 0, '0', 'admin');

-- 2. 散文天地
INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, create_by) VALUES
    ('散文天地', 'prose', '人文书写与情感表达', 'fa-pen-fancy', 2, 0, '0', 'admin');

-- 3. 技术笔记
INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, create_by) VALUES
    ('技术笔记', 'tech-notes', '开发记录、技术解析、AI编程实践', 'fa-code', 3, 0, '0', 'admin');

-- 4. 读书空间
INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, create_by) VALUES
    ('读书空间', 'reading', '读书心得、经典共读、书单推荐', 'fa-book', 4, 0, '0', 'admin');

-- 5. 面试指南
INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, create_by) VALUES
    ('面试指南', 'interview', '真题整理、面经复盘、简历优化', 'fa-briefcase', 5, 0, '0', 'admin');

-- 6. 技能工坊
INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, create_by) VALUES
    ('技能工坊', 'skills', '写作技巧、代码技巧、学习方法论', 'fa-tools', 6, 0, '0', 'admin');

-- 7. 社区互动
INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, create_by) VALUES
    ('社区互动', 'community', '话题讨论、投稿征集、用户动态', 'fa-users', 7, 0, '0', 'admin');

-- =============================================
-- 二、二级分类结构
-- =============================================

-- 1. 散文天地的二级分类 (parent_id = 2)
INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, create_by) VALUES
                                                                                                    ('人间烟火', 'life-stories', '饮食、市井、生活琐记', 'fa-utensils', 1, 2, '0', 'admin'),
                                                                                                    ('山河行吟', 'travel-nature', '游记、自然书写、生态散文', 'fa-mountain', 2, 2, '0', 'admin'),
                                                                                                    ('心灵独白', 'inner-thoughts', '孤独、成长、疗愈随笔', 'fa-heart', 3, 2, '0', 'admin'),
                                                                                                    ('城市笔记', 'city-notes', '北上广深、小镇观察', 'fa-city', 4, 2, '0', 'admin'),
                                                                                                    ('四季专栏', 'seasons', '春之思、夏之躁、秋之静、冬之藏', 'fa-leaf', 5, 2, '0', 'admin'),
                                                                                                    ('声音散文', 'audio-prose', '作者自读、背景音效沉浸体验', 'fa-volume-up', 6, 2, '0', 'admin'),
                                                                                                    ('读者来信', 'reader-letters', '短篇心声刊发与回声计划', 'fa-envelope', 7, 2, '0', 'admin');

-- 2. 技术笔记的二级分类 (parent_id = 3)
INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, create_by) VALUES
                                                                                                    ('新手入门', 'beginner', '环境配置、第一行代码实录', 'fa-play-circle', 1, 3, '0', 'admin'),
                                                                                                    ('技术栈手册', 'tech-stack', 'Java/SpringBoot、React/Vue、Flutter/UniApp', 'fa-book-open', 2, 3, '0', 'admin'),
                                                                                                    ('架构札记', 'architecture', '微服务、缓存策略、分布式事务', 'fa-project-diagram', 3, 3, '0', 'admin'),
                                                                                                    ('性能日志', 'performance', 'SQL优化、前端加载、JVM调优', 'fa-tachometer-alt', 4, 3, '0', 'admin'),
                                                                                                    ('AI编程', 'ai-coding', 'Cursor使用、ChatGPT提示工程、AI排错记录', 'fa-robot', 5, 3, '0', 'admin'),
                                                                                                    ('开源日志', 'open-source', 'PR提交、Issue解决、源码阅读', 'fa-code-branch', 6, 3, '0', 'admin');

-- 3. 读书空间的二级分类 (parent_id = 4)
INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, create_by) VALUES
                                                                                                    ('人文经典', 'humanities', '散文集、小说、哲学随笔', 'fa-book-reader', 1, 4, '0', 'admin'),
                                                                                                    ('技术书籍', 'tech-books', '《代码整洁之道》《Java并发编程》', 'fa-laptop-code', 2, 4, '0', 'admin'),
                                                                                                    ('共读计划', 'reading-club', '每月共读一本书+线上讨论', 'fa-calendar-alt', 3, 4, '0', 'admin'),
                                                                                                    ('书单推荐', 'book-lists', '入门书单、进阶书单、冷门好书', 'fa-list', 4, 4, '0', 'admin'),
                                                                                                    ('金句摘录', 'quotes', '高光语句+个人批注', 'fa-quote-left', 5, 4, '0', 'admin');

-- 4. 面试指南的二级分类 (parent_id = 5)
INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, create_by) VALUES
                                                                                                    ('真题库', 'question-bank', '算法题、系统设计、行为面试', 'fa-clipboard-list', 1, 5, '0', 'admin'),
                                                                                                    ('面经复盘', 'interview-reviews', '大厂面试全流程还原', 'fa-chart-line', 2, 5, '0', 'admin'),
                                                                                                    ('简历优化', 'resume-tips', '技术亮点提炼、项目描述技巧', 'fa-file-alt', 3, 5, '0', 'admin'),
                                                                                                    ('模拟面试', 'mock-interview', '自测题集、答题思路拆解', 'fa-microphone', 4, 5, '0', 'admin'),
                                                                                                    ('职业规划', 'career-planning', '3年成长路径、技能图谱构建', 'fa-chart-pie', 5, 5, '0', 'admin');

-- 5. 技能工坊的二级分类 (parent_id = 6)
INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, create_by) VALUES
                                                                                                    ('写作技巧', 'writing-skills', '抒情结构、时间线设计、情感线索', 'fa-feather-alt', 1, 6, '0', 'admin'),
                                                                                                    ('代码技巧', 'coding-skills', '命名规范、异常处理、日志实践', 'fa-code', 2, 6, '0', 'admin'),
                                                                                                    ('学习方法', 'learning-methods', '费曼学习法、间隔复习、知识卡片', 'fa-brain', 3, 6, '0', 'admin'),
                                                                                                    ('工具指南', 'tools-guide', 'Markdown写作、Notion管理、Git协作', 'fa-wrench', 4, 6, '0', 'admin'),
                                                                                                    ('输出训练', 'output-practice', '每日一写、代码日记、周总结模板', 'fa-edit', 5, 6, '0', 'admin');

-- 6. 社区互动的二级分类 (parent_id = 7)
INSERT INTO portal_category (name, slug, description, icon, sort, parent_id, status, create_by) VALUES
                                                                                                    ('话题讨论', 'topics', '每周主题帖（如"你最难忘的一顿饭"）', 'fa-comments', 1, 7, '0', 'admin'),
                                                                                                    ('投稿征集', 'contributions', '征文活动、技术挑战赛', 'fa-file-upload', 2, 7, '0', 'admin'),
                                                                                                    ('用户动态', 'user-activity', '关注流、点赞更新、新文提醒', 'fa-stream', 3, 7, '0', 'admin'),
                                                                                                    ('互评圈', 'peer-review', '匿名互评、结对共改', 'fa-users-cog', 4, 7, '0', 'admin'),
                                                                                                    ('成长打卡', 'check-in', '写作打卡、刷题打卡、读书打卡', 'fa-calendar-check', 5, 7, '0', 'admin');

-- =============================================
-- 三、标签体系规范（28个标签）
-- =============================================

-- 人文类标签
-- 说明：name 字段存储纯文本，不带 # 前缀（# 属于展示符号，由前端按需拼接）
INSERT INTO portal_tag (name, slug, sort, status, create_by, remark) VALUES
                                                                         ('生活哲思', 'life-philosophy', 1, '0', 'admin', '人文类'),
                                                                         ('城市记忆', 'city-memory', 2, '0', 'admin', '人文类'),
                                                                         ('自然写作', 'nature-writing', 3, '0', 'admin', '人文类'),
                                                                         ('情感随笔', 'emotional-essay', 4, '0', 'admin', '人文类'),
                                                                         ('人间烟火', 'life-fireworks', 5, '0', 'admin', '人文类'),
                                                                         ('乡愁记忆', 'nostalgia', 6, '0', 'admin', '人文类'),
                                                                         ('孤独成长', 'loneliness-growth', 7, '0', 'admin', '人文类'),
                                                                         ('四季感悟', 'seasons-feeling', 8, '0', 'admin', '人文类');

-- 技术类标签
INSERT INTO portal_tag (name, slug, sort, status, create_by, remark) VALUES
                                                                         ('SpringBoot实战', 'springboot-practice', 9, '0', 'admin', '技术类'),
                                                                         ('React Hooks', 'react-hooks', 10, '0', 'admin', '技术类'),
                                                                         ('AI辅助开发', 'ai-assisted-dev', 11, '0', 'admin', '技术类'),
                                                                         ('算法突破', 'algorithm-breakthrough', 12, '0', 'admin', '技术类'),
                                                                         ('Java并发', 'java-concurrency', 13, '0', 'admin', '技术类'),
                                                                         ('Vue3实践', 'vue3-practice', 14, '0', 'admin', '技术类'),
                                                                         ('微服务架构', 'microservices', 15, '0', 'admin', '技术类'),
                                                                         ('MySQL优化', 'mysql-optimization', 16, '0', 'admin', '技术类'),
                                                                         ('Git协作', 'git-collaboration', 17, '0', 'admin', '技术类'),
                                                                         ('前端性能', 'frontend-performance', 18, '0', 'admin', '技术类'),
                                                                         ('JVM调优', 'jvm-tuning', 19, '0', 'admin', '技术类'),
                                                                         ('系统设计', 'system-design', 20, '0', 'admin', '技术类');

-- 通用类标签
INSERT INTO portal_tag (name, slug, sort, status, create_by, remark) VALUES
                                                                         ('新手入门', 'beginner-guide', 21, '0', 'admin', '通用类'),
                                                                         ('进阶提升', 'advanced-improvement', 22, '0', 'admin', '通用类'),
                                                                         ('面试备战', 'interview-prep', 23, '0', 'admin', '通用类'),
                                                                         ('读书心得', 'reading-notes', 24, '0', 'admin', '通用类'),
                                                                         ('写作技巧', 'writing-tips', 25, '0', 'admin', '通用类'),
                                                                         ('学习方法', 'learning-methods-tag', 26, '0', 'admin', '通用类'),
                                                                         ('职场经验', 'career-experience', 27, '0', 'admin', '通用类'),
                                                                         ('个人成长', 'personal-growth', 28, '0', 'admin', '通用类');

-- =============================================
-- 四、完成提示
-- =============================================

SELECT '分类体系初始化完成！' AS message;
SELECT CONCAT('共创建 ', COUNT(*), ' 个分类') AS summary FROM portal_category;
SELECT CONCAT('共创建 ', COUNT(*), ' 个标签') AS tag_summary FROM portal_tag;
