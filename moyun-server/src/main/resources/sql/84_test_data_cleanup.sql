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
