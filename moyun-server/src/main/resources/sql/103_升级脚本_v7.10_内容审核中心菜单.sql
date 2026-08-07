-- ====================================================================
-- v7.10 升级脚本：内容审核中心菜单注册（文章+专栏+话题审核 Tab 合并）
-- 适配 MySQL 8.x
-- 说明：本脚本幂等，可重复执行（基于 perms 去重）
-- 背景：
--   将分散在 文章/专栏/话题 各自 index.vue 的「审核」入口整合为统一「内容审核中心」菜单，
--   通过 Tab 容器（cms/audit-center/index）聚合三个审核页，嵌入模式(:embedded)隐藏各自返回头。
--   审核闭环不变：提交→待审→通过/驳回→通知→操作日志保留（各 audit.vue 逻辑保持原样）。
--   原 /cms/article/audit 等静态路由与各 index.vue 的审核按钮保留，作为单条快捷入口兼容历史。
-- ====================================================================

SET @db := DATABASE();

SELECT @cms_parent_id := menu_id FROM sys_menu WHERE menu_name = '内容管理' AND parent_id = 0 LIMIT 1;
SET @cms_parent_id := IFNULL(@cms_parent_id, 0);

-- 注册「内容审核中心」合并菜单（幂等）
SET @sql := IF(
  (SELECT COUNT(*) FROM sys_menu WHERE perms = 'cms:audit-center:list') = 0,
  CONCAT('INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES (''内容审核中心'', ', @cms_parent_id, ', 25, ''audit-center'', ''cms/audit-center/index'', NULL, 1, 0, ''C'', ''0'', ''0'', ''cms:audit-center:list'', ''check'', ''admin'', NOW(), ''文章/专栏/话题审核合并入口（Tab，嵌入模式）'')'),
  'SELECT ''cms:audit-center:list 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 角色权限分配提示：
--   admin 超级管理员通过 role_key='admin' 通配权限自动放行，无需额外分配。
--   内容审核员等角色需在「角色管理」中分配 cms:audit-center:list 权限。
--   各审核 Tab 内的操作权限（cms:article:audit / cms:column:audit / cms:topic:audit）仍沿用各模块原有权限项。

-- ====================================================================
-- 升级完成
-- ====================================================================
