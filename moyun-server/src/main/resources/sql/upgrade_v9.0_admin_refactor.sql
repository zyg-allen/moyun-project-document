-- ============================================================
-- upgrade_v9.0_admin_refactor.sql
-- 墨韵·智库 Admin 后台管理系统改造迁移脚本
-- 版本: v9.0
-- 说明: PK/圈子模块菜单停用 + tip菜单停用 + 新增VIP/钱包/看板菜单
--       + 菜单重排 + AI模块瘦身 + growth-config 5Tab
-- 特性: 全部幂等（可重复执行）
-- ============================================================

-- ============================================================
-- 第一节：停用已删除模块的菜单（PK/圈子/tip）
-- ============================================================

-- 停用 PK 对战菜单
UPDATE sys_menu SET visible = '1', status = '1', remark = 'v9.0: PK对战模块已删除'
WHERE perms LIKE '%pk%' AND perms LIKE '%learn%'
  AND (status = '0' OR visible = '0');

-- 停用圈子菜单
UPDATE sys_menu SET visible = '1', status = '1', remark = 'v9.0: 圈子模块已删除'
WHERE (perms LIKE '%circle%' OR path LIKE '%circle%')
  AND (status = '0' OR visible = '0');

-- 停用打赏管理菜单
UPDATE sys_menu SET visible = '1', status = '1', remark = 'v9.0: 打赏管理已下线，交易管理已接管'
WHERE perms = 'portal:tip:list' AND (status = '0' OR visible = '0');

-- 清理已停用菜单的角色关联
DELETE FROM sys_role_menu
WHERE menu_id IN (
  SELECT menu_id FROM sys_menu
  WHERE (status = '1' AND visible = '1')
    AND (perms LIKE '%pk%' AND perms LIKE '%learn%'
         OR perms LIKE '%circle%' OR path LIKE '%circle%'
         OR perms = 'portal:tip:list')
);

-- ============================================================
-- 第二节：恢复交易管理菜单可见
-- ============================================================

UPDATE sys_menu SET visible = '0', remark = 'v9.0: 恢复可见，订单与打赏合并查询'
WHERE perms = 'cms:transaction:list' AND visible = '1';

-- ============================================================
-- 第三节：新增商业化目录（一级菜单）
-- ============================================================

-- 查找或创建"商业化"顶级目录
SELECT @commerce_parent_id := menu_id FROM sys_menu WHERE menu_name = '商业化' AND parent_id = 0 LIMIT 1;
SET @commerce_exists := IFNULL(@commerce_parent_id, 0);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '商业化', 0, 6, 'commerce', NULL, NULL, 1, 0, 'M', '0', '0', '', 'money', 'admin', NOW(), 'v9.0: 商业化管理目录'
FROM DUAL WHERE @commerce_exists = 0;

SELECT @commerce_parent_id := IFNULL(@commerce_parent_id, (SELECT menu_id FROM sys_menu WHERE menu_name = '商业化' AND parent_id = 0 LIMIT 1));

-- ============================================================
-- 第四节：新增 VIP 套餐管理菜单 + 权限
-- ============================================================

-- VIP 套餐管理（菜单）
SET @vip_menu_exists := (SELECT COUNT(*) FROM sys_menu WHERE perms = 'cms:vip:list');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'VIP套餐管理', @commerce_parent_id, 1, 'vip', 'cms/vip/index', NULL, 1, 0, 'C', '0', '0', 'cms:vip:list', 'crown', 'admin', NOW(), 'v9.0: VIP套餐管理'
FROM DUAL WHERE @vip_menu_exists = 0;

SELECT @vip_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:vip:list' LIMIT 1;

-- VIP 按钮权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'VIP查询', @vip_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:vip:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:vip:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'VIP新增', @vip_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:vip:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:vip:add');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'VIP修改', @vip_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:vip:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:vip:edit');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'VIP删除', @vip_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:vip:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:vip:remove');

-- ============================================================
-- 第五节：新增钱包管理菜单 + 权限
-- ============================================================

SET @wallet_menu_exists := (SELECT COUNT(*) FROM sys_menu WHERE perms = 'cms:wallet:list');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '钱包管理', @commerce_parent_id, 2, 'wallet', 'cms/wallet/index', NULL, 1, 0, 'C', '0', '0', 'cms:wallet:list', 'wallet', 'admin', NOW(), 'v9.0: 钱包余额与交易流水'
FROM DUAL WHERE @wallet_menu_exists = 0;

SELECT @wallet_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:wallet:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '钱包查询', @wallet_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:wallet:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:wallet:query');

