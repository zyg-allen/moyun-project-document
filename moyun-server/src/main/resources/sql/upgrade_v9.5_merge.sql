-- ============================================================
-- upgrade_v9.5_merge.sql
-- 墨韵·智库 v9.5 合并版迁移脚本（main-dev-article × moyun-dev-kouzi）
-- 说明: 1) 恢复打赏流水后台查询入口（作为"交易管理"页的 Tab，不建独立菜单）
--       2) 清理"学习辅助"重复菜单并迁移至面试指南目录（修复 /cms/learn-aux 404）
--       3) 去重交易入口：删除"财务/付费订单"独立菜单，权限转为"交易管理"下 F 按钮
-- 特性: 全部幂等（可重复执行）
-- 执行顺序: 在 init_v7.8.sql 与 upgrade_v9.0_admin_refactor.sql 之后执行
-- ============================================================

-- ============================================================
-- 第一节：恢复打赏权限按钮（F 类型，挂在"交易管理"菜单下）
-- 说明: v9.0 曾物理删除 portal:tip:% 菜单；v9.5 恢复打赏流水查询（Tab 形式），
--       因此重建权限按钮。超管（*:*:*）天然放行，本节只为可授权的非超管角色服务。
--       挂载点为 cms:transaction:list（交易管理 Tab 容器）——它是 Tab 内
--       付费订单/打赏流水两个组件接口权限的唯一载体。
-- ============================================================

-- 定位"交易管理"菜单（Tab 容器，v9.0 已迁移至商业化目录）
SELECT @txn_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:transaction:list' AND menu_type = 'C' LIMIT 1;

-- 兜底：若交易管理菜单缺失，改挂"商业化"目录（与 v9.0 upgrade 的定位方式一致）
SELECT @commerce_parent_id := menu_id FROM sys_menu WHERE menu_name = '商业化' AND parent_id = 0 LIMIT 1;
SET @txn_menu_id := IFNULL(@txn_menu_id, IFNULL(@commerce_parent_id, 0));

-- 1.1 打赏流水查询（list）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '打赏流水查询', @txn_menu_id, 20, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:tip:list', '#', 'admin', NOW(), 'v9.5: 恢复打赏流水Tab查询权限'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:tip:list');

-- 1.2 打赏订单详情（query）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '打赏详情查询', @txn_menu_id, 21, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:tip:query', '#', 'admin', NOW(), 'v9.5: 恢复打赏详情权限'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:tip:query');

-- 修正历史数据：旧库残留（v9.0 置 visible=1,status=1）或曾挂在"付费订单"菜单下的，
-- 恢复启用并统一归位到"交易管理"菜单下
UPDATE sys_menu
SET visible = '0', status = '0', parent_id = @txn_menu_id, menu_type = 'F', path = '', component = NULL,
    update_by = 'admin', update_time = NOW(), remark = 'v9.5: 恢复打赏流水Tab查询权限'
WHERE perms IN ('portal:tip:list', 'portal:tip:query')
  AND (visible = '1' OR status = '1' OR menu_type <> 'F' OR parent_id <> @txn_menu_id);

-- ============================================================
-- 第二节：给 admin 角色授权（幂等）
-- ============================================================

-- RuoYi 标准：role_id=1 为超级管理员，拥有 *:*:* 通配，无需授权；
-- 此处给"运营"类常见自定义角色兜底授权（若存在），保证菜单分配时可见
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m ON m.perms IN ('portal:tip:list', 'portal:tip:query')
WHERE r.role_key = 'admin' AND r.role_id <> 1
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm
    WHERE rm.role_id = r.role_id AND rm.menu_id = m.menu_id
  );

-- ============================================================
-- 第三节：确保"交易管理"菜单可见（v9.0 已恢复，此处幂等兜底）
-- ============================================================

UPDATE sys_menu
SET visible = '0', status = '0', update_by = 'admin', update_time = NOW(),
    remark = 'v9.5: 交易管理（付费订单+打赏流水双Tab）'
WHERE perms = 'cms:transaction:list'
  AND (visible = '1' OR status = '1');

-- ============================================================
-- 第四节：验证 SQL（执行后人工检查）
-- ============================================================

-- 验证1：打赏权限按钮已恢复（应 ≥ 2 行）
SELECT 'tip权限按钮' AS check_item,
  IF(COUNT(*) >= 2, 'OK - 已恢复', 'ERROR - 缺失') AS result
FROM sys_menu WHERE perms LIKE 'portal:tip:%' AND status = '0';

