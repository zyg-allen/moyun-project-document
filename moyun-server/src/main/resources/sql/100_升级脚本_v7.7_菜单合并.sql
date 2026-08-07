-- ====================================================================
-- v7.7 升级脚本：菜单合并（第一批 - Tab 容器页）
-- 适配 MySQL 8.x
-- 说明：本脚本幂等，可重复执行（使用 information_schema / sys_menu 存在性判断）
-- 背景：
--   将同构、同业务域的二级菜单合并为 Tab 容器页，减少菜单数量。
--   合并后菜单 component 指向新的容器页（如 cms/promotion/index），
--   旧菜单（如 cms/ad/index、cms/friend-link/index）设为隐藏（visible=1），
--   保留旧菜单与权限项以兼容历史角色分配，支持回滚。
--   前端容器页通过懒加载方式复用原页面组件，各 Tab 保留独立权限控制。
--
--   合并清单（8 项）：
--     1. 推广位管理  ← 广告位 + 友情链接
--     2. 用户反馈处理 ← 反馈管理 + 举报管理
--     3. 缓存管理    ← 缓存监控 + 缓存列表
--     4. 日志审计    ← 操作日志 + 登录日志
--     5. 学习辅助    ← 学习计划 + 错题本
--     6. 成长配置    ← 成长规则 + 成就管理
--     7. 交易管理    ← 付费订单 + 打赏管理
--     8. 帮助中心    ← 帮助分类 + 帮助文章
-- ====================================================================

SET @db := DATABASE();

-- ====================================================================
-- 工具函数：幂等插入菜单（基于 perms 去重）
-- 调用：SELECT register_menu(parentId, orderNum, path, component, name, perms, icon, remark);
-- ====================================================================

-- ====================================================================
-- 1. 推广位管理（广告位 + 友情链接）
-- ====================================================================
SELECT @cms_parent_id := menu_id FROM sys_menu WHERE menu_name = '内容管理' AND parent_id = 0 LIMIT 1;
SET @cms_parent_id := IFNULL(@cms_parent_id, 0);

