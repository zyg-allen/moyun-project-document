-- =============================================================
-- v11.35.1 审核待办 routePath 修正
-- 背景：AuditTaskType.FEEDBACK/REPORT 的 defaultRoutePath 原为
--       /cms/feedback、/cms/report（页面不存在，首页待办点详情 404）。
--       已统一改为 /portal/audit-center（所有审核类型默认路由）。
--       本脚本修正存量任务（新任务由枚举生成正确值）。
-- 幂等：可重复执行。
-- =============================================================

-- 1. 存量待办任务的 route_path 修正
UPDATE `moyun-db`.sys_audit_task
SET route_path = '/portal/audit-center'
WHERE route_path IN ('/cms/feedback', '/cms/report');

-- 2. 校验：应返回 0 行
SELECT id, task_type, route_path
FROM `moyun-db`.sys_audit_task
WHERE route_path LIKE '/cms/%';