-- 验证2：交易管理菜单可见（应 1 行 OK）
SELECT '交易管理菜单' AS check_item,
  IF(COUNT(*) = 1, 'OK - 可见', 'ERROR - 不可见') AS result
FROM sys_menu WHERE perms = 'cms:transaction:list' AND visible = '0' AND status = '0';

-- 验证3：打赏Tab依赖的后端接口权限链（列出最终状态）
SELECT menu_id, menu_name, perms, parent_id, visible, status
FROM sys_menu WHERE perms LIKE 'portal:tip:%';

-- ============================================================
-- 第五节：清理"学习辅助"重复菜单 + 迁移至面试指南（修复 /cms/learn-aux 404 与归属错位）
-- 背景1: init_v7.8 第8节兜底在"内容管理"下注册 path=learn-aux（perms=portal:learn-aux:list），
--        v7.13/7.15 又在"读书空间"下注册 path=learn-aux（perms=portal:learn:list）。
--        两个同 path C 菜单 → RuoYi-Vue3 生成同名路由（Learn-aux）→
--        vue-router 4 同名 addRoute 移除先注册者 → /cms/learn-aux 被
--        /book/learn-aux 覆盖 → 点击侧边栏"内容管理→学习辅助"报 404。
-- 背景2: 学习辅助 = 学习计划（每日刷题为主）+ 错题本（100% 源于题库刷题），
--        数据均产自面试指南的题库体系，挂"读书空间"归属错位。
-- 处理: 删除重复菜单，保留 portal:learn:list 正式菜单并迁移至"面试指南"目录。
-- ============================================================
SELECT @learnaux_dup_id := menu_id FROM sys_menu WHERE perms = 'portal:learn-aux:list' AND menu_type = 'C' LIMIT 1;
SELECT @learnaux_keep_id := menu_id FROM sys_menu WHERE perms = 'portal:learn:list' AND menu_type = 'C' LIMIT 1;

-- 授权转移：曾有重复菜单的角色 → 补正式菜单权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT rm.role_id, @learnaux_keep_id
  FROM sys_role_menu rm
 WHERE rm.menu_id = @learnaux_dup_id
   AND @learnaux_dup_id IS NOT NULL
   AND @learnaux_keep_id IS NOT NULL
   AND @learnaux_keep_id <> @learnaux_dup_id
   AND NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.role_id = rm.role_id AND x.menu_id = @learnaux_keep_id);

-- 删除重复菜单及其角色关联
DELETE FROM sys_role_menu WHERE menu_id = @learnaux_dup_id AND @learnaux_dup_id IS NOT NULL;
DELETE FROM sys_menu WHERE menu_id = @learnaux_dup_id AND @learnaux_dup_id IS NOT NULL;

-- 迁移归属：学习辅助 → 面试指南目录（数据源于题库：错题本/每日刷题）
SELECT @interview_dir_id := menu_id FROM sys_menu WHERE path = 'interview' AND parent_id = 0 AND menu_type = 'M' LIMIT 1;
UPDATE sys_menu
SET parent_id = @interview_dir_id, order_num = 9,
    remark = 'v9.5: 迁移至面试指南（错题本/学习计划数据源于题库刷题）',
    update_by = 'admin', update_time = NOW()
WHERE menu_id = @learnaux_keep_id
  AND @interview_dir_id IS NOT NULL
  AND (parent_id <> @interview_dir_id OR order_num <> 9);

-- 验证4：learn-aux 路由应仅剩面试指南下 1 条
SELECT '学习辅助菜单' AS check_item,
  IF(COUNT(*) = 1, 'OK - 仅面试指南一处', 'ERROR - 重复或缺失') AS result
FROM sys_menu WHERE path = 'learn-aux' AND menu_type = 'C' AND status = '0';

-- ============================================================
-- 第六节：去重交易入口 — 删除"财务/付费订单"独立菜单，权限归一至"交易管理"
-- 背景: init_v7.8 第十六节注册"财务(finance)/付费订单"（component=cms/order/index，已下线隐藏），
--       v9.0 第七节迁移条件为 parent_id=内容管理，而它挂在财务目录下 → 未迁移成死角。
--       "交易管理"Tab 容器第一个 Tab 引用的正是同一组件 cms/order/index → 功能完全重复。
--       另: CmsOrderController 接口权限为 portal:order:list/query（Tab 内"付费订单"Tab
--       依赖），因此权限不能删，统一转为"交易管理"菜单下的 F 按钮。
-- 处理: 1) portal:order:list/query 由 C 菜单/F按钮 转为挂"交易管理"下的 F 按钮
--       2) 物理删除"财务"目录及其下残余 C 菜单（path=order 的重复路由入口）
-- ============================================================