-- 1.1 注册合并菜单（幂等）
SET @sql := IF(
  (SELECT COUNT(*) FROM sys_menu WHERE perms = 'cms:promotion:list') = 0,
  CONCAT('INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES (''推广位管理'', ', @cms_parent_id, ', 20, ''promotion'', ''cms/promotion/index'', NULL, 1, 0, ''C'', ''0'', ''0'', ''cms:promotion:list'', ''component'', ''admin'', NOW(), ''广告位与友情链接合并管理（Tab）'')'),
  'SELECT ''cms:promotion:list 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.2 隐藏旧菜单（广告位、友情链接）- visible=1 隐藏，status=0 正常，保留权限
UPDATE sys_menu SET visible = '1', update_by = 'admin', update_time = NOW()
  WHERE perms IN ('portal:ad:list', 'cms:friend-link:list') AND visible = '0';

-- ====================================================================
-- 2. 用户反馈处理（反馈管理 + 举报管理）
-- ====================================================================
SET @sql := IF(
  (SELECT COUNT(*) FROM sys_menu WHERE perms = 'cms:feedback-center:list') = 0,
  CONCAT('INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES (''用户反馈处理'', ', @cms_parent_id, ', 21, ''feedback-center'', ''cms/feedback-center/index'', NULL, 1, 0, ''C'', ''0'', ''0'', ''cms:feedback-center:list'', ''message'', ''admin'', NOW(), ''反馈与举报合并处理（Tab）'')'),
  'SELECT ''cms:feedback-center:list 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.2 隐藏旧菜单
UPDATE sys_menu SET visible = '1', update_by = 'admin', update_time = NOW()
  WHERE perms IN ('cms:feedback:list', 'cms:report:list') AND visible = '0';

-- ====================================================================
-- 3. 帮助中心（帮助分类 + 帮助文章）
-- ====================================================================
SET @sql := IF(
  (SELECT COUNT(*) FROM sys_menu WHERE perms = 'cms:help-center:list') = 0,
  CONCAT('INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES (''帮助中心'', ', @cms_parent_id, ', 22, ''help-center'', ''cms/help-center/index'', NULL, 1, 0, ''C'', ''0'', ''0'', ''cms:help-center:list'', ''question'', ''admin'', NOW(), ''帮助分类与文章合并管理（Tab）'')'),
  'SELECT ''cms:help-center:list 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3.2 隐藏旧菜单
UPDATE sys_menu SET visible = '1', update_by = 'admin', update_time = NOW()
  WHERE perms IN ('cms:help-category:list', 'cms:help-article:list') AND visible = '0';

-- ====================================================================
-- 4. 成长配置（成长规则 + 成就管理）
-- 注：成长体系菜单若未注册（如运行时手动添加），此合并菜单仍可独立注册；
--     旧菜单隐藏采用 perms 模糊匹配，无匹配则跳过（幂等）。
-- ====================================================================
SET @sql := IF(
  (SELECT COUNT(*) FROM sys_menu WHERE perms = 'cms:growth-config:list') = 0,
  CONCAT('INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES (''成长配置'', ', @cms_parent_id, ', 23, ''growth-config'', ''cms/growth-config/index'', NULL, 1, 0, ''C'', ''0'', ''0'', ''cms:growth-config:list'', ''star'', ''admin'', NOW(), ''成长规则与成就合并配置（Tab）'')'),
  'SELECT ''cms:growth-config:list 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4.2 隐藏旧菜单（成长规则、成就管理 - perms 可能是 cms:growth:rule:list / cms:growth:achievement:list）
UPDATE sys_menu SET visible = '1', update_by = 'admin', update_time = NOW()
  WHERE perms IN ('cms:growth:rule:list', 'cms:growth:achievement:list',
                  'cms:growth-rule:list', 'cms:growth-achievement:list')
  AND visible = '0';

-- ====================================================================
-- 5. 交易管理（付费订单 + 打赏管理）
-- 注：原菜单已 visible=1 隐藏（已下线），此处注册合并菜单并保持隐藏状态，
--     便于未来如恢复交易功能时统一入口。
-- ====================================================================
SET @sql := IF(
  (SELECT COUNT(*) FROM sys_menu WHERE perms = 'cms:transaction:list') = 0,
  CONCAT('INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES (''交易管理'', ', @cms_parent_id, ', 24, ''transaction'', ''cms/transaction/index'', NULL, 1, 0, ''C'', ''1'', ''0'', ''cms:transaction:list'', ''money'', ''admin'', NOW(), ''订单与打赏合并查询（Tab，当前隐藏）'')'),
  'SELECT ''cms:transaction:list 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5.2 旧菜单（portal:tip:list / portal:order:list）已是 visible=1，无需重复更新

-- ====================================================================
-- 6. 缓存管理（缓存监控 + 缓存列表）- 系统监控目录
-- ====================================================================
SELECT @monitor_parent_id := menu_id FROM sys_menu WHERE menu_name = '系统监控' AND parent_id = 0 LIMIT 1;
SET @monitor_parent_id := IFNULL(@monitor_parent_id, 0);

-- 6.1 注册合并菜单（幂等）
SET @sql := IF(
  (SELECT COUNT(*) FROM sys_menu WHERE perms = 'monitor:cache-manage:list') = 0,
  CONCAT('INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES (''缓存管理'', ', @monitor_parent_id, ', 7, ''cache-manage'', ''monitor/cache-manage/index'', NULL, 1, 0, ''C'', ''0'', ''0'', ''monitor:cache-manage:list'', ''redis'', ''admin'', NOW(), ''缓存监控与列表合并管理（Tab）'')'),
  'SELECT ''monitor:cache-manage:list 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 6.2 隐藏旧菜单（缓存监控 113、缓存列表 114）
UPDATE sys_menu SET visible = '1', update_by = 'admin', update_time = NOW()
  WHERE menu_id IN (113, 114) AND visible = '0';

-- ====================================================================
-- 7. 日志审计（操作日志 + 登录日志）- 系统管理 > 日志管理目录
-- ====================================================================
SELECT @log_parent_id := menu_id FROM sys_menu WHERE menu_name = '日志管理' AND parent_id != 0 LIMIT 1;
SET @log_parent_id := IFNULL(@log_parent_id, 108);

-- 7.1 注册合并菜单（幂等）
SET @sql := IF(
  (SELECT COUNT(*) FROM sys_menu WHERE perms = 'monitor:log-audit:list') = 0,
  CONCAT('INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES (''日志审计'', ', @log_parent_id, ', 3, ''log-audit'', ''monitor/log-audit/index'', NULL, 1, 0, ''C'', ''0'', ''0'', ''monitor:log-audit:list'', ''log'', ''admin'', NOW(), ''操作日志与登录日志合并查询（Tab）'')'),
  'SELECT ''monitor:log-audit:list 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 7.2 隐藏旧菜单（操作日志 500、登录日志 501）
UPDATE sys_menu SET visible = '1', update_by = 'admin', update_time = NOW()
  WHERE menu_id IN (500, 501) AND visible = '0';

-- ====================================================================
-- 8. 学习辅助（学习计划 + 错题本）- Portal 读书目录
-- 注：portal 读书模块菜单可能未注册，此合并菜单独立注册；
--     若旧菜单存在则隐藏，无匹配则跳过（幂等）。
-- ====================================================================
SELECT @portal_parent_id := menu_id FROM sys_menu WHERE menu_name IN ('读书门户', '门户读书', '读书管理', '读书') AND parent_id = 0 LIMIT 1;
SET @portal_parent_id := IFNULL(@portal_parent_id, 0);

-- 8.1 注册合并菜单（幂等）
--     若 portal 顶级目录不存在，则挂到内容管理目录下作为兜底
SET @final_portal_parent := IF(@portal_parent_id = 0, @cms_parent_id, @portal_parent_id);
SET @sql := IF(
  (SELECT COUNT(*) FROM sys_menu WHERE perms = 'portal:learn-aux:list') = 0,
  CONCAT('INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES (''学习辅助'', ', @final_portal_parent, ', 30, ''learn-aux'', ''portal/learn-aux/index'', NULL, 1, 0, ''C'', ''0'', ''0'', ''portal:learn-aux:list'', ''education'', ''admin'', NOW(), ''学习计划与错题本合并查看（Tab）'')'),
  'SELECT ''portal:learn-aux:list 已存在'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 8.2 隐藏旧菜单（学习计划、错题本 - perms 可能未注册，幂等跳过）
UPDATE sys_menu SET visible = '1', update_by = 'admin', update_time = NOW()
  WHERE perms IN ('portal:studyPlan:list', 'portal:wrongQuestion:list')
  AND visible = '0';

-- ====================================================================
-- 9. 角色权限分配提示
-- ====================================================================
-- 新合并菜单的权限项（cms:promotion:list / cms:feedback-center:list / ...）
-- 需要在角色管理中分配给对应角色。admin 超级管理员通过 role_key='admin'
-- 通配权限自动放行，无需额外分配。
-- 旧菜单虽隐藏，但其按钮权限（如 cms:friend-link:add）仍保留，
-- 容器页内各 Tab 通过 v-hasPermi 复用原权限控制。

-- ====================================================================
-- 升级完成 - 合并后菜单结构
-- ====================================================================
-- 内容管理 (cms)
--   ├─ ... 原有保留菜单 ...
--   ├─ 推广位管理 (cms:promotion:list)         ← 广告位 + 友情链接（隐藏旧）
--   ├─ 用户反馈处理 (cms:feedback-center:list) ← 反馈 + 举报（隐藏旧）
--   ├─ 帮助中心 (cms:help-center:list)         ← 帮助分类 + 文章（隐藏旧）
--   ├─ 成长配置 (cms:growth-config:list)      ← 成长规则 + 成就（隐藏旧）
--   └─ 交易管理 (cms:transaction:list,隐藏)    ← 订单 + 打赏（原已隐藏）
--
-- 系统监控 (monitor)
--   └─ 缓存管理 (monitor:cache-manage:list)    ← 缓存监控 + 列表（隐藏旧）
--
-- 系统管理 > 日志管理
--   └─ 日志审计 (monitor:log-audit:list)       ← 操作 + 登录日志（隐藏旧）
--
-- 读书门户/内容管理
--   └─ 学习辅助 (portal:learn-aux:list)        ← 学习计划 + 错题本（隐藏旧）
-- ====================================================================