-- ============================================================
-- 第六节：新增业务看板菜单
-- ============================================================

-- 查找"内容管理"目录
SELECT @cms_parent_id := menu_id FROM sys_menu WHERE menu_name = '内容管理' AND parent_id = 0 LIMIT 1;

SET @dashboard_menu_exists := (SELECT COUNT(*) FROM sys_menu WHERE perms = 'cms:dashboard:list');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '业务看板', @cms_parent_id, 0, 'biz-dashboard', 'cms/dashboard/index', NULL, 1, 0, 'C', '0', '0', 'cms:dashboard:list', 'dashboard', 'admin', NOW(), 'v9.0: 核心业务指标看板'
FROM DUAL WHERE @dashboard_menu_exists = 0;

-- ============================================================
-- 第七节：迁移现有商业化菜单到"商业化"目录
-- ============================================================

-- 迁移：订单管理、广告位、友情链接、认证审核、交易管理
-- 仅迁移 parent_id 指向"内容管理"的商业化相关菜单
UPDATE sys_menu SET parent_id = @commerce_parent_id, remark = CONCAT(IFNULL(remark, ''), ' v9.0: 迁移至商业化目录')
WHERE perms IN ('portal:order:list', 'cms:ad:list', 'portal:friendlink:list', 'portal:certification:list', 'cms:transaction:list')
  AND parent_id = @cms_parent_id;

-- ============================================================
-- 第八节：菜单重排（调整 order_num 反映新优先级）
-- ============================================================

-- 内容管理目录：业务看板(0) > 文章(1) > 话题(2) > 标签(3) > 分类(4) > 专栏(5) > 评论(6) > 征文(7) > 反馈(8) > 帮助(9)
-- 面试管理、读书空间、成长体系保持原有 order_num
-- 商业化目录 order_num = 6
-- AI 目录 order_num = 7（降低优先级）

UPDATE sys_menu SET order_num = 7 WHERE menu_name = '智能AI' AND parent_id = 0;

-- ============================================================
-- 第九节：AI 模块瘦身 — 隐藏辅助工具
-- ============================================================

-- 隐藏架构图生成
UPDATE sys_menu SET visible = '1', remark = 'v9.0: AI瘦身，暂时隐藏'
WHERE perms = 'cms:ai:diagram:list' AND visible = '0';

-- 隐藏领域词典
UPDATE sys_menu SET visible = '1', remark = 'v9.0: AI瘦身，暂时隐藏'
WHERE perms = 'cms:ai:dictionary:list' AND visible = '0';

-- 隐藏数据分析
UPDATE sys_menu SET visible = '1', remark = 'v9.0: AI瘦身，暂时隐藏'
WHERE perms = 'cms:ai:query:list' AND visible = '0';

-- AI 大屏改名
UPDATE sys_menu SET menu_name = 'AI运营概览', remark = 'v9.0: 从Lynx AI更名为墨韵AI运营概览'
WHERE perms = 'cms:ai:dashboard:list' AND menu_name != 'AI运营概览';

-- 确认 knowledge-base 和 knowledge-library 隐藏（knowledge-center 已是 Tab 容器）
UPDATE sys_menu SET visible = '1', remark = 'v9.0: 已合并到知识中心Tab'
WHERE perms = 'cms:ai:knowledge-base:list' AND visible = '0';

UPDATE sys_menu SET visible = '1', remark = 'v9.0: 已合并到知识中心Tab'
WHERE perms = 'cms:ai:knowledge-library:list' AND visible = '0';

-- ============================================================
-- 第十节：growth-config 菜单备注更新
-- ============================================================

UPDATE sys_menu SET remark = 'v9.0: 成长体系5Tab统一管理（规则/成就/徽章/日志/用户成长）'
WHERE perms = 'cms:growth-config:list';

-- 确保旧的独立 growth/badge、growth/log、growth/user 菜单（如果有）保持隐藏
UPDATE sys_menu SET visible = '1', remark = 'v9.0: 已合并到成长配置Tab'
WHERE perms IN ('cms:achievement:badge:list', 'cms:growth:log:list', 'cms:growth:user:list')
  AND visible = '0';

-- ============================================================
-- 第十一节：将新增菜单分配给管理员角色
-- ============================================================

-- 查找管理员角色
SELECT @admin_role_id := role_id FROM sys_role WHERE role_key = 'admin' LIMIT 1;

-- VIP 相关菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @admin_role_id, menu_id FROM sys_menu
WHERE perms LIKE 'cms:vip:%'
  AND menu_id NOT IN (SELECT menu_id FROM sys_role_menu WHERE role_id = @admin_role_id);

