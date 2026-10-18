-- ============================================================
-- 记账-仓库库存管理：货架（存放位置）+ 库存按货架拆分 + 图片 1:N
--
-- 设计文档：docs/05-方案设计-分模块/2-记账模块/记账App-仓库库存管理-设计方案-V1.0.md
--
-- 本次变更：
--   ① 新建 ledger_shelf（货架：位置/归属/类型/容量）
--   ② ledger_stock 增加 shelf_id（0=未上架），唯一键 (warehouse_id,product_id)
--      → (product_id,shelf_id)：同一商品可分布在多个货架
--   ③ ledger_stock_flow 增加 shelf_id / to_shelf_id：流水可追溯到具体货架
--   ④ 图片不再塞 ledger_product.image_urls(text)，统一写 sys_file
--      （business_type='ledger_product'/'ledger_shelf'，business_id=实体id）
--      不新建附件表，不迁移历史数据；原 image_urls 字段保留但不再读取
--
-- 幂等：所有 DDL 均由 information_schema 守卫，可重复执行。
-- ============================================================

-- ① 货架表 ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ledger_shelf` (
  `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `warehouse_id`  bigint       NOT NULL COMMENT '归属：所属仓库',
  `user_id`       bigint       NOT NULL COMMENT '归属：归属人',
  `code`          varchar(50)  NOT NULL COMMENT '货架编号（仓库内唯一，如 A-01）',
  `name`          varchar(100) NOT NULL COMMENT '货架名称',
  `zone`          varchar(50)  DEFAULT NULL COMMENT '区域/分区（如 A区、冷藏区）',
  `row_no`        int          DEFAULT NULL COMMENT '排',
  `layer_no`      int          DEFAULT NULL COMMENT '层',
  `position_no`   int          DEFAULT NULL COMMENT '位',
  `location_text` varchar(200) DEFAULT NULL COMMENT '位置描述（后端按区/排/层/位自动拼装）',
  `shelf_type`    varchar(20)  NOT NULL DEFAULT 'normal' COMMENT '类型：normal/refrigerated/locked/temp',
  `capacity`      int          DEFAULT NULL COMMENT '容量（件，超容量仅提示不阻断）',
  `sort_order`    int          NOT NULL DEFAULT 0 COMMENT '排序',
  `remark`        varchar(500) DEFAULT NULL COMMENT '备注',
  `version`       int          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `status`        tinyint      NOT NULL DEFAULT 1 COMMENT '状态：1=启用 0=停用',
  `create_by`     varchar(64)  DEFAULT NULL COMMENT '创建者',
  `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_warehouse_code` (`warehouse_id`, `code`),
  KEY `idx_warehouse` (`warehouse_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='仓库货架（存放位置）';

-- ② ledger_stock 增加 shelf_id ---------------------------------------
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_stock' AND COLUMN_NAME = 'shelf_id');
SET @sql := IF(@has = 0,
  'ALTER TABLE `ledger_stock` ADD COLUMN `shelf_id` bigint NOT NULL DEFAULT 0 COMMENT ''货架ID，0=未上架'' AFTER `product_id`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 唯一键切换：先删旧键（若存在），再加新键（若不存在）
SET @has := (SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_stock' AND INDEX_NAME = 'uk_warehouse_product');
SET @sql := IF(@has > 0,
  'ALTER TABLE `ledger_stock` DROP INDEX `uk_warehouse_product`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has := (SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_stock' AND INDEX_NAME = 'uk_product_shelf');
SET @sql := IF(@has = 0,
  'ALTER TABLE `ledger_stock` ADD UNIQUE KEY `uk_product_shelf` (`product_id`, `shelf_id`)',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ③ ledger_stock_flow 增加货架维度 ------------------------------------
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_stock_flow' AND COLUMN_NAME = 'shelf_id');
SET @sql := IF(@has = 0,
  'ALTER TABLE `ledger_stock_flow` ADD COLUMN `shelf_id` bigint NOT NULL DEFAULT 0 COMMENT ''本次操作货架（移库时为源货架），0=未上架'' AFTER `product_id`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_stock_flow' AND COLUMN_NAME = 'to_shelf_id');
SET @sql := IF(@has = 0,
  'ALTER TABLE `ledger_stock_flow` ADD COLUMN `to_shelf_id` bigint NOT NULL DEFAULT 0 COMMENT ''移库目标货架（非移库为0）'' AFTER `shelf_id`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
