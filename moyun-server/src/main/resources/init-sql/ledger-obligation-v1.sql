-- ============================================================
-- 记账-债权债务统一表 ledger_obligation（一张表 + type 字段，替代原 receivable/payable 两张表）
--
-- 设计动机（与用户对齐）：
--   应收（别人欠我）与应付（我欠别人）本质是同一类"往来"，只是方向相反、
--   风险属性不同。分两张表意味着两套 CRUD / 两套统计 / 两套扩展，
--   后期加"投资"（有收回风险）还要再来一张表。
--   统一成一张表 + type 字段，一套代码覆盖全部，统计按 type 归组即可。
--
-- type 取值：
--   receivable  应收（借出/赊销产生，计入资产）
--   payable     应付（借款/赊购产生，计入负债）
--   （后期可加 investment=投资，届时再加 risk_level 等风险字段）
--
-- 相对旧两表：字段完全相同（两表本就对称），仅多一个 type 判别。
-- ============================================================

drop table if exists `ledger_obligation`;
CREATE TABLE `ledger_obligation` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '往来ID',
    `user_id` bigint NOT NULL COMMENT '归属用户ID（主体）',
    `type` varchar(20) NOT NULL DEFAULT 'receivable' COMMENT '类型：receivable=应收 payable=应付（后期可加 investment）',
    `counterparty` varchar(100) NOT NULL COMMENT '对方名称',
    `counterparty_id` bigint DEFAULT NULL COMMENT '对方用户ID（对方也是本系统用户时）',
    `warehouse_id` bigint DEFAULT NULL COMMENT '关联仓库ID（赊销/赊购产生时）',
    `source_type` varchar(30) NOT NULL DEFAULT 'manual' COMMENT '来源：manual=手工 lend=借出 sale_credit=赊销 purchase_credit=赊购 borrow=借款',
    `source_id` bigint DEFAULT NULL COMMENT '来源单据ID',
    `total_amount` decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '总额（元）',
    `paid_amount` decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '已结金额（元）；未结余额 = total - paid，不落列',
    `settle_status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT '结算状态：pending=未结 partial=部分结 settled=已结清',
    `due_date` date DEFAULT NULL COMMENT '到期日',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '行状态：1=正常 0=已删除',
    `create_by` varchar(64) DEFAULT NULL,
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_type` (`user_id`,`type`,`status`),
    KEY `idx_settle` (`settle_status`),
    KEY `idx_source` (`source_type`,`source_id`),
    KEY `idx_due_date` (`due_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='记账-债权债务统一表（应收/应付，一张表+type）';
