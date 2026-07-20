-- ################################################################################
-- ################################################################################
-- ##                                                                            ##
-- ##   ⚠⚠⚠  警告 / WARNING  ⚠⚠⚠                                            ##
-- ##                                                                            ##
-- ##   本脚本仅用于【开发/测试环境】初始化！                                    ##
-- ##   This script is for DEVELOPMENT / TEST environment ONLY!                  ##
-- ##                                                                            ##
-- ##   ⚠ 生产环境严禁执行！/ DO NOT execute in PRODUCTION!                     ##
-- ##                                                                            ##
-- ##   包含内容：                                                               ##
-- ##     - 测试用户（密码统一为 123456 的 BCrypt 哈希）                         ##
-- ##     - 大量测试文章、评论、通知数据                                         ##
-- ##     - 读书空间、面试空间、推荐位等测试数据                                 ##
-- ##                                                                            ##
-- ##   执行后果：                                                               ##
-- ##     - 会清空文章、读书、面试等业务表的所有现有数据（先清后插）             ##
-- ##     - 仅保留 portal_user 中 @keep_user_ids 指定的测试用户                 ##
-- ##     - 生产环境执行将导致数据丢失！                                         ##
-- ##                                                                            ##
-- ##   生产部署时请跳过此脚本 / Skip this script when deploying to PRODUCTION.  ##
-- ##                                                                            ##
-- ################################################################################
-- ################################################################################
--
-- ============================================================
-- 合并文件：02_test_data.sql（测试数据合集）
-- 整理人：SQL 整理工程师
-- 整理策略：先清后插（cleanup → users/articles → reading/interview → book seed → book seed v2）
-- 合并来源（按文件内出现顺序）：
--   1. 84_test_data_cleanup.sql       —— 测试数据清空脚本（TRUNCATE/DELETE，置顶以便"先清后插"）
--   2. 06_portal_test_data.sql        —— 门户测试数据（用户、文章、评论、通知）
--   3. 26_reading_interview_test_data.sql —— 读书空间 & 面试空间测试数据
--   4. 48_portal_book_seed_data.sql   —— 读书模块 v1.0 第三阶段：种子数据补全（章节正文 + 推荐位）
--   5. 91_book_seed_data.sql          —— 读书空间示例书籍《工程师修炼之道》初始化（10章+6金句+2推荐位）
-- 说明：
--   - 原 INSERT 语句保持不变（测试数据为一次性使用，无需改写为幂等）
--   - 84 清理脚本置顶，确保"先清后插"逻辑清晰
--   - 48 与 91 数据互补（48 针对书籍 1-15，91 新增独立书籍），无重复，全部保留
-- ============================================================


-- ################################################################################
-- ##  段落 1 / 5
-- ##  来源：84_test_data_cleanup.sql
-- ##  说明：测试数据清空脚本（TRUNCATE / DELETE，先清后插的"清"）
-- ################################################################################

-- =====================================================================
-- [测试数据清空脚本]  复用：每次全量测试前执行
-- ---------------------------------------------------------------------
-- 用途：清空文章模块 + 面试模块 + 读书空间模块 + 公共统计/Feed/成长/消息
--       的所有业务数据，仅保留若干测试用户账号，便于从干净状态重新测试文章流程。
--
-- 保留：
--   1. portal_user            仅保留 KEEP_USER_IDS 中指定的用户（见下方定义）
--   2. portal_category        文章分类（字典）
--   3. portal_tag             标签（字典）
--   4. portal_writing_prompt  每日写作提示（字典）
--   5. portal_interview_category / portal_interview_company  面试分类/公司（字典）
--   6. portal_task / portal_achievement / portal_growth_rule / portal_shop_item / portal_vip_package  规则配置
--
-- 注意：
--   - TRUNCATE 会重置自增 ID，新发布的文章 ID 会从 1 开始，便于测试观察。
--   - 交易/钱包相关表默认不清（见文末【可选段】），测试付费阅读时按需取消注释。
--   - 执行前务必确认数据库环境，生产环境严禁执行。
-- =====================================================================

-- ★ 请修改为你要保留的测试用户 ID（多个用逗号分隔）
-- 查询当前用户：SELECT id, username, nickname FROM portal_user;
SET @keep_user_ids = '1,2,3';

SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================================
-- A. 文章模块（文章 + 审核 + 阅读量 + 点赞 + 收藏 + 评论 + 版本 + 草稿）
-- =====================================================================
TRUNCATE TABLE portal_article;            -- 文章主表（含 status 审核状态、views 阅读量、likes、comments、bookmark_count 等）
TRUNCATE TABLE portal_article_version;    -- 文章历史版本（草稿/发布快照）
TRUNCATE TABLE portal_article_view;       -- 文章浏览明细（用于阅读量防刷）
TRUNCATE TABLE portal_article_tag;        -- 文章-标签旧关联表
TRUNCATE TABLE portal_entity_tag;         -- 实体-标签通用关联（article/booklist/experience 等）
TRUNCATE TABLE portal_like;               -- 文章点赞记录
TRUNCATE TABLE portal_bookmark;           -- 文章收藏记录
TRUNCATE TABLE portal_comment;            -- 评论（文章/面经/书评通用，按 entity_type 区分）
TRUNCATE TABLE portal_comment_like;       -- 评论点赞
TRUNCATE TABLE portal_column;             -- 专栏主表
TRUNCATE TABLE portal_column_article;     -- 专栏-文章关联
TRUNCATE TABLE portal_column_subscribe;   -- 专栏订阅
TRUNCATE TABLE portal_contest_submission; -- 写作大赛投稿（关联文章）
TRUNCATE TABLE portal_contest_vote;       -- 大赛投票

-- =====================================================================
-- B. 公共统计 / Feed 流 / 成长体系 / 消息 / 关注 / 举报
--     （文章发布会写入这些表，必须一并清空，否则看板/Feed 残留旧数据）
-- =====================================================================
TRUNCATE TABLE portal_feed_event;         -- Feed 动态事件源（publish_article 等）
TRUNCATE TABLE portal_feed_inbox;         -- Feed 收件箱（粉丝推送）
TRUNCATE TABLE portal_growth_log;         -- 成长行为流水（publish_article/write_quote/solve_question 等 20+ action）
TRUNCATE TABLE portal_user_growth;        -- 用户成长值/等级
TRUNCATE TABLE portal_user_stats;         -- 用户统计（创作字数、签到连续天数、最后签到日期等）
TRUNCATE TABLE portal_user_badge;         -- 用户徽章发放记录
TRUNCATE TABLE portal_user_task;          -- 用户任务完成记录
TRUNCATE TABLE portal_message;            -- 私信消息
TRUNCATE TABLE portal_message_session;    -- 私信会话
TRUNCATE TABLE portal_follow;             -- 关注关系（关注/粉丝）
TRUNCATE TABLE portal_report;             -- 举报记录
TRUNCATE TABLE portal_feedback;           -- 用户反馈

-- =====================================================================
-- C. 读书空间（书籍 + 书架 + 书单 + 金句 + 阅读进度 + 读书会）
-- =====================================================================
TRUNCATE TABLE portal_book;                       -- 书籍
TRUNCATE TABLE portal_book_chapter;               -- 书籍章节
TRUNCATE TABLE portal_bookshelf;                  -- 书架
TRUNCATE TABLE portal_book_list;                  -- 书单
TRUNCATE TABLE portal_book_list_item;             -- 书单条目
TRUNCATE TABLE portal_book_list_bookmark;         -- 书单收藏
TRUNCATE TABLE portal_book_list_like;             -- 书单点赞
TRUNCATE TABLE portal_book_quote;                 -- 金句
TRUNCATE TABLE portal_book_quote_like;            -- 金句点赞
TRUNCATE TABLE portal_book_recommend;             -- 书籍推荐
TRUNCATE TABLE portal_reading_progress;           -- 阅读进度
TRUNCATE TABLE portal_reading_preference;         -- 阅读偏好
TRUNCATE TABLE portal_book_club_activity;        -- 读书会活动
TRUNCATE TABLE portal_book_club_participant;      -- 读书会参与者
TRUNCATE TABLE portal_book_club_record;           -- 读书会打卡记录
TRUNCATE TABLE portal_book_club_record_like;      -- 打卡点赞

-- =====================================================================
-- D. 面试模块（题库 + 答题 + 面经 + 模拟面试 + 错题本 + 代码运行 + 学习计划 + 简历）
--    注：portal_interview_category / portal_interview_company 为字典配置，保留
-- =====================================================================
TRUNCATE TABLE portal_interview_question;              -- 面试题
TRUNCATE TABLE portal_interview_question_company;      -- 题目-公司关联
TRUNCATE TABLE portal_interview_question_like;         -- 题目点赞
TRUNCATE TABLE portal_interview_submission;             -- 答题提交
TRUNCATE TABLE portal_interview_attempt;                -- 答题尝试明细
TRUNCATE TABLE portal_interview_comment;                -- 面经评论
TRUNCATE TABLE portal_interview_comment_like;           -- 面经评论点赞
TRUNCATE TABLE portal_interview_experience;             -- 面经
TRUNCATE TABLE portal_interview_experience_like;       -- 面经点赞
TRUNCATE TABLE portal_interview_bookmark;               -- 题目收藏
TRUNCATE TABLE portal_interview_resume_template;        -- 简历模板
TRUNCATE TABLE portal_interview_resume_template_like;  -- 简历模板点赞
TRUNCATE TABLE portal_mock_interview;                   -- 模拟面试
TRUNCATE TABLE portal_mock_interview_qa;                -- 模拟面试 QA
TRUNCATE TABLE portal_wrong_question;                   -- 错题本
TRUNCATE TABLE portal_code_run;                        -- 代码运行记录
TRUNCATE TABLE portal_study_plan;                       -- 学习计划
TRUNCATE TABLE portal_study_plan_log;                   -- 学习计划日志
TRUNCATE TABLE portal_user_resume;                     -- 用户简历

-- =====================================================================
-- E. 系统通知（文章审核/评论/点赞会触发通知）
--    若表不存在会报错，可按需注释掉对应行
-- =====================================================================
DELETE FROM sys_notification_read WHERE user_type = 'portal';
DELETE FROM sys_notification       WHERE user_type = 'portal';

-- =====================================================================
-- F. 仅保留指定测试用户（其余 portal_user 一并删除）
--    保留的用户登录后可重新发布文章进行测试
-- =====================================================================
DELETE FROM portal_user WHERE FIND_IN_SET(CAST(id AS CHAR), @keep_user_ids) = 0;

-- =====================================================================
-- G. 【可选】交易与钱包（测试付费阅读时按需取消注释）
-- =====================================================================
-- TRUNCATE TABLE portal_order;              -- 订单
-- TRUNCATE TABLE portal_tip_order;         -- 打赏订单
-- TRUNCATE TABLE portal_wallet_transaction;-- 钱包流水
-- UPDATE portal_wallet SET balance = 0, total_income = 0, total_expense = 0 WHERE 1=1;  -- 钱包余额归零

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================
-- 完成校验（可选执行，确认各表已清空）
-- =====================================================================
-- SELECT 'portal_article'        AS tbl, COUNT(*) AS cnt FROM portal_article
-- UNION ALL SELECT 'portal_article_version', COUNT(*) FROM portal_article_version
-- UNION ALL SELECT 'portal_like',             COUNT(*) FROM portal_like
-- UNION ALL SELECT 'portal_bookmark',         COUNT(*) FROM portal_bookmark
-- UNION ALL SELECT 'portal_comment',          COUNT(*) FROM portal_comment
-- UNION ALL SELECT 'portal_feed_event',       COUNT(*) FROM portal_feed_event
-- UNION ALL SELECT 'portal_growth_log',       COUNT(*) FROM portal_growth_log
-- UNION ALL SELECT 'portal_user_stats',      COUNT(*) FROM portal_user_stats
-- UNION ALL SELECT 'portal_book',             COUNT(*) FROM portal_book
-- UNION ALL SELECT 'portal_interview_question', COUNT(*) FROM portal_interview_question
-- UNION ALL SELECT 'portal_user (保留)',      COUNT(*) FROM portal_user;


-- ################################################################################
-- ##  段落 2 / 5
-- ##  来源：06_portal_test_data.sql
-- ##  说明：门户测试数据（用户、文章、评论、通知）
-- ################################################################################

-- ============================================================
-- ⚠ 警告：本脚本仅用于开发/测试环境初始化
-- ⚠ 生产环境严禁执行！包含测试用户（密码123456）和测试文章数据
-- ⚠ 生产部署时请跳过此脚本
-- ============================================================
-- =============================================
-- 墨韵智库 - 门户测试数据脚本
-- 版本: v2.0
-- 创建时间: 2026-05-28
-- 描述: 包含用户、文章等测试数据（分类和标签请执行 05_moyun_v2_init.sql）
-- 适用环境: 仅开发/测试环境（DEV/TEST），生产环境（PROD）禁止执行
-- =============================================

