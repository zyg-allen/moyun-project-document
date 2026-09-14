-- =====================================================================
-- v11.79 全平台支付统一标准重构（单钱包 + 公账记账 + 提现闭环）
-- 文档依据：docs/05-方案设计-分模块/07-支付模块/支付问题以及解决方案v-2.md
-- 统一规范：
--   业务订单 status：pending / paid / refunded / closed（默认 pending）
--   通道订单 pay_order.status：CREATED / PAID / SETTLED / CLOSED（通道层保持）
--   提现单 status：auditing / paid / rejected（默认 auditing）
--   支付渠道字段统一命名 pay_channel：wechat / alipay / points
--   金额单位：元 DECIMAL(18,2)
-- 原则：不考虑历史数据迁移负担，业务逻辑直接切换（存量 1/0 → paid/closed 一次性换算）
-- =====================================================================

-- ===== 1. ledger_tip_order：状态字符串化 + 渠道字段统一 + 关联通道单据 =====
ALTER TABLE `moyun-db`.ledger_tip_order
    MODIFY COLUMN status VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT '状态：pending=待支付 paid=已支付 refunded=已退款 closed=已关闭（v11.79 统一）';

ALTER TABLE `moyun-db`.ledger_tip_order
    CHANGE COLUMN pay_way pay_channel VARCHAR(16) NOT NULL DEFAULT 'wechat' COMMENT '支付渠道：wechat/alipay（v11.79 与 portal_tip_order 统一命名）';

ALTER TABLE `moyun-db`.ledger_tip_order
    ADD COLUMN pay_no VARCHAR(40) NULL COMMENT '关联公共通道单据号（pay_order.pay_no；演示模式为空）' AFTER pay_channel;

-- 存量状态换算（1=成功→paid，0=已撤销→closed）
UPDATE `moyun-db`.ledger_tip_order SET status = 'paid' WHERE status = '1';
UPDATE `moyun-db`.ledger_tip_order SET status = 'closed' WHERE status = '0';

-- ===== 2. pay_order：补全对账维度（下单用户 / 归属平台） =====
ALTER TABLE `moyun-db`.pay_order
    ADD COLUMN user_id BIGINT NULL COMMENT '下单用户（portal_user.id，v11.79 对账维度）' AFTER biz_no;

ALTER TABLE `moyun-db`.pay_order
    ADD COLUMN platform VARCHAR(16) NULL COMMENT '归属平台：ledger_app/portal（v11.79 对账维度）' AFTER user_id;

ALTER TABLE `moyun-db`.pay_order
    ADD INDEX idx_pay_order_user (user_id), ADD INDEX idx_pay_order_platform (platform);

-- ===== 3. pay_withdraw_order：状态统一小写 + 打款时间（提现闭环 v11.79 启用） =====
ALTER TABLE `moyun-db`.pay_withdraw_order
    MODIFY COLUMN status VARCHAR(16) NOT NULL DEFAULT 'auditing' COMMENT '状态：auditing=审核中 paid=已打款 rejected=已驳回（v11.79 统一小写）';

ALTER TABLE `moyun-db`.pay_withdraw_order
    ADD COLUMN paid_time DATETIME NULL COMMENT '打款完成时间（真实出金到账）' AFTER reject_reason;

-- ===== 4. 废弃社区钱包（单钱包架构：pay_user_account 为全平台唯一钱包） =====
DROP TABLE IF EXISTS `moyun-db`.portal_wallet_transaction;
DROP TABLE IF EXISTS `moyun-db`.portal_wallet;

-- =====================================================================
-- ===== 5. 菜单重组：支付管理 → 收入管理（加3删1重排） =====
-- =====================================================================

-- 5.1 父菜单改名
UPDATE `moyun-db`.sys_menu
SET menu_name = '收入管理', remark = 'v11.79 全平台支付汇集：公账+单钱包+提现闭环'
WHERE menu_id = 5300 AND parent_id = 0;

