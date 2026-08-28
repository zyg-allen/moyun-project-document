-- =========================================================================================
-- 增量补丁：修复"运营首页/index 待办列表空但实际有业务数据"
-- 根因：cms_article 等业务表 status='pending' 但 sys_audit_task 审核任务索引缺失。
-- 本 SQL 将所有已存在但缺失审核任务的业务行，回填到 sys_audit_task。
-- 执行日期：2026-08-28
-- 执行顺序：1 先运行本 SQL（补数）→ 2 重启 moyun-server → 3 后台首页点"刷新缓存"按钮（或等 dashboard:full 短 TTL 20s）
-- =========================================================================================
USE `moyun-db`;

-- ----------------------------------------------------------------
-- 1. 回填 cms_article（PortalArticle） status='pending' 的审核任务
--    task_type = 'article'，route_path 统一走 /portal/audit-center（后台审核中心路径）
-- ----------------------------------------------------------------
INSERT INTO sys_audit_task (task_type, biz_type, biz_id, title, description, submitter_id, submitter_name, status, auditor_id, auditor_name, audit_opinion, audit_action, submit_time, audit_time, priority, route_path, extra_data, create_time, update_time)
SELECT
    'article'                                          AS task_type,
    NULL                                               AS biz_type,
    a.id                                               AS biz_id,
    a.title                                            AS title,
    a.excerpt                                          AS description,
    a.author_id                                        AS submitter_id,
    COALESCE(pu.nickname, pu.username, '系统')         AS submitter_name,
    'pending'                                          AS status,
    NULL                                               AS auditor_id,
    NULL                                               AS auditor_name,
    NULL                                               AS audit_opinion,
    NULL                                               AS audit_action,
    COALESCE(a.publish_time, a.create_time, NOW())     AS submit_time,
    NULL                                               AS audit_time,
    'medium'                                           AS priority,
    '/portal/audit-center'                             AS route_path,
    NULL                                               AS extra_data,
    NOW()                                              AS create_time,
    NOW()                                              AS update_time
FROM cms_article a
LEFT JOIN portal_user pu ON pu.id = a.author_id
WHERE a.status = 'pending'
  AND a.del_flag = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_audit_task t
      WHERE t.task_type = 'article' AND t.biz_id = a.id
  );

-- ----------------------------------------------------------------
-- 2. 回填已通过/已驳回文章的审核任务（让"已办"可见，避免业务表有终态但已办空）
--    状态映射：published→approved；rejected→rejected；archived→approved
-- ----------------------------------------------------------------
INSERT INTO sys_audit_task (task_type, biz_type, biz_id, title, description, submitter_id, submitter_name, status, auditor_id, auditor_name, audit_opinion, audit_action, submit_time, audit_time, priority, route_path, extra_data, create_time, update_time)
SELECT
    'article'                                          AS task_type,
    NULL                                               AS biz_type,
    a.id                                               AS biz_id,
    a.title                                            AS title,
    a.excerpt                                          AS description,
    a.author_id                                        AS submitter_id,
    COALESCE(pu.nickname, pu.username, '系统')         AS submitter_name,
    CASE a.status
        WHEN 'published' THEN 'approved'
        WHEN 'rejected'  THEN 'rejected'
        WHEN 'archived'  THEN 'approved'
        ELSE NULL END                                  AS status,
    COALESCE(a.update_by, NULL)                        AS auditor_id,
    COALESCE(a.update_by, NULL)                        AS auditor_name,
    NULL                                               AS audit_opinion,
    CASE a.status
        WHEN 'rejected'  THEN 'reject'
        WHEN 'published' THEN 'approve'
        WHEN 'archived'  THEN 'approve'
        ELSE NULL END                                  AS audit_action,
    COALESCE(a.publish_time, a.create_time, NOW())     AS submit_time,
    a.update_time                                      AS audit_time,
    'medium'                                           AS priority,
    '/portal/audit-center'                             AS route_path,
    NULL                                               AS extra_data,
    NOW()                                              AS create_time,
    NOW()                                              AS update_time
FROM cms_article a
LEFT JOIN portal_user pu ON pu.id = a.author_id
WHERE a.status IN ('published', 'rejected', 'archived')
  AND a.del_flag = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_audit_task t
      WHERE t.task_type = 'article' AND t.biz_id = a.id
  )
  AND CASE a.status
        WHEN 'published' THEN 'approved'
        WHEN 'rejected'  THEN 'rejected'
        WHEN 'archived'  THEN 'approved'
      END IS NOT NULL;

-- ----------------------------------------------------------------
-- 3. 同理，如果有其他业务类型的待办也需要补数，可参考下方模板：
--    topic / column / certification / feedback / report / interview_exp / interview_comment
--    建议：按实际需要手动放开对应块，或使用系统启动时的自动补数工具。
-- ----------------------------------------------------------------
-- TODO 按需补充（以下模板按实际表结构调整字段）：
/*
INSERT INTO sys_audit_task (task_type, biz_type, biz_id, title, description, submitter_id, submitter_name, status, priority, route_path, create_time, update_time)
SELECT 'topic', NULL, t.id, t.title, t.content, t.author_id, pu.nickname, 'pending', 'medium', '/portal/audit-center', NOW(), NOW()
FROM cms_topic t LEFT JOIN portal_user pu ON pu.id = t.author_id
WHERE t.status = 'pending' AND t.del_flag = 0
  AND NOT EXISTS (SELECT 1 FROM sys_audit_task x WHERE x.task_type='topic' AND x.biz_id=t.id);
*/

-- ----------------------------------------------------------------
-- 4. 清理首页缓存（直接删除对应 Redis key；若 Redis 没连上，重启后点"刷新缓存"按钮也可）
--    说明：下面是 SQL 注释，需要手动执行 Redis CLI 或通过后台按钮执行：
--      redis-cli DEL "dashboard:full" "dashboard:todoTasks" "dashboard:myTasks" "dashboard:myTasks:*"
--    或：登录后台 → 运营首页 → 右上角 「刷新缓存」按钮。
-- ----------------------------------------------------------------
SELECT 'PLEASE RUN: redis-cli DEL dashboard:full dashboard:todoTasks dashboard:myTasks' AS _hint;
SELECT 'OR: 后台 /index 首页点「刷新缓存」按钮后，最多等 20s 短缓存自动失效即可重新拉数据。' AS _tip;