-- ----------------------------
-- 1. 测试用户（不同权限等级）
-- 密码都是：123456（BCrypt加密后）
-- ----------------------------
INSERT IGNORE INTO `portal_user` (`username`, `nickname`, `email`, `phone`, `password`, `avatar`, `bio`, `position`, `role`, `status`, `create_by`) VALUES
('admin', '墨韵管理员', 'admin@moyun.com', '13800138000', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIbtS7O', 'https://api.dicebear.com/7.x/avataaars/svg?seed=admin', '墨韵智库管理员，致力于打造优质内容社区', '产品经理', 'admin', '0', 'admin'),
('zhangsan', '张三', 'zhangsan@moyun.com', '13800138001', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIbtS7O', 'https://api.dicebear.com/7.x/avataaars/svg?seed=zhangsan', '热爱技术，喜欢分享，前端开发工程师', '前端工程师', 'user', '0', 'admin'),
('lisi', '李四', 'lisi@moyun.com', '13800138002', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIbtS7O', 'https://api.dicebear.com/7.x/avataaars/svg?seed=lisi', 'Java后端开发，专注微服务架构', '后端工程师', 'user', '0', 'admin'),
('wangwu', '王五', 'wangwu@moyun.com', '13800138003', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIbtS7O', 'https://api.dicebear.com/7.x/avataaars/svg?seed=wangwu', '文学爱好者，喜欢写散文', '自由职业', 'user', '0', 'admin');

-- ----------------------------
-- 2. 测试文章（技术笔记 - 15篇，用于分页测试）
-- 注意: 执行前请确保已执行 07_moyun_v2_init.sql 创建分类数据
-- ----------------------------
INSERT INTO `portal_article` (`title`, `content`, `excerpt`, `cover`, `author_id`, `category_id`, `status`, `is_featured`, `is_top`, `is_carousel`, `views`, `likes`, `comments`, `published_at`, `create_by`) VALUES
('Vue3 组合式 API 完全指南', '<h2>什么是组合式 API</h2><p>Vue3 引入了组合式 API，这是一种新的编写组件逻辑的方式。</p><pre><code>import { ref, computed, onMounted } from \'vue\'\nexport default {\n  setup() {\n    const count = ref(0)\n    const doubled = computed(() => count.value * 2)\n    \n    onMounted(() => {\n      console.log(\'组件已挂载\')\n    })\n    \n    return { count, doubled }\n  }\n}</code></pre>', 'Vue3 组合式 API 完全指南，带你从零开始掌握 setup 函数、ref、reactive、computed 等核心概念。', 'https://picsum.photos/seed/vue3-1/800/400', 2, 10, 'published', 1, 1, 1, 1520, 256, 32, NOW(), 'admin'),
('深入理解 Vue3 响应式原理', '<h2>Proxy vs Object.defineProperty</h2><p>Vue3 使用 Proxy 替代了 Object.defineProperty 来实现响应式。</p>', '深入解析 Vue3 的响应式原理，对比 Proxy 和 Object.defineProperty 的差异。', 'https://picsum.photos/seed/vue3-2/800/400', 2, 10, 'published', 1, 0, 0, 980, 178, 21, NOW(), 'admin'),
('Vue3 生命周期钩子详解', '<h2>生命周期钩子</h2><p>onBeforeMount, onMounted, onBeforeUpdate, onUpdated, onBeforeUnmount, onUnmounted</p>', 'Vue3 生命周期钩子的完整解析和最佳实践。', 'https://picsum.photos/seed/vue3-3/800/400', 2, 10, 'published', 0, 0, 0, 756, 123, 15, NOW(), 'admin'),
('Pinia 状态管理实战', '<h2>Pinia 简介</h2><p>Pinia 是 Vue3 官方推荐的状态管理库。</p>', 'Pinia 状态管理入门到实战，包含完整的项目示例。', 'https://picsum.photos/seed/pinia/800/400', 2, 10, 'published', 0, 0, 0, 623, 98, 12, NOW(), 'admin'),
('VueRouter4 新特性解析', '<h2>VueRouter4 新特性</h2><p>动态路由、路由守卫、组合式 API 支持</p>', 'VueRouter4 新特性完全解析，助力你的 Vue3 项目。', 'https://picsum.photos/seed/router/800/400', 2, 10, 'published', 0, 0, 0, 543, 87, 8, NOW(), 'admin'),
('TypeScript 在 Vue3 中的最佳实践', '<h2>为什么用 TypeScript</h2><p>类型安全、更好的 IDE 支持、更易维护的代码</p>', 'TypeScript 与 Vue3 结合的最佳实践指南。', 'https://picsum.photos/seed/ts/800/400', 2, 10, 'published', 0, 0, 0, 432, 65, 5, NOW(), 'admin'),
('React Hooks 深入理解', '<h2>Hooks 解决的问题</h2><p>在不编写 class 的情况下使用 state 以及其他的 React 特性</p>', 'React Hooks 从入门到精通，包含 useState, useEffect 等核心 Hook 的详解。', 'https://picsum.photos/seed/react-1/800/400', 2, 10, 'published', 0, 0, 0, 890, 145, 18, NOW(), 'admin'),
('React 性能优化指南', '<h2>性能优化策略</h2><p>memo, useMemo, useCallback, 懒加载</p>', 'React 应用性能优化的完整指南。', 'https://picsum.photos/seed/react-2/800/400', 2, 10, 'published', 0, 0, 0, 678, 112, 10, NOW(), 'admin'),
('Java8 Stream API 完全指南', '<h2>什么是 Stream</h2><p>Stream 是 Java8 中处理集合的关键抽象概念。</p>', 'Java8 Stream API 的完全教程，让你的代码更优雅。', 'https://picsum.photos/seed/java-1/800/400', 3, 11, 'published', 1, 0, 0, 1234, 201, 25, NOW(), 'admin'),
('Spring Boot 微服务架构实践', '<h2>微服务架构</h2><p>将单体应用拆分为多个小型服务</p>', '基于 Spring Boot 的微服务架构实战指南。', 'https://picsum.photos/seed/spring-1/800/400', 3, 11, 'published', 1, 0, 0, 1567, 289, 35, NOW(), 'admin'),
('MyBatis-Plus 高级用法', '<h2>MyBatis-Plus</h2><p>MyBatis 的增强工具，在 MyBatis 的基础上只做增强不做改变</p>', 'MyBatis-Plus 高级用法详解，提高开发效率。', 'https://picsum.photos/seed/mybatis/800/400', 3, 11, 'published', 0, 0, 0, 789, 134, 16, NOW(), 'admin'),
('Spring Security JWT 认证', '<h2>JWT 认证流程</h2><p>登录 → 获取 Token → 携带 Token 访问</p>', 'Spring Security 集成 JWT 认证完整教程。', 'https://picsum.photos/seed/jwt/800/400', 3, 11, 'published', 0, 0, 0, 654, 102, 12, NOW(), 'admin'),
('MySQL 索引优化实战', '<h2>索引类型</h2><p>普通索引、唯一索引、主键索引、联合索引</p>', 'MySQL 索引优化实战，提升查询性能。', 'https://picsum.photos/seed/mysql/800/400', 3, 11, 'published', 0, 0, 0, 823, 145, 18, NOW(), 'admin'),
('Redis 高性能缓存实践', '<h2>Redis 数据结构</h2><p>String, Hash, List, Set, ZSet</p>', 'Redis 在项目中的高性能缓存实践。', 'https://picsum.photos/seed/redis/800/400', 3, 11, 'published', 0, 0, 0, 712, 115, 13, NOW(), 'admin'),
('Docker 容器化部署指南', '<h2>什么是 Docker</h2><p>Docker 是一个开源的容器化平台</p>', 'Docker 容器化部署从入门到实战。', 'https://picsum.photos/seed/docker/800/400', 3, 11, 'published', 0, 0, 0, 567, 89, 7, NOW(), 'admin');

-- ----------------------------
-- 3. 测试文章（散文天地 - 8篇）
-- ----------------------------
INSERT INTO `portal_article` (`title`, `content`, `excerpt`, `cover`, `author_id`, `category_id`, `status`, `is_featured`, `is_top`, `is_carousel`, `views`, `likes`, `comments`, `published_at`, `create_by`) VALUES
('春日里的杭州西湖', '<h2>西湖美景</h2><p>清明时节，西湖岸边柳絮纷飞，正是春游好时节。</p>', '春日里游杭州西湖，感受江南水乡的诗意与浪漫。', 'https://picsum.photos/seed/westlake/800/400', 4, 12, 'published', 1, 0, 1, 2345, 456, 67, NOW(), 'admin'),
('城市夜归人', '<h2>深夜的城市</h2><p>夜晚十点的地铁，载着疲惫的人们回家。</p>', '记录城市里普通人为生活打拼的日常故事。', 'https://picsum.photos/seed/night/800/400', 4, 15, 'published', 0, 0, 0, 1876, 345, 45, NOW(), 'admin'),
('我的读书时光', '<h2>阅读的乐趣</h2><p>在这个信息爆炸的时代，静下心来读书是一种奢侈。</p>', '分享我的读书心得和阅读方法。', 'https://picsum.photos/seed/reading/800/400', 4, 20, 'published', 0, 0, 0, 1234, 234, 34, NOW(), 'admin'),
('故乡的四季', '<h2>故乡的回忆</h2><p>故乡的春夏秋冬，每一季都有独特的风景。</p>', '回忆故乡的四季变化，那些美好的童年时光。', 'https://picsum.photos/seed/hometown/800/400', 4, 14, 'published', 0, 0, 0, 1567, 289, 41, NOW(), 'admin'),
('咖啡馆里的下午', '<h2>慢时光</h2><p>一杯咖啡，一本书，一个慵懒的下午。</p>', '在咖啡馆里度过的美好时光。', 'https://picsum.photos/seed/coffee/800/400', 4, 13, 'published', 0, 0, 0, 987, 178, 23, NOW(), 'admin'),
('雨中漫步', '<h2>雨的诗意</h2><p>下雨了，撑一把伞，在雨中漫步。</p>', '感受雨中的宁静与美好。', 'https://picsum.photos/seed/rain/800/400', 4, 13, 'published', 0, 0, 0, 654, 123, 15, NOW(), 'admin'),
('一封家书', '<h2>致远方的家人</h2><p>亲爱的爸爸妈妈，见字如面。</p>', '一封写给远方家人的信。', 'https://picsum.photos/seed/letter/800/400', 4, 16, 'published', 0, 0, 0, 543, 98, 12, NOW(), 'admin'),
('秋意浓', '<h2>秋天来了</h2><p>树叶变黄，天气渐凉，秋天是一个诗意的季节。</p>', '描写秋天的美景和感受。', 'https://picsum.photos/seed/autumn/800/400', 4, 14, 'published', 0, 0, 0, 789, 134, 18, NOW(), 'admin');

-- ----------------------------
-- 4. 测试文章（读书空间 - 6篇）
-- ----------------------------
INSERT INTO `portal_article` (`title`, `content`, `excerpt`, `cover`, `author_id`, `category_id`, `status`, `is_featured`, `is_top`, `is_carousel`, `views`, `likes`, `comments`, `published_at`, `create_by`) VALUES
('《活着》读后感', '<h2>活着的意义</h2><p>余华的《活着》讲述了福贵一生的苦难与希望。</p>', '读《活着》有感，探讨生命的意义与价值。', 'https://picsum.photos/seed/huozhe/800/400', 4, 20, 'published', 1, 0, 0, 1456, 234, 28, NOW(), 'admin'),
('程序员必读的10本书', '<h2>推荐书单</h2><p>作为程序员，这些书值得一读再读。</p>', '推荐程序员必读的10本经典书籍。', 'https://picsum.photos/seed/books10/800/400', 2, 21, 'published', 0, 0, 0, 987, 167, 19, NOW(), 'admin'),
('如何高效阅读', '<h2>阅读方法</h2><p>掌握正确的阅读方法，让阅读更高效。</p>', '分享高效阅读的方法和技巧。', 'https://picsum.photos/seed/read/800/400', 3, 22, 'published', 0, 0, 0, 765, 123, 14, NOW(), 'admin'),
('《深入理解计算机系统》笔记', '<h2>CSAPP</h2><p>这本书是计算机专业必读的经典之作。</p>', '《深入理解计算机系统》读书笔记。', 'https://picsum.photos/seed/csapp/800/400', 3, 21, 'published', 0, 0, 0, 654, 98, 11, NOW(), 'admin'),
('唐诗三百首赏析', '<h2>唐诗之美</h2><p>品读唐诗，感受古典文学的魅力。</p>', '唐诗三百首经典作品赏析。', 'https://picsum.photos/seed/tangshi/800/400', 4, 20, 'published', 0, 0, 0, 543, 87, 9, NOW(), 'admin'),
('我的2024年读书清单', '<h2>年终总结</h2><p>2024年读了50本书，分享我的阅读清单。</p>', '2024年读书总结与推荐。', 'https://picsum.photos/seed/2024books/800/400', 2, 23, 'published', 0, 0, 0, 432, 65, 7, NOW(), 'admin');

-- ----------------------------
-- 5. 测试文章（面试指南 - 5篇）
-- ----------------------------
INSERT INTO `portal_article` (`title`, `content`, `excerpt`, `cover`, `author_id`, `category_id`, `status`, `is_featured`, `is_top`, `is_carousel`, `views`, `likes`, `comments`, `published_at`, `create_by`) VALUES
('Java面试题大全', '<h2>面试必备</h2><p>Java开发常见面试题整理。</p>', 'Java开发面试题整理，助力你拿到心仪offer。', 'https://picsum.photos/seed/javainterview/800/400', 3, 25, 'published', 1, 1, 0, 2345, 356, 42, NOW(), 'admin'),
('我的面试经历分享', '<h2>面试复盘</h2><p>分享我的春招面试经历和心得。</p>', '真实面试经历分享，希望对大家有帮助。', 'https://picsum.photos/seed/interviewexp/800/400', 2, 26, 'published', 0, 0, 0, 1876, 289, 35, NOW(), 'admin'),
('如何写一份优秀的简历', '<h2>简历技巧</h2><p>简历是求职的第一步，很重要。</p>', '简历优化技巧，让你的简历脱颖而出。', 'https://picsum.photos/seed/resume/800/400', 3, 27, 'published', 0, 0, 0, 1234, 201, 25, NOW(), 'admin'),
('前端面试高频考点', '<h2>前端面试</h2><p>前端开发常见面试题总结。</p>', '前端面试高频考点整理。', 'https://picsum.photos/seed/frontendinterview/800/400', 2, 25, 'published', 0, 0, 0, 987, 156, 18, NOW(), 'admin'),
('程序员职业规划', '<h2>职业发展</h2><p>技术人的职业发展路径规划。</p>', '分享程序员职业规划的一些思考。', 'https://picsum.photos/seed/career/800/400', 3, 29, 'published', 0, 0, 0, 765, 123, 14, NOW(), 'admin');

-- ----------------------------
-- 6. 测试文章（技能工坊 - 5篇）
-- ----------------------------
INSERT INTO `portal_article` (`title`, `content`, `excerpt`, `cover`, `author_id`, `category_id`, `status`, `is_featured`, `is_top`, `is_carousel`, `views`, `likes`, `comments`, `published_at`, `create_by`) VALUES
('如何提升写作能力', '<h2>写作技巧</h2><p>好的写作能力需要长期练习和积累。</p>', '分享提升写作能力的方法和技巧。', 'https://picsum.photos/seed/writing/800/400', 4, 30, 'published', 1, 0, 0, 1456, 234, 28, NOW(), 'admin'),
('Git高级技巧', '<h2>Git</h2><p>Git是程序员必备的版本控制工具。</p>', 'Git高级使用技巧，提高开发效率。', 'https://picsum.photos/seed/git/800/400', 3, 31, 'published', 0, 0, 0, 1123, 187, 22, NOW(), 'admin'),
('高效学习方法', '<h2>学习方法</h2><p>掌握正确的学习方法，事半功倍。</p>', '分享高效学习的方法和心得。', 'https://picsum.photos/seed/learn/800/400', 2, 32, 'published', 0, 0, 0, 987, 156, 18, NOW(), 'admin'),
('VSCode插件推荐', '<h2>效率工具</h2><p>好的工具能大大提升开发效率。</p>', 'VSCode必备插件推荐。', 'https://picsum.photos/seed/vscode/800/400', 2, 33, 'published', 0, 0, 0, 876, 134, 16, NOW(), 'admin'),
('坚持写作100天', '<h2>输出训练</h2><p>写作是最好的思考方式。</p>', '分享我坚持写作100天的收获。', 'https://picsum.photos/seed/100days/800/400', 4, 34, 'published', 0, 0, 0, 654, 98, 11, NOW(), 'admin');

-- ----------------------------
-- 7. 测试文章（社区互动 - 5篇）
-- ----------------------------
INSERT INTO `portal_article` (`title`, `content`, `excerpt`, `cover`, `author_id`, `category_id`, `status`, `is_featured`, `is_top`, `is_carousel`, `views`, `likes`, `comments`, `published_at`, `create_by`) VALUES
('欢迎加入墨韵智库', '<h2>社区公告</h2><p>欢迎大家来到墨韵智库，一起交流学习！</p>', '社区公告，欢迎新成员加入。', 'https://picsum.photos/seed/welcome/800/400', 1, 35, 'published', 1, 1, 1, 3456, 567, 78, NOW(), 'admin'),
('征文活动公告', '<h2>活动预告</h2><p>墨韵智库第一届征文活动开始啦！</p>', '社区征文活动公告，期待大家的参与。', 'https://picsum.photos/seed/contest/800/400', 1, 36, 'published', 0, 0, 0, 2345, 345, 45, NOW(), 'admin'),
('2024年度总结', '<h2>年度总结</h2><p>回顾这一年，我们一起成长。</p>', '社区2024年度总结与展望。', 'https://picsum.photos/seed/2024summary/800/400', 1, 37, 'published', 0, 0, 0, 1876, 289, 35, NOW(), 'admin'),
('互评活动规则', '<h2>活动规则</h2><p>社区互评活动规则说明。</p>', '社区互评圈活动规则介绍。', 'https://picsum.photos/seed/reviewrules/800/400', 1, 38, 'published', 0, 0, 0, 1234, 198, 24, NOW(), 'admin'),
('打卡挑战活动', '<h2>打卡活动</h2><p>30天写作打卡挑战开始了！</p>', '社区成长打卡活动介绍。', 'https://picsum.photos/seed/checkin/800/400', 1, 39, 'published', 0, 0, 0, 987, 156, 18, NOW(), 'admin');

-- ----------------------------
-- 8. 测试评论
-- ----------------------------
INSERT INTO `portal_comment` (`article_id`, `author_id`, `content`, `parent_id`, `like_count`, `status`, `create_by`) VALUES
(1, 3, '写得太好了！Vue3 的组合式 API 确实比选项式 API 更灵活。', 0, 25, '0', 'admin'),
(2, 4, '收藏了，慢慢学习。', 0, 18, '0', 'admin'),
(3, 2, '深入浅出，理解了 Proxy 的原理。', 0, 15, '0', 'admin'),
(9, 2, 'Stream API 让代码更简洁了！', 0, 20, '0', 'admin'),
(10, 4, '微服务架构是趋势，学习了。', 0, 16, '0', 'admin'),
(16, 2, '写得太美了，想去西湖看看。', 0, 30, '0', 'admin'),
(17, 3, '江南好，风景旧曾谙。', 0, 22, '0', 'admin'),
(18, 4, '打工人的真实写照。', 0, 18, '0', 'admin'),
(24, 2, '《活着》确实是本好书。', 0, 25, '0', 'admin'),
(30, 4, '面试题整理得很全面！', 0, 28, '0', 'admin'),
(35, 3, '欢迎欢迎！', 0, 35, '0', 'admin');

-- ----------------------------
-- 9. 测试通知
-- ----------------------------
-- 【已注释】portal_notification 表已废弃（见 34_merge_notification_tables.sql：RENAME 为 portal_notification_bak）
-- 新通知表为 sys_notification + sys_notification_read 两表结构：
--   - sys_notification（通知主体）：scope=user 个人 / scope=all 广播，新增 user_type/notice_type/status 等必填字段，移除了 is_read
--   - sys_notification_read（已读关系表）：用 NOT EXISTS 计算未读，原 is_read 字段无法直接映射
-- 因字段差异较大（旧 is_read 列丢失、新增多个 NOT NULL 字段），测试数据非必需，通知测试数据需手工创建或使用 sys_notification。
-- 下方保留原始 INSERT 语句作为参考。
-- INSERT INTO `portal_notification` (`user_id`, `type`, `title`, `content`, `data`, `is_read`) VALUES
-- (2, 'comment', '你的文章收到了新评论', '张三评论了你的文章《Vue3 组合式 API 完全指南》', '{"articleId": 1, "commentId": 1}', 0),
-- (2, 'like', '你的文章被点赞了', '李四点赞了你的文章《Vue3 组合式 API 完全指南》', '{"articleId": 1}', 0),
-- (3, 'comment', '你的文章收到了新评论', '张三评论了你的文章《React Hooks 深入理解》', '{"articleId": 7, "commentId": 3}', 1),
-- (4, 'comment', '你的文章收到了新评论', '张三评论了你的文章《春日里的杭州西湖》', '{"articleId": 16, "commentId": 6}', 0),
-- (2, 'system', '系统通知', '欢迎加入墨韵智库！', NULL, 1),
-- (3, 'system', '系统通知', '你的文章已通过审核', NULL, 0);

-- ----------------------------
-- 完成！
-- ----------------------------
SELECT '测试数据初始化完成！共 43 篇文章，4 个用户' AS message;
SELECT '注意: 分类和标签数据请通过 07_moyun_v2_init.sql 创建' AS note;


-- ################################################################################
-- ##  段落 3 / 5
-- ##  来源：26_reading_interview_test_data.sql
-- ##  说明：读书空间 & 面试空间 测试数据
-- ################################################################################

-- =====================================================
-- 墨韵·智库 - 读书空间 & 面试空间 测试数据
-- 版本: v1.0
-- 日期: 2026-06-22
-- 说明: 可重复执行，先清理旧测试数据再插入
-- =====================================================

-- 安全关闭外键检查
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS=0;

-- =====================================================
-- 一、读书空间 - 清理旧数据
-- =====================================================
DELETE FROM `portal_book_list_item`      WHERE `book_list_id` IN (SELECT `id` FROM `portal_book_list` WHERE `id` BETWEEN 1 AND 100);
DELETE FROM `portal_book_quote`          WHERE `id` BETWEEN 1 AND 100;
DELETE FROM `portal_book_list`           WHERE `id` BETWEEN 1 AND 100;
DELETE FROM `portal_book`                 WHERE `id` BETWEEN 1 AND 100;
DELETE FROM `portal_reading_progress`    WHERE `id` BETWEEN 1 AND 100;

-- 重置自增ID
ALTER TABLE `portal_book`               AUTO_INCREMENT = 1;
ALTER TABLE `portal_book_list`          AUTO_INCREMENT = 1;
ALTER TABLE `portal_book_list_item`     AUTO_INCREMENT = 1;
ALTER TABLE `portal_book_quote`         AUTO_INCREMENT = 1;
ALTER TABLE `portal_reading_progress`  AUTO_INCREMENT = 1;

-- =====================================================
-- 二、读书空间 - 书籍数据（15本）
-- =====================================================
INSERT INTO `portal_book` (`id`, `title`, `author`, `cover`, `description`, `isbn`, `publisher`, `publish_date`, `page_count`, `category_id`, `tags`, `rating`, `reading_count`, `status`, `access_level`, `is_featured`, `is_recommended`, `summary`, `author_bio`) VALUES
(1, '代码整洁之道', 'Robert C. Martin',
 'https://images.unsplash.com/photo-1461749280684-dccba630e2f6?w=400&h=600&fit=crop',
 '本书是软件工程领域的经典著作，阐述如何编写整洁、可维护的代码。作者从命名、函数、注释、格式等多个维度，给出了一系列实用的编码规范和最佳实践。',
 '9787115216872', '人民邮电出版社', '2010-01-01', 350, 1, '编程,代码质量,软件工程', 4.80, 1520, 'active', 'free', 1, 1,
 '软件工程经典著作，讲解如何写出整洁、可维护的代码。',
 'Robert C. Martin（鲍勃大叔），Object Mentor 公司创始人，软件工程领域知名顾问，敏捷宣言起草人之一。'),

(2, '深入理解计算机系统', 'Randal E. Bryant',
 'https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=400&h=600&fit=crop',
 '本书从程序员视角深入解析计算机系统工作原理，涵盖数据表示、程序结构、存储器层次、链接、异常控制流、虚拟内存、系统级 I/O、网络与并发编程等核心主题。',
 '9787111544937', '机械工业出版社', '2016-11-01', 719, 1, '计算机系统,操作系统,编程基础', 4.90, 2340, 'active', 'free', 1, 1,
 '从程序员视角解析计算机系统，是程序员的"内功心法"。',
 'Randal E. Bryant，卡内基梅隆大学计算机科学学院院长，IEEE Fellow，ACM Fellow。'),

(3, '活着', '余华',
 'https://images.unsplash.com/photo-1495446815901-a7297e633e8d?w=400&h=600&fit=crop',
 '讲述了福贵一生的悲欢故事，从地主少爷到贫农，历经家国变迁与亲人离散，却始终坚韧地活着。作品以平实笔触揭示生命的本质与意义。',
 '9787506365437', '作家出版社', '2012-08-01', 191, 3, '文学,小说,经典', 4.70, 5680, 'active', 'free', 1, 1,
 '余华代表作，讲述福贵一生的悲欢离合，深刻反映人生苦难与希望。',
 '余华，中国当代著名作家，代表作有《活着》《许三观卖血记》《兄弟》等，曾获意大利格林扎纳·卡佛文学奖。'),

(4, '人类简史', '尤瓦尔·赫拉利',
 'https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=400&h=600&fit=crop',
 '从认知革命到科学革命，讲述人类从一种普通动物成为地球主宰的宏大历史。作者以全新视角审视人类文明，引发对未来的深刻思考。',
 '9787508647357', '中信出版社', '2014-11-01', 414, 4, '历史,人文,社科', 4.60, 4230, 'active', 'free', 1, 1,
 '从认知革命到科学革命，讲述人类演化与文明发展的宏大历史。',
 '尤瓦尔·赫拉利，以色列历史学家，耶路撒冷希伯来大学历史系教授，全球知名公共知识分子。'),

(5, '设计模式：可复用面向对象软件的基础', 'Erich Gamma',
 'https://images.unsplash.com/photo-1498050108023-c5249f4df085?w=400&h=600&fit=crop',
 '经典设计模式著作，收录 23 种设计模式，是面向对象软件设计的圣经级参考书，由 GoF（四人帮）合著。',
 '9787111075752', '机械工业出版社', '2000-09-01', 395, 1, '设计模式,软件工程,面向对象', 4.50, 2890, 'active', 'free', 0, 1,
 'GoF 经典之作，收录 23 种设计模式，面向对象设计的必读参考。',
 'Erich Gamma，瑞士苏黎世联邦理工学院博士，Eclipse 项目核心开发者，JUnit 作者之一。'),

(6, 'Java 编程思想', 'Bruce Eckel',
 'https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=400&h=600&fit=crop',
 'Java 编程经典著作，全面介绍 Java 语言特性、面向对象编程思想、并发编程、设计模式等内容，适合有一定基础的程序员深入阅读。',
 '9787111558422', '机械工业出版社', '2019-01-01', 880, 1, 'Java,编程,面向对象', 4.60, 3120, 'active', 'free', 1, 1,
 'Java 程序员的必读经典，全面讲解 Java 语言与面向对象思想。',
 'Bruce Eckel，MindView 公司创始人，著有《Java 编程思想》《C++ 编程思想》等经典技术书籍。'),

(7, '深入浅出 MySQL', '翟振兴',
 'https://images.unsplash.com/photo-1544383835-254be186d4b1?w=400&h=600&fit=crop',
 '全面介绍 MySQL 数据库的基础知识、开发技巧、优化方法和运维管理，是 MySQL 开发者和管理员的实用参考书。',
 '9787115419390', '人民邮电出版社', '2017-09-01', 620, 1, 'MySQL,数据库,运维', 4.40, 1560, 'active', 'free', 0, 0,
 'MySQL 开发与运维实用指南，涵盖基础、优化、管理全流程。',
 '翟振兴，资深数据库专家，长期从事 MySQL 数据库的开发与运维工作。'),

(8, 'Redis 设计与实现', '黄健宏',
 'https://images.unsplash.com/photo-1633356122544-f134324a6cee?w=400&h=600&fit=crop',
 '深入剖析 Redis 底层数据结构与实现原理，涵盖字符串、列表、哈希、集合、有序集合等核心数据结构，以及持久化、事务、发布订阅等功能。',
 '9787111464747', '机械工业出版社', '2014-06-01', 410, 1, 'Redis,缓存,数据库', 4.70, 2340, 'active', 'free', 1, 1,
 '深入剖析 Redis 底层数据结构与实现原理，Redis 进阶必读。',
 '黄健宏，Redis 深度研究者，翻译了 Redis 官方文档，对 Redis 源码有深入研究。'),

(9, 'Spring 实战', 'Craig Walls',
 'https://images.unsplash.com/photo-1517842645767-c639042777db?w=400&h=600&fit=crop',
 '系统介绍 Spring 框架的核心概念和实战应用，涵盖依赖注入、AOP、数据访问、Web 开发、消息、安全等主题，是 Spring 入门与进阶的经典读物。',
 '9787115574670', '人民邮电出版社', '2022-03-01', 420, 1, 'Spring,Java,框架', 4.50, 1890, 'active', 'free', 0, 1,
 'Spring 框架入门与进阶经典，系统讲解 Spring 核心特性与实战应用。',
 'Craig Walls，Spring 资深开发者，Pivotal 公司工程师，知名技术作者。'),

(10, '算法导论', 'Thomas H. Cormen',
 'https://images.unsplash.com/photo-1551288049-bebda4e38f71?w=400&h=600&fit=crop',
 '算法领域权威教材，全面介绍算法设计与分析，涵盖排序、数据结构、图算法、动态规划、贪心算法、字符串匹配等核心主题，是计算机科学必备参考书。',
 '9787111407010', '机械工业出版社', '2013-01-01', 1292, 1, '算法,数据结构,计算机科学', 4.80, 4560, 'active', 'vip', 1, 1,
 '算法领域权威教材，全面介绍算法设计与分析，计算机科学必备。',
 'Thomas H. Cormen，达特茅斯学院计算机科学系教授，曾任达特茅斯学院计算机科学系主任。'),

(11, '百年孤独', '加西亚·马尔克斯',
 'https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=400&h=600&fit=crop',
 '魔幻现实主义文学经典，讲述布恩迪亚家族七代人的传奇故事，展现拉丁美洲百年兴衰史。作品融合现实与魔幻，是 20 世纪最重要的小说之一。',
 '9787544253994', '南海出版公司', '2011-06-01', 360, 3, '文学,小说,魔幻现实主义', 4.80, 6780, 'active', 'free', 1, 1,
 '魔幻现实主义文学经典，布恩迪亚家族七代人的传奇故事。',
 '加西亚·马尔克斯，哥伦比亚作家、记者，1982 年诺贝尔文学奖得主，魔幻现实主义文学代表人物。'),

(12, '深入理解 Java 虚拟机', '周志明',
 'https://images.unsplash.com/photo-1555066931-4365d14bab2c?w=400&h=600&fit=crop',
 '全面深入讲解 JVM 原理，涵盖内存管理、垃圾收集、类加载、字节码、并发、性能调优等核心主题，是 Java 工程师进阶必读经典。',
 '9787111649870', '机械工业出版社', '2019-12-01', 580, 1, 'JVM,Java,虚拟机', 4.90, 3890, 'active', 'vip', 1, 1,
 'JVM 原理深度解析，Java 工程师进阶必读经典。',
 '周志明，资深 Java 技术专家，著有《深入理解 Java 虚拟机》《凤凰架构》等技术畅销书。'),

(13, '围城', '钱钟书',
 'https://images.unsplash.com/photo-1495446815901-a7297e633e8d?w=400&h=600&fit=crop',
 '中国现代文学经典，以幽默讽刺的笔调描写知识分子方鸿渐的人生际遇，深刻揭示人性的弱点与时代的困境，被誉为"新儒林外史"。',
 '9787020024759', '人民文学出版社', '1991-02-01', 359, 3, '文学,小说,经典', 4.70, 4120, 'active', 'free', 0, 1,
 '钱钟书代表作，幽默讽刺的现代文学经典，揭示人性弱点与时代困境。',
 '钱钟书，中国现代著名作家、文学研究家，著有《围城》《管锥编》《谈艺录》等。'),

(14, '重构：改善既有代码的设计', 'Martin Fowler',
 'https://images.unsplash.com/photo-1542831371-29b0f74f9713?w=400&h=600&fit=crop',
 '软件工程经典著作，系统介绍代码重构的方法与实践，涵盖各种重构手法、代码坏味道识别、测试驱动等内容，是提升代码质量的必读参考。',
 '9787115511955', '人民邮电出版社', '2019-04-01', 432, 1, '重构,代码质量,软件工程', 4.70, 2340, 'active', 'free', 1, 1,
 '代码重构经典著作，系统介绍重构方法与实践，提升代码质量必读。',
 'Martin Fowler，ThoughtWorks 首席科学家，软件工程领域知名作者，著有《重构》《企业应用架构模式》等。'),

(15, '三体', '刘慈欣',
 'https://images.unsplash.com/photo-1532012197267-da84d127e7c5?w=400&h=600&fit=crop',
 '中国科幻文学里程碑之作，讲述地球文明与三体文明的接触与对抗，涵盖"地球往事"三部曲，展现宏大的宇宙观与深刻的哲学思考。',
 '9787536692930', '重庆出版社', '2008-01-01', 302, 3, '科幻,小说,宇宙', 4.80, 8900, 'active', 'free', 1, 1,
 '中国科幻文学里程碑之作，地球文明与三体文明的宏大叙事。',
 '刘慈欣，中国著名科幻作家，代表作"地球往事"三部曲（《三体》《黑暗森林》《死神永生》），2015 年雨果奖得主。');

-- =====================================================
-- 三、读书空间 - 书单数据（5个）
-- =====================================================
INSERT INTO `portal_book_list` (`id`, `title`, `description`, `cover`, `user_id`, `category_id`, `is_public`, `book_count`, `view_count`, `like_count`, `status`, `is_featured`, `access_level`, `tags`) VALUES
(1, '程序员必读书单', '精选编程领域的经典著作，从代码质量到系统设计，助你构建扎实的技术功底。',
 'https://images.unsplash.com/photo-1512820790803-83ca734da794?w=400&h=300&fit=crop', 1, 1, 1, 5, 1250, 234, 'active', 1, 'free', '编程,技术,经典'),
(2, '经典文学作品', '经典文学著作合集，感受文字的力量与思想的深度。',
 'https://images.unsplash.com/photo-1476275466078-4007374efbbe?w=400&h=300&fit=crop', 1, 3, 1, 4, 890, 156, 'active', 1, 'free', '文学,小说,经典'),
(3, 'Java 工程师进阶书单', '从基础到进阶，全面覆盖 Java 工程师成长路径，包含语言、JVM、框架、数据库等核心技能。',
 'https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=400&h=300&fit=crop', 1, 1, 1, 4, 680, 198, 'active', 1, 'free', 'Java,进阶,后端'),
(4, '科幻小说精选', '探索宇宙与未来的想象之旅，感受科幻文学的魅力。',
 'https://images.unsplash.com/photo-1532012197267-da84d127e7c5?w=400&h=300&fit=crop', 1, 3, 1, 2, 560, 178, 'active', 0, 'free', '科幻,小说,想象'),
(5, '数据库与缓存实战', '数据库与缓存技术实战书单，涵盖 MySQL、Redis 等核心存储技术。',
 'https://images.unsplash.com/photo-1544383835-254be186d4b1?w=400&h=300&fit=crop', 1, 1, 1, 2, 420, 134, 'active', 0, 'vip', '数据库,Redis,MySQL');

-- 书单-书籍关联
INSERT INTO `portal_book_list_item` (`book_list_id`, `book_id`, `sort`, `note`) VALUES
-- 书单1：程序员必读
(1, 1, 1, '代码质量的基石，必读经典'),
(1, 2, 2, '深入理解计算机底层'),
(1, 5, 3, '设计模式必备'),
(1, 14, 4, '重构改善代码设计'),
(1, 10, 5, '算法是程序员的内功'),
-- 书单2：经典文学
(2, 3, 1, '余华代表作，生命的重量'),
(2, 11, 2, '魔幻现实主义巅峰'),
(2, 13, 3, '钱钟书的讽刺艺术'),
(2, 4, 4, '人类文明的宏大叙事'),
-- 书单3：Java 进阶
(3, 6, 1, 'Java 编程思想，打牢基础'),
(3, 12, 2, 'JVM 原理深度解析'),
(3, 9, 3, 'Spring 框架实战'),
(3, 7, 4, 'MySQL 深入浅出'),
-- 书单4：科幻精选
(4, 15, 1, '中国科幻里程碑'),
(4, 11, 2, '魔幻现实主义经典'),
-- 书单5：数据库与缓存
(5, 7, 1, 'MySQL 实战指南'),
(5, 8, 2, 'Redis 底层原理');

-- =====================================================
-- 四、读书空间 - 金句数据（10条）
-- =====================================================
INSERT INTO `portal_book_quote` (`id`, `user_id`, `book_id`, `content`, `page`, `chapter`, `like_count`, `is_public`, `is_featured`, `location`) VALUES
(1,  1, 1,  '代码是写给人看的，顺便给机器执行。', '第15页', '第一章 有意义的命名', 156, 1, 1, '第一章'),
(2,  1, 3,  '人是为活着本身而活着的，而不是为了活着之外的任何事物所活着。', '第50页', '第一章', 423, 1, 1, '第一章'),
(3,  1, 4,  '我们之所以研究历史，不是为了知道未来，而是为了拓展自己的视野。', '第30页', '认知革命', 289, 1, 1, '第一章 认知革命'),
(4,  1, 11, '多年以后，奥雷里亚诺·布恩迪亚上校面对行刑队，将会回想起父亲带他去见识冰块的那个遥远的下午。', '第1页', '开篇', 567, 1, 1, '开篇'),
(5,  1, 10, '算法是计算机科学的核心，它告诉我们如何高效地解决问题。', '第5页', '前言', 234, 1, 0, '前言'),
(6,  1, 14, '任何傻瓜都能写出计算机能理解的代码，但优秀的程序员能写出人类能理解的代码。', '第20页', '第一章', 345, 1, 1, '第一章'),
(7,  1, 13, '城外的人想冲进去，城里的人想逃出来。', '第80页', '第三章', 678, 1, 1, '第三章'),
(8,  1, 2,  '信息就是位加上上下文。', '第25页', '第一章', 198, 1, 0, '第一章'),
(9,  1, 15, '弱小和无知不是生存的障碍，傲慢才是。', '第120页', '黑暗森林', 892, 1, 1, '第二部 黑暗森林'),
(10, 1, 8,  '简单是可靠的先决条件。', '第45页', '第一章', 167, 1, 0, '第一章');

-- =====================================================
-- 五、读书空间 - 阅读进度数据（模拟用户1的阅读记录）
-- =====================================================
INSERT INTO `portal_reading_progress` (`user_id`, `book_id`, `status`, `progress`, `pages_read`, `start_date`, `finish_date`, `note`) VALUES
(1, 1,  'finished',  100, 350, '2026-05-01', '2026-05-15', '经典之作，受益匪浅。命名、函数、注释这些看似细节的东西，其实决定了代码的可维护性。'),
(1, 2,  'reading',    45, 324, '2026-06-01', NULL,         '正在啃，内容很硬核，需要反复理解。'),
(1, 3,  'finished',  100, 191, '2026-04-10', '2026-04-15', '读完久久不能平静，福贵的一生让人唏嘘。活着本身就是意义。'),
(1, 4,  'finished',  100, 414, '2026-03-20', '2026-04-05', '视角宏大，重新审视了人类文明的发展。'),
(1, 8,  'want_to_read', 0,  0,   NULL,         NULL,         NULL),
(1, 11, 'reading',    30, 108, '2026-06-10', NULL,         '魔幻现实主义，人名太多有点绕，但文笔迷人。'),
(1, 12, 'want_to_read', 0,  0,   NULL,         NULL,         NULL),
(1, 15, 'finished',  100, 302, '2026-02-15', '2026-02-25', '震撼！黑暗森林法则和降维打击的想象力令人叹为观止。');

-- =====================================================
-- 六、面试空间 - 清理旧数据
-- =====================================================
DELETE FROM `portal_interview_question`     WHERE `id` BETWEEN 1 AND 100;
DELETE FROM `portal_interview_experience`    WHERE `id` BETWEEN 1 AND 100;
DELETE FROM `portal_interview_resume_template` WHERE `id` BETWEEN 1 AND 100;
DELETE FROM `portal_interview_submission`    WHERE `id` BETWEEN 1 AND 100;
DELETE FROM `portal_interview_bookmark`     WHERE `id` BETWEEN 1 AND 100;
DELETE FROM `portal_interview_attempt`      WHERE `id` BETWEEN 1 AND 100;
DELETE FROM `portal_interview_comment`       WHERE `id` BETWEEN 1 AND 100;

-- 重置自增ID
ALTER TABLE `portal_interview_question`         AUTO_INCREMENT = 1;
ALTER TABLE `portal_interview_experience`      AUTO_INCREMENT = 1;
ALTER TABLE `portal_interview_resume_template` AUTO_INCREMENT = 1;
ALTER TABLE `portal_interview_submission`      AUTO_INCREMENT = 1;
ALTER TABLE `portal_interview_bookmark`        AUTO_INCREMENT = 1;
ALTER TABLE `portal_interview_attempt`         AUTO_INCREMENT = 1;
ALTER TABLE `portal_interview_comment`         AUTO_INCREMENT = 1;

-- =====================================================
-- 七、面试空间 - 题目数据（20道，覆盖各分类各难度）
-- 分类：1=Java后端 2=Go 3=前端 4=数据库 5=系统设计 6=网络 7=算法 8=安全
-- =====================================================
INSERT INTO `portal_interview_question` (`id`, `title`, `description`, `difficulty`, `category_id`, `tags`, `companies`, `acceptance_rate`, `submission_count`, `like_count`, `hint`, `solution`, `sort`, `status`) VALUES
-- 算法题
(1, '两数之和',
 '给定一个整数数组 nums 和一个整数目标值 target，请你在该数组中找出和为目标值的那两个整数，并返回它们的数组下标。你可以假设每种输入只会对应一个答案。示例：输入 nums = [2,7,11,15], target = 9，输出 [0,1]。',
 'easy', 7, '数组,哈希表,高频题', '字节跳动,阿里巴巴,腾讯,美团', 65.50, 12500, 2580,
 '可以使用哈希表降低时间复杂度至 O(n)',
 '使用哈希表存储已访问数字及其索引，遍历时查找 target - nums[i] 是否在哈希表中。时间复杂度 O(n)，空间复杂度 O(n)。',
 1, 'active'),

(2, '最长无重复字符子串',
 '给定一个字符串 s，请你找出其中不含有重复字符的最长子串的长度。示例：输入 s = "abcabcbb"，输出 3（"abc"）。',
 'medium', 7, '字符串,滑动窗口,经典题', '字节跳动,阿里巴巴', 48.20, 8900, 1680,
 '使用滑动窗口维护当前无重复子串',
 '使用滑动窗口，用哈希表记录字符最后出现位置。右指针扩展窗口，遇到重复字符时左指针跳到重复字符下一位。时间复杂度 O(n)。',
 2, 'active'),

(3, 'LRU 缓存机制',
 '请你设计并实现一个满足 LRU（最近最少使用）缓存约束的数据结构。实现 LRUCache 类：LRUCache(int capacity) 初始化；int get(int key) 获取值；void put(int key, int value) 更新或插入。',
 'medium', 7, '设计,哈希表,双向链表', '字节跳动,腾讯,美团', 35.80, 6500, 1250,
 '需要 O(1) 时间复杂度的 get 和 put 操作',
 '使用哈希表 + 双向链表。哈希表存储 key 到节点的映射，双向链表维护访问顺序。访问时移到链表头部，淘汰时删除尾部。时间复杂度 O(1)。',
 3, 'active'),

(4, '二叉树的前序遍历',
 '给你二叉树的根节点 root，返回它节点值的前序遍历。',
 'easy', 7, '二叉树,树,递归,迭代', '阿里巴巴,腾讯', 72.30, 4500, 890,
 '可以使用递归或迭代方式',
 '递归法：根-左-右。迭代法：使用栈，先压右子节点再压左子节点。时间复杂度 O(n)。',
 4, 'active'),

-- Java 后端题
(5, 'HashMap 的底层实现原理',
 '请详细描述 Java 中 HashMap 的底层实现原理，包括数据结构、扩容机制、哈希冲突处理等。',
 'medium', 1, 'Java,集合,HashMap,八股文', '阿里巴巴,字节跳动,腾讯,美团', 55.30, 8200, 1890,
 '从 JDK 1.7 和 JDK 1.8 的区别入手',
 'JDK 1.8 中 HashMap 采用数组 + 链表/红黑树。数组长度为 2 的幂次，通过 hash & (n-1) 定位桶。链表长度 >= 8 且数组长度 >= 64 时转红黑树。扩容因子 0.75，扩容为 2 倍。',
 5, 'active'),

(6, 'synchronized 和 ReentrantLock 的区别',
 '请比较 synchronized 关键字与 ReentrantLock 的异同，并说明各自的使用场景。',
 'medium', 1, 'Java,并发,锁,高频题', '阿里巴巴,字节跳动,腾讯', 62.10, 6800, 1450,
 '从锁的实现、特性、性能三个维度比较',
 'synchronized 是 JVM 层面的关键字，自动释放锁；ReentrantLock 是 API 层面的类，需手动 unlock。ReentrantLock 支持公平锁、可中断、多条件变量、超时获取。性能上 JDK 1.6 后 synchronized 优化后差距不大。',
 6, 'active'),

(7, 'JVM 垃圾回收机制',
 '请描述 JVM 的垃圾回收机制，包括如何判断对象存活、GC 算法、常见垃圾收集器等。',
 'hard', 1, 'JVM,GC,垃圾回收,八股文', '阿里巴巴,字节跳动,美团,百度', 42.50, 5600, 1230,
 '从"判断存活-回收算法-收集器"三个层次回答',
 '判断存活：可达性分析（GC Roots）。回收算法：标记-清除、标记-复制、标记-整理。收集器：CMS（老年代并发）、G1（分区回收）、ZGC（低延迟）。G1 将堆分为 Region，优先回收垃圾最多的 Region。',
 7, 'active'),

(8, 'Spring Bean 的生命周期',
 '请描述 Spring 中 Bean 的完整生命周期。',
 'medium', 1, 'Spring,Bean,生命周期', '阿里巴巴,腾讯,字节跳动', 58.40, 4200, 980,
 '从实例化到销毁的完整流程',
 '实例化 → 属性赋值 → BeanNameAware/BeanFactoryAware → BeanPostProcessor.before → InitializingBean.afterPropertiesSet/init-method → BeanPostProcessor.after → 使用 → DisposableBean.destroy/destroy-method。',
 8, 'active'),

-- 数据库题
(9, 'MySQL 索引底层原理',
 '请描述 MySQL InnoDB 引擎的索引底层实现，并解释为什么用 B+ 树而不是 B 树或红黑树。',
 'medium', 4, 'MySQL,索引,B+树,八股文', '阿里巴巴,腾讯,字节跳动,美团', 51.20, 7100, 1560,
 '从 B+ 树的结构特点出发',
 'InnoDB 采用 B+ 树索引。B+ 树非叶子节点不存数据，扇出更大，树更矮，IO 次数少。叶子节点用双向链表连接，范围查询高效。相比 B 树，B+ 树查询稳定；相比红黑树，B+ 树高度更低，磁盘 IO 更少。',
 9, 'active'),

(10, 'MySQL 事务隔离级别与 MVCC',
 '请说明 MySQL 的四种事务隔离级别，以及 InnoDB 的 MVCC 机制是如何实现的。',
 'hard', 4, 'MySQL,事务,MVCC,隔离级别', '阿里巴巴,字节跳动,腾讯', 38.60, 5400, 1180,
 '从隔离级别的问题（脏读/不可重复读/幻读）入手',
 '四种隔离级别：读未提交、读已提交、可重复读（默认）、串行化。MVCC 通过隐藏列（trx_id/roll_pointer）+ undo log 版本链 + ReadView 实现。RC 每次查询生成新 ReadView，RR 在事务开始时生成一次。',
 10, 'active'),

(11, 'Redis 持久化机制',
 '请描述 Redis 的两种持久化机制 RDB 和 AOF 的原理与区别。',
 'medium', 4, 'Redis,持久化,RDB,AOF', '阿里巴巴,腾讯,字节跳动', 60.30, 4800, 1020,
 '从触发时机、文件格式、恢复速度比较',
 'RDB：二进制快照，bgsave 触发，恢复快但可能丢数据。AOF：追加命令，可配置 always/everysec/no，数据安全但文件大。Redis 4.0+ 支持混合持久化（RDB + AOF 增量）。',
 11, 'active'),

-- 前端题
(12, 'JavaScript 事件循环机制',
 '请描述 JavaScript 的事件循环（Event Loop）机制，包括宏任务和微任务的执行顺序。',
 'medium', 3, 'JavaScript,事件循环,异步,高频题', '字节跳动,腾讯,阿里巴巴', 54.60, 5600, 1340,
 '从同步任务、微任务、宏任务的执行顺序理解',
 'JS 是单线程。执行栈清空后，先执行所有微任务（Promise.then、MutationObserver），再执行一个宏任务（setTimeout、setInterval、I/O）。每轮循环：同步代码 → 微任务队列清空 → 一个宏任务。',
 12, 'active'),

(13, 'Vue 响应式原理',
 '请描述 Vue 3 的响应式系统原理，与 Vue 2 有何区别？',
 'medium', 3, 'Vue,响应式,Proxy,高频题', '字节跳动,腾讯,阿里巴巴', 49.80, 4200, 980,
 '从 Object.defineProperty 和 Proxy 的区别入手',
 'Vue 2 用 Object.defineProperty 劫持属性，无法监听新增属性和数组下标修改。Vue 3 用 Proxy 代理整个对象，支持新增/删除属性监听，性能更好。配合 effect/track/trigger 实现依赖收集与派发更新。',
 13, 'active'),

-- 系统设计题
(14, '设计一个短链系统',
 '请设计一个短链服务，要求：给定长 URL 生成短链，访问短链时跳转到原 URL。支持 QPS 10万。',
 'hard', 5, '系统设计,分布式,高频题', '字节跳动,阿里巴巴,腾讯', 32.10, 3800, 890,
 '从发号器、存储、缓存、跳转四个模块设计',
 '发号器：可雪花算法或 Redis 自增。存储：MySQL 存长短链映射，短链字段加唯一索引。缓存：Redis 缓存热点短链。跳转：查询缓存 → DB → 302 重定向。可考虑布隆过滤器防穿透。',
 14, 'active'),

(15, '设计一个秒杀系统',
 '请设计一个电商秒杀系统，要求：支持 10 万并发，防止超卖，保证公平。',
 'hard', 5, '系统设计,高并发,秒杀,真题', '阿里巴巴,京东,美团,拼多多', 28.50, 4500, 1230,
 '从限流、缓存、异步、库存四个维度设计',
 '前端：静态化 + 按钮防抖。网关：限流 + 黑名单。服务层：Redis 预扣库存（Lua 保证原子），MQ 异步下单。数据库：乐观锁或分布式锁最终扣减。库存预热到 Redis，用 Lua 脚本判断库存并扣减。',
 15, 'active'),

-- 网络题
(16, 'TCP 三次握手与四次挥手',
 '请描述 TCP 的三次握手和四次挥手过程，为什么握手是三次而挥手是四次？',
 'easy', 6, 'TCP,网络,握手,八股文', '阿里巴巴,腾讯,字节跳动,百度', 68.90, 9200, 2100,
 '从全双工通信和状态转换理解',
 '三次握手：SYN → SYN+ACK → ACK，建立双向连接。四次挥手：FIN → ACK → FIN → ACK，因为 TCP 全双工，关闭需双向确认。挥手多一次是因为收到 FIN 时可能还有数据未发完，需先 ACK 再等数据发完后发 FIN。',
 16, 'active'),

(17, 'HTTP 与 HTTPS 的区别',
 '请说明 HTTP 和 HTTPS 的区别，HTTPS 的握手过程是怎样的？',
 'medium', 6, 'HTTP,HTTPS,加密,高频题', '阿里巴巴,腾讯,字节跳动', 61.20, 6800, 1560,
 '从端口、加密、证书、性能四个维度比较',
 'HTTP 明文传输端口 80；HTTPS = HTTP + SSL/TLS，端口 443。HTTPS 握手：客户端发送支持的加密套件 → 服务端返回证书和选定套件 → 客户端验证证书并生成对称密钥用公钥加密发送 → 双方用对称密钥通信。',
 17, 'active'),

-- Go 语言题
(18, 'Go 的 goroutine 调度原理',
 '请描述 Go 的 GMP 调度模型。',
 'hard', 2, 'Go,goroutine,GMP,并发', '字节跳动,腾讯,哔哩哔哩', 40.20, 3200, 890,
 '从 G、M、P 三个角色理解',
 'G = goroutine，M = 操作系统线程，P = 处理器（调度上下文，持有本地运行队列）。P 的数量等于 GOMAXPROCS。M 必须绑定 P 才能执行 G。当本地队列空时，P 会从其他 P 偷取一半 G（work stealing）。',
 18, 'active'),

-- 安全题
(19, 'SQL 注入原理与防御',
 '请说明 SQL 注入的攻击原理，以及如何防御。',
 'easy', 8, '安全,SQL注入,防御', '阿里巴巴,腾讯,百度', 70.50, 4100, 920,
 '从拼接 SQL 的危害和参数化查询入手',
 '原理：用户输入被拼接到 SQL 中，导致执行恶意 SQL。防御：1. 使用参数化查询（预编译）；2. 使用 ORM 框架；3. 输入校验与转义；4. 最小权限原则；5. WAF 防火墙。',
 19, 'active'),

(20, 'JWT 原理与使用场景',
 '请描述 JWT（JSON Web Token）的结构、原理与使用场景。',
 'medium', 8, 'JWT,认证,Token,高频题', '阿里巴巴,腾讯,字节跳动', 57.80, 5200, 1240,
 '从 Header.Payload.Signature 三部分理解',
 'JWT 由 Header（算法类型）、Payload（声明数据）、Signature（签名）三部分 Base64URL 编码后用 . 连接。服务端用密钥签名，客户端携带 Token，服务端验签。适合无状态认证、单点登录、API 鉴权。缺点是无法主动失效（需配合 Redis 黑名单）。',
 20, 'active');

-- =====================================================
-- 八、面试空间 - 面经数据（5篇）
-- =====================================================
INSERT INTO `portal_interview_experience` (`id`, `user_id`, `title`, `company`, `position`, `year`, `month`, `content`, `tags`, `view_count`, `like_count`, `comment_count`, `status`) VALUES
(1, 1, '字节跳动后端开发面试经验分享（已拿 offer）',
 '字节跳动', '后端开发工程师', 2026, 5,
 '## 面试背景\n\n双非本科，3 年 Java 后端经验，投递字节跳动后端开发岗。\n\n## 一面（技术面，60分钟）\n\n1. 自我介绍\n2. 项目深挖：问了简历上的分布式锁项目，如何实现、Redisson 原理\n3. 基础八股：\n   - HashMap 底层原理，为什么用红黑树\n   - synchronized 锁升级过程\n   - JVM GC 算法，G1 和 CMS 区别\n4. 算法题：LRU 缓存（手撕）\n5. 反问环节\n\n## 二面（技术面，75分钟）\n\n1. 项目架构：画系统架构图，问缓存一致性方案\n2. MySQL：索引失效场景、explain 执行计划\n3. 场景题：设计一个秒杀系统，如何防超卖\n4. 算法题：最长递增子序列\n\n## 三面（技术面，50分钟）\n\n1. 职业规划\n2. 技术深度：分布式事务、消息队列幂等\n3. 场景题：线上 OOM 如何排查\n4. HR 面：薪资、到岗时间\n\n## 总结\n\n字节面试重基础和算法，八股要背熟，算法要手撕。建议刷 LeetCode 200+ 题。',
 '后端,Java,字节跳动,面经', 5600, 234, 45, 'published'),

(2, 1, '腾讯前端开发面试总结（含真题）',
 '腾讯', '前端开发工程师', 2026, 4,
 '## 个人背景\n\n985 硕士，前端方向，投递腾讯 PCG 前端岗。\n\n## 一面\n\n1. 自我介绍 + 项目介绍\n2. JS 基础：\n   - 事件循环机制，宏任务微任务\n   - 闭包、原型链\n   - Promise 实现\n3. CSS：BFC、Flex 布局、居中方案\n4. 框架：Vue 响应式原理、Diff 算法\n5. 算法：反转链表\n\n## 二面\n\n1. HTTP：缓存策略、跨域方案\n2. 性能优化：首屏加载、白屏\n3. 场景题：实现一个无限滚动列表\n4. 算法：二叉树层序遍历\n\n## 三面\n\n1. 项目难点\n2. 团队协作\n3. 反问\n\n## 体验\n\n腾讯前端面试重基础，JS/CSS/HTTP 八股要扎实，算法中等难度。',
 '前端,Vue,腾讯,面经', 3800, 189, 32, 'published'),

(3, 1, '阿里巴巴 Java 后端校招面经',
 '阿里巴巴', 'Java 后端工程师', 2026, 3,
 '## 背景\n\n211 本科校招，投递阿里中间件团队。\n\n## 一面（电话面）\n\n1. 自我介绍\n2. 项目：秒杀系统如何设计\n3. Java 基础：\n   - ConcurrentHashMap 原理\n   - ThreadLocal 内存泄漏\n   - 线程池参数配置\n4. 数据库：MySQL 索引、MVCC\n5. 场景：如何设计一个分布式 ID 生成器\n\n## 二面（视频面）\n\n1. 项目深挖\n2. JVM：内存模型、GC 调优经验\n3. Spring：AOP 原理、循环依赖\n4. 中间件：RocketMQ 消息丢失、重复消费\n5. 算法：合并 K 个有序链表\n\n## 三面（交叉面）\n\n1. 技术视野：对云原生的理解\n2. 开放题：如何设计一个微博 feed 流\n3. HR 面\n\n## 总结\n\n阿里面试重深度和广度，会追问到底层原理。建议把 JVM、并发、中间件吃透。',
 '后端,Java,阿里巴巴,校招', 4200, 198, 28, 'published'),

(4, 1, '美团后端实习面试经验',
 '美团', '后端开发实习生', 2026, 2,
 '## 背景\n\n大三实习，投递美团到店事业群。\n\n## 一面\n\n1. 自我介绍\n2. 项目：简单的 CRUD 项目\n3. 基础：\n   - Java 集合：ArrayList vs LinkedList\n   - HashMap put 流程\n   - 多线程：synchronized 和 Lock 区别\n4. MySQL：索引、事务隔离级别\n5. 算法：两数之和\n\n## 二面\n\n1. 项目优化点\n2. Spring：IOC、AOP\n3. Redis：数据类型、持久化\n4. 场景：如何防止重复下单\n5. 算法：最长回文子串\n\n## 体验\n\n美团面试相对友好，基础为主，算法简单。适合作为练手。',
 '后端,Java,美团,实习', 2800, 156, 18, 'published'),

(5, 1, '拼多多后端社招面经（3年经验）',
 '拼多多', '后端开发工程师', 2026, 1,
 '## 背景\n\n3 年 Java 后端经验，跳槽拼多多。\n\n## 一面\n\n1. 项目：高并发场景设计\n2. Java：\n   - JMM 内存模型\n   - volatile 原理\n   - AQS 原理\n3. MySQL：\n   - 索引优化\n   - 分库分表方案\n4. Redis：集群模式、分布式锁\n5. 算法：合并两个有序数组\n\n## 二面\n\n1. 分布式：CAP、BASE 理论\n2. 消息队列：Kafka 架构、消息顺序\n3. 场景：设计一个抢红包系统\n4. 算法：二叉树最近公共祖先\n\n## 三面\n\n1. 架构设计：微服务拆分\n2. 技术选型：为什么选 Spring Cloud\n3. HR：薪资期望\n\n## 总结\n\n拼多多面试节奏快，问题密集，重实战。薪资有竞争力但工作强度大。',
 '后端,Java,拼多多,社招', 3500, 178, 22, 'published');

-- =====================================================
-- 九、面试空间 - 简历模板数据（4个）
-- =====================================================
INSERT INTO `portal_interview_resume_template` (`id`, `title`, `description`, `cover`, `download_url`, `category`, `file_type`, `file_size`, `is_premium`, `usage_guide`, `like_count`, `download_count`, `sort`, `status`) VALUES
(1, '技术岗通用简历模板',
 '适用于后端、前端、算法等技术岗位的通用简历模板，简洁大方，突出技术栈与项目经验。包含教育背景、工作经历、项目经验、技能清单等模块。',
 'https://images.unsplash.com/photo-1586281380349-632531db7ed4?w=400&h=300&fit=crop',
 '/download/resume/tech-general.docx', '技术岗', 'docx', 245760, 0,
 '1. 下载后用 Word 打开；2. 替换个人信息；3. 突出量化成果；4. 控制在 1-2 页。',
 456, 1234, 1, '0'),

(2, '应届生简洁简历模板',
 '适合应届生求职使用的简洁风格简历模板，重点突出实习经历、校园活动与学术成果。',
 'https://images.unsplash.com/photo-1553877522-43269d4ea984?w=400&h=300&fit=crop',
 '/download/resume/student-simple.docx', '应届生', 'docx', 198400, 0,
 '1. 教育背景放最前；2. 实习经历详写；3. 校园活动简写；4. 技能匹配岗位。',
 389, 856, 2, '0'),

(3, '高级工程师简历模板（VIP）',
 '面向 5 年以上经验的高级工程师，突出架构设计、技术领导力与复杂项目经验。VIP 专享。',
 'https://images.unsplash.com/photo-1517245386807-bb43f82fee33?w=400&h=300&fit=crop',
 '/download/resume/senior-engineer.docx', '社招', 'docx', 312400, 1,
 '1. 突出架构能力；2. 量化业务影响；3. 展示团队管理；4. 技术深度与广度并重。',
 234, 432, 3, '0'),

(4, '产品经理简历模板',
 '适用于产品经理岗位，突出产品规划、数据分析与项目落地能力。',
 'https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?w=400&h=300&fit=crop',
 '/download/resume/product-manager.docx', '产品岗', 'docx', 267800, 0,
 '1. 突出产品方法论；2. 数据驱动决策；3. 项目成果量化；4. 跨团队协作能力。',
 178, 345, 4, '0');

-- =====================================================
-- 十、面试空间 - 题目收藏数据（模拟用户1收藏）
-- =====================================================
INSERT INTO `portal_interview_bookmark` (`question_id`, `user_id`, `note`, `create_time`) VALUES
(5, 1, 'HashMap 必考题，要背熟底层原理', NOW()),
(7, 1, 'JVM GC 重点，G1 和 ZGC 的区别要搞清楚', NOW()),
(9, 1, 'MySQL 索引原理，B+ 树是核心', NOW()),
(14, 1, '系统设计题，短链系统是经典案例', NOW()),
(15, 1, '秒杀系统设计，高并发必考', NOW());

-- =====================================================
-- 十一、面试空间 - 做题记录数据（模拟用户1答题）
-- =====================================================
INSERT INTO `portal_interview_attempt` (`question_id`, `user_id`, `attempt_count`, `last_attempt_at`, `status`, `first_solved_at`, `last_solved_at`) VALUES
(1, 1, 3, NOW(), 'solved', '2026-06-01 10:00:00', '2026-06-15 14:00:00'),
(2, 1, 2, NOW(), 'solved', '2026-06-05 11:00:00', '2026-06-10 16:00:00'),
(3, 1, 5, NOW(), 'solved', '2026-06-08 09:00:00', '2026-06-20 15:00:00'),
(4, 1, 1, NOW(), 'solved', '2026-06-12 10:00:00', '2026-06-12 10:00:00'),
(5, 1, 2, NOW(), 'attempted', NULL, NULL),
(9, 1, 1, NOW(), 'solved', '2026-06-18 14:00:00', '2026-06-18 14:00:00');

-- =====================================================
-- 十二、面试空间 - 面经评论数据
-- =====================================================
INSERT INTO `portal_interview_comment` (`experience_id`, `user_id`, `parent_id`, `reply_to_user_id`, `content`, `like_count`, `status`, `create_time`) VALUES
(1, 2, 0, 0, '感谢分享！请问字节二面的分布式锁项目，用的是 Redisson 还是手写的？', 12, '0', NOW()),
(1, 1, 0, 2, '用的 Redisson，但面试官会追问手写 Redis 分布式锁的坑（如锁续期、可重入）', 8, '0', NOW()),
(1, 3, 0, 0, '同拿到字节 offer，确实重算法，建议刷到 300 题', 5, '0', NOW()),
(2, 2, 0, 0, '前端八股确实多，事件循环和 Promise 几乎必考', 8, '0', NOW()),
(3, 4, 0, 0, '校招阿里竞争激烈，楼主能拿到很厉害了', 3, '0', NOW());

-- =====================================================
-- 恢复外键检查
-- =====================================================
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;

-- =====================================================
-- 测试数据插入完成（第 3/5 部分）
-- 统计：
--   读书空间：15 本书 + 5 个书单 + 17 条关联 + 10 条金句 + 8 条阅读进度
--   面试空间：20 道题目 + 5 篇面经 + 4 个简历模板 + 5 条收藏 + 6 条做题记录 + 5 条评论
-- =====================================================


-- #############################################################
-- # 第 4/5 部分：48_portal_book_seed_data.sql                  #
-- # 来源：读书模块 v1.0 第三阶段：种子数据补全                    #
-- # 说明：为已有书籍补充第三阶段字段 + 创建章节正文 + 创建推荐位数据  #
-- #       补全发现页/首页/章节阅读页所需的真实内容                  #
-- #############################################################
-- ============================================================
-- 读书模块 v1.0 第三阶段：种子数据补全
-- 创建时间：2026-07-01
-- 说明：
--   补全第三阶段功能所需的数据，让发现页/首页/章节阅读页有真实内容可展示：
--     1. 为已有书籍补充第三阶段字段（type/serial_status/word_count/chapter_count/latest_chapter 等）
--     2. 为 3 本书创建真实章节正文（portal_book_chapter，每本 3 章）
--     3. 创建推荐位数据（portal_book_recommend：discover_banner / limit_free / home_hot）
--   特性：幂等可重复执行（先清理旧数据再重建）
--   依赖：
--     - 26_reading_interview_test_data.sql 已执行（存在 portal_book 1-15 号书籍）
--     - 42_portal_book_chapter_init.sql 已执行（portal_book_chapter 表已建）
--     - 46_portal_book_recommend_init.sql 已执行（portal_book_recommend 表已建）
-- ============================================================

-- ============================================================
-- 一、清理旧数据（幂等：可重复执行）
-- ============================================================

-- 1.1 清理旧章节正文（清理本脚本管理的所有 15 本书）
DELETE FROM `portal_book_chapter` WHERE `book_id` BETWEEN 1 AND 15;

-- 1.2 清理旧推荐位数据（仅清理本脚本管理的 3 个位置）
DELETE FROM `portal_book_recommend`
WHERE `position` IN ('discover_banner', 'limit_free', 'home_hot');

-- ============================================================
-- 二、章节正文数据（3 本书 × 3 章 = 9 章）
-- ============================================================

-- -------------------------------------------------------
-- 书 1：代码整洁之道（3 章）
-- -------------------------------------------------------
INSERT INTO `portal_book_chapter`
(`book_id`, `title`, `content`, `content_markdown`, `editor_mode`, `word_count`, `chapter_no`, `is_free`, `is_published`, `publish_time`, `view_count`, `create_by`, `create_time`)
VALUES
(1, '第1章 整洁代码',
 '<h2>整洁代码的含义</h2><p>写整洁代码，是从"能让机器运行"走向"让人能读懂"的第一步。Bjarne Stroustrup 说过："整洁代码让别人读起来感到愉悦。"这不仅关乎审美，更关乎软件的可维护性。</p><h3>1.1 为什么要整洁</h3><p>代码被阅读的次数远多于被编写的次数。一份混乱的代码，在三个月后连作者自己都难以理解。整洁代码的核心目标，是降低理解成本，让团队协作更顺畅。</p><h3>1.2 童子军规则</h3><p>"让营地比你来时更干净。"每次提交代码时，都比之前更好一点。改名一个含糊的变量、拆分一个过长的函数、删除一段无用的注释，都是改善。</p><blockquote>整洁代码不是一次性的重构，而是日复一日的习惯。</blockquote>',
 '# 第1章 整洁代码\n\n整洁代码让别人读起来感到愉悦。代码被阅读的次数远多于被编写的次数。\n\n## 1.1 为什么要整洁\n\n一份混乱的代码，在三个月后连作者自己都难以理解。整洁代码的核心目标，是降低理解成本。\n\n## 1.2 童子军规则\n\n让营地比你来时更干净。每次提交代码时，都比之前更好一点。',
 'markdown', 320, 1, 1, 1, NOW(), 120, 'admin', NOW()),
(1, '第2章 有意义的命名',
 '<h2>命名是程序员的第一难题</h2><p>好的命名能让代码自解释，坏的命名会让读者陷入猜谜游戏。Phil Karlton 说："在计算机科学中只有两件难事：缓存失效和命名。"</p><h3>2.1 名副其实</h3><p>变量名应该说明它"是什么"，而不是"怎么做"。例如 <code>d</code> 不如 <code>daysSinceCreation</code> 清晰。</p><h3>2.2 避免误导</h3><p>不要用 <code>accountList</code> 来表示一个账号组——除非它真的是 List 类型，否则用 <code>accounts</code> 更安全。</p><h3>2.3 有意义的区分</h3><p><code>getActiveAccount()</code> 和 <code>getActiveAccounts()</code> 同时存在是无意义的区分，<code>getActiveAccountInfo()</code> 同样糟糕。</p>',
 '# 第2章 有意义的命名\n\n好的命名能让代码自解释。Phil Karlton 说：在计算机科学中只有两件难事：缓存失效和命名。\n\n## 2.1 名副其实\n\n变量名应该说明它"是什么"。\n\n## 2.2 避免误导\n\n不要用 accountList 来表示一个账号组。\n\n## 2.3 有意义的区分\n\ngetActiveAccount 和 getActiveAccounts 同时存在是无意义的区分。',
 'markdown', 380, 2, 1, 1, NOW(), 85, 'admin', NOW()),
(1, '第3章 函数',
 '<h2>函数应该短小精悍</h2><p>函数的第一规则：要短小。函数的第二规则：还要更短小。20 世纪 60 年代的函数平均 50 行，而今天我们追求 4-10 行的函数。</p><h3>3.1 只做一件事</h3><p>函数应该只做一件事，做好这件事，只做这一件事。判断方法：能否再拆出一个子函数？</p><h3>3.2 参数</h3><p>最理想的参数数量是 0，其次是 1，尽量避免 3 个以上的参数。参数越多，测试组合越多。</p><h3>3.3 无副作用</h3><p>函数承诺做一件事，却偷偷做了别的事——这是 bug 的温床。</p><pre><code>// 坏例子：检查密码的同时初始化了会话\nfunction checkPassword(user, password) {\n  if (user.password === password) {\n    Session.initialize();  // 副作用\n    return true;\n  }\n  return false;\n}</code></pre>',
 '# 第3章 函数\n\n函数的第一规则：要短小。函数的第二规则：还要更短小。\n\n## 3.1 只做一件事\n\n函数应该只做一件事，做好这件事，只做这一件事。\n\n## 3.2 参数\n\n最理想的参数数量是 0，其次是 1，尽量避免 3 个以上的参数。\n\n## 3.3 无副作用\n\n函数承诺做一件事，却偷偷做了别的事——这是 bug 的温床。',
 'markdown', 450, 3, 1, 1, NOW(), 96, 'admin', NOW());

-- -------------------------------------------------------
-- 书 3：活着（3 章，文学小说）
-- -------------------------------------------------------
INSERT INTO `portal_book_chapter`
(`book_id`, `title`, `content`, `content_markdown`, `editor_mode`, `word_count`, `chapter_no`, `is_free`, `is_published`, `publish_time`, `view_count`, `create_by`, `create_time`)
VALUES
(3, '第一章 少爷福贵',
 '<p>我比现在年轻十岁的时候，获得了一个游手好闲的职业，去乡间收集民间歌谣。那一年的整个夏天，如同一只乱飞的麻雀，游荡在知了和阳光充斥的村庄。我喜欢喝农民那种带有苦味的茶水，他们的茶桶就放在田埂的树下，我毫无顾忌地拿起被他们用过的满是尘土的碗，舀水喝。</p><p>那时候我刚刚结束了和家珍的婚事，家珍是米行老板的女儿，她家有良田百亩。我爹总是说，我们家从前也是地主，只是到了我爷爷那一辈败了不少。我爹说这话的时候，总是拿眼睛瞟我，我知道他的意思是让我别学我爷爷。</p><p>可我那时候是个败家子，穿着丝绸，整天往城里跑，不是赌钱就是去妓院。我爹气得直跺脚，他说："福贵啊，你这样下去，迟早要把家产败光。"我听了只是笑笑，心想，家产那么多，哪里败得光。</p><p>后来我遇见了龙二，他是个赌徒，赌技高超。我和他赌了一夜，输了一百多亩地。我爹知道后，当场气得吐血，没过几天就死了。我把剩下的地都卖了，搬出大宅院，住进了茅草屋。</p>',
 '# 第一章 少爷福贵\n\n我比现在年轻十岁的时候，获得了一个游手好闲的职业，去乡间收集民间歌谣。\n\n那时候我刚刚结束了和家珍的婚事，家珍是米行老板的女儿，她家有良田百亩。\n\n可我那时候是个败家子，穿着丝绸，整天往城里跑，不是赌钱就是去妓院。我爹气得直跺脚。\n\n后来我遇见了龙二，他是个赌徒，赌技高超。我和他赌了一夜，输了一百多亩地。',
 'markdown', 580, 1, 1, 1, NOW(), 320, 'admin', NOW()),
(3, '第二章 战乱与归乡',
 '<p>被抓壮丁的那一年，我正在城里给我娘抓药。一队国民党兵把我抓走了，一路上枪炮声不断，我吓得躲在战壕里，身边是成堆的尸体。老全说："福贵，你得活着回去，家里还有人等你。"</p><p>老全是个老兵，他知道怎么在战场上活下来。他教我趴下、装死、抢干粮。后来他被流弹打中，死在我怀里。我把他埋了，心里想着，我一定要活着回去。</p><p>打了三年仗，我被解放军俘虏了。解放军对我们这些俘虏很好，愿意回家的发路费。我拿着路费，一路往南走，走了半个月，终于回到了村里。</p><p>家珍抱着有庆出来接我，有庆已经三岁了，不认识我。我娘已经去世了，是家珍一个人拉扯着孩子。我抱着家珍哭了，说："我回来了，再也不走了。"</p>',
 '# 第二章 战乱与归乡\n\n被抓壮丁的那一年，我正在城里给我娘抓药。一队国民党兵把我抓走了。\n\n老全是个老兵，他知道怎么在战场上活下来。他教我趴下、装死、抢干粮。后来他被流弹打中，死在我怀里。\n\n打了三年仗，我被解放军俘虏了。解放军对我们这些俘虏很好，愿意回家的发路费。\n\n家珍抱着有庆出来接我，有庆已经三岁了，不认识我。',
 'markdown', 540, 2, 1, 1, NOW(), 280, 'admin', NOW()),
(3, '第三章 苦难与坚韧',
 '<p>大跃进那年，村里成立了人民公社，家里的锅都被收去炼钢了。有庆长大了，每天去放羊。有一天，县长夫人生孩子大出血，学校组织学生去献血，有庆的血型对得上，结果抽了太多血，人就没了。</p><p>我抱着有庆的身体，他从温热慢慢变凉。我恨那个县长，后来发现县长是春生——当年和我一起被俘虏的兄弟。我没办法恨他，只是说："你欠我们家一条命。"</p><p>家珍的病越来越重，软骨病，治不好。凤霞长大了，嫁给了城里的搬运工二喜。凤霞生孩子的时候，也大出血，孩子活了下来，凤霞却没了。</p><p>家珍熬到凤霞走后不久，也走了。二喜带着苦根（凤霞的孩子）过日子，后来工地上出事，二喜被水泥板砸死了。我带着苦根，苦根七岁那年，吃豆子撑死了——那时候太穷，孩子没吃过饱饭。</p><p>最后就剩我和一头老牛，我叫它福贵。我们两个老家伙，一起在田里慢慢活。</p>',
 '# 第三章 苦难与坚韧\n\n大跃进那年，村里成立了人民公社，家里的锅都被收去炼钢了。\n\n有庆长大了，每天去放羊。有一天，县长夫人生孩子大出血，学校组织学生去献血，有庆的血型对得上，结果抽了太多血，人就没了。\n\n家珍的病越来越重。凤霞生孩子的时候也大出血走了。\n\n最后就剩我和一头老牛，我叫它福贵。',
 'markdown', 620, 3, 1, 1, NOW(), 310, 'admin', NOW());

-- -------------------------------------------------------
-- 书 15：三体（3 章，科幻连载小说）
-- -------------------------------------------------------
INSERT INTO `portal_book_chapter`
(`book_id`, `title`, `content`, `content_markdown`, `editor_mode`, `word_count`, `chapter_no`, `is_free`, `is_published`, `publish_time`, `view_count`, `create_by`, `create_time`)
VALUES
(15, '第一章 科学边界',
 '<p>汪淼是一位纳米材料研究员，他在一场莫名其妙的邀请下，接触到了一个名为"科学边界"的神秘组织。这个组织的成员都是顶尖科学家，但他们似乎都在研究着同一个奇怪的现象——物理学不存在了。</p><p>就在汪淼接到邀请的同一周，物理学家杨冬自杀了。她在遗书中写道："物理学是一门不能自洽的学科。"她的自杀像一颗石子投入湖面，在科学界激起了一圈又一圈的涟漪。</p><p>汪淼发现，自己的视野中开始出现一个倒计时——一串只有他能看见的数字，每分每秒都在跳动。他试着停下手中的纳米研究，倒计时果然停下了。这意味着，有什么东西在监视他，并且能控制他所看到的世界。</p><p>警察史强找到了汪淼，这个粗犷的刑警告诉他，最近有太多科学家自杀了，上级让他调查。史强带汪淼去参加了一个秘密会议，会上有人提到"三体"——一个让所有接触它的人都陷入绝望的词。</p>',
 '# 第一章 科学边界\n\n汪淼是一位纳米材料研究员，他在一场莫名其妙的邀请下，接触到了一个名为"科学边界"的神秘组织。\n\n物理学家杨冬自杀了。她在遗书中写道：物理学是一门不能自洽的学科。\n\n汪淼发现，自己的视野中开始出现一个倒计时——一串只有他能看见的数字。\n\n警察史强找到了汪淼，这个粗犷的刑警告诉他，最近有太多科学家自杀了。',
 'markdown', 560, 1, 1, 1, NOW(), 450, 'admin', NOW()),
(15, '第二章 三体游戏',
 '<p>史强让汪淼去玩一个叫"三体"的网络游戏。汪淼登录后，发现自己置身于一个奇异的世界——这里的天空时而出现三个太阳，时而一个也没有。文明在"恒纪元"（稳定的气候）中诞生，又在"乱纪元"（极端气候）中毁灭，如此循环往复。</p><p>汪淼在游戏中遇到了周文王、墨子、牛顿——这些人都在尝试预测三体世界的运行规律，但都失败了。因为三体问题本质上是无解的，三个太阳的运动是混沌的。</p><p>汪淼逐渐意识到，这个游戏并不是虚构的，它是对一个真实存在的世界的模拟——一个被三个太阳交替统治的星球。那个世界的文明，已经经历了数百次的毁灭与重生。</p><p>在一次聚会中，汪淼见到了"科学边界"的核心成员申玉菲。申玉菲冷淡地告诉他："主在看着你。"汪淼不明白"主"是谁，但他感觉到，一个远超人类理解的智慧，正在注视着地球。</p>',
 '# 第二章 三体游戏\n\n史强让汪淼去玩一个叫"三体"的网络游戏。\n\n汪淼在游戏中遇到了周文王、墨子、牛顿——这些人都在尝试预测三体世界的运行规律，但都失败了。\n\n汪淼逐渐意识到，这个游戏并不是虚构的，它是对一个真实存在的世界的模拟。\n\n在一次聚会中，汪淼见到了"科学边界"的核心成员申玉菲。申玉菲冷淡地告诉他：主在看着你。',
 'markdown', 590, 2, 1, 1, NOW(), 380, 'admin', NOW()),
(15, '第三章 红岸基地',
 '<p>叶文洁的故事，要从文化大革命说起。她的父亲是一位物理学家，在批斗中被活活打死。叶文洁被下放到大兴安岭，在那里她遇到了一个改变她命运的信号——一段来自太空的电波。</p><p>叶文洁被秘密调到了一个名为"红岸"的基地——一个对外宣称是普通军事基地，实则是用于搜索外星文明的射电望远镜基地。叶文洁在红岸工作了多年，她利用太阳作为天线放大器，向宇宙发送了一段信号。</p><p>八年后，她收到了回复。回复的内容让她颤抖："不要回答！不要回答！不要回答！"——这是一位三体世界的和平主义者发来的警告，他告诉叶文洁，如果她再发一次信号，地球的位置就会被三体世界锁定，届时将面临毁灭。</p><p>但叶文洁还是按下了发送键。她对人类已经彻底失望——父亲被打死、母亲背叛、爱人利用她。她希望三体文明来到地球，"他们"会比人类更文明。这一按，开启了长达四百年的地球与三体的恩怨。</p>',
 '# 第三章 红岸基地\n\n叶文洁的故事，要从文化大革命说起。她的父亲是一位物理学家，在批斗中被活活打死。\n\n叶文洁被秘密调到了一个名为"红岸"的基地——一个用于搜索外星文明的射电望远镜基地。\n\n八年后，她收到了回复。回复的内容让她颤抖：不要回答！不要回答！不要回答！\n\n但叶文洁还是按下了发送键。她对人类已经彻底失望。这一按，开启了长达四百年的地球与三体的恩怨。',
 'markdown', 610, 3, 1, 1, NOW(), 420, 'admin', NOW());

-- ============================================================
-- 三、为其余 12 本书补章节正文（每本 1 章，保证书籍详情页有"开始阅读"按钮可点）
-- ============================================================

INSERT INTO `portal_book_chapter`
(`book_id`, `title`, `content`, `content_markdown`, `editor_mode`, `word_count`, `chapter_no`, `is_free`, `is_published`, `publish_time`, `view_count`, `create_by`, `create_time`)
VALUES
-- 书 2：深入理解计算机系统
(2, '第1章 计算机系统漫游',
 '<h2>信息就是位+上下文</h2><p>计算机系统由硬件和系统软件组成，它们共同协作来运行应用程序。信息在计算机内部以二进制位的形式存储，相同的字节序列可能表示整数、浮点数、字符串或机器指令，具体含义由上下文决定。</p><h3>编译系统</h3><p>一个 C 程序的生命周期从源文件开始，经过预处理、编译、汇编、链接四个阶段，最终生成可执行文件。理解编译过程有助于排查问题、优化性能、理解安全漏洞。</p><h3>存储器层次</h3><p>从寄存器、L1/L2/L3 缓存、主存到磁盘，每一层都更快但更小。程序员可以利用局部性原理让程序跑得更快。</p><blockquote>理解计算机系统，是写出高性能、可靠、安全程序的前提。</blockquote>',
 '# 第1章 计算机系统漫游\n\n信息就是位+上下文。\n\n## 编译系统\n\n一个 C 程序经过预处理、编译、汇编、链接四个阶段。\n\n## 存储器层次\n\n从寄存器到磁盘，每一层都更快但更小。',
 'markdown', 380, 1, 1, 1, NOW(), 220, 'admin', NOW()),

-- 书 4：人类简史
(4, '第1章 认知革命',
 '<p>大约 7 万年前，智人开始做出非常特别的事情——他们开始讲故事，讲不存在的事情。神话、传说、神祇、宗教由此诞生。这种虚构的能力，让智人能够大规模协作，最终征服了整个地球。</p><p>"标致"汽车公司只是一个法律虚构，但全世界数百万人都相信它存在。这种集体虚构让陌生人之间能够合作，这是人类独有的能力。</p><p>农业革命后，人类开始定居，人口爆炸，但个体生活质量可能下降——这就是历史的吊诡之处。我们以为我们驯化了小麦，其实是小麦驯化了我们。</p><blockquote>认知革命让智人学会了讲故事，也学会了共同相信一个虚构。</blockquote>',
 '# 第1章 认知革命\n\n大约 7 万年前，智人开始做出非常特别的事情——他们开始讲故事。\n\n"标致"汽车公司只是一个法律虚构。\n\n农业革命后，人类开始定居，人口爆炸，但个体生活质量可能下降。',
 'markdown', 420, 1, 1, 1, NOW(), 380, 'admin', NOW()),

-- 书 5：设计模式
(5, '第1章 引言',
 '<h2>什么是设计模式</h2><p>设计模式是面向对象软件设计的经验总结。每一个设计模式都系统地命名、解释和评价了面向对象系统中的一个重要且可复用的设计。</p><p>设计模式让使用者可以更加方便地复用成功的设计和架构。它们帮助开发者做出有利于系统复用的选择，避免那些会损害系统复用性的设计。</p><h3>四人帮（GoF）</h3><p>《设计模式》一书由 Erich Gamma、Richard Helm、Ralph Johnson、John Vlissides 四人合著，故称 GoF。书中收录 23 种经典设计模式，分为创建型、结构型、行为型三大类。</p><blockquote>设计模式不是教条，而是经验沉淀。</blockquote>',
 '# 第1章 引言\n\n设计模式是面向对象软件设计的经验总结。\n\n## 四人帮（GoF）\n\n《设计模式》一书由 Erich Gamma、Richard Helm、Ralph Johnson、John Vlissides 四人合著。',
 'markdown', 320, 1, 1, 1, NOW(), 180, 'admin', NOW()),

-- 书 6：Java 编程思想
(6, '第1章 对象导论',
 '<h2>万物皆对象</h2><p>Java 是一门纯粹的面向对象语言。在 Java 中，一切皆对象——每个变量都是某个类的实例，每个方法都依附于某个对象。这种思想让程序更易理解、更易复用。</p><p>Alan Kay 总结过面向对象的五大特征：万物皆对象、程序是对象的集合、对象通过发消息通信、每个对象都有内存、每个对象都是某个类的实例。Smalltalk 是最早实践这一思想的语言。</p><p>Java 借鉴了 C++ 的语法和 Smalltalk 的对象模型，但又去掉了 C++ 的多重继承、指针等复杂特性，让语言更简单、更安全。</p><blockquote>对象不是银弹，但它是组织复杂软件的有效工具。</blockquote>',
 '# 第1章 对象导论\n\n万物皆对象。Java 是一门纯粹的面向对象语言。\n\nAlan Kay 总结过面向对象的五大特征。',
 'markdown', 360, 1, 1, 1, NOW(), 240, 'admin', NOW()),

-- 书 7：深入浅出 MySQL
(7, '第1章 MySQL 架构',
 '<h2>MySQL 整体架构</h2><p>MySQL 的整体架构分为三层：客户端/连接层、服务器层（含 SQL 接口、解析器、优化器、缓存）、存储引擎层。存储引擎是 MySQL 最具特色的部分，它采用插件式架构，允许开发者选择 InnoDB、MyISAM、Memory 等不同引擎。</p><p>InnoDB 是 MySQL 5.5 之后的默认引擎，支持事务、行级锁、外键。它通过 redo log 保证持久性，undo log 保证原子性，MVCC 实现可重复读隔离级别。</p><h3>一条 SQL 的旅程</h3><p>从客户端发起到查询缓存命中、解析器语法检查、优化器生成执行计划、执行器调用存储引擎接口，最终返回结果。每一步都可能影响 SQL 的执行效率。</p><blockquote>理解架构，才能优化数据库。</blockquote>',
 '# 第1章 MySQL 架构\n\nMySQL 的整体架构分为三层。\n\nInnoDB 是 MySQL 5.5 之后的默认引擎。\n\n一条 SQL 从客户端发起到返回结果，要经历多步。',
 'markdown', 400, 1, 1, 1, NOW(), 160, 'admin', NOW()),

-- 书 8：Redis 设计与实现
(8, '第1章 数据结构',
 '<h2>SDS：动态字符串</h2><p>Redis 没有直接使用 C 的字符串，而是自定义了 SDS（Simple Dynamic String）。SDS 在字符串头部记录了 len 和 free 字段，让 O(1) 获取长度、二进制安全、空间预分配成为可能。</p><h3>链表、字典、跳表</h3><p>链表用于 List 类型；字典用于 Hash，采用渐进式 rehash 避免一次性搬迁阻塞；跳表用于 ZSet，让有序集合的平均查找复杂度为 O(logN)。</p><p>Redis 之所以快：单线程避免上下文切换、内存操作、IO 多路复用、高效数据结构。</p><blockquote>理解 Redis，从理解它的数据结构开始。</blockquote>',
 '# 第1章 数据结构\n\nSDS：动态字符串。Redis 没有直接使用 C 的字符串。\n\n## 链表、字典、跳表\n\n跳表让有序集合的平均查找复杂度为 O(logN)。',
 'markdown', 340, 1, 1, 1, NOW(), 200, 'admin', NOW()),

-- 书 9：Spring 实战
(9, '第1章 Spring 核心',
 '<h2>Spring 是什么</h2><p>Spring 是一个开源的轻量级 Java 应用框架。它的核心是 IoC（控制反转）和 AOP（面向切面编程）。IoC 让对象的创建和管理交由容器负责，AOP 让横切关注点（日志、事务、安全）从业务代码中分离。</p><h3>依赖注入</h3><p>传统写法：对象自己 new 依赖。Spring 写法：对象声明依赖，容器注入。这让代码解耦、易于测试。</p><pre><code>@Service\npublic class UserService {\n    @Autowired\n    private UserRepository repo;\n}</code></pre><p>Spring Boot 在 Spring 之上做了约定优于配置的封装，让微服务开发更简单。</p><blockquote>Spring 的核心是 DI + AOP。</blockquote>',
 '# 第1章 Spring 核心\n\nSpring 是一个开源的轻量级 Java 应用框架。核心是 IoC 和 AOP。\n\n## 依赖注入\n\n传统写法：对象自己 new 依赖。Spring 写法：对象声明依赖，容器注入。',
 'markdown', 380, 1, 1, 1, NOW(), 170, 'admin', NOW()),

-- 书 10：算法导论
(10, '第1章 算法基础',
 '<h2>插入排序</h2><p>插入排序是一种原地排序算法，最坏情况 O(n²)，但对小规模或近似有序数据非常高效。它的核心思想是：把每个元素插入到已排序部分的合适位置。</p><h3>分治法</h3><p>归并排序采用分治思想：把数组对半切，递归排序，然后合并。时间复杂度 O(n log n)，但需要 O(n) 额外空间。</p><h3>渐进记号</h3><p>Θ 给出函数的上下界；O 给出上界；Ω 给出下界。大 O 是最常用的，它告诉我们算法在最坏情况下不会比什么更差。</p><blockquote>算法分析从渐进记号开始。</blockquote>',
 '# 第1章 算法基础\n\n插入排序是一种原地排序算法，最坏情况 O(n²)。\n\n## 分治法\n\n归并排序采用分治思想：把数组对半切，递归排序，然后合并。',
 'markdown', 350, 1, 1, 1, NOW(), 290, 'admin', NOW()),

-- 书 11：百年孤独
(11, '第1章 马孔多的诞生',
 '<p>多年以后，面对行刑队，奥雷里亚诺·布恩迪亚上校将会回想起父亲带他去见识冰块的那个遥远的下午。那时的马孔多是一个二十户人家的村落，房屋沿河而建，河水清澈，河床里卵石洁白光滑宛如史前巨蛋。</p><p>世界新生伊始，许多事物还没有名字，提到的时候尚需用手指指点点。每年三月前后，一家衣衫褴褛的吉卜赛人都会来到村边扎下帐篷，吹笛击鼓，吵吵嚷嚷地向人们展示新近的发明。</p><p>最初他们带来了磁铁。一个身形高大的吉卜赛人，胡须蓬乱，雀爪般的双手，自称梅尔基亚德斯，当众进行了惊人的演示——他把两块磁铁拖过房屋，铁锅、铁盆、铁钳、小炭炉纷纷从原地落下，木板因钉子和螺丝奋力挣脱而吱呀作响。</p><blockquote>多年以后，奥雷里亚诺·布恩迪亚上校面对行刑队，将会回想起父亲带他去见识冰块的那个遥远的下午。</blockquote>',
 '# 第1章 马孔多的诞生\n\n多年以后，面对行刑队，奥雷里亚诺·布恩迪亚上校将会回想起父亲带他去见识冰块的那个遥远的下午。\n\n世界新生伊始，许多事物还没有名字。',
 'markdown', 480, 1, 1, 1, NOW(), 410, 'admin', NOW()),

-- 书 12：围城
(12, '第1章 归国',
 '<p>红海早过了，船在印度洋面上开驶着，但是太阳依然不饶人地迟落，依然劝诱着人们早早地休息。在这蒸笼般的热天气里，<strong>方鸿渐</strong>正靠在船舷上，思念着家乡。</p><p>方鸿渐在欧洲游学了四年，辗转了伦敦、巴黎、柏林三所大学，却没拿到任何学位。他从爱尔兰人手里买了一张"克莱登大学"的假博士文凭，准备回国蒙混过关。</p><p>船到上海，他先去了岳父母家——他的未婚妻周淑英已经去世，但岳父仍念旧情，让他住在家里，并为他谋了一个银行的差事。方鸿渐从此开始了他在上海、内地之间的漂泊与"围城"人生。</p><blockquote>城外的人想进去，城里的人想出来。</blockquote>',
 '# 第1章 归国\n\n红海早过了，船在印度洋面上开驶着。\n\n方鸿渐在欧洲游学了四年，却没拿到任何学位。他从爱尔兰人手里买了一张"克莱登大学"的假博士文凭。',
 'markdown', 460, 1, 1, 1, NOW(), 350, 'admin', NOW()),

-- 书 13：平凡的世界
(13, '第1章 双水村的清晨',
 '<p>1975 年二、三月间，一个平平常常的日子，细蒙蒙的雨丝夹着一星半点的雪花，正纷纷淋淋地向大地飘洒着。时令已快到惊蛰，雪当然再不会下大了，但依然让人感到一种春寒料峭的凉意。</p><p>在黄土高原千沟万壑的褶皱里，有一个叫<strong>双水村</strong>的村庄。村东的庙坪上，一座破庙正进行着一种几乎原始的劳动——一群衣衫褴褛的农民，正光着膀子用木锨扬场。</p><p>村中半山腰的一孔窑洞里，<strong>孙少平</strong>正背着书包上学去。他是这村里少有的高中生，他的哥哥孙少安早早辍学务农，把希望寄托在了弟弟身上。这是一个平凡的世界，却有一群不甘平凡的人。</p><blockquote>生活不能等待别人来安排，要自己去争取。</blockquote>',
 '# 第1章 双水村的清晨\n\n1975 年二、三月间，一个平平常常的日子，细蒙蒙的雨丝夹着一星半点的雪花。\n\n在黄土高原千沟万壑的褶皱里，有一个叫双水村的村庄。',
 'markdown', 470, 1, 1, 1, NOW(), 320, 'admin', NOW()),

-- 书 14：重构
(14, '第1章 重构的第一个案例',
 '<h2>重构是什么</h2><p>重构是在不改变软件外部行为的前提下，调整其内部结构，使其更易理解、更易修改。它不是重写，而是小步、安全的改进。</p><h3>影片租赁系统</h3><p>一个简单的影片租赁系统：顾客租了影片，系统计算费用和积分。最初的代码把所有逻辑塞在一个方法里，每次新增影片类型都要修改这个方法。</p><p>重构步骤：先提取"计算费用"和"计算积分"两个方法，再引入"影片类型"的多态，最后把状态模式应用到"租赁"上。每一步都通过测试验证。</p><blockquote>重构是程序员的健身操，每天做一点，代码更健康。</blockquote>',
 '# 第1章 重构的第一个案例\n\n重构是在不改变软件外部行为的前提下，调整其内部结构。\n\n## 影片租赁系统\n\n一个简单的影片租赁系统：顾客租了影片，系统计算费用和积分。',
 'markdown', 360, 1, 1, 1, NOW(), 150, 'admin', NOW());

-- ============================================================
-- 四、更新全部 15 本书的第三阶段字段
-- ============================================================

-- 4.1 书 1（代码整洁之道）：已完结出版物
UPDATE `portal_book` SET
    `type` = 'published',
    `serial_status` = 'completed',
    `is_finished` = 1,
    `chapter_count` = 3,
    `word_count` = 1150,
    `latest_chapter_id` = (SELECT max_id FROM (SELECT MAX(`id`) AS max_id FROM `portal_book_chapter` WHERE `book_id` = 1) t),
    `latest_chapter_title` = '第3章 函数',
    `last_update_time` = NOW()
WHERE `id` = 1;

-- 4.2 书 3（活着）：已完结网络小说
UPDATE `portal_book` SET
    `type` = 'novel',
    `serial_status` = 'completed',
    `is_finished` = 1,
    `chapter_count` = 3,
    `word_count` = 1740,
    `latest_chapter_id` = (SELECT max_id FROM (SELECT MAX(`id`) AS max_id FROM `portal_book_chapter` WHERE `book_id` = 3) t),
    `latest_chapter_title` = '第三章 苦难与坚韧',
    `last_update_time` = NOW()
WHERE `id` = 3;

-- 4.3 书 15（三体）：连载中网络小说（让发现页"连载中"区块有数据）
UPDATE `portal_book` SET
    `type` = 'novel',
    `serial_status` = 'ongoing',
    `is_finished` = 0,
    `chapter_count` = 3,
    `word_count` = 1760,
    `latest_chapter_id` = (SELECT max_id FROM (SELECT MAX(`id`) AS max_id FROM `portal_book_chapter` WHERE `book_id` = 15) t),
    `latest_chapter_title` = '第三章 红岸基地',
    `last_update_time` = NOW()
WHERE `id` = 15;

-- 4.4 其余 12 本书（每本 1 章）：统一标记 + latest_chapter_id 关联（每本一条 UPDATE，兼容所有 MySQL 版本）
-- 书 2
UPDATE `portal_book` SET
    `type` = 'published', `serial_status` = 'completed', `is_finished` = 1, `chapter_count` = 1, `word_count` = 380,
    `latest_chapter_id` = (SELECT max_id FROM (SELECT MAX(`id`) AS max_id FROM `portal_book_chapter` WHERE `book_id` = 2) t),
    `latest_chapter_title` = '第1章 计算机系统漫游', `last_update_time` = NOW()
WHERE `id` = 2;
-- 书 4
UPDATE `portal_book` SET
    `type` = 'published', `serial_status` = 'completed', `is_finished` = 1, `chapter_count` = 1, `word_count` = 420,
    `latest_chapter_id` = (SELECT max_id FROM (SELECT MAX(`id`) AS max_id FROM `portal_book_chapter` WHERE `book_id` = 4) t),
    `latest_chapter_title` = '第1章 认知革命', `last_update_time` = NOW()
WHERE `id` = 4;
-- 书 5
UPDATE `portal_book` SET
    `type` = 'published', `serial_status` = 'completed', `is_finished` = 1, `chapter_count` = 1, `word_count` = 320,
    `latest_chapter_id` = (SELECT max_id FROM (SELECT MAX(`id`) AS max_id FROM `portal_book_chapter` WHERE `book_id` = 5) t),
    `latest_chapter_title` = '第1章 引言', `last_update_time` = NOW()
WHERE `id` = 5;
-- 书 6
UPDATE `portal_book` SET
    `type` = 'published', `serial_status` = 'completed', `is_finished` = 1, `chapter_count` = 1, `word_count` = 360,
    `latest_chapter_id` = (SELECT max_id FROM (SELECT MAX(`id`) AS max_id FROM `portal_book_chapter` WHERE `book_id` = 6) t),
    `latest_chapter_title` = '第1章 对象导论', `last_update_time` = NOW()
WHERE `id` = 6;
-- 书 7
UPDATE `portal_book` SET
    `type` = 'published', `serial_status` = 'completed', `is_finished` = 1, `chapter_count` = 1, `word_count` = 400,
    `latest_chapter_id` = (SELECT max_id FROM (SELECT MAX(`id`) AS max_id FROM `portal_book_chapter` WHERE `book_id` = 7) t),
    `latest_chapter_title` = '第1章 MySQL 架构', `last_update_time` = NOW()
WHERE `id` = 7;
-- 书 8
UPDATE `portal_book` SET
    `type` = 'published', `serial_status` = 'completed', `is_finished` = 1, `chapter_count` = 1, `word_count` = 340,
    `latest_chapter_id` = (SELECT max_id FROM (SELECT MAX(`id`) AS max_id FROM `portal_book_chapter` WHERE `book_id` = 8) t),
    `latest_chapter_title` = '第1章 数据结构', `last_update_time` = NOW()
WHERE `id` = 8;
-- 书 9
UPDATE `portal_book` SET
    `type` = 'published', `serial_status` = 'completed', `is_finished` = 1, `chapter_count` = 1, `word_count` = 380,
    `latest_chapter_id` = (SELECT max_id FROM (SELECT MAX(`id`) AS max_id FROM `portal_book_chapter` WHERE `book_id` = 9) t),
    `latest_chapter_title` = '第1章 Spring 核心', `last_update_time` = NOW()
WHERE `id` = 9;
-- 书 10
UPDATE `portal_book` SET
    `type` = 'published', `serial_status` = 'completed', `is_finished` = 1, `chapter_count` = 1, `word_count` = 350,
    `latest_chapter_id` = (SELECT max_id FROM (SELECT MAX(`id`) AS max_id FROM `portal_book_chapter` WHERE `book_id` = 10) t),
    `latest_chapter_title` = '第1章 算法基础', `last_update_time` = NOW()
WHERE `id` = 10;
-- 书 11
UPDATE `portal_book` SET
    `type` = 'published', `serial_status` = 'completed', `is_finished` = 1, `chapter_count` = 1, `word_count` = 480,
    `latest_chapter_id` = (SELECT max_id FROM (SELECT MAX(`id`) AS max_id FROM `portal_book_chapter` WHERE `book_id` = 11) t),
    `latest_chapter_title` = '第1章 马孔多的诞生', `last_update_time` = NOW()
WHERE `id` = 11;
-- 书 12
UPDATE `portal_book` SET
    `type` = 'published', `serial_status` = 'completed', `is_finished` = 1, `chapter_count` = 1, `word_count` = 460,
    `latest_chapter_id` = (SELECT max_id FROM (SELECT MAX(`id`) AS max_id FROM `portal_book_chapter` WHERE `book_id` = 12) t),
    `latest_chapter_title` = '第1章 归国', `last_update_time` = NOW()
WHERE `id` = 12;
-- 书 13
UPDATE `portal_book` SET
    `type` = 'published', `serial_status` = 'completed', `is_finished` = 1, `chapter_count` = 1, `word_count` = 470,
    `latest_chapter_id` = (SELECT max_id FROM (SELECT MAX(`id`) AS max_id FROM `portal_book_chapter` WHERE `book_id` = 13) t),
    `latest_chapter_title` = '第1章 双水村的清晨', `last_update_time` = NOW()
WHERE `id` = 13;
-- 书 14
UPDATE `portal_book` SET
    `type` = 'published', `serial_status` = 'completed', `is_finished` = 1, `chapter_count` = 1, `word_count` = 360,
    `latest_chapter_id` = (SELECT max_id FROM (SELECT MAX(`id`) AS max_id FROM `portal_book_chapter` WHERE `book_id` = 14) t),
    `latest_chapter_title` = '第1章 重构的第一个案例', `last_update_time` = NOW()
WHERE `id` = 14;

-- ============================================================
-- 五、推荐位数据（让发现页 Banner + 限免专区 + 首页热门有内容）
-- ============================================================

INSERT INTO `portal_book_recommend`
(`book_id`, `position`, `sort`, `start_time`, `end_time`, `is_active`, `remark`, `create_by`, `create_time`)
VALUES
-- 发现页 Banner（DiscoverPage 顶部轮播）
(4,  'discover_banner', 1, DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_ADD(NOW(), INTERVAL 90 DAY), 1, '发现页Banner-人类简史', 'admin', NOW()),
(11, 'discover_banner', 2, DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_ADD(NOW(), INTERVAL 90 DAY), 1, '发现页Banner-百年孤独', 'admin', NOW()),
(15, 'discover_banner', 3, DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_ADD(NOW(), INTERVAL 90 DAY), 1, '发现页Banner-三体', 'admin', NOW()),
-- 限免专区（DiscoverPage + ReadingPage 限免区块）
(5,  'limit_free', 1, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), 1, '限免-设计模式', 'admin', NOW()),
(9,  'limit_free', 2, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), 1, '限免-Spring实战', 'admin', NOW()),
(7,  'limit_free', 3, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), 1, '限免-深入浅出MySQL', 'admin', NOW()),
-- 首页热门（ReadingPage 入口位，预留）
(1,  'home_hot', 1, NULL, NULL, 1, '首页热门-代码整洁之道', 'admin', NOW()),
(6,  'home_hot', 2, NULL, NULL, 1, '首页热门-Java编程思想', 'admin', NOW()),
(14, 'home_hot', 3, NULL, NULL, 1, '首页热门-重构', 'admin', NOW());

-- ============================================================
-- 第 4/5 部分执行完成
-- 已创建章节：3本书×3章 + 12本书×1章 = 共21章；推荐位：discover_banner=3, limit_free=3, home_hot=3
-- ============================================================


-- #############################################################
-- # 第 5/5 部分：91_book_seed_data.sql                          #
-- # 来源：读书空间示例书籍数据初始化                              #
-- # 说明：导入一本完整的技术书籍《工程师修炼之道：从码农到架构师》  #
-- #       含 10 章正文 + 6 条金句摘录 + 2 条推荐位               #
-- #       与第 4 部分互补（48 针对书籍 1-15，91 新增第 16 本书）   #
-- #############################################################
-- ============================================================
-- 脚本编号：91
-- 脚本名称：读书空间示例书籍数据初始化
-- 说明：导入一本完整的技术书籍《工程师修炼之道：从码农到架构师》
--       含 10 章正文 + 6 条金句摘录 + 2 条推荐位
-- 涉及表：portal_book / portal_book_chapter / portal_book_quote / portal_book_recommend
-- 幂等设计：可重复执行（INSERT IGNORE + 变量引用 book_id）
-- ============================================================

-- ------------------------------------------------------------
-- 1. 书籍主表 portal_book（幂等：按 title 去重）
-- ------------------------------------------------------------
INSERT IGNORE INTO `portal_book` (
    `title`, `author`, `cover`, `description`, `summary`, `isbn`,
    `publisher`, `publish_date`, `page_count`, `category_id`, `tags`,
    `rating`, `reading_count`, `status`, `type`, `serial_status`,
    `word_count`, `chapter_count`, `is_finished`, `access_level`,
    `preview_ratio`, `price`, `is_featured`, `is_recommended`,
    `author_bio`, `create_by`, `create_time`
) VALUES (
    '工程师修炼之道：从码农到架构师',
    '墨韵技术社',
    'https://images.moyun.com/books/engineer-way-cover.jpg',
    '本书写给所有在代码世界里摸爬滚打的工程师。从写出第一行可运行的代码，到设计支撑百万并发的系统，这条路没有捷径，但有方向。十个章节，覆盖代码质量、系统设计、数据库、API、并发、安全、性能、DevOps 到技术领导力，每一章都是一次认知升级。',
    '一本面向中高级工程师的实战进阶指南，覆盖代码质量、架构设计、数据库优化、并发编程、安全防护、性能调优与团队协作，用真实案例讲透从"能写代码"到"能扛系统"的完整路径。',
    '978-7-2026-0001-1',
    '墨韵出版社',
    '2026-06-15',
    320,
    NULL,
    '工程师,架构,后端,成长,系统设计',
    4.85,
    0,
    'active',
    'published',
    'completed',
    85000,
    10,
    1,
    'free',
    100,
    0.00,
    1,
    1,
    '墨韵技术社，由多位一线互联网公司资深工程师组成，专注于技术写作与工程实践传播。',
    'admin',
    NOW()
);

-- 取回书籍 ID（幂等：已存在则查已存记录）
SET @book_id = (SELECT id FROM `portal_book` WHERE `title` = '工程师修炼之道：从码农到架构师' LIMIT 1);

-- ------------------------------------------------------------
-- 2. 章节表 portal_book_chapter（10 章，幂等：uk_book_chapter_no 去重）
-- ------------------------------------------------------------
INSERT IGNORE INTO `portal_book_chapter` (
    `book_id`, `title`, `content`, `content_markdown`, `editor_mode`,
    `word_count`, `chapter_no`, `is_free`, `price`, `is_published`,
    `publish_time`, `view_count`, `create_by`, `create_time`
) VALUES
(@book_id, '第一章 工程师的成长路径',
'<h2>1.1 从码农到工程师</h2><p>很多人把"写代码"等同于"做工程"，这是一个常见的认知偏差。写代码只是手段，解决问题才是目的。一个成熟的工程师，首先想的不是用什么框架，而是这个问题本质是什么、边界在哪、谁来用、用多久。</p><p>成长路径通常分为三个阶段：能完成（把需求变成可运行代码）、能做对（考虑边界、异常、可维护性）、能扛事（对系统的可用性、成本、演进负责）。多数人卡在第一阶段到第二阶段的跨越，因为那意味着从"实现思维"转向"工程思维"。</p><h2>1.2 技术深度的三个层次</h2><p>第一层：会用。知道 API 怎么调，框架怎么配。</p><p>第二层：懂原理。知道 API 背后做了什么，框架的设计权衡是什么。</p><p>第三层：能造轮子。在理解原理的基础上，能针对自己的场景设计替代方案。注意，能造轮子不等于一定要造，但具备这个能力，意味着你对技术的理解已经穿透了表象。</p><h2>1.3 刻意练习</h2><p>读源码是高效的刻意练习方式之一。但不要从头到尾通读，而是带着问题去读：为什么这里用策略模式而不是 if-else？为什么这个缓存要用 ConcurrentHashMap 而不是 HashMap 加锁？每一个"为什么"的答案，都是一次认知边界的扩展。</p>',
'## 1.1 从码农到工程师\n\n很多人把"写代码"等同于"做工程"，这是一个常见的认知偏差。写代码只是手段，解决问题才是目的。一个成熟的工程师，首先想的不是用什么框架，而是这个问题本质是什么、边界在哪、谁来用、用多久。\n\n成长路径通常分为三个阶段：能完成（把需求变成可运行代码）、能做对（考虑边界、异常、可维护性）、能扛事（对系统的可用性、成本、演进负责）。多数人卡在第一阶段到第二阶段的跨越，因为那意味着从"实现思维"转向"工程思维"。\n\n## 1.2 技术深度的三个层次\n\n第一层：会用。知道 API 怎么调，框架怎么配。\n\n第二层：懂原理。知道 API 背后做了什么，框架的设计权衡是什么。\n\n第三层：能造轮子。在理解原理的基础上，能针对自己的场景设计替代方案。注意，能造轮子不等于一定要造，但具备这个能力，意味着你对技术的理解已经穿透了表象。\n\n## 1.3 刻意练习\n\n读源码是高效的刻意练习方式之一。但不要从头到尾通读，而是带着问题去读：为什么这里用策略模式而不是 if-else？为什么这个缓存要用 ConcurrentHashMap 而不是 HashMap 加锁？每一个"为什么"的答案，都是一次认知边界的扩展。',
'richtext', 580, 1, 1, 0.00, 1, NOW(), 0, 'admin', NOW()),

(@book_id, '第二章 代码质量与整洁之道',
'<h2>2.1 命名：最廉价的工程质量</h2><p>好的命名是自解释的，读到名字就知道它在做什么，不需要跳进去看实现。坏命名有三个典型特征：缩写（usr、cnt、flg）、泛化（data、info、manager）、误导（叫 list 实际是 map）。</p><h2>2.2 函数：短小再短小</h2><p>一个函数只做一件事。判断标准：能否用一句话描述它的职责而不用"和"字。如果"做A和做B"，就该拆成两个函数。短函数的好处不是"代码行数少"，而是降低认知负担——人脑能同时持有的上下文是有限的。</p><h2>2.3 注释：写"为什么"而不是"是什么"</h2><p>代码已经说了"是什么"，注释要补的是"为什么"。比如 <code>// 这里 +1 是因为后端分页从0开始，前端从1开始</code> 是好注释；<code>// 循环数组</code> 就是废话。</p><h2>2.4 异常处理</h2><p>不要吞异常。<code>catch(Exception e) {}</code> 是工程灾难。要么处理、要么抛出、要么转换成业务异常并记日志。静默吞掉的异常，会在生产环境变成无法定位的幽灵问题。</p>',
'## 2.1 命名：最廉价的工程质量\n\n好的命名是自解释的，读到名字就知道它在做什么，不需要跳进去看实现。坏命名有三个典型特征：缩写（usr、cnt、flg）、泛化（data、info、manager）、误导（叫 list 实际是 map）。\n\n## 2.2 函数：短小再短小\n\n一个函数只做一件事。判断标准：能否用一句话描述它的职责而不用"和"字。如果"做A和做B"，就该拆成两个函数。短函数的好处不是"代码行数少"，而是降低认知负担——人脑能同时持有的上下文是有限的。\n\n## 2.3 注释：写"为什么"而不是"是什么"\n\n代码已经说了"是什么"，注释要补的是"为什么"。比如 `// 这里 +1 是因为后端分页从0开始，前端从1开始` 是好注释；`// 循环数组` 就是废话。\n\n## 2.4 异常处理\n\n不要吞异常。`catch(Exception e) {}` 是工程灾难。要么处理、要么抛出、要么转换成业务异常并记日志。静默吞掉的异常，会在生产环境变成无法定位的幽灵问题。',
'richtext', 520, 2, 1, 0.00, 1, NOW(), 0, 'admin', NOW()),

(@book_id, '第三章 系统设计与架构思维',
'<h2>3.1 架构的本质是权衡</h2><p>没有"最好"的架构，只有"最合适"的架构。单体还是微服务、强一致还是最终一致、同步还是异步，每个选择背后都是 trade-off。架构师的工作不是选"最优解"，而是在成本、人力、时间、复杂度之间找到当前阶段最合理的平衡点。</p><h2>3.2 分层与解耦</h2><p>经典三层架构（Controller-Service-Mapper）不是教条，而是关注点分离的实践。每一层只关心自己的职责：Controller 校验入参和组装响应，Service 编排业务，Mapper 持久化。跨层调用（比如 Controller 直接调 Mapper）是架构腐化的开始。</p><h2>3.3 面向接口编程</h2><p>依赖抽象，不依赖具体。Service 调用 Mapper 时依赖接口（IPortalBookMapper），而不是实现类。这样换实现（比如从 MySQL 换 ES）时，上层无需改动。这是开闭原则在工程中的落地。</p><h2>3.4 演进式架构</h2><p>不要一开始就设计"完美架构"。先做能跑的，再做能扩展的，最后才是能演进的。过早优化和过度设计，比不做设计更危险。</p>',
'## 3.1 架构的本质是权衡\n\n没有"最好"的架构，只有"最合适"的架构。单体还是微服务、强一致还是最终一致、同步还是异步，每个选择背后都是 trade-off。架构师的工作不是选"最优解"，而是在成本、人力、时间、复杂度之间找到当前阶段最合理的平衡点。\n\n## 3.2 分层与解耦\n\n经典三层架构（Controller-Service-Mapper）不是教条，而是关注点分离的实践。每一层只关心自己的职责：Controller 校验入参和组装响应，Service 编排业务，Mapper 持久化。跨层调用（比如 Controller 直接调 Mapper）是架构腐化的开始。\n\n## 3.3 面向接口编程\n\n依赖抽象，不依赖具体。Service 调用 Mapper 时依赖接口（IPortalBookMapper），而不是实现类。这样换实现（比如从 MySQL 换 ES）时，上层无需改动。这是开闭原则在工程中的落地。\n\n## 3.4 演进式架构\n\n不要一开始就设计"完美架构"。先做能跑的，再做能扩展的，最后才是能演进的。过早优化和过度设计，比不做设计更危险。',
'richtext', 610, 3, 1, 0.00, 1, NOW(), 0, 'admin', NOW()),

(@book_id, '第四章 数据库设计与优化',
'<h2>4.1 索引：查询的加速器</h2><p>索引不是越多越好。每个索引都有写入开销和维护成本。建立索引的三原则：查询频次高、区分度高、覆盖查询字段。区分度低于 30% 的列建索引基本无效（比如 status 只有 0/1 两个值）。</p><p>联合索引遵循最左前缀原则。索引 (user_id, status, create_time) 能命中 user_id 单独查询，但命中不了 status 单独查询。</p><h2>4.2 事务与隔离级别</h2><p>MySQL 默认隔离级别是 RR（可重复读），通过 MVCC + 间隙锁实现。但在高并发写场景下，RR 的间隙锁会导致锁等待，适当降到 RC（读已提交）能提升吞吐。注意 RC 会引入幻读，需业务层兜底。</p><h2>4.3 分页优化</h2><p>深分页 <code>LIMIT 1000000, 20</code> 极慢，因为要扫描 100 万行再丢弃。优化方案：用游标分页 <code>WHERE id &gt; #{lastId} LIMIT 20</code>，或用覆盖索引子查询。</p><pre><code>-- 慢\nSELECT * FROM article ORDER BY id LIMIT 1000000, 20;\n-- 快（游标分页）\nSELECT * FROM article WHERE id &gt; #{lastId} ORDER BY id LIMIT 20;</code></pre><h2>4.4 避免 N+1 查询</h2><p>循环里查数据库是性能杀手。批量查询 + 内存组装，比循环单查快几个数量级。</p>',
'## 4.1 索引：查询的加速器\n\n索引不是越多越好。每个索引都有写入开销和维护成本。建立索引的三原则：查询频次高、区分度高、覆盖查询字段。区分度低于 30% 的列建索引基本无效（比如 status 只有 0/1 两个值）。\n\n联合索引遵循最左前缀原则。索引 `(user_id, status, create_time)` 能命中 user_id 单独查询，但命中不了 status 单独查询。\n\n## 4.2 事务与隔离级别\n\nMySQL 默认隔离级别是 RR（可重复读），通过 MVCC + 间隙锁实现。但在高并发写场景下，RR 的间隙锁会导致锁等待，适当降到 RC（读已提交）能提升吞吐。注意 RC 会引入幻读，需业务层兜底。\n\n## 4.3 分页优化\n\n深分页 `LIMIT 1000000, 20` 极慢，因为要扫描 100 万行再丢弃。优化方案：用游标分页 `WHERE id > #{lastId} LIMIT 20`，或用覆盖索引子查询。\n\n```sql\n-- 慢\nSELECT * FROM article ORDER BY id LIMIT 1000000, 20;\n-- 快（游标分页）\nSELECT * FROM article WHERE id > #{lastId} ORDER BY id LIMIT 20;\n```\n\n## 4.4 避免 N+1 查询\n\n循环里查数据库是性能杀手。批量查询 + 内存组装，比循环单查快几个数量级。',
'richtext', 750, 4, 1, 0.00, 1, NOW(), 0, 'admin', NOW()),

(@book_id, '第五章 API 设计原则',
'<h2>5.1 RESTful 不是教条</h2><p>REST 风格的 API 用 HTTP 动词表达意图（GET 查、POST 增、PUT 改、DELETE 删），用 URL 表达资源。但不要为了 REST 而 REST——比如"批量删除"用 DELETE 传数组 body 就很别扭，此时 POST /batch-delete 更务实。</p><h2>5.2 版本控制</h2><p>API 一旦上线就有人依赖，改动是破坏性的。用 <code>/v1/articles</code> 而不是 <code>/articles</code>，给未来留演进空间。大版本用路径区分，小版本用 header 或 query。</p><h2>5.3 统一响应结构</h2><p>所有接口返回统一结构：<code>{code, msg, data}</code>。code=200 成功，其他失败。这样前端只需一套拦截器处理，不用每个接口判断不同格式。</p><pre><code>{\n  "code": 200,\n  "msg": "success",\n  "data": {...}\n}</code></pre><h2>5.4 幂等性</h2><p>POST 创建接口要考虑幂等：用户点两次"提交"按钮，不应该创建两条数据。方案：前端传 clientToken，后端用 Redis SETNX 去重，或用唯一索引兜底。</p>',
'## 5.1 RESTful 不是教条\n\nREST 风格的 API 用 HTTP 动词表达意图（GET 查、POST 增、PUT 改、DELETE 删），用 URL 表达资源。但不要为了 REST 而 REST——比如"批量删除"用 DELETE 传数组 body 就很别扭，此时 `POST /batch-delete` 更务实。\n\n## 5.2 版本控制\n\nAPI 一旦上线就有人依赖，改动是破坏性的。用 `/v1/articles` 而不是 `/articles`，给未来留演进空间。大版本用路径区分，小版本用 header 或 query。\n\n## 5.3 统一响应结构\n\n所有接口返回统一结构：`{code, msg, data}`。code=200 成功，其他失败。这样前端只需一套拦截器处理，不用每个接口判断不同格式。\n\n```json\n{\n  "code": 200,\n  "msg": "success",\n  "data": {...}\n}\n```\n\n## 5.4 幂等性\n\nPOST 创建接口要考虑幂等：用户点两次"提交"按钮，不应该创建两条数据。方案：前端传 clientToken，后端用 Redis SETNX 去重，或用唯一索引兜底。',
'richtext', 560, 5, 1, 0.00, 1, NOW(), 0, 'admin', NOW()),

(@book_id, '第六章 并发编程实战',
'<h2>6.1 并发问题的根源</h2><p>并发问题的本质是"共享可变状态"。多个线程同时读写同一份数据，没有同步就会出错。解决思路三条：不共享（ThreadLocal）、不修改（不可变对象）、加锁（同步）。</p><h2>6.2 锁的层级</h2><p>从轻到重：原子类（CAS）→ 读写锁（ReentrantReadWriteLock）→ 互斥锁（synchronized / ReentrantLock）。能用原子类就别用锁，能用读写锁就别用互斥锁。锁粒度越小，并发度越高。</p><h2>6.3 ConcurrentHashMap 的正确用法</h2><p>CHM 的 get/put 是线程安全的，但"读-判断-写"复合操作不是。</p><pre><code>// 错误：复合操作非原子\nif (!map.containsKey(key)) {\n    map.put(key, value);\n}\n// 正确：用原子方法\nmap.putIfAbsent(key, value);</code></pre><h2>6.4 线程池不要用 Executors 创建</h2><p>Executors.newFixedThreadPool 用的是无界队列，OOM 风险。用 ThreadPoolExecutor 显式指定队列容量和拒绝策略。</p><pre><code>new ThreadPoolExecutor(\n    8, 16, 60, TimeUnit.SECONDS,\n    new LinkedBlockingQueue<>(200),\n    new ThreadFactoryBuilder().setNameFormat("biz-pool-%d").build(),\n    new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略：让调用方执行，形成背压\n);</code></pre>',
'## 6.1 并发问题的根源\n\n并发问题的本质是"共享可变状态"。多个线程同时读写同一份数据，没有同步就会出错。解决思路三条：不共享（ThreadLocal）、不修改（不可变对象）、加锁（同步）。\n\n## 6.2 锁的层级\n\n从轻到重：原子类（CAS）→ 读写锁（ReentrantReadWriteLock）→ 互斥锁（synchronized / ReentrantLock）。能用原子类就别用锁，能用读写锁就别用互斥锁。锁粒度越小，并发度越高。\n\n## 6.3 ConcurrentHashMap 的正确用法\n\nCHM 的 get/put 是线程安全的，但"读-判断-写"复合操作不是。\n\n```java\n// 错误：复合操作非原子\nif (!map.containsKey(key)) {\n    map.put(key, value);\n}\n// 正确：用原子方法\nmap.putIfAbsent(key, value);\n```\n\n## 6.4 线程池不要用 Executors 创建\n\nExecutors.newFixedThreadPool 用的是无界队列，OOM 风险。用 ThreadPoolExecutor 显式指定队列容量和拒绝策略。\n\n```java\nnew ThreadPoolExecutor(\n    8, 16, 60, TimeUnit.SECONDS,\n    new LinkedBlockingQueue<>(200),\n    new ThreadFactoryBuilder().setNameFormat("biz-pool-%d").build(),\n    new ThreadPoolExecutor.CallerRunsPolicy()\n);\n```',
'richtext', 820, 6, 1, 0.00, 1, NOW(), 0, 'admin', NOW()),

(@book_id, '第七章 安全防护要点',
'<h2>7.1 SQL 注入</h2><p>永远用参数化查询，不要拼接 SQL。MyBatis 的 #{} 是参数化（安全），${} 是字符串拼接（危险）。${} 只能用于动态表名/列名等不能参数化的场景，且必须做白名单校验。</p><h2>7.2 XSS</h2><p>用户输入的内容渲染到 HTML 时必须转义。用白名单方式：只允许安全标签和属性，其余全部过滤。Markdown 渲染器输出后必须过 sanitize。</p><h2>7.3 越权（IDOR）</h2><p>所有写操作前校验资源归属：这篇文章的 authorId 是不是当前用户？这个订单的 buyerId 是不是当前用户？不校验就是越权漏洞，任意用户可改/删他人数据。</p><pre><code>// 错误：只校验登录，不校验归属\nLong userId = getUserId();\narticleService.update(article); // article.authorId 可能是别人的\n\n// 正确：校验归属\nif (!article.getAuthorId().equals(userId)) {\n    throw new RuntimeException("无权操作他人文章");\n}</code></pre><h2>7.4 密码存储</h2><p>BCrypt 加盐哈希，永远不要明文存储。BCrypt 自带盐，且可调成本因子，是当前最推荐的方案。</p><h2>7.5 最小权限与 fail-fast</h2><p>安全配置缺失时应该拒绝启动，而不是回退到默认弱配置。密钥未配置就抛异常，而不是用硬编码备用密钥继续跑。</p>',
'## 7.1 SQL 注入\n\n永远用参数化查询，不要拼接 SQL。MyBatis 的 `#{}` 是参数化（安全），`${}` 是字符串拼接（危险）。`${}` 只能用于动态表名/列名等不能参数化的场景，且必须做白名单校验。\n\n## 7.2 XSS\n\n用户输入的内容渲染到 HTML 时必须转义。用白名单方式：只允许安全标签和属性，其余全部过滤。Markdown 渲染器输出后必须过 sanitize。\n\n## 7.3 越权（IDOR）\n\n所有写操作前校验资源归属：这篇文章的 authorId 是不是当前用户？这个订单的 buyerId 是不是当前用户？不校验就是越权漏洞，任意用户可改/删他人数据。\n\n```java\n// 错误：只校验登录，不校验归属\nLong userId = getUserId();\narticleService.update(article);\n\n// 正确：校验归属\nif (!article.getAuthorId().equals(userId)) {\n    throw new RuntimeException("无权操作他人文章");\n}\n```\n\n## 7.4 密码存储\n\nBCrypt 加盐哈希，永远不要明文存储。BCrypt 自带盐，且可调成本因子，是当前最推荐的方案。\n\n## 7.5 最小权限与 fail-fast\n\n安全配置缺失时应该拒绝启动，而不是回退到默认弱配置。密钥未配置就抛异常，而不是用硬编码备用密钥继续跑。',
'richtext', 780, 7, 1, 0.00, 1, NOW(), 0, 'admin', NOW()),

(@book_id, '第八章 性能调优方法论',
'<h2>8.1 先测量，再优化</h2><p>"过早优化是万恶之源"——但更糟的是凭感觉优化。优化前先用 APM 或日志定位瓶颈：是 CPU、IO、数据库、还是网络？优化数据库索引和优化网络调用是两个完全不同方向，不测量就是瞎猜。</p><h2>8.2 缓存分层</h2><p>缓存不是银弹，引入缓存就引入了一致性问题。分层策略：浏览器缓存 → CDN → 本地缓存（Caffeine）→ 分布式缓存（Redis）→ 数据库。能短就不长，能近就不远。</p><p>缓存三大问题：穿透（查不存在的 key）、击穿（热 key 过期）、雪崩（大量 key 同时过期）。穿透用布隆过滤器，击穿用互斥锁，雪崩用随机过期时间。</p><h2>8.3 异步化</h2><p>耗时操作（发邮件、推送、写日志、统计）异步化，主流程快速返回。用消息队列削峰填谷。但异步意味着最终一致，要考虑消息丢失和重复消费。</p><h2>8.4 数据库优化优先级</h2><p>SQL 优化 &gt; 索引优化 &gt; 表结构优化 &gt; 分库分表。成本从低到高，收益从快到慢。不要一上来就分库分表，先把 SQL 和索引调好。</p>',
'## 8.1 先测量，再优化\n\n"过早优化是万恶之源"——但更糟的是凭感觉优化。优化前先用 APM 或日志定位瓶颈：是 CPU、IO、数据库、还是网络？优化数据库索引和优化网络调用是两个完全不同方向，不测量就是瞎猜。\n\n## 8.2 缓存分层\n\n缓存不是银弹，引入缓存就引入了一致性问题。分层策略：浏览器缓存 → CDN → 本地缓存（Caffeine）→ 分布式缓存（Redis）→ 数据库。能短就不长，能近就不远。\n\n缓存三大问题：穿透（查不存在的 key）、击穿（热 key 过期）、雪崩（大量 key 同时过期）。穿透用布隆过滤器，击穿用互斥锁，雪崩用随机过期时间。\n\n## 8.3 异步化\n\n耗时操作（发邮件、推送、写日志、统计）异步化，主流程快速返回。用消息队列削峰填谷。但异步意味着最终一致，要考虑消息丢失和重复消费。\n\n## 8.4 数据库优化优先级\n\nSQL 优化 > 索引优化 > 表结构优化 > 分库分表。成本从低到高，收益从快到慢。不要一上来就分库分表，先把 SQL 和索引调好。',
'richtext', 690, 8, 1, 0.00, 1, NOW(), 0, 'admin', NOW()),

(@book_id, '第九章 DevOps 与持续交付',
'<h2>9.1 自动化是工程化的前提</h2><p>手动部署 = 不稳定。构建、测试、部署、回滚全链路自动化，是团队规模超过 5 人后的必备能力。CI/CD 不是工具，是一种工程纪律。</p><h2>9.2 容器化的边界</h2><p>容器不是银弹。无状态服务适合容器化（API、Web），有状态服务谨慎（数据库、消息队列建议用托管服务而非自建容器）。容器的价值在于环境一致性和快速伸缩，不是"用了就显得先进"。</p><h2>9.3 监控与告警</h2><p>没有监控的系统等于盲飞。三层监控：基础设施（CPU/内存/磁盘）、应用（QPS/延迟/错误率）、业务（订单量/转化率）。告警要精准，噪声告警会让团队麻木，最终忽略真正的故障。</p><h2>9.4 灰度发布</h2><p>不要一次性全量发布。灰度策略：先小流量验证，再逐步放量。有问题快速回滚，而不是在线上 debug。回滚机制比发版机制更重要。</p>',
'## 9.1 自动化是工程化的前提\n\n手动部署 = 不稳定。构建、测试、部署、回滚全链路自动化，是团队规模超过 5 人后的必备能力。CI/CD 不是工具，是一种工程纪律。\n\n## 9.2 容器化的边界\n\n容器不是银弹。无状态服务适合容器化（API、Web），有状态服务谨慎（数据库、消息队列建议用托管服务而非自建容器）。容器的价值在于环境一致性和快速伸缩，不是"用了就显得先进"。\n\n## 9.3 监控与告警\n\n没有监控的系统等于盲飞。三层监控：基础设施（CPU/内存/磁盘）、应用（QPS/延迟/错误率）、业务（订单量/转化率）。告警要精准，噪声告警会让团队麻木，最终忽略真正的故障。\n\n## 9.4 灰度发布\n\n不要一次性全量发布。灰度策略：先小流量验证，再逐步放量。有问题快速回滚，而不是在线上 debug。回滚机制比发版机制更重要。',
'richtext', 600, 9, 1, 0.00, 1, NOW(), 0, 'admin', NOW()),

(@book_id, '第十章 技术领导力',
'<h2>10.1 技术领导力 ≠ 管理岗</h2><p>技术领导力不是"带几个人"，而是"用技术影响团队的方向"。一个资深工程师的价值，不只是自己能写多少代码，而是能让团队少踩多少坑、少走多少弯路。</p><h2>10.2 技术决策的责任</h2><p>选型决策要留痕——写技术方案文档（RFC），记录为什么选 A 不选 B、当时的前提假设是什么。半年后回头看，能复盘决策是否正确，而不是凭记忆争论"当时为什么这么选"。</p><h2>10.3 代码审查的价值</h2><p>Code Review 不是找茬，是知识传递。好的 CR 关注三点：逻辑是否正确、边界是否覆盖、可维护性是否及格。风格问题交给 lint 工具，CR 聚焦在人和机器都难发现的问题上。</p><h2>10.4 成长是长期主义</h2><p>技术的红利是复利的。今天多读的一篇源码、多写的一个测试、多复盘的一个事故，短期看不出差别，三年后是分水岭。保持学习，保持输出，保持对技术的好奇心——这是工程师能走多远的根本。</p>',
'## 10.1 技术领导力 ≠ 管理岗\n\n技术领导力不是"带几个人"，而是"用技术影响团队的方向"。一个资深工程师的价值，不只是自己能写多少代码，而是能让团队少踩多少坑、少走多少弯路。\n\n## 10.2 技术决策的责任\n\n选型决策要留痕——写技术方案文档（RFC），记录为什么选 A 不选 B、当时的前提假设是什么。半年后回头看，能复盘决策是否正确，而不是凭记忆争论"当时为什么这么选"。\n\n## 10.3 代码审查的价值\n\nCode Review 不是找茬，是知识传递。好的 CR 关注三点：逻辑是否正确、边界是否覆盖、可维护性是否及格。风格问题交给 lint 工具，CR 聚焦在人和机器都难发现的问题上。\n\n## 10.4 成长是长期主义\n\n技术的红利是复利的。今天多读的一篇源码、多写的一个测试、多复盘的一个事故，短期看不出差别，三年后是分水岭。保持学习，保持输出，保持对技术的好奇心——这是工程师能走多远的根本。',
'richtext', 620, 10, 1, 0.00, 1, NOW(), 0, 'admin', NOW());

-- 更新书籍的最新章节信息
SET @latest_chapter_id = (SELECT id FROM `portal_book_chapter` WHERE `book_id` = @book_id ORDER BY `chapter_no` DESC LIMIT 1);
SET @latest_chapter_title = (SELECT `title` FROM `portal_book_chapter` WHERE `id` = @latest_chapter_id);

UPDATE `portal_book` SET
    `latest_chapter_id` = @latest_chapter_id,
    `latest_chapter_title` = @latest_chapter_title,
    `last_update_time` = NOW()
WHERE `id` = @book_id;

-- ------------------------------------------------------------
-- 3. 金句摘录 portal_book_quote（6 条，幂等：用内容去重）
-- ------------------------------------------------------------
-- 取一个已存在的用户作为金句作者（admin = user_id 1）
SET @quote_user_id = (SELECT id FROM `portal_user` WHERE `username` = 'admin' LIMIT 1);

INSERT IGNORE INTO `portal_book_quote` (
    `user_id`, `book_id`, `content`, `page`, `chapter`, `location`,
    `like_count`, `is_public`, `is_featured`, `create_by`, `create_time`
) VALUES
(@quote_user_id, @book_id,
 '架构师的工作不是选"最优解"，而是在成本、人力、时间、复杂度之间找到当前阶段最合理的平衡点。',
 NULL, '第一章', '1.1 从码农到工程师', 0, 1, 1, 'admin', NOW()),

(@quote_user_id, @book_id,
 '能造轮子不等于一定要造，但具备这个能力，意味着你对技术的理解已经穿透了表象。',
 NULL, '第一章', '1.2 技术深度的三个层次', 0, 1, 0, 'admin', NOW()),

(@quote_user_id, @book_id,
 '一个函数只做一件事。判断标准：能否用一句话描述它的职责而不用"和"字。',
 NULL, '第二章', '2.2 函数：短小再短小', 0, 1, 1, 'admin', NOW()),

(@quote_user_id, @book_id,
 '静默吞掉的异常，会在生产环境变成无法定位的幽灵问题。',
 NULL, '第二章', '2.4 异常处理', 0, 1, 0, 'admin', NOW()),

(@quote_user_id, @book_id,
 '安全配置缺失时应该拒绝启动，而不是回退到默认弱配置。',
 NULL, '第七章', '7.5 最小权限与 fail-fast', 0, 1, 1, 'admin', NOW()),

(@quote_user_id, @book_id,
 '技术的红利是复利的。今天多读的一篇源码、多复盘的一个事故，三年后是分水岭。',
 NULL, '第十章', '10.4 成长是长期主义', 0, 1, 1, 'admin', NOW());

-- ------------------------------------------------------------
-- 4. 推荐位 portal_book_recommend（2 条，幂等：uk_book_position 去重）
-- ------------------------------------------------------------
INSERT IGNORE INTO `portal_book_recommend` (
    `book_id`, `position`, `sort`, `start_time`, `end_time`, `is_active`,
    `create_by`, `create_time`
) VALUES
(@book_id, 'home_hot', 1, NULL, NULL, 1, 'admin', NOW()),
(@book_id, 'discover_banner', 3, NULL, NULL, 1, 'admin', NOW());

-- ============================================================
-- 第 5/5 部分执行完成
-- 涉及表：portal_book(1) + portal_book_chapter(10) + portal_book_quote(6) + portal_book_recommend(2)
-- 总计：19 条记录
-- ============================================================


-- #############################################################
-- #                                                            #
-- #          合并文件 02_test_data.sql 全部执行完成             #
-- #                                                            #
-- #############################################################
-- 合并来源汇总：
--   第 1/5 部分：84_test_data_cleanup.sql        （测试数据清理：TRUNCATE/DELETE）
--   第 2/5 部分：06_portal_test_data.sql         （门户测试数据：用户/文章/评论/通知）
--   第 3/5 部分：26_reading_interview_test_data.sql （读书空间 & 面试空间测试数据）
--   第 4/5 部分：48_portal_book_seed_data.sql    （读书模块第三阶段种子数据补全）
--   第 5/5 部分：91_book_seed_data.sql           （读书空间示例书籍数据初始化）
-- ============================================================
-- 执行顺序遵循"先清后插"原则：
--   1. 84 脚本先清理所有业务表数据（保留 portal_user）
--   2. 06 / 26 / 48 / 91 脚本依次插入测试数据
-- 注意：48 与 91 内容互补，无重复数据（48 针对书籍 1-15，91 新增第 16 本书）
-- ============================================================
