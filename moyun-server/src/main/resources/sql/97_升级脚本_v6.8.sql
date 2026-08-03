-- ====================================================================
-- v6.8 升级脚本：评论审核对齐文章审核模式
-- 适配 MySQL 8.x
-- 说明：本脚本幂等，可重复执行（使用 information_schema 判断列/索引是否存在）
-- 背景：
--   1. portal_comment 表 status 字段语义不一致（DDL 注释为"0正常 1停用"，
--      代码使用为"0待审核 1已发布"），CMS 评论审核接口仅更新 status，
--      缺审核人/审核时间/审核意见，无法追溯。
--   2. 本脚本：
--      a) 为 portal_comment 增加 auditor_id / audit_time / audit_remark 字段，
--         对齐 portal_article / portal_topic / portal_column 审核字段设计；
--      b) 修正 status 字段注释，明确"0=待审核 1=已发布 2=审核驳回"语义；
--      c) 增加审核人索引。
--   3. 业务层（CmsCommentServiceImpl.auditComment）将同时升级为：
--      状态白名单校验、乐观锁、审核人/时间/意见写入、事务化，
--      与 CmsArticleServiceImpl.auditArticle 保持一致。
-- ====================================================================

SET @db := DATABASE();

-- 1.1 portal_comment.auditor_id
SET @col := 'auditor_id';
SELECT COUNT(*) INTO @exists FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'portal_comment' AND COLUMN_NAME = @col;
SET @sql := IF(@exists = 0,
  'ALTER TABLE portal_comment ADD COLUMN auditor_id BIGINT NULL COMMENT ''审核人ID（系统用户ID，CMS审核时写入）'' AFTER status',
  'SELECT ''portal_comment.auditor_id 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.2 portal_comment.audit_remark
SET @col := 'audit_remark';
SELECT COUNT(*) INTO @exists FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'portal_comment' AND COLUMN_NAME = @col;
SET @sql := IF(@exists = 0,
  'ALTER TABLE portal_comment ADD COLUMN audit_remark VARCHAR(500) NULL COMMENT ''审核意见/驳回原因（独立字段）'' AFTER auditor_id',
  'SELECT ''portal_comment.audit_remark 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.3 portal_comment.audit_time
SET @col := 'audit_time';
SELECT COUNT(*) INTO @exists FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'portal_comment' AND COLUMN_NAME = @col;
SET @sql := IF(@exists = 0,
  'ALTER TABLE portal_comment ADD COLUMN audit_time DATETIME NULL COMMENT ''审核时间'' AFTER audit_remark',
  'SELECT ''portal_comment.audit_time 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.4 修正 status 字段注释（语义对齐文章审核模式，仅修改注释，不影响存量数据）
--     0=待审核（敏感词扫描后转人工审核场景），1=已发布（默认值，未命中敏感词直接发布），
--     2=审核驳回（CMS 后台驳回）
ALTER TABLE portal_comment MODIFY COLUMN status CHAR(1) DEFAULT '1' COMMENT '状态：0=待审核 1=已发布 2=审核驳回';

-- 1.5 审核人索引（CMS 审核员维度查询）
SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'portal_comment' AND INDEX_NAME = 'idx_auditor_id') = 0,
  'ALTER TABLE portal_comment ADD INDEX idx_auditor_id (auditor_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ====================================================================
-- 2. 审核权限独立化（cms:article:audit / cms:comment:audit）
-- 背景：
--   历史上 CmsArticleController#audit 与 CmsCommentController#audit 复用 cms:*:edit
--   权限标识，导致审核员必须被授予 edit 权限才能审核，无法实现"审核员/编辑员"
--   角色分离。本批次已将 Controller 端权限标识切换为独立的 cms:article:audit /
--   cms:comment:audit（与 91_菜单权限_CMS.sql 已定义的权限项一致）。
--   此处补幂等保障：若权限项缺失则补建（防止历史库未运行 91 脚本导致 hasPermi 始终 false）。
-- ====================================================================

-- 2.1 文章审核权限项（幂等）
SET @article_menu_id := (SELECT menu_id FROM sys_menu WHERE perms = 'cms:article:list' LIMIT 1);
SET @sql := IF(
  @article_menu_id IS NOT NULL
  AND (SELECT COUNT(*) FROM sys_menu WHERE perms = 'cms:article:audit') = 0,
  CONCAT('INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES (''文章审核'', ', @article_menu_id, ', 5, '''', NULL, NULL, 1, 0, ''F'', ''0'', ''0'', ''cms:article:audit'', ''#'', ''admin'', NOW(), '''')'),
  'SELECT ''cms:article:audit 已存在或父菜单缺失'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.2 评论审核权限项（幂等）
SET @comment_menu_id := (SELECT menu_id FROM sys_menu WHERE perms = 'cms:comment:list' LIMIT 1);
SET @sql := IF(
  @comment_menu_id IS NOT NULL
  AND (SELECT COUNT(*) FROM sys_menu WHERE perms = 'cms:comment:audit') = 0,
  CONCAT('INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES (''评论审核'', ', @comment_menu_id, ', 2, '''', NULL, NULL, 1, 0, ''F'', ''0'', ''0'', ''cms:comment:audit'', ''#'', ''admin'', NOW(), '''')'),
  'SELECT ''cms:comment:audit 已存在或父菜单缺失'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 提示：admin 超级管理员角色通过 role_key='admin' 通配权限自动放行，无需额外分配；
-- 普通审核员角色请在系统管理 → 角色管理 中勾选 cms:article:audit / cms:comment:audit 权限项。

-- ====================================================================
-- 升级完成
-- ====================================================================
