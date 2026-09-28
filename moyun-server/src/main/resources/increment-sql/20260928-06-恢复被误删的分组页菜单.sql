-- =============================================================================
-- 恢复被误删的两个 TabContainer 分组页菜单（5145 / 5146）
-- -----------------------------------------------------------------------------
-- 背景：20260928-01 把 5145「用户反馈处理」与 5146「帮助中心」当作"重复菜单"删除，
--       复核（git show HEAD:...）后确认判断有误 —— 二者不是重复功能，而是**分组容器**：
--
--         5145 用户反馈处理 = 反馈管理(5118) + 举报管理(5114)   ← TabContainer
--         5146 帮助中心     = 帮助分类(5104) + 帮助文章(5109)   ← TabContainer
--
--       其被包裹的子页面本身也各有独立菜单，故删除**不丢功能**，但丢失了
--         · "一处看完"的分组入口；
--         · 项目既有的 TabContainer 归一范式（同类既有：monitor/server-panel、monitor/cache-manage，
--           即"子页面各自也有菜单，但仍提供一个合并入口"）。
--       本脚本恢复这两个入口。
--
-- 幂等性：INSERT ... ON DUPLICATE KEY UPDATE + INSERT IGNORE，可重复执行。
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. 用户反馈处理（反馈 + 举报 分组页）
-- ---------------------------------------------------------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (5145, '用户反馈处理', 5068, 18, 'feedback-center', 'cms/feedback-center/index', 1, 0, 'C', '0', '0', 'cms:feedback-center:list', 'message', 'admin', NOW(), '反馈+举报分组页（TabContainer）；子页面 5118/5114 亦各有菜单')
ON DUPLICATE KEY UPDATE menu_name='用户反馈处理', parent_id=5068, order_num=18, path='feedback-center',
  component='cms/feedback-center/index', menu_type='C', visible='0', status='0',
  perms='cms:feedback-center:list', icon='message',
  remark='反馈+举报分组页（TabContainer）；子页面 5118/5114 亦各有菜单';

-- ---------------------------------------------------------------------------
-- 2. 帮助中心（帮助分类 + 帮助文章 分组页）
-- ---------------------------------------------------------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (5146, '帮助中心', 5068, 19, 'help-center', 'cms/help-center/index', 1, 0, 'C', '0', '0', 'cms:help-center:list', 'question', 'admin', NOW(), '帮助分类+文章分组页（TabContainer）；子页面 5104/5109 亦各有菜单')
ON DUPLICATE KEY UPDATE menu_name='帮助中心', parent_id=5068, order_num=19, path='help-center',
  component='cms/help-center/index', menu_type='C', visible='0', status='0',
  perms='cms:help-center:list', icon='question',
  remark='帮助分类+文章分组页（TabContainer）；子页面 5104/5109 亦各有菜单';

-- ---------------------------------------------------------------------------
-- 3. 角色绑定自愈
-- ---------------------------------------------------------------------------
DELETE rm FROM sys_role_menu rm LEFT JOIN sys_menu m ON m.menu_id = rm.menu_id WHERE m.menu_id IS NULL;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id, create_by, create_time, remark)
SELECT 1, m.menu_id, 'admin', NOW(), 'v13.26 恢复分组页菜单默认授权'
  FROM sys_menu m WHERE m.menu_id IN (5145, 5146);

-- ---------------------------------------------------------------------------
-- 4. 复核 SQL
-- ---------------------------------------------------------------------------
-- SELECT menu_id, menu_name, path, component FROM sys_menu WHERE menu_id IN (5145,5146);
