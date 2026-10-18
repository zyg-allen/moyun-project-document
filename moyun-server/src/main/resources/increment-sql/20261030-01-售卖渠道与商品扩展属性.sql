-- ============================================================
-- 库存：售卖渠道 + 商品扩展属性 + 商品渠道关联
--
-- 维度：仓库 → 货架 → 分类 → 商品 → 渠道
--
-- ① ledger_sale_channel  售卖渠道（系统预设 + 用户自定义）
--    出库(卖出)时记录「哪个平台卖的」，作为统计维度
-- ② ledger_product_channel  商品可售渠道（1:N）
--    一个商品可以同时在抖音/淘宝/闲鱼上架
-- ③ ledger_product 扩展：生产日期 / 保质期天数 / 到期日期 /
--    包装类型 / 标签 / 描述（建议售价复用 sell_price，成本复用 cost_price）
--
-- 幂等：information_schema 守卫 + NOT EXISTS 种子。
-- ============================================================

-- ① 售卖渠道 ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ledger_sale_channel` (
  `id`         bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`    bigint      NOT NULL DEFAULT 0 COMMENT '归属用户，0=系统预设',
  `name`       varchar(50) NOT NULL COMMENT '渠道名称（抖音/淘宝/闲鱼…）',
  `icon`       varchar(255) DEFAULT NULL COMMENT '图标：emoji key 或图片URL',
  `sort_order` int         NOT NULL DEFAULT 0 COMMENT '排序',
  `is_system`  tinyint     NOT NULL DEFAULT 0 COMMENT '1=系统预设 0=用户自建',
  `status`     tinyint     NOT NULL DEFAULT 1 COMMENT '状态：1=启用 0=停用',
  `create_time` datetime   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_name` (`user_id`, `name`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='售卖渠道';

-- ② 商品-渠道 关联（1:N） --------------------------------------------
CREATE TABLE IF NOT EXISTS `ledger_product_channel` (
  `id`         bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `channel_id` bigint NOT NULL COMMENT '渠道ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_channel` (`product_id`, `channel_id`),
  KEY `idx_channel` (`channel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品可售渠道';

-- ③ 商品扩展属性 -----------------------------------------------------
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_product' AND COLUMN_NAME = 'production_date');
SET @sql := IF(@has = 0,
  'ALTER TABLE `ledger_product` ADD COLUMN `production_date` date DEFAULT NULL COMMENT ''生产日期'' AFTER `sell_price`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_product' AND COLUMN_NAME = 'shelf_life_days');
SET @sql := IF(@has = 0,
  'ALTER TABLE `ledger_product` ADD COLUMN `shelf_life_days` int DEFAULT NULL COMMENT ''保质期天数'' AFTER `production_date`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_product' AND COLUMN_NAME = 'expiry_date');
SET @sql := IF(@has = 0,
  'ALTER TABLE `ledger_product` ADD COLUMN `expiry_date` date DEFAULT NULL COMMENT ''到期日期（生产日期+保质期，可手改）'' AFTER `shelf_life_days`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_product' AND COLUMN_NAME = 'packaging_type');
SET @sql := IF(@has = 0,
  'ALTER TABLE `ledger_product` ADD COLUMN `packaging_type` varchar(20) NOT NULL DEFAULT ''none'' COMMENT ''包装类型：none无需 standard标准 fragile防摔 custom特殊'' AFTER `expiry_date`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_product' AND COLUMN_NAME = 'tags');
SET @sql := IF(@has = 0,
  'ALTER TABLE `ledger_product` ADD COLUMN `tags` varchar(500) DEFAULT NULL COMMENT ''标签，逗号分隔'' AFTER `packaging_type`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_product' AND COLUMN_NAME = 'description');
SET @sql := IF(@has = 0,
  'ALTER TABLE `ledger_product` ADD COLUMN `description` varchar(1000) DEFAULT NULL COMMENT ''商品描述'' AFTER `tags`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ④ 系统预设渠道 -----------------------------------------------------
INSERT INTO `ledger_sale_channel` (`user_id`, `name`, `icon`, `sort_order`, `is_system`, `status`)
SELECT 0, '抖音', 'douyin', 10, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ledger_sale_channel` WHERE `user_id`=0 AND `name`='抖音');
INSERT INTO `ledger_sale_channel` (`user_id`, `name`, `icon`, `sort_order`, `is_system`, `status`)
SELECT 0, '淘宝', 'taobao', 20, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ledger_sale_channel` WHERE `user_id`=0 AND `name`='淘宝');
INSERT INTO `ledger_sale_channel` (`user_id`, `name`, `icon`, `sort_order`, `is_system`, `status`)
SELECT 0, '拼多多', 'pdd', 30, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ledger_sale_channel` WHERE `user_id`=0 AND `name`='拼多多');
INSERT INTO `ledger_sale_channel` (`user_id`, `name`, `icon`, `sort_order`, `is_system`, `status`)
SELECT 0, '闲鱼', 'xianyu', 40, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ledger_sale_channel` WHERE `user_id`=0 AND `name`='闲鱼');
INSERT INTO `ledger_sale_channel` (`user_id`, `name`, `icon`, `sort_order`, `is_system`, `status`)
SELECT 0, '小红书', 'xhs', 50, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ledger_sale_channel` WHERE `user_id`=0 AND `name`='小红书');
INSERT INTO `ledger_sale_channel` (`user_id`, `name`, `icon`, `sort_order`, `is_system`, `status`)
SELECT 0, '微信', 'wechat', 60, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ledger_sale_channel` WHERE `user_id`=0 AND `name`='微信');
INSERT INTO `ledger_sale_channel` (`user_id`, `name`, `icon`, `sort_order`, `is_system`, `status`)
SELECT 0, '线下', 'offline', 70, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ledger_sale_channel` WHERE `user_id`=0 AND `name`='线下');