-- 5.2 删除分账流水菜单（功能并入用户钱包页"资金流水"Tab）
DELETE FROM `moyun-db`.sys_role_menu WHERE menu_id IN (5302, 5315, 5316);
DELETE FROM `moyun-db`.sys_menu WHERE menu_id IN (5302, 5315, 5316);

-- 5.3 新增菜单（幂等：先删后插）
DELETE FROM `moyun-db`.sys_menu WHERE menu_id IN (5306, 5307, 5308, 5318, 5319, 5320, 5321);
DELETE FROM `moyun-db`.sys_role_menu WHERE role_id = 1 AND menu_id IN (5306, 5307, 5308, 5318, 5319, 5320, 5321);

-- 收入订单（统一业务订单视图：合并 ledger_tip_order + portal_tip_order）
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5306, '收入订单', 5300, 2, 'income-order', 'cms/pay/income-order/index', null, '', 1, 0, 'C', '0', '0', 'cms:payIncomeOrder:list', 'shopping', 'admin', NOW(), '', null, 'v11.79 全平台业务订单统一视图（平台/渠道/状态筛选）', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5318, '收入订单查询', 5306, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:payIncomeOrder:list', '#', 'admin', NOW(), '', null, '', '0');

-- 提现审核（通过=扣款+打款记账，驳回=退回）
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5308, '提现审核', 5300, 3, 'withdraw', 'cms/pay/withdraw/index', null, '', 1, 0, 'C', '0', '0', 'cms:payWithdraw:list', 'validCode', 'admin', NOW(), '', null, 'v11.79 提现闭环：auditing→paid/rejected', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5320, '提现单列表', 5308, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:payWithdraw:list', '#', 'admin', NOW(), '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5321, '提现审核操作', 5308, 2, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:payWithdraw:audit', '#', 'admin', NOW(), '', null, '', '0');

-- 用户钱包（唯一钱包 pay_user_account + 资金流水 Tab，取代原分账流水页/社区钱包死页）
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5307, '用户钱包', 5300, 4, 'wallet', 'cms/pay/wallet/index', null, '', 1, 0, 'C', '0', '0', 'cms:payWallet:list', 'peoples', 'admin', NOW(), '', null, 'v11.79 单钱包：余额列表 + 资金流水 + 守恒对账', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5319, '钱包查询', 5307, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:payWallet:list', '#', 'admin', NOW(), '', null, '', '0');

-- 5.4 存量菜单重排（收入总览1 收入订单2 提现审核3 用户钱包4 支付订单5 银行卡6 支付配置7）
UPDATE `moyun-db`.sys_menu SET order_num = 1 WHERE menu_id = 5305 AND parent_id = 5300;
UPDATE `moyun-db`.sys_menu SET order_num = 5 WHERE menu_id = 5301 AND parent_id = 5300;
UPDATE `moyun-db`.sys_menu SET order_num = 6 WHERE menu_id = 5303 AND parent_id = 5300;
UPDATE `moyun-db`.sys_menu SET order_num = 7 WHERE menu_id = 5304 AND parent_id = 5300;

-- 5.5 授权超级管理员（role_id=1）
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id) VALUES
    (1, 5306), (1, 5318),
    (1, 5307), (1, 5319),
    (1, 5308), (1, 5320), (1, 5321);

-- ===== 6. 排序规则统一（修复收入订单 UNION 报错：Illegal mix of collations） =====
-- 原因：portal_tip_order 建表仅指定 charset=utf8mb4（继承库默认 utf8mb4_0900_ai_ci），
--       与 ledger_tip_order 显式的 utf8mb4_general_ci 不一致，UNION 时字符列排序规则冲突。
-- 方案：统一到 pay 表族标准 utf8mb4_general_ci（CONVERT 全表转换，幂等可重复执行）。
ALTER TABLE `moyun-db`.portal_tip_order CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE `moyun-db`.ledger_tip_order CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
