-- ============================================================
-- 仓库常用配置：独立表（替代 portal_user.warehouse_cfg 单列）
--
-- 为什么用表而不是用户表加一列：
--   ① 配置项会长（现在是 6 项，以后可能加过期提醒开关、默认货架等）
--   ② 可以按仓库维度存（不同仓库不同的低库存阈值）
--   ③ key-value 结构，加配置不用改表结构
--
-- warehouse_id = 0 表示「该用户的全局配置」（跨仓库共用）。
--
-- 幂等：CREATE TABLE IF NOT EXISTS + information_schema 守卫。
-- ============================================================

CREATE TABLE IF NOT EXISTS `ledger_warehouse_config` (
  `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`      bigint       NOT NULL COMMENT '归属用户',
  `warehouse_id` bigint       NOT NULL DEFAULT 0 COMMENT '仓库ID，0=用户全局配置',
  `cfg_key`      varchar(50)  NOT NULL COMMENT '配置键：lowStock/showValue/defaultReason/pageSize/showTags/showExpiry',
  `cfg_value`    varchar(500) DEFAULT NULL COMMENT '配置值（字符串/JSON）',
  `create_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_wh_key` (`user_id`, `warehouse_id`, `cfg_key`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='仓库常用配置';

-- 回收上一步误加到 portal_user 的单列（配置已改由独立表承载）
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_user' AND COLUMN_NAME = 'warehouse_cfg');
SET @sql := IF(@has = 1,
  'ALTER TABLE `portal_user` DROP COLUMN `warehouse_cfg`',
  'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
