-- ============================================================
-- VIP 与支付模块端维度统一整改
-- 1. 统一命名 platform → platform_code（pay_order / pay_ledger_entry）
-- 2. 缺端维度表补列（withdraw_order / pay_notification / pay_notify_log）
-- 3. portal_user 加注册来源端（VIP 设计方案 §10）
-- 4. 索引
-- ============================================================

-- ========== 1. 统一命名 platform → platform_code ==========
-- pay_order: platform → platform_code
ALTER TABLE pay_order CHANGE COLUMN platform platform_code VARCHAR(50) DEFAULT NULL COMMENT '归属端代码（sys_platform.platform_code）';

-- pay_ledger_entry: platform → platform_code（列已存在，仅改名）
ALTER TABLE pay_ledger_entry CHANGE COLUMN platform platform_code VARCHAR(50) DEFAULT NULL COMMENT '归属端代码';

-- ========== 2. 缺端维度表补列 ==========
-- withdraw_order
ALTER TABLE pay_withdraw_order ADD COLUMN platform_code VARCHAR(50) DEFAULT NULL COMMENT '归属端代码';

-- pay_notification
ALTER TABLE pay_notification ADD COLUMN platform_code VARCHAR(50) DEFAULT NULL COMMENT '归属端代码';

-- pay_notify_log
ALTER TABLE pay_notify_log ADD COLUMN platform_code VARCHAR(50) DEFAULT NULL COMMENT '归属端代码';

-- ========== 3. portal_user 加注册来源端 ==========
ALTER TABLE portal_user ADD COLUMN platform_code VARCHAR(50) DEFAULT 'portal' COMMENT '注册来源端';

-- ========== 4. 数据迁移：ledger_app → ledger 统一值域 ==========
-- 历史数据中 pay_order.platform 存在 'ledger_app' 值（记账端打赏旧代码写入），统一为 'ledger'
UPDATE pay_order SET platform_code = 'ledger' WHERE platform_code = 'ledger_app';

-- ========== 5. 索引 ==========
-- pay_ledger_entry 已有 platform 列改名后需要重建索引
CREATE INDEX idx_pay_ledger_platform ON pay_ledger_entry(platform_code);
CREATE INDEX idx_pay_withdraw_platform ON pay_withdraw_order(platform_code);
CREATE INDEX idx_pay_notification_platform ON pay_notification(platform_code);
CREATE INDEX idx_portal_user_platform ON portal_user(platform_code);
