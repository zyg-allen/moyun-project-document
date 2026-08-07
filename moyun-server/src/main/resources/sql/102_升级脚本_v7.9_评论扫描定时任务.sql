-- ====================================================================
-- v7.9 升级脚本：评论类敏感词定时扫描任务注册
-- 适配 MySQL 8.x
-- 说明：本脚本幂等，可重复执行（WHERE NOT EXISTS 防重）
-- 背景：
--   1. 评论类统一为"不审核 + 定时扫描兜底"策略（见 v6.7 话题/观点扫描范式）。
--   2. SensitiveScanTask 已新增 scanArticleComments / scanInterviewComments 两个方法
--      （分别对应 portal_comment 与 portal_interview_comment 表）。
--   3. 本脚本将这两个方法注册为 sys_job 定时任务，cron 错峰避开既有扫描任务：
--      - scanTopics()             0 0  3 * * ?  （v6.7 已注册，每天 03:00）
--      - scanTopicPosts()         0 5  3 * * ?  （v6.7 已注册，每天 03:05）
--      - scanArticleComments()    0 10 3 * * ?  （本脚本新增，每天 03:10）
--      - scanInterviewComments() 0 15 3 * * ?  （本脚本新增，每天 03:15）
--   4. 任务默认启用（status='0'），misfire_policy='3'（放弃补偿，避免堆积），
--      concurrent='1'（禁止并发，防止上一批未扫完就启动下一批）。
-- ====================================================================

-- 1. 文章评论敏感词扫描
INSERT INTO sys_job (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
SELECT '敏感词扫描-文章评论', 'DEFAULT', 'sensitiveScanTask.scanArticleComments()', '0 10 3 * * ?', '3', '1', '0', 'admin', NOW(),
       '扫描已发布文章评论(portal_comment.status=1)，命中敏感词转驳回(status=2)并通知作者'
WHERE NOT EXISTS (SELECT 1 FROM sys_job WHERE invoke_target = 'sensitiveScanTask.scanArticleComments()');

-- 2. 面经评论敏感词扫描
INSERT INTO sys_job (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
SELECT '敏感词扫描-面经评论', 'DEFAULT', 'sensitiveScanTask.scanInterviewComments()', '0 15 3 * * ?', '3', '1', '0', 'admin', NOW(),
       '扫描已发布面经评论(portal_interview_comment.status=published)，命中敏感词转rejected并通知作者'
WHERE NOT EXISTS (SELECT 1 FROM sys_job WHERE invoke_target = 'sensitiveScanTask.scanInterviewComments()');

-- ====================================================================
-- 升级完成
-- ====================================================================
