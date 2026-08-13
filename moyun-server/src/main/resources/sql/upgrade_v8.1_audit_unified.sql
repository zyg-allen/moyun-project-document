-- =====================================================================
-- 墨韵智库 v8.1 · 审核模块统一整合升级脚本
-- =====================================================================
-- 用途：
--   1. 新建统一审核任务表 sys_audit_task（替代分散的各业务表 status 聚合）
--   2. 新建定时任务扫描结果表 sys_job_scan_issue（定时任务异常/待处理项记录）
--   3. 数据回填：将各业务表现有 pending 记录回填到 sys_audit_task
--   4. 菜单调整：新增「任务管理」一级菜单，挪入定时任务/我的待办/我的已办/扫描结果
--   5. audit-center 保留为「全部审核」入口，权限沿用 cms:audit-center:list
--
-- 设计原则：
--   - 业务表 status 字段保留作为真实状态（双写）
--   - sys_audit_task 作为审核入口索引 + 审核记录
--   - 提交审核时双写：业务表 status=pending + sys_audit_task status=pending
--   - 处理审核时双写：sys_audit_task 终态 + 业务表终态
--
-- 适配：MySQL 8.0+（utf8mb4 / utf8mb4_0900_ai_ci）
-- 幂等：所有 DDL 使用 IF NOT EXISTS / information_schema 校验
-- =====================================================================

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

SELECT '================================================' AS info;
SELECT '墨韵智库 v8.1 审核模块统一整合升级开始' AS info;
SELECT CONCAT('数据库: ', CONVERT(DATABASE() USING utf8mb4) COLLATE utf8mb4_0900_ai_ci, ' | 时间: ', NOW()) AS info;
SELECT '================================================' AS info;