-- 6.1 订单查询权限转为"交易管理"下 F 按钮（存在即归位，幂等）
-- 重新定位 @txn_menu_id（防分段执行丢失会话变量；与第一节一致）
SELECT @txn_menu_id := menu_id FROM sys_menu WHERE perms = 'cms:transaction:list' AND menu_type = 'C' LIMIT 1;
SELECT @commerce_parent_id := menu_id FROM sys_menu WHERE menu_name = '商业化' AND parent_id = 0 LIMIT 1;
SET @txn_menu_id := IFNULL(@txn_menu_id, IFNULL(@commerce_parent_id, 0));

UPDATE sys_menu
SET menu_name = '付费订单Tab查询', parent_id = @txn_menu_id, order_num = 10,
    menu_type = 'F', path = '', component = NULL, visible = '0', status = '0',
    update_by = 'admin', update_time = NOW(), remark = 'v9.5: 交易管理Tab-付费订单组件查询权限'
WHERE perms = 'portal:order:list'
  AND (menu_type <> 'F' OR parent_id <> @txn_menu_id OR visible = '1' OR status = '1');

-- 6.2 订单详情权限同样归位（原挂"付费订单"下的 F 按钮，且为已下线状态）
UPDATE sys_menu
SET menu_name = '付费订单Tab详情', parent_id = @txn_menu_id, order_num = 11,
    menu_type = 'F', path = '', component = NULL, visible = '0', status = '0',
    update_by = 'admin', update_time = NOW(), remark = 'v9.5: 交易管理Tab-付费订单组件详情权限'
WHERE perms = 'portal:order:query'
  AND (parent_id <> @txn_menu_id OR visible = '1' OR status = '1' OR menu_type <> 'F');

-- 6.3 兜底：若权限项整体缺失（如被历史脚本物理删除），重建 F 按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '付费订单Tab查询', @txn_menu_id, 10, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:order:list', '#', 'admin', NOW(), 'v9.5: 交易管理Tab-付费订单组件查询权限'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:order:list');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '付费订单Tab详情', @txn_menu_id, 11, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:order:query', '#', 'admin', NOW(), 'v9.5: 交易管理Tab-付费订单组件详情权限'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:order:query');

-- 6.4 物理删除"财务"目录及其下残余 C 菜单（6.1-6.3 已将 order 权限转出为 F 挂交易管理）
SELECT @finance_dir_id := menu_id FROM sys_menu WHERE menu_name = '财务' AND parent_id = 0 AND menu_type = 'M' LIMIT 1;

-- 删除财务目录下残余的 C 菜单（防御性：连同其他历史残留一并清理，重复路由入口即在此消除）
DELETE FROM sys_role_menu
WHERE @finance_dir_id IS NOT NULL
  AND menu_id IN (
    SELECT menu_id FROM (
      SELECT menu_id FROM sys_menu WHERE parent_id = @finance_dir_id AND menu_type = 'C'
    ) t
  );
DELETE FROM sys_menu WHERE parent_id = @finance_dir_id AND menu_type = 'C' AND @finance_dir_id IS NOT NULL;

-- 删除目录本身
DELETE FROM sys_role_menu WHERE menu_id = @finance_dir_id AND @finance_dir_id IS NOT NULL;
DELETE FROM sys_menu WHERE menu_id = @finance_dir_id AND @finance_dir_id IS NOT NULL;

-- 验证5：财务目录应不存在（交易入口唯一）
SELECT '财务目录' AS check_item,
  IF(COUNT(*) = 0, 'OK - 已删除', 'ERROR - 仍存在') AS result
FROM sys_menu WHERE menu_name = '财务' AND parent_id = 0 AND menu_type = 'M';

-- 验证6：交易管理下的 Tab 组件权限链完整（应 4 条 F 按钮）
SELECT '交易管理权限链' AS check_item,
  IF(COUNT(*) = 4, 'OK - order/tip 各2条', CONCAT('WARN - 实际', COUNT(*), '条(人工核对)')) AS result
FROM sys_menu WHERE parent_id = @txn_menu_id
  AND perms IN ('portal:order:list','portal:order:query','portal:tip:list','portal:tip:query')
  AND menu_type = 'F';
