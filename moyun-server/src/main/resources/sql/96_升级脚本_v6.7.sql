-- ====================================================================
-- v6.7 升级脚本：统一审核字段 + 话题审核流 + 举报扩展 + 敏感词过滤
-- 适配 MySQL 8.x
-- 说明：本脚本幂等，可重复执行（使用 IF NOT EXISTS / information_schema 判断）
-- ====================================================================

-- ----------------------------------------------------------------
-- 1. 统一审核字段：为 portal_article / portal_topic / portal_column 增加审核字段
--    字段：auditor_id（审核人）、audit_remark（审核意见/驳回原因）、audit_time（审核时间）
--    设计：独立于 BaseEntity.remark，专用于审核记录，语义清晰、可追溯
-- ----------------------------------------------------------------

-- 1.1 portal_article
SET @db := DATABASE();
SET @col := 'auditor_id';
SELECT COUNT(*) INTO @exists FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'portal_article' AND COLUMN_NAME = @col;
SET @sql := IF(@exists = 0,
  'ALTER TABLE portal_article ADD COLUMN auditor_id BIGINT NULL COMMENT ''审核人ID（系统用户ID）'' AFTER status',
  'SELECT ''portal_article.auditor_id 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := 'audit_remark';
SELECT COUNT(*) INTO @exists FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'portal_article' AND COLUMN_NAME = @col;
SET @sql := IF(@exists = 0,
  'ALTER TABLE portal_article ADD COLUMN audit_remark VARCHAR(500) NULL COMMENT ''审核意见/驳回原因'' AFTER auditor_id',
  'SELECT ''portal_article.audit_remark 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := 'audit_time';
SELECT COUNT(*) INTO @exists FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'portal_article' AND COLUMN_NAME = @col;
SET @sql := IF(@exists = 0,
  'ALTER TABLE portal_article ADD COLUMN audit_time DATETIME NULL COMMENT ''审核时间'' AFTER audit_remark',
  'SELECT ''portal_article.audit_time 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.2 portal_topic
SET @col := 'auditor_id';
SELECT COUNT(*) INTO @exists FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'portal_topic' AND COLUMN_NAME = @col;
SET @sql := IF(@exists = 0,
  'ALTER TABLE portal_topic ADD COLUMN auditor_id BIGINT NULL COMMENT ''审核人ID（系统用户ID）'' AFTER status',
  'SELECT ''portal_topic.auditor_id 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := 'audit_remark';
SELECT COUNT(*) INTO @exists FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'portal_topic' AND COLUMN_NAME = @col;
SET @sql := IF(@exists = 0,
  'ALTER TABLE portal_topic ADD COLUMN audit_remark VARCHAR(500) NULL COMMENT ''审核意见/驳回原因'' AFTER auditor_id',
  'SELECT ''portal_topic.audit_remark 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := 'audit_time';
SELECT COUNT(*) INTO @exists FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'portal_topic' AND COLUMN_NAME = @col;
SET @sql := IF(@exists = 0,
  'ALTER TABLE portal_topic ADD COLUMN audit_time DATETIME NULL COMMENT ''审核时间'' AFTER audit_remark',
  'SELECT ''portal_topic.audit_time 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.3 portal_column
SET @col := 'auditor_id';
SELECT COUNT(*) INTO @exists FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'portal_column' AND COLUMN_NAME = @col;
SET @sql := IF(@exists = 0,
  'ALTER TABLE portal_column ADD COLUMN auditor_id BIGINT NULL COMMENT ''审核人ID（系统用户ID）'' AFTER status',
  'SELECT ''portal_column.auditor_id 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := 'audit_remark';
SELECT COUNT(*) INTO @exists FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'portal_column' AND COLUMN_NAME = @col;
SET @sql := IF(@exists = 0,
  'ALTER TABLE portal_column ADD COLUMN audit_remark VARCHAR(500) NULL COMMENT ''审核意见/驳回原因'' AFTER auditor_id',
  'SELECT ''portal_column.audit_remark 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := 'audit_time';
SELECT COUNT(*) INTO @exists FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'portal_column' AND COLUMN_NAME = @col;
SET @sql := IF(@exists = 0,
  'ALTER TABLE portal_column ADD COLUMN audit_time DATETIME NULL COMMENT ''审核时间'' AFTER audit_remark',
  'SELECT ''portal_column.audit_time 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.4 话题状态默认值改为 pending（新发起话题需审核）
-- 注意：仅修改 DEFAULT 值，不影响存量数据
ALTER TABLE portal_topic MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending 待审核/active 活跃/archived 归档/deleted 删除/rejected 审核驳回';

-- 1.5 专栏状态注释更新（默认仍为 draft，专栏可由认证创作者直发 published）
ALTER TABLE portal_column MODIFY COLUMN status VARCHAR(16) NOT NULL DEFAULT 'draft' COMMENT '状态：draft 草稿/pending 待审核/published 已发布/archived 归档/rejected 审核驳回';

-- ----------------------------------------------------------------
-- 2. 举报扩展：portal_report.target_type 支持 topic / topic_post / column
--    说明：target_type 为 VARCHAR，无需 DDL 变更，仅更新注释
-- ----------------------------------------------------------------
ALTER TABLE portal_report MODIFY COLUMN target_type VARCHAR(32) NULL DEFAULT NULL COMMENT '举报目标类型：article=文章/comment=评论/user=用户/topic=话题/topic_post=话题观点/topic_comment=话题评论/column=专栏（为空表示通用举报，仅 target_url）';

-- ----------------------------------------------------------------
-- 3. 敏感词过滤基础设施
-- 3.1 敏感词库表：sys_sensitive_word（管理员可维护，运行时加载到内存）
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_sensitive_word (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  word         VARCHAR(128) NOT NULL COMMENT '敏感词',
  category     VARCHAR(32)  NULL DEFAULT NULL COMMENT '分类：politics=政治/porn=色情/ad=广告/insult=辱骂/other=其他',
  status       CHAR(1)      NOT NULL DEFAULT '0' COMMENT '状态：0=启用 1=禁用',
  create_by    VARCHAR(64)  NULL DEFAULT NULL COMMENT '创建者',
  create_time  DATETIME     NULL DEFAULT NULL COMMENT '创建时间',
  update_by    VARCHAR(64)  NULL DEFAULT NULL COMMENT '更新者',
  update_time  DATETIME     NULL DEFAULT NULL COMMENT '更新时间',
  remark       VARCHAR(255) NULL DEFAULT NULL COMMENT '备注',
  del_flag     CHAR(1)      NOT NULL DEFAULT '0' COMMENT '删除标记：0=存在 2=删除（BaseEntity 逻辑删除）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_word (word),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感词库';

-- 3.2 敏感词命中记录表：sys_sensitive_word_log（用于审计与误判复核）
CREATE TABLE IF NOT EXISTS sys_sensitive_word_log (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  biz_type      VARCHAR(32)  NOT NULL COMMENT '业务类型：article/column/topic/topic_post/topic_comment/report',
  biz_id        BIGINT       NULL DEFAULT NULL COMMENT '业务主键ID',
  user_id       BIGINT       NULL DEFAULT NULL COMMENT '提交人ID（portal_user.id）',
  content       TEXT         NULL COMMENT '被检测的原始内容片段（截断）',
  hit_words     VARCHAR(500) NULL DEFAULT NULL COMMENT '命中的敏感词列表（逗号分隔）',
  hit_count     INT          NOT NULL DEFAULT 0 COMMENT '命中数量',
  action        VARCHAR(16)  NOT NULL DEFAULT 'block' COMMENT '处理动作：block=拦截/pending=转待审核/flag=标记',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '检测时间',
  PRIMARY KEY (id),
  KEY idx_biz (biz_type, biz_id),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感词命中记录';

-- 3.3 初始化少量示例敏感词（生产环境请导入完整词库）
INSERT INTO sys_sensitive_word (word, category, status, create_by, create_time, remark) VALUES
  ('示例敏感词1', 'other', '0', 'admin', NOW(), '示例词，生产环境请替换为真实词库'),
  ('示例敏感词2', 'ad',     '0', 'admin', NOW(), '示例词，生产环境请替换为真实词库')
ON DUPLICATE KEY UPDATE word = VALUES(word);

-- ----------------------------------------------------------------
-- 4. 话题审核菜单与权限（CMS 后台）
--    归类方案（对齐 91_菜单权限_CMS.sql 现有结构）：
--    - 话题审核   → 内容管理目录（@cms_parent_id），与文章/专栏管理平级
--    - 专栏审核   → 内容管理目录（@cms_parent_id），与专栏管理平级
--    - 敏感词管理 → 系统管理目录（@system_parent_id），系统配置类
--    修复历史 NPE：原脚本父菜单子查询条件不可靠（'话题管理'菜单不存在 /
--    系统管理目录 perms 为 NULL），返回 NULL 后写入 parent_id=NULL，
--    导致 SysMenuServiceImpl#getChildPerms 自动拆箱 NPE。
--    本节使用 path+parent_id+menu_type 可靠条件 + IFNULL 兜底，
--    并补 UPDATE 语句修复已存在的脏数据。
-- ----------------------------------------------------------------

-- 4.0 解析父菜单 ID（与 91 范式一致，使用 @变量传递）
-- 内容管理目录：CMS 顶级目录，menu_name='内容管理'，parent_id=0，menu_type='M'
SELECT @cms_parent_id := menu_id FROM sys_menu
 WHERE menu_name = '内容管理' AND parent_id = 0 AND menu_type = 'M' LIMIT 1;
-- 系统管理目录：若依标准目录，path='system'，parent_id=0，menu_type='M'
-- 注意：若依顶级目录 perms 字段为 NULL，不能用 perms='system' 查询
SELECT @system_parent_id := menu_id FROM sys_menu
 WHERE path = 'system' AND parent_id = 0 AND menu_type = 'M' LIMIT 1;

-- 兜底：父菜单查不到时回退到顶级（0），绝不写入 NULL，避免 getRouters NPE
SET @cms_parent_id := IFNULL(@cms_parent_id, 0);
SET @system_parent_id := IFNULL(@system_parent_id, 0);

-- 4.1 话题管理菜单（挂在 内容管理 下，与文章/专栏管理平级）
--     历史遗漏：CMS 仅有审核入口，缺话题列表管理菜单，导致审核页 goBack(/cms/topic) 路由 404。
--     此菜单提供 /cms/topic 路由，承载 cms/topic/index.vue，对应后端 CmsTopicController#list。
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '话题管理', @cms_parent_id, 14, 'topic', 'cms/topic/index', 1, 0, 'C', '0', '0', 'cms:topic:list', 'message', 'admin', NOW(), '话题列表与状态管理'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:topic:list');
UPDATE sys_menu SET parent_id = @cms_parent_id, path = 'topic', component = 'cms/topic/index', update_by = 'admin', update_time = NOW()
 WHERE perms = 'cms:topic:list' AND parent_id IS NULL;

-- 话题管理按钮权限（查询/详情）
SELECT @topic_list_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:topic:list' LIMIT 1;
SET @topic_list_menu_id := IFNULL(@topic_list_menu_id, @cms_parent_id);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '话题查询', @topic_list_menu_id, 1, '#', '', 1, 0, 'F', '0', '0', 'cms:topic:query', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:topic:query');
UPDATE sys_menu SET parent_id = @topic_list_menu_id WHERE perms = 'cms:topic:query' AND parent_id IS NULL;

-- 4.2 话题审核菜单（挂在 内容管理 下，与文章/专栏管理平级）
--     path='topic-audit'，避免与其他菜单 path 冲突导致前端动态路由 route name 撞车
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '话题审核', @cms_parent_id, 15, 'topic-audit', 'cms/topic/audit', 1, 0, 'C', '0', '0', 'cms:topic:audit', 'checkbox', 'admin', NOW(), '话题审核（通过/驳回）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:topic:audit');
-- 修复历史脏数据：parent_id IS NULL 的话题审核菜单归类到内容管理目录
UPDATE sys_menu SET parent_id = @cms_parent_id, path = 'topic-audit', component = 'cms/topic/audit', update_by = 'admin', update_time = NOW()
 WHERE perms = 'cms:topic:audit' AND parent_id IS NULL;

-- 4.3 专栏审核菜单（挂在 内容管理 下，与专栏管理平级）
--     path='column-audit'，避免与专栏管理 path='column' 冲突
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '专栏审核', @cms_parent_id, 16, 'column-audit', 'cms/column/audit', 1, 0, 'C', '0', '0', 'cms:column:audit', 'checkbox', 'admin', NOW(), '专栏审核（通过/驳回）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:column:audit');
-- 修复历史脏数据
UPDATE sys_menu SET parent_id = @cms_parent_id, path = 'column-audit', component = 'cms/column/audit', update_by = 'admin', update_time = NOW()
 WHERE perms = 'cms:column:audit' AND parent_id IS NULL;

-- 4.3 敏感词管理菜单（挂在 系统管理 下）
--     path='sensitiveWord'，component='system/sensitiveWord/index'
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '敏感词管理', @system_parent_id, 6, 'sensitiveWord', 'system/sensitiveWord/index', 1, 0, 'C', '0', '0', 'system:sensitiveWord:list', 'dict', 'admin', NOW(), '敏感词库维护与词树刷新'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:sensitiveWord:list');
-- 修复历史脏数据
UPDATE sys_menu SET parent_id = @system_parent_id, update_by = 'admin', update_time = NOW()
 WHERE perms = 'system:sensitiveWord:list' AND parent_id IS NULL;

-- 敏感词管理按钮权限（查询/新增/修改/删除），挂在敏感词管理菜单下
SELECT @sensitive_word_menu_id := menu_id FROM sys_menu WHERE perms = 'system:sensitiveWord:list' LIMIT 1;
SET @sensitive_word_menu_id := IFNULL(@sensitive_word_menu_id, @system_parent_id);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '敏感词查询', @sensitive_word_menu_id, 1, '#', '', 1, 0, 'F', '0', '0', 'system:sensitiveWord:query', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:sensitiveWord:query');
UPDATE sys_menu SET parent_id = @sensitive_word_menu_id WHERE perms = 'system:sensitiveWord:query' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '敏感词新增', @sensitive_word_menu_id, 2, '#', '', 1, 0, 'F', '0', '0', 'system:sensitiveWord:add', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:sensitiveWord:add');
UPDATE sys_menu SET parent_id = @sensitive_word_menu_id WHERE perms = 'system:sensitiveWord:add' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '敏感词修改', @sensitive_word_menu_id, 3, '#', '', 1, 0, 'F', '0', '0', 'system:sensitiveWord:edit', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:sensitiveWord:edit');
UPDATE sys_menu SET parent_id = @sensitive_word_menu_id WHERE perms = 'system:sensitiveWord:edit' AND parent_id IS NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '敏感词删除', @sensitive_word_menu_id, 4, '#', '', 1, 0, 'F', '0', '0', 'system:sensitiveWord:remove', '#', 'admin', NOW(), NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:sensitiveWord:remove');
UPDATE sys_menu SET parent_id = @sensitive_word_menu_id WHERE perms = 'system:sensitiveWord:remove' AND parent_id IS NULL;

-- ----------------------------------------------------------------
-- 5. 定时任务：话题/观点轻量扫描（RuoYi Quartz 范式，需在 sys_job 配置）
--    示例 invoke_target: sensitiveScanTask.scanTopics()
--    示例 cron: 0 0 3 * * ?  （每天凌晨3点）
-- ----------------------------------------------------------------
INSERT INTO sys_job (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
SELECT '敏感词扫描-话题', 'DEFAULT', 'sensitiveScanTask.scanTopics()', '0 0 3 * * ?', '3', '1', '0', 'admin', NOW(), '定时扫描话题内容，命中则转待审核'
WHERE NOT EXISTS (SELECT 1 FROM sys_job WHERE invoke_target = 'sensitiveScanTask.scanTopics()');

INSERT INTO sys_job (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
SELECT '敏感词扫描-观点', 'DEFAULT', 'sensitiveScanTask.scanTopicPosts()', '0 5 3 * * ?', '3', '1', '0', 'admin', NOW(), '定时扫描话题观点，命中则标记'
WHERE NOT EXISTS (SELECT 1 FROM sys_job WHERE invoke_target = 'sensitiveScanTask.scanTopicPosts()');

-- ----------------------------------------------------------------
-- 6. 角色菜单关联（参照 92/93 范式，为 admin 角色补关联）
--    说明：admin 用户在 SysMenuServiceImpl 中走 isAdmin 旁路，本身能看到所有菜单；
--    补 sys_role_menu 关联是为了"角色菜单分配"界面观感一致，以及让非 admin 角色
--    （如运营、内容审核员）能通过角色分配获得这些菜单权限。
-- ----------------------------------------------------------------
INSERT INTO sys_role_menu (role_id, menu_id, create_by, create_time)
SELECT 1, menu_id, 'admin', NOW()
FROM sys_menu
WHERE perms IN (
    'cms:topic:list', 'cms:topic:query',
    'cms:topic:audit',
    'cms:column:audit',
    'system:sensitiveWord:list', 'system:sensitiveWord:query',
    'system:sensitiveWord:add', 'system:sensitiveWord:edit', 'system:sensitiveWord:remove'
  )
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu rm
      WHERE rm.role_id = 1 AND rm.menu_id = sys_menu.menu_id
  );

-- ----------------------------------------------------------------
-- 6.5 永久禁用 RuoYi 自带的 3 个 ryTask 测试任务
--     原因：ryTask.ryNoParams / ryParams / ryMultipleParams 是 RuoYi 框架演示任务，
--     每 10/15/20 秒执行一次，生产环境若误启用会持续刷日志污染。
--     status: 0=正常 1=暂停（即使误启用也不会执行）
-- ----------------------------------------------------------------
UPDATE sys_job SET status = '1', remark = CONCAT(IFNULL(remark, ''), ' [v6.7已永久禁用-测试任务]')
WHERE invoke_target LIKE 'ryTask.%' AND status = '0';

-- ----------------------------------------------------------------
-- 6.6 补充审核相关索引
--     背景：v6.7 新增 auditor_id 字段，CMS 审核员维度查询走全表扫描；
--     话题/专栏待审核列表按 created_time 排序无联合索引会 filesort。
--     使用 information_schema 判断索引是否存在，确保幂等。
-- ----------------------------------------------------------------

-- portal_article: 审核人索引 + 状态+发布时间联合索引（首页最高频查询）
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_article' AND INDEX_NAME = 'idx_auditor_id') = 0,
  'ALTER TABLE portal_article ADD INDEX idx_auditor_id (auditor_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_article' AND INDEX_NAME = 'idx_status_published_at') = 0,
  'ALTER TABLE portal_article ADD INDEX idx_status_published_at (status, published_at)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_topic: 审核人索引 + 状态+创建时间联合索引（待审核列表）
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_topic' AND INDEX_NAME = 'idx_auditor_id') = 0,
  'ALTER TABLE portal_topic ADD INDEX idx_auditor_id (auditor_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_topic' AND INDEX_NAME = 'idx_status_created_time') = 0,
  'ALTER TABLE portal_topic ADD INDEX idx_status_created_time (status, created_time)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- portal_column: 审核人索引 + 状态+创建时间联合索引 + 分类索引
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_column' AND INDEX_NAME = 'idx_auditor_id') = 0,
  'ALTER TABLE portal_column ADD INDEX idx_auditor_id (auditor_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_column' AND INDEX_NAME = 'idx_status_created_time') = 0,
  'ALTER TABLE portal_column ADD INDEX idx_status_created_time (status, created_time)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_column' AND INDEX_NAME = 'idx_category_id') = 0,
  'ALTER TABLE portal_column ADD INDEX idx_category_id (category_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_sensitive_word_log: 用户维度索引
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_sensitive_word_log' AND INDEX_NAME = 'idx_user_id') = 0,
  'ALTER TABLE sys_sensitive_word_log ADD INDEX idx_user_id (user_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ----------------------------------------------------------------
-- 6.7 敏感词词库扩充（示例占位词，生产环境需导入完整词库）
--     说明：以下为分类示例词，仅用于验证 DFA 过滤链路是否畅通。
--     ⚠️ 生产环境投产前必须替换为完整敏感词库（政治/色情/广告/辱骂等），
--     可从第三方词库导入或通过 CMS 后台「敏感词管理」页面批量录入。
-- ----------------------------------------------------------------
INSERT INTO sys_sensitive_word (word, category, status, create_by, create_time)
SELECT '示例-广告', 'ad', '0', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_sensitive_word WHERE word = '示例-广告');

INSERT INTO sys_sensitive_word (word, category, status, create_by, create_time)
SELECT '示例-辱骂', 'insult', '0', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_sensitive_word WHERE word = '示例-辱骂');

INSERT INTO sys_sensitive_word (word, category, status, create_by, create_time)
SELECT '示例-色情', 'porn', '0', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_sensitive_word WHERE word = '示例-色情');

INSERT INTO sys_sensitive_word (word, category, status, create_by, create_time)
SELECT '示例-政治', 'politics', '0', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_sensitive_word WHERE word = '示例-政治');

INSERT INTO sys_sensitive_word (word, category, status, create_by, create_time)
SELECT '示例-其他', 'other', '0', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_sensitive_word WHERE word = '示例-其他');

-- ====================================================================
-- 升级完成
-- ====================================================================