-- =====================================================================
-- 一、统一审核任务表 sys_audit_task
-- =====================================================================
-- 设计：
--   - task_type：任务类型（article/column/topic/interview_exp/interview_comment/
--     certification/feedback/report），覆盖系统全部审核业务
--   - biz_type：业务子类型（如 report 的 spam/infringement；article 的原创/转载）
--   - biz_id：业务记录主键
--   - status：pending（待处理）/ approved（通过）/ rejected（驳回）
--   - audit_action：approve（通过操作）/ reject（驳回操作），与 audit_opinion 配合
--   - 双写策略：业务表 status 保留作真实状态，本表作索引 + 记录
--   - route_path：点击「查看详情」跳转的业务管理页路径（如 /cms/article）
CREATE TABLE IF NOT EXISTS sys_audit_task (
  id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  task_type       VARCHAR(32)  NOT NULL                COMMENT '任务类型：article/column/topic/interview_exp/interview_comment/certification/feedback/report',
  biz_type        VARCHAR(32)  DEFAULT NULL            COMMENT '业务子类型（如 report 的 spam/infringement）',
  biz_id          BIGINT       NOT NULL                COMMENT '业务记录ID',
  title           VARCHAR(255) NOT NULL                COMMENT '任务标题（有标题用标题，无则用类型名+ID）',
  description     TEXT         DEFAULT NULL            COMMENT '任务描述/摘要',
  submitter_id    BIGINT       DEFAULT NULL            COMMENT '提交人ID（门户用户ID）',
  submitter_name  VARCHAR(64)  DEFAULT NULL            COMMENT '提交人用户名',
  status          VARCHAR(20)  NOT NULL DEFAULT 'pending' COMMENT '状态：pending/approved/rejected',
  auditor_id      BIGINT       DEFAULT NULL            COMMENT '处理人ID（系统用户ID）',
  auditor_name    VARCHAR(64)  DEFAULT NULL            COMMENT '处理人用户名',
  audit_opinion   VARCHAR(1000) DEFAULT NULL           COMMENT '审核意见（驳回时必填）',
  audit_action    VARCHAR(20)  DEFAULT NULL            COMMENT '审核操作类型：approve/reject',
  submit_time     DATETIME     DEFAULT NULL            COMMENT '提交时间',
  audit_time     DATETIME     DEFAULT NULL            COMMENT '处理时间',
  priority        VARCHAR(10)  NOT NULL DEFAULT 'medium' COMMENT '优先级：high/medium/low',
  route_path      VARCHAR(255) DEFAULT NULL            COMMENT '查看详情跳转路径（业务管理页）',
  extra_data      TEXT         DEFAULT NULL            COMMENT '扩展数据 JSON（如举报图片、反馈联系方式）',
  create_time     DATETIME     DEFAULT NULL            COMMENT '创建时间',
  update_time     DATETIME     DEFAULT NULL            COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_audit_status (status),
  KEY idx_audit_auditor (auditor_id),
  KEY idx_audit_submitter (submitter_id),
  KEY idx_audit_biz (biz_type, biz_id),
  KEY idx_audit_task_type (task_type, status),
  KEY idx_audit_submit_time (submit_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='统一审核任务表（v8.1）';


-- =====================================================================
-- 二、定时任务扫描结果表 sys_job_scan_issue
-- =====================================================================
-- 设计：
--   - 定时任务执行过程中扫描出的异常/待处理项记录
--   - 人工在「任务管理 > 扫描结果」页面处理
--   - 支持关联日志摘要、目标对象（如扫描到的违规文章 ID）
CREATE TABLE IF NOT EXISTS sys_job_scan_issue (
  id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  job_id          BIGINT       DEFAULT NULL            COMMENT '触发扫描的定时任务ID（sys_job.job_id）',
  job_name        VARCHAR(64)  DEFAULT NULL            COMMENT '定时任务名称',
  issue_type      VARCHAR(32)  NOT NULL                COMMENT '问题类型：sensitive_word/pending_overdue/anomaly/other',
  issue_desc      VARCHAR(500) NOT NULL                COMMENT '问题描述',
  target_type     VARCHAR(32)  DEFAULT NULL            COMMENT '目标对象类型（如 article/comment/user）',
  target_id       BIGINT       DEFAULT NULL            COMMENT '目标对象ID',
  target_title    VARCHAR(255) DEFAULT NULL            COMMENT '目标对象标题/摘要',
  log_excerpt     TEXT         DEFAULT NULL            COMMENT '日志摘要（便于人工排查）',
  status          VARCHAR(20)  NOT NULL DEFAULT 'pending' COMMENT '状态：pending/handled/ignored',
  handler_id      BIGINT       DEFAULT NULL            COMMENT '处理人ID',
  handler_name    VARCHAR(64)  DEFAULT NULL            COMMENT '处理人用户名',
  handle_result   VARCHAR(500) DEFAULT NULL            COMMENT '处理结果说明',
  handle_time     DATETIME     DEFAULT NULL            COMMENT '处理时间',
  create_time     DATETIME     DEFAULT NULL            COMMENT '扫描发现时间',
  PRIMARY KEY (id),
  KEY idx_scan_status (status),
  KEY idx_scan_job (job_id),
  KEY idx_scan_target (target_type, target_id),
  KEY idx_scan_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='定时任务扫描结果表（v8.1）';


-- =====================================================================
-- 三、数据回填：现有各业务 pending 记录回填到 sys_audit_task
-- =====================================================================
-- 说明：
--   - 仅回填 status=pending 的待审核记录（已处理的历史记录不回填，避免污染待办）
--   - 使用 INSERT IGNORE 防止重复回填（基于 biz_type+biz_id 唯一性，由应用层保证）
--   - 回填后 sys_audit_task.status=pending，submitter/auditor 等字段从业务表取
--   - route_path 回填为对应业务管理页

-- 3.1 文章待审核回填
INSERT INTO sys_audit_task (task_type, biz_type, biz_id, title, description,
  submitter_id, submitter_name, status, priority, route_path, submit_time, create_time)
SELECT
  'article' AS task_type,
  NULL AS biz_type,
  a.id AS biz_id,
  a.title AS title,
  a.excerpt AS description,
  a.author_id AS submitter_id,
  a.author_name AS submitter_name,
  'pending' AS status,
  'high' AS priority,
  '/cms/article' AS route_path,
  a.create_time AS submit_time,
  NOW() AS create_time
FROM portal_article a
WHERE a.status = 'pending'
  AND NOT EXISTS (
    SELECT 1 FROM sys_audit_task t
    WHERE t.task_type = 'article' AND t.biz_id = a.id
  );

-- 3.2 专栏待审核回填
INSERT INTO sys_audit_task (task_type, biz_type, biz_id, title, description,
  submitter_id, submitter_name, status, priority, route_path, submit_time, create_time)
SELECT
  'column' AS task_type,
  NULL AS biz_type,
  c.id AS biz_id,
  c.name AS title,
  c.description AS description,
  c.creator_id AS submitter_id,
  c.creator_name AS submitter_name,
  'pending' AS status,
  'medium' AS priority,
  '/cms/column' AS route_path,
  c.create_time AS submit_time,
  NOW() AS create_time
FROM portal_column c
WHERE c.status = 'pending'
  AND NOT EXISTS (
    SELECT 1 FROM sys_audit_task t
    WHERE t.task_type = 'column' AND t.biz_id = c.id
  );

-- 3.3 话题待审核回填
INSERT INTO sys_audit_task (task_type, biz_type, biz_id, title, description,
  submitter_id, submitter_name, status, priority, route_path, submit_time, create_time)
SELECT
  'topic' AS task_type,
  NULL AS biz_type,
  t.id AS biz_id,
  t.title AS title,
  t.description AS description,
  t.creator_id AS submitter_id,
  t.creator_name AS submitter_name,
  'pending' AS status,
  'medium' AS priority,
  '/cms/topic' AS route_path,
  t.create_time AS submit_time,
  NOW() AS create_time
FROM portal_topic t
WHERE t.status = 'pending'
  AND NOT EXISTS (
    SELECT 1 FROM sys_audit_task at
    WHERE at.task_type = 'topic' AND at.biz_id = t.id
  );

-- 3.4 面经待审核回填
INSERT INTO sys_audit_task (task_type, biz_type, biz_id, title, description,
  submitter_id, submitter_name, status, priority, route_path, submit_time, create_time)
SELECT
  'interview_exp' AS task_type,
  NULL AS biz_type,
  e.id AS biz_id,
  e.title AS title,
  LEFT(IFNULL(e.content, ''), 500) AS description,
  e.user_id AS submitter_id,
  e.username AS submitter_name,
  'pending' AS status,
  'high' AS priority,
  '/cms/interview/experience' AS route_path,
  e.create_time AS submit_time,
  NOW() AS create_time
FROM portal_interview_experience e
WHERE e.status = 'pending'
  AND NOT EXISTS (
    SELECT 1 FROM sys_audit_task t
    WHERE t.task_type = 'interview_exp' AND t.biz_id = e.id
  );

-- 3.5 面经评论待审核回填
INSERT INTO sys_audit_task (task_type, biz_type, biz_id, title, description,
  submitter_id, submitter_name, status, priority, route_path, submit_time, create_time)
SELECT
  'interview_comment' AS task_type,
  NULL AS biz_type,
  c.id AS biz_id,
  CONCAT('面经评论 #', c.id) AS title,
  LEFT(IFNULL(c.content, ''), 500) AS description,
  c.user_id AS submitter_id,
  c.username AS submitter_name,
  'pending' AS status,
  'low' AS priority,
  '/cms/interview/comment' AS route_path,
  c.create_time AS submit_time,
  NOW() AS create_time
FROM portal_interview_comment c
WHERE c.status = 'pending'
  AND NOT EXISTS (
    SELECT 1 FROM sys_audit_task t
    WHERE t.task_type = 'interview_comment' AND t.biz_id = c.id
  );

-- 3.6 创作者认证待审核回填
INSERT INTO sys_audit_task (task_type, biz_type, biz_id, title, description,
  submitter_id, submitter_name, status, priority, route_path, submit_time, create_time)
SELECT
  'certification' AS task_type,
  NULL AS biz_type,
  cert.id AS biz_id,
  CONCAT('创作者认证申请 #', cert.id) AS title,
  LEFT(IFNULL(cert.real_name, ''), 500) AS description,
  cert.user_id AS submitter_id,
  cert.username AS submitter_name,
  'pending' AS status,
  'medium' AS priority,
  '/certification/audit' AS route_path,
  cert.create_time AS submit_time,
  NOW() AS create_time
FROM portal_creator_certification cert
WHERE cert.status = 'pending'
  AND NOT EXISTS (
    SELECT 1 FROM sys_audit_task t
    WHERE t.task_type = 'certification' AND t.biz_id = cert.id
  );

-- 3.7 意见反馈待处理回填
INSERT INTO sys_audit_task (task_type, biz_type, biz_id, title, description,
  submitter_id, submitter_name, status, priority, route_path, submit_time, create_time)
SELECT
  'feedback' AS task_type,
  f.feedback_type AS biz_type,
  f.id AS biz_id,
  IFNULL(f.subject, CONCAT('意见反馈 #', f.id)) AS title,
  f.description AS description,
  f.user_id AS submitter_id,
  f.username AS submitter_name,
  'pending' AS status,
  'medium' AS priority,
  '/cms/feedback' AS route_path,
  f.create_time AS submit_time,
  NOW() AS create_time
FROM portal_feedback f
WHERE f.status = 'pending'
  AND NOT EXISTS (
    SELECT 1 FROM sys_audit_task t
    WHERE t.task_type = 'feedback' AND t.biz_id = f.id
  );

-- 3.8 举报待处理回填
INSERT INTO sys_audit_task (task_type, biz_type, biz_id, title, description,
  submitter_id, submitter_name, status, priority, route_path, submit_time, create_time)
SELECT
  'report' AS task_type,
  r.report_type AS biz_type,
  r.id AS biz_id,
  CONCAT('举报：', IFNULL(r.target_url, CONCAT('#', r.id))) AS title,
  r.description AS description,
  r.user_id AS submitter_id,
  r.username AS submitter_name,
  'pending' AS status,
  'high' AS priority,
  '/cms/report' AS route_path,
  r.create_time AS submit_time,
  NOW() AS create_time
FROM portal_report r
WHERE r.status = 'pending'
  AND NOT EXISTS (
    SELECT 1 FROM sys_audit_task t
    WHERE t.task_type = 'report' AND t.biz_id = r.id
  );

SELECT CONCAT('数据回填完成，sys_audit_task 待办总数: ',
  (SELECT COUNT(*) FROM sys_audit_task WHERE status = 'pending')) AS info;


-- =====================================================================
-- 四、菜单调整
-- =====================================================================

-- 4.1 新增一级菜单「任务管理」（parent_id=0）
SET @sql := IF(
  (SELECT COUNT(*) FROM sys_menu WHERE menu_name = '任务管理' AND parent_id = 0) = 0,
  CONCAT('INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES (''任务管理'', 0, 15, ''task'', NULL, NULL, 1, 0, ''M'', ''0'', ''0'', NULL, ''set-up'', ''admin'', NOW(), ''任务管理一级目录（v8.1）：定时任务/我的待办/我的已办/扫描结果'')'),
  'SELECT ''任务管理菜单已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SELECT @task_menu_id := menu_id FROM sys_menu WHERE menu_name = '任务管理' AND parent_id = 0 LIMIT 1;
SELECT CONCAT('任务管理菜单ID: ', @task_menu_id) AS info;

-- 4.2 我的待办（统一待办列表，从 sys_audit_task 查 pending）
-- perms 与 AuditTaskController 列表接口对齐：system:auditTask:list
SET @sql := IF(
  (SELECT COUNT(*) FROM sys_menu WHERE perms = 'system:auditTask:todo') = 0,
  CONCAT('INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES (''我的待办'', ', @task_menu_id, ', 1, ''todo'', ''system/audit/todo'', NULL, 1, 0, ''C'', ''0'', ''0'', ''system:auditTask:todo'', ''bell'', ''admin'', NOW(), ''我的待办（v8.1）：统一审核待办列表'')'),
  'SELECT ''我的待办菜单已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4.3 我的已办（已处理列表，从 sys_audit_task 查 auditor_id=当前用户）
SET @sql := IF(
  (SELECT COUNT(*) FROM sys_menu WHERE perms = 'system:auditTask:done') = 0,
  CONCAT('INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES (''我的已办'', ', @task_menu_id, ', 2, ''done'', ''system/audit/done'', NULL, 1, 0, ''C'', ''0'', ''0'', ''system:auditTask:done'', ''checkbox'', ''admin'', NOW(), ''我的已办（v8.1）：已处理审核记录'')'),
  'SELECT ''我的已办菜单已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4.4 扫描结果（定时任务扫描出的问题，从 sys_job_scan_issue 查）
SET @sql := IF(
  (SELECT COUNT(*) FROM sys_menu WHERE perms = 'system:scan:list') = 0,
  CONCAT('INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES (''扫描结果'', ', @task_menu_id, ', 3, ''scan'', ''system/scan/index'', NULL, 1, 0, ''C'', ''0'', ''0'', ''system:scan:list'', ''eye'', ''admin'', NOW(), ''扫描结果（v8.1）：定时任务扫描出的异常/待处理项'')'),
  'SELECT ''扫描结果菜单已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4.5 定时任务菜单从「系统监控」挪到「任务管理」下
-- 原 menu_id=110，parent_id=2（系统监控），改为 parent_id=@task_menu_id
UPDATE sys_menu SET parent_id = @task_menu_id, order_num = 4
WHERE menu_id = 110 AND parent_id = 2;
SELECT CONCAT('定时任务菜单已挪至任务管理（parent_id=', @task_menu_id, '）') AS info;

-- 4.6 内容审核中心菜单（audit-center）保留，更新备注 + 权限对齐
-- v7.10 创建，原 perms=cms:audit-center:list。v8.1 统一改为 system:auditTask:list，
-- 使审核中心/我的待办/我的已办共享同一权限标识，简化授权。
UPDATE sys_menu SET remark = '内容审核中心（v8.1 整合入口）：统一列表 + 详情弹窗 + 审核操作，聚合文章/专栏/话题/面经/面经评论/认证/反馈/举报'
WHERE perms = 'cms:audit-center:list' AND menu_name = '内容审核中心';
SELECT '内容审核中心菜单已更新备注' AS info;

-- 4.7 审核操作按钮权限（同意/驳回、查看详情）
-- 挂在内容审核中心菜单下，作为按钮型菜单（menu_type=F）
SET @audit_center_menu_id := (SELECT menu_id FROM sys_menu WHERE perms = 'cms:audit-center:list' LIMIT 1);
SET @sql := IF(
  @audit_center_menu_id IS NOT NULL AND (SELECT COUNT(*) FROM sys_menu WHERE perms = 'system:auditTask:handle') = 0,
  CONCAT('INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES (''审核处理'', ', @audit_center_menu_id, ', 1, '''', NULL, NULL, 1, 0, ''F'', ''0'', ''0'', ''system:auditTask:handle'', ''button'', ''admin'', NOW(), ''同意/驳回审核任务'')'),
  'SELECT ''审核处理按钮已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(
  @audit_center_menu_id IS NOT NULL AND (SELECT COUNT(*) FROM sys_menu WHERE perms = 'system:auditTask:query') = 0,
  CONCAT('INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES (''审核详情'', ', @audit_center_menu_id, ', 2, '''', NULL, NULL, 1, 0, ''F'', ''0'', ''0'', ''system:auditTask:query'', ''button'', ''admin'', NOW(), ''查看审核任务详情'')'),
  'SELECT ''审核详情按钮已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4.8 admin 角色关联新增菜单（任务管理目录 + 我的待办/已办/扫描结果 + 审核按钮）
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu
WHERE perms IN ('system:auditTask:todo', 'system:auditTask:done', 'system:scan:list',
                'system:auditTask:list', 'system:auditTask:handle', 'system:auditTask:query')
   OR menu_id = @task_menu_id;
SELECT CONCAT('admin 角色已关联 ', ROW_COUNT(), ' 个新菜单') AS info;


-- =====================================================================
-- 五、校验段
-- =====================================================================
SELECT '================================================' AS info;
SELECT 'v8.1 审核模块统一整合升级校验' AS info;
SELECT '================================================' AS info;

SELECT
  (SELECT COUNT(*) FROM sys_audit_task) AS audit_task_total,
  (SELECT COUNT(*) FROM sys_audit_task WHERE status = 'pending') AS audit_pending,
  (SELECT COUNT(*) FROM sys_audit_task WHERE status = 'approved') AS audit_approved,
  (SELECT COUNT(*) FROM sys_audit_task WHERE status = 'rejected') AS audit_rejected;

SELECT
  (SELECT COUNT(*) FROM sys_job_scan_issue) AS scan_issue_total,
  (SELECT COUNT(*) FROM sys_job_scan_issue WHERE status = 'pending') AS scan_pending;

SELECT
  (SELECT menu_id FROM sys_menu WHERE menu_name = '任务管理' AND parent_id = 0) AS task_menu_id,
  (SELECT menu_id FROM sys_menu WHERE perms = 'system:audit:todo') AS todo_menu_id,
  (SELECT menu_id FROM sys_menu WHERE perms = 'system:audit:done') AS done_menu_id,
  (SELECT menu_id FROM sys_menu WHERE perms = 'system:scan:list') AS scan_menu_id,
  (SELECT parent_id FROM sys_menu WHERE menu_id = 110) AS job_new_parent;

SELECT '================================================' AS info;
SELECT '墨韵智库 v8.1 审核模块统一整合升级完成' AS info;
SELECT CONCAT('完成时间: ', NOW()) AS info;
SELECT '================================================' AS info;
