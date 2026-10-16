-- ============================================================
-- 增量脚本（存量库执行件）—— 库存核心表（第一批）
-- 对应 init-sql/moyun-db-ddl.sql 的目标态；存量库执行用。
-- 一律 CREATE TABLE IF NOT EXISTS，绝不 DROP（线上库有数据）。
-- ============================================================

-- ============================================================
-- 记账-库存投资管理：核心表（第一批：仓库 / 商品 / 库存 / 库存流水）
--
-- 设计说明（相对参考文档《库存投资管理模块_核心数据表SQL》的取舍）：
-- 1) SQLite 原型 → MySQL 8.0：INTEGER PRIMARY KEY AUTOINCREMENT → bigint AUTO_INCREMENT、
--    BOOLEAN → tinyint(1)。
-- 2) 表名统一 ledger_ 前缀，与库内记账域 14 张表一致。
-- 3) 金额列一律 decimal(18,2)（元），与 DdlConventionGuardTest 一致。
-- 4) 软删列用 status（1=正常 0=已删除），与 ledger_asset_account 等既有表一致；
--    不引入 del_flag —— 同一业务域内软删语义保持一致。
-- 5) owner（主体）表省略：本项目"主体"= 当前登录用户(user_id)，
--    仓库的三种模式（个人/家庭/合伙）由 ledger_warehouse.warehouse_type 表达。
-- 6) 三个订单/明细、结算、素材、操作日志留待第二批（赊账联动需要订单表时再加）。
-- ============================================================

-- -----------------------------------------------------------
-- 1. 仓库（个人仓 / 家庭仓 / 合伙仓）
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ledger_warehouse` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '仓库ID',
    `user_id` bigint NOT NULL COMMENT '创建者用户ID（主体）',
    `name` varchar(100) NOT NULL COMMENT '仓库名称',
    `warehouse_type` varchar(20) NOT NULL DEFAULT 'personal' COMMENT '仓库模式：personal=个人仓 family=家庭仓 partner=合伙仓',
    `is_active` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用：1=启用 0=停用',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '行状态：1=正常 0=已删除',
    `create_by` varchar(64) DEFAULT NULL,
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_status` (`user_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='记账-仓库（个人/家庭/合伙）';

-- -----------------------------------------------------------
-- 2. 仓库成员（家庭仓/合伙仓共享）
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ledger_warehouse_member` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '成员ID',
    `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
    `user_id` bigint NOT NULL COMMENT '成员用户ID',
    `role` varchar(20) NOT NULL DEFAULT 'member' COMMENT '角色：owner=创建者 manager=管理 member=成员',
    `invited_by` bigint DEFAULT NULL COMMENT '邀请人用户ID',
    `joined_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `is_active` tinyint(1) NOT NULL DEFAULT 1,
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '行状态：1=正常 0=已删除',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_warehouse_user` (`warehouse_id`,`user_id`),
    KEY `idx_user` (`user_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='记账-仓库成员';

-- -----------------------------------------------------------
-- 3. 商品（基础档案）
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ledger_product` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    `warehouse_id` bigint NOT NULL COMMENT '所属仓库ID',
    `user_id` bigint NOT NULL COMMENT '创建者用户ID（冗余，便于按人查）',
    `name` varchar(200) NOT NULL COMMENT '商品名称',
    `barcode` varchar(50) DEFAULT NULL COMMENT '条码',
    `category` varchar(50) DEFAULT NULL COMMENT '商品分类（自有字段，非记账分类）',
    `unit` varchar(20) NOT NULL DEFAULT '个' COMMENT '计量单位',
    `cost_price` decimal(18,2) DEFAULT NULL COMMENT '成本价（元）',
    `sell_price` decimal(18,2) DEFAULT NULL COMMENT '售价（元）',
    `image_urls` text COMMENT '图片地址（JSON 数组字符串）',
    `is_secondhand` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否二手：1=是 0=否',
    `condition_grade` varchar(20) DEFAULT NULL COMMENT '成色（二手用）',
    `source_channel` varchar(50) DEFAULT NULL COMMENT '来源渠道',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '行状态：1=正常 0=已删除',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_warehouse` (`warehouse_id`,`status`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='记账-商品';

-- -----------------------------------------------------------
-- 4. 库存（实时数量，按 仓库×商品 唯一）
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ledger_stock` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '库存ID',
    `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
    `product_id` bigint NOT NULL COMMENT '商品ID',
    `quantity` int NOT NULL DEFAULT 0 COMMENT '当前数量',
    `cost_value` decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '库存成本价值（成本价×数量）',
    `last_updated` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '行状态：1=正常 0=已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_warehouse_product` (`warehouse_id`,`product_id`),
    KEY `idx_warehouse` (`warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='记账-库存';

-- -----------------------------------------------------------
-- 5. 库存流水（所有变动留痕，最重要的追溯表）
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ledger_stock_flow` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '流水ID',
    `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
    `product_id` bigint NOT NULL COMMENT '商品ID',
    `flow_type` varchar(30) NOT NULL COMMENT '变动类型：in=入库 out=出库 adjust=盘点调整',
    `quantity_before` int DEFAULT NULL COMMENT '变动前数量',
    `quantity_change` int NOT NULL COMMENT '变动量（正入负出）',
    `quantity_after` int NOT NULL COMMENT '变动后数量',
    `cost_before` decimal(18,2) DEFAULT NULL COMMENT '变动前成本价值',
    `cost_after` decimal(18,2) DEFAULT NULL COMMENT '变动后成本价值',
    `operator_id` bigint NOT NULL COMMENT '操作人用户ID',
    `related_order` varchar(50) DEFAULT NULL COMMENT '关联单据号（销售/采购/盘点）',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '行状态：1=正常 0=已删除',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_warehouse_time` (`warehouse_id`,`create_time`),
    KEY `idx_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='记账-库存流水';
