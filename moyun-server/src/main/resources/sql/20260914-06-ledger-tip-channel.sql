-- =====================================================================
-- 20260914-06 记账App打赏接入公共支付通道（v11.80）
-- 依据：docs/05-方案设计-分模块/07-支付模块/支付问题以及解决方案v-2.md
--
-- 链路（与门户打赏 createWechatTipOrder 同构，bizType 区分）：
--   落 pending 打赏单 → payGateway.createOrder(bizType='ledger_tip',
--   platform='ledger_app', channel='wechat') → 回填 pay_no → 收银台
--   （扫码 / mock 模拟支付，复用 /portal/pay/status 与 /portal/pay/mock）
--   → 网关回调 LedgerTipPayCallbackHandler：pending→paid + settlePlatform
--   平台全额分账（pay_ledger_entry PLATFORM/credit）→ 站内通知。
--
-- 变更：
--   1. client_uuid   客户端幂等号（防重复提交，对齐记一笔 clientUuid 机制）
--   2. paid_time     支付完成时间（回调置 paid 时写入，对齐 portal_tip_order.paid_time）
--   3. 存量数据清理  历史演示单（无真实资金、无 pay_no）为测试数据，彻底删除
--                    （不迁就历史数据；清零后 GMV/公账守恒口径纯净）
--   4. 索引          uk_client_uuid 幂等唯一索引（NULL 可重复，兼容历史空值）
-- =====================================================================

-- 1. 存量演示单清理：历史单无真实资金且无 pay_no，属测试数据，彻底删除
--    （先清数据后改结构；重复执行时 DELETE 为空集，ALTER 报"列已存在"可忽略）
DELETE FROM `moyun-db`.ledger_tip_order WHERE pay_no IS NULL;

-- 2. 客户端幂等号
ALTER TABLE `moyun-db`.ledger_tip_order
    ADD COLUMN client_uuid VARCHAR(64) NULL COMMENT '客户端幂等号（防重复提交，v11.80 对齐记一笔机制）' AFTER pay_no;

ALTER TABLE `moyun-db`.ledger_tip_order
    ADD UNIQUE KEY uk_client_uuid (client_uuid);

-- 3. 支付完成时间（回调置 paid 时写入）
ALTER TABLE `moyun-db`.ledger_tip_order
    ADD COLUMN paid_time DATETIME NULL COMMENT '支付完成时间（网关回调置 paid 时写入，v11.80）' AFTER create_time;

-- 4. 校验注释：状态枚举与全平台统一（pending/paid/refunded/closed）
ALTER TABLE `moyun-db`.ledger_tip_order
    MODIFY COLUMN status VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT '状态：pending=待支付 paid=已支付（网关回调推进） refunded=已退款 closed=已关闭（v11.80 接公共通道）';
