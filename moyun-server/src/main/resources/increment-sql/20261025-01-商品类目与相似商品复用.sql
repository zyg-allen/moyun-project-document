-- ============================================================
-- 记账-库存：商品类目（两级树）+ 商品关联类目 + 系统预设类目种子
--
-- 背景：原先 ledger_product.category 只是自由文本，无法管理、无法筛选。
-- 本次引入独立类目表（仿 ledger_category 的两级树 + 图标），
-- 并把 ledger_product 关联到 category_id；旧的 category 文本保留但不再写入。
--
-- 幂等：所有 DDL 由 information_schema 守卫；种子数据用 NOT EXISTS 防重复。
-- ============================================================

-- ① 商品类目表 --------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ledger_product_category` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     bigint       NOT NULL DEFAULT 0 COMMENT '归属用户，0=系统预设',
  `name`        varchar(50)  NOT NULL COMMENT '类目名称',
  `parent_id`   bigint       NOT NULL DEFAULT 0 COMMENT '父类目ID，0=一级',
  `icon`        varchar(255) DEFAULT NULL COMMENT '图标：emoji key 或图片URL',
  `sort_order`  int          NOT NULL DEFAULT 0 COMMENT '排序',
  `is_system`   tinyint      NOT NULL DEFAULT 0 COMMENT '1=系统预设 0=用户自建',
  `status`      tinyint      NOT NULL DEFAULT 1 COMMENT '状态：1=启用 0=停用',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_parent_name` (`user_id`, `parent_id`, `name`),
  KEY `idx_user` (`user_id`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品类目（两级树）';

-- ② ledger_product 增加 category_id -----------------------------------
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_product' AND COLUMN_NAME = 'category_id');
SET @sql := IF(@has = 0,
  'ALTER TABLE `ledger_product` ADD COLUMN `category_id` bigint DEFAULT NULL COMMENT ''类目ID（ledger_product_category.id）'' AFTER `name`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 类目筛选走索引
SET @has := (SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_product' AND INDEX_NAME = 'idx_category');
SET @sql := IF(@has = 0,
  'ALTER TABLE `ledger_product` ADD KEY `idx_category` (`category_id`)',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ③ 系统预设类目种子（user_id=0, is_system=1）--------------------------
INSERT INTO `ledger_product_category` (`user_id`, `name`, `parent_id`, `icon`, `sort_order`, `is_system`, `status`)
SELECT 0, '食品', 0, 'food', 10, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ledger_product_category` WHERE `user_id`=0 AND `parent_id`=0 AND `name`='食品');

INSERT INTO `ledger_product_category` (`user_id`, `name`, `parent_id`, `icon`, `sort_order`, `is_system`, `status`)
SELECT 0, '饮料', 0, 'drink', 20, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ledger_product_category` WHERE `user_id`=0 AND `parent_id`=0 AND `name`='饮料');

INSERT INTO `ledger_product_category` (`user_id`, `name`, `parent_id`, `icon`, `sort_order`, `is_system`, `status`)
SELECT 0, '日用品', 0, 'daily', 30, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ledger_product_category` WHERE `user_id`=0 AND `parent_id`=0 AND `name`='日用品');

INSERT INTO `ledger_product_category` (`user_id`, `name`, `parent_id`, `icon`, `sort_order`, `is_system`, `status`)
SELECT 0, '电子产品', 0, 'digital', 40, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ledger_product_category` WHERE `user_id`=0 AND `parent_id`=0 AND `name`='电子产品');

INSERT INTO `ledger_product_category` (`user_id`, `name`, `parent_id`, `icon`, `sort_order`, `is_system`, `status`)
SELECT 0, '服装鞋帽', 0, 'clothes', 50, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ledger_product_category` WHERE `user_id`=0 AND `parent_id`=0 AND `name`='服装鞋帽');

INSERT INTO `ledger_product_category` (`user_id`, `name`, `parent_id`, `icon`, `sort_order`, `is_system`, `status`)
SELECT 0, '其他', 0, 'other', 90, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ledger_product_category` WHERE `user_id`=0 AND `parent_id`=0 AND `name`='其他');
