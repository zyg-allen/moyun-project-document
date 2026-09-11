-- ===================================================================
-- 记账模块·分类体系扩展（v11.24）
-- 1. ALTER TABLE ledger_category ADD group_name（语义分组）
-- 2. UPDATE 现有系统预设分类补 group_name
-- 3. INSERT 新增 transfer/repayment/borrow/adjust 四类系统预设分类
-- ===================================================================

-- 1. 增加语义分组列
ALTER TABLE `moyun-db`.ledger_category ADD COLUMN group_name VARCHAR(50) NULL COMMENT '语义分组（前端展示分组用）' AFTER type;

-- 2. 给现有系统预设分类补 group_name
-- 支出
UPDATE `moyun-db`.ledger_category SET group_name = '生活刚需' WHERE user_id = 0 AND is_system = 1 AND type = 'expense' AND name IN ('餐饮','交通','居住','日用','通讯');
UPDATE `moyun-db`.ledger_category SET group_name = '消费娱乐' WHERE user_id = 0 AND is_system = 1 AND type = 'expense' AND name IN ('购物','娱乐','旅行','宠物');
UPDATE `moyun-db`.ledger_category SET group_name = '人情教育' WHERE user_id = 0 AND is_system = 1 AND type = 'expense' AND name IN ('人情往来','教育','医疗');
UPDATE `moyun-db`.ledger_category SET group_name = '负债还款' WHERE user_id = 0 AND is_system = 1 AND type = 'expense' AND name IN ('房贷/房租','车贷','还款','利息');
UPDATE `moyun-db`.ledger_category SET group_name = '其他' WHERE user_id = 0 AND is_system = 1 AND type = 'expense' AND name = '其他支出';
-- 收入
UPDATE `moyun-db`.ledger_category SET group_name = '劳动收入' WHERE user_id = 0 AND is_system = 1 AND type = 'income' AND name IN ('工资','奖金','兼职');
UPDATE `moyun-db`.ledger_category SET group_name = '投资理财' WHERE user_id = 0 AND is_system = 1 AND type = 'income' AND name IN ('理财收益','二手闲置');
UPDATE `moyun-db`.ledger_category SET group_name = '其他收入' WHERE user_id = 0 AND is_system = 1 AND type = 'income' AND name IN ('红包','退款','借入','其他收入');

-- 3. 新增 transfer / repayment / borrow / adjust 四类系统预设分类
INSERT INTO `moyun-db`.ledger_category (user_id, name, type, group_name, parent_id, icon, color, sort_order, is_system, status) VALUES
-- 转账分类
(0, '账户间互转', 'transfer', '账户间', NULL, 'transfer-self', '#4A90D9', 1, 1, 1),
(0, '转给亲友',   'transfer', '亲友间', NULL, 'transfer-friend', '#E67E22', 2, 1, 1),
(0, '代付代收',   'transfer', '亲友间', NULL, 'transfer-proxy', '#16A085', 3, 1, 1),
(0, '退款退回',   'transfer', '账户间', NULL, 'transfer-refund', '#5DADE2', 4, 1, 1),
(0, '其他转账',   'transfer', '其他', NULL, 'transfer-other', '#BDC3C7', 5, 1, 1),
-- 还款分类
(0, '信用卡还款', 'repayment', '信用卡', NULL, 'repay-card', '#E74C3C', 1, 1, 1),
(0, '贷款还款',   'repayment', '贷款', NULL, 'repay-loan', '#8E44AD', 2, 1, 1),
(0, '私人借款还', 'repayment', '私人', NULL, 'repay-personal', '#D35400', 3, 1, 1),
(0, '利息支出',   'repayment', '利息', NULL, 'repay-interest', '#D4AC0D', 4, 1, 1),
(0, '其他还款',   'repayment', '其他', NULL, 'repay-other', '#BDC3C7', 5, 1, 1),
-- 借款分类
(0, '信用卡消费', 'borrow', '信用卡', NULL, 'borrow-card', '#E74C3C', 1, 1, 1),
(0, '网贷借款',   'borrow', '网贷', NULL, 'borrow-online', '#E67E22', 2, 1, 1),
(0, '银行贷款',   'borrow', '贷款', NULL, 'borrow-bank', '#8E44AD', 3, 1, 1),
(0, '消费分期',   'borrow', '分期', NULL, 'borrow-installment', '#3498DB', 4, 1, 1),
(0, '私人借款',   'borrow', '私人', NULL, 'borrow-personal', '#D35400', 5, 1, 1),
(0, '其他借款',   'borrow', '其他', NULL, 'borrow-other', '#BDC3C7', 6, 1, 1),
-- 校准分类
(0, '余额修正',   'adjust', '余额修正', NULL, 'adjust-balance', '#34495E', 1, 1, 1),
(0, '手续费调整', 'adjust', '其他调整', NULL, 'adjust-fee', '#95A5A6', 2, 1, 1),
(0, '汇率差异',   'adjust', '其他调整', NULL, 'adjust-fx', '#7F8C8D', 3, 1, 1),
(0, '其他调整',   'adjust', '其他调整', NULL, 'adjust-other', '#BDC3C7', 4, 1, 1);