-- 钱包相关菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @admin_role_id, menu_id FROM sys_menu
WHERE perms LIKE 'cms:wallet:%'
  AND menu_id NOT IN (SELECT menu_id FROM sys_role_menu WHERE role_id = @admin_role_id);

-- 业务看板菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @admin_role_id, menu_id FROM sys_menu
WHERE perms = 'cms:dashboard:list'
  AND menu_id NOT IN (SELECT menu_id FROM sys_role_menu WHERE role_id = @admin_role_id);

-- 商业化目录
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @admin_role_id, @commerce_parent_id
FROM DUAL WHERE @commerce_parent_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = @admin_role_id AND menu_id = @commerce_parent_id);

-- ============================================================
-- 第十二节：彻底删除 PK + 圈子数据库表
-- 注意：portal_tip_order 表保留！付费阅读逻辑依赖它（target_type=article_paid）
-- ============================================================

-- 删除 PK 对战表
DROP TABLE IF EXISTS `portal_pk_challenge`;

-- 删除圈子相关表（如果存在）
DROP TABLE IF EXISTS `portal_circle_post`;
DROP TABLE IF EXISTS `portal_circle_member`;
DROP TABLE IF EXISTS `portal_circle`;

-- 清理 init_v7.8.sql 中 portal_pk_challenge 的注释引用（仅注释，不影响功能）
-- 原: 用途：门户互动社交表 DDL（... portal_pk_challenge ...）
-- 已通过 DROP TABLE 彻底移除

-- ============================================================
-- 第十三节：彻底删除 tip 菜单和权限（从 sys_menu 物理删除）
-- 注意：portal_tip_order 表保留！付费阅读逻辑依赖它
-- ============================================================

-- 物理删除 tip 菜单及其子菜单
DELETE FROM sys_role_menu WHERE menu_id IN (
  SELECT menu_id FROM sys_menu WHERE perms LIKE 'portal:tip:%'
);
DELETE FROM sys_menu WHERE perms LIKE 'portal:tip:%';

-- 物理删除 PK 相关菜单（如果存在）
DELETE FROM sys_role_menu WHERE menu_id IN (
  SELECT menu_id FROM sys_menu WHERE perms LIKE '%learn%pk%' OR perms LIKE '%pk%challenge%'
);
DELETE FROM sys_menu WHERE perms LIKE '%learn%pk%' OR perms LIKE '%pk%challenge%';

-- 物理删除圈子相关菜单（如果存在）
DELETE FROM sys_role_menu WHERE menu_id IN (
  SELECT menu_id FROM sys_menu WHERE perms LIKE '%circle%' OR path LIKE '%circle%'
);
DELETE FROM sys_menu WHERE perms LIKE '%circle%' OR path LIKE '%circle%';

-- ============================================================
-- 第十四节：验证 SQL（执行后检查）
-- ============================================================

-- 验证1：已删除的表（应全部不存在）
SELECT 'PK表' AS check_item, 
  IF(COUNT(*) = 0, 'OK - 已删除', 'ERROR - 仍存在') AS result
FROM information_schema.tables 
WHERE table_schema = DATABASE() AND table_name = 'portal_pk_challenge';

SELECT '圈子表' AS check_item,
  IF(COUNT(*) = 0, 'OK - 已删除', 'ERROR - 仍存在') AS result
FROM information_schema.tables 
WHERE table_schema = DATABASE() AND table_name IN ('portal_circle', 'portal_circle_member', 'portal_circle_post');

-- 验证2：tip 菜单已物理删除
SELECT 'tip菜单' AS check_item,
  IF(COUNT(*) = 0, 'OK - 已删除', 'ERROR - 仍存在') AS result
FROM sys_menu WHERE perms LIKE 'portal:tip:%';

-- 验证3：新增菜单
SELECT '新增菜单' AS check_item, GROUP_CONCAT(menu_name) AS menu_names
FROM sys_menu WHERE remark LIKE '%v9.0%' AND status = '0' AND visible = '0';

-- 验证4：商业化目录子菜单
SELECT menu_name, perms, visible, status
FROM sys_menu WHERE parent_id = @commerce_parent_id ORDER BY order_num;

-- 验证5：portal_tip_order 表仍存在（付费阅读依赖）
SELECT '打赏订单表' AS check_item,
  IF(COUNT(*) = 1, 'OK - 保留', 'ERROR - 丢失') AS result
FROM information_schema.tables 
WHERE table_schema = DATABASE() AND table_name = 'portal_tip_order';
