-- ============================================================
-- 记账-库存：订单表（销售/采购 + 明细）—— 赊账联动的地基
--
-- 设计要点（相对参考文档的取舍）：
-- 1) 一张订单带 payment_type（cash=现结 / credit=赊账）：
--    credit 时自动联动 ledger_obligation（应收/应付）与库存，无需用户手动记。
-- 2) 金额一律 decimal(18,2)，软删 status（与记账域一致）。
-- 3) 订单只做「单据」，库存变动由 ledger_stock_flow 留痕（可追溯）。
-- ============================================================

-- 销售单（卖出 / 赊销）
drop table if exists `ledger_sale_order`;
CREATE TABLE `ledger_sale_order` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '销售单ID',
    `user_id` bigint NOT NULL COMMENT '归属用户ID',
    `warehouse_id` bigint NOT NULL COMMENT '出库仓库ID',
    `order_no` varchar(50) DEFAULT NULL COMMENT '单号',
    `customer_name` varchar(100) NOT NULL COMMENT '客户名称（赊账时=应收对方）',
    `customer_id` bigint DEFAULT NULL COMMENT '客户用户ID（对方是系统用户时）',
    `payment_type` varchar(20) NOT NULL DEFAULT 'cash' COMMENT '收款方式：cash=现结 credit=赊账',
    `total_amount` decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '应收总额（按售价）',
    `cost_amount` decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '成本总额',
    `profit_amount` decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '利润（售价-成本）',
    `obligation_id` bigint DEFAULT NULL COMMENT '赊账时关联的 ledger_obligation.id',
    `transaction_id` bigint DEFAULT NULL COMMENT '现结时关联的资金流水ID',
    `operator_id` bigint NOT NULL COMMENT '操作人用户ID',
    `remark` varchar(500) DEFAULT NULL,
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '行状态：1=正常 0=已删除',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_wh` (`user_id`,`warehouse_id`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_obligation` (`obligation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='记账-销售单';

-- 销售明细
drop table if exists `ledger_sale_order_item`;
CREATE TABLE `ledger_sale_order_item` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `sale_id` bigint NOT NULL COMMENT '销售单ID',
    `product_id` bigint NOT NULL COMMENT '商品ID',
    `quantity` int NOT NULL DEFAULT 1 COMMENT '数量',
    `unit_price` decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '售价',
    `cost_price` decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '成本价（结转成本）',
    `subtotal` decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '小计（售价×数量）',
    `cost_subtotal` decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '成本小计',
    `profit` decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '利润',
    `status` tinyint NOT NULL DEFAULT 1,
    PRIMARY KEY (`id`),
    KEY `idx_sale` (`sale_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='记账-销售明细';

-- 采购单（买入 / 赊购）
drop table if exists `ledger_purchase_order`;
CREATE TABLE `ledger_purchase_order` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '采购单ID',
    `user_id` bigint NOT NULL COMMENT '归属用户ID',
    `warehouse_id` bigint NOT NULL COMMENT '入库仓库ID',
    `order_no` varchar(50) DEFAULT NULL COMMENT '单号',
    `supplier_name` varchar(100) NOT NULL COMMENT '供应商名称（赊账时=应付对方）',
    `supplier_id` bigint DEFAULT NULL,
    `payment_type` varchar(20) NOT NULL DEFAULT 'cash' COMMENT '付款方式：cash=现结 credit=赊账',
    `total_amount` decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '应付总额',
    `obligation_id` bigint DEFAULT NULL COMMENT '赊账时关联的 ledger_obligation.id',
    `transaction_id` bigint DEFAULT NULL COMMENT '现结时关联的资金流水ID',
    `operator_id` bigint NOT NULL COMMENT '操作人用户ID',
    `remark` varchar(500) DEFAULT NULL,
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '行状态：1=正常 0=已删除',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_wh` (`user_id`,`warehouse_id`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_obligation` (`obligation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='记账-采购单';

-- 采购明细
drop table if exists `ledger_purchase_order_item`;
CREATE TABLE `ledger_purchase_order_item` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `purchase_id` bigint NOT NULL COMMENT '采购单ID',
    `product_id` bigint NOT NULL COMMENT '商品ID',
    `quantity` int NOT NULL DEFAULT 1 COMMENT '数量',
    `unit_price` decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '单价',
    `subtotal` decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '小计',
    `status` tinyint NOT NULL DEFAULT 1,
    PRIMARY KEY (`id`),
    KEY `idx_purchase` (`purchase_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='记账-采购明细';
