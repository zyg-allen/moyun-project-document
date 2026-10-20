-- ============================================================
-- 记账模块主键统一为雪花 ID（MyBatis-Plus IdType.ASSIGN_ID）
-- 只覆盖 ledger_* 表；其它模块（sys_*/portal_*/ai_*/pay_*/vip_*）保持自增不动
--
-- 背景：雪花 ID 自带时间戳，便于流水按编号排序/排查，扩展性更好
-- 实体侧：@TableId(type = IdType.ASSIGN_ID)
-- 全局：mybatis-plus.global-config.db-config.id-type = assign_id
-- ============================================================

-- ledger_ai_analysis_report
SET @sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_ai_analysis_report' AND COLUMN_NAME = 'id' AND EXTRA LIKE '%auto_increment%') > 0,
    'ALTER TABLE `ledger_ai_analysis_report` MODIFY COLUMN `id` bigint NOT NULL', 'SELECT ''ledger_ai_analysis_report 已是非自增'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ledger_app_feature_config
SET @sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_app_feature_config' AND COLUMN_NAME = 'id' AND EXTRA LIKE '%auto_increment%') > 0,
    'ALTER TABLE `ledger_app_feature_config` MODIFY COLUMN `id` bigint NOT NULL', 'SELECT ''ledger_app_feature_config 已是非自增'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ledger_asset_account
SET @sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_asset_account' AND COLUMN_NAME = 'id' AND EXTRA LIKE '%auto_increment%') > 0,
    'ALTER TABLE `ledger_asset_account` MODIFY COLUMN `id` bigint NOT NULL', 'SELECT ''ledger_asset_account 已是非自增'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ledger_budget
SET @sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_budget' AND COLUMN_NAME = 'id' AND EXTRA LIKE '%auto_increment%') > 0,
    'ALTER TABLE `ledger_budget` MODIFY COLUMN `id` bigint NOT NULL', 'SELECT ''ledger_budget 已是非自增'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ledger_category
SET @sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_category' AND COLUMN_NAME = 'id' AND EXTRA LIKE '%auto_increment%') > 0,
    'ALTER TABLE `ledger_category` MODIFY COLUMN `id` bigint NOT NULL', 'SELECT ''ledger_category 已是非自增'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ledger_invite
SET @sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_invite' AND COLUMN_NAME = 'id' AND EXTRA LIKE '%auto_increment%') > 0,
    'ALTER TABLE `ledger_invite` MODIFY COLUMN `id` bigint NOT NULL', 'SELECT ''ledger_invite 已是非自增'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ledger_liability_account
SET @sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_liability_account' AND COLUMN_NAME = 'id' AND EXTRA LIKE '%auto_increment%') > 0,
    'ALTER TABLE `ledger_liability_account` MODIFY COLUMN `id` bigint NOT NULL', 'SELECT ''ledger_liability_account 已是非自增'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ledger_memo
SET @sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_memo' AND COLUMN_NAME = 'id' AND EXTRA LIKE '%auto_increment%') > 0,
    'ALTER TABLE `ledger_memo` MODIFY COLUMN `id` bigint NOT NULL', 'SELECT ''ledger_memo 已是非自增'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ledger_net_worth_snapshot
SET @sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_net_worth_snapshot' AND COLUMN_NAME = 'id' AND EXTRA LIKE '%auto_increment%') > 0,
    'ALTER TABLE `ledger_net_worth_snapshot` MODIFY COLUMN `id` bigint NOT NULL', 'SELECT ''ledger_net_worth_snapshot 已是非自增'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ledger_product
SET @sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_product' AND COLUMN_NAME = 'id' AND EXTRA LIKE '%auto_increment%') > 0,
    'ALTER TABLE `ledger_product` MODIFY COLUMN `id` bigint NOT NULL', 'SELECT ''ledger_product 已是非自增'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ledger_product_category
SET @sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_product_category' AND COLUMN_NAME = 'id' AND EXTRA LIKE '%auto_increment%') > 0,
    'ALTER TABLE `ledger_product_category` MODIFY COLUMN `id` bigint NOT NULL', 'SELECT ''ledger_product_category 已是非自增'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ledger_product_channel
SET @sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_product_channel' AND COLUMN_NAME = 'id' AND EXTRA LIKE '%auto_increment%') > 0,
    'ALTER TABLE `ledger_product_channel` MODIFY COLUMN `id` bigint NOT NULL', 'SELECT ''ledger_product_channel 已是非自增'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ledger_purchase_order
SET @sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_purchase_order' AND COLUMN_NAME = 'id' AND EXTRA LIKE '%auto_increment%') > 0,
    'ALTER TABLE `ledger_purchase_order` MODIFY COLUMN `id` bigint NOT NULL', 'SELECT ''ledger_purchase_order 已是非自增'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ledger_purchase_order_item
SET @sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_purchase_order_item' AND COLUMN_NAME = 'id' AND EXTRA LIKE '%auto_increment%') > 0,
    'ALTER TABLE `ledger_purchase_order_item` MODIFY COLUMN `id` bigint NOT NULL', 'SELECT ''ledger_purchase_order_item 已是非自增'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ledger_sale_channel
SET @sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_sale_channel' AND COLUMN_NAME = 'id' AND EXTRA LIKE '%auto_increment%') > 0,
    'ALTER TABLE `ledger_sale_channel` MODIFY COLUMN `id` bigint NOT NULL', 'SELECT ''ledger_sale_channel 已是非自增'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ledger_sale_order
SET @sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_sale_order' AND COLUMN_NAME = 'id' AND EXTRA LIKE '%auto_increment%') > 0,
    'ALTER TABLE `ledger_sale_order` MODIFY COLUMN `id` bigint NOT NULL', 'SELECT ''ledger_sale_order 已是非自增'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ledger_sale_order_item
SET @sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_sale_order_item' AND COLUMN_NAME = 'id' AND EXTRA LIKE '%auto_increment%') > 0,
    'ALTER TABLE `ledger_sale_order_item` MODIFY COLUMN `id` bigint NOT NULL', 'SELECT ''ledger_sale_order_item 已是非自增'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ledger_saving_plan
SET @sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_saving_plan' AND COLUMN_NAME = 'id' AND EXTRA LIKE '%auto_increment%') > 0,
    'ALTER TABLE `ledger_saving_plan` MODIFY COLUMN `id` bigint NOT NULL', 'SELECT ''ledger_saving_plan 已是非自增'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ledger_saving_record
SET @sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_saving_record' AND COLUMN_NAME = 'id' AND EXTRA LIKE '%auto_increment%') > 0,
    'ALTER TABLE `ledger_saving_record` MODIFY COLUMN `id` bigint NOT NULL', 'SELECT ''ledger_saving_record 已是非自增'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ledger_schedule_log
SET @sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_schedule_log' AND COLUMN_NAME = 'id' AND EXTRA LIKE '%auto_increment%') > 0,
    'ALTER TABLE `ledger_schedule_log` MODIFY COLUMN `id` bigint NOT NULL', 'SELECT ''ledger_schedule_log 已是非自增'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ledger_schedule_task
SET @sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_schedule_task' AND COLUMN_NAME = 'id' AND EXTRA LIKE '%auto_increment%') > 0,
    'ALTER TABLE `ledger_schedule_task` MODIFY COLUMN `id` bigint NOT NULL', 'SELECT ''ledger_schedule_task 已是非自增'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ledger_shelf
SET @sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_shelf' AND COLUMN_NAME = 'id' AND EXTRA LIKE '%auto_increment%') > 0,
    'ALTER TABLE `ledger_shelf` MODIFY COLUMN `id` bigint NOT NULL', 'SELECT ''ledger_shelf 已是非自增'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ledger_stock
SET @sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_stock' AND COLUMN_NAME = 'id' AND EXTRA LIKE '%auto_increment%') > 0,
    'ALTER TABLE `ledger_stock` MODIFY COLUMN `id` bigint NOT NULL', 'SELECT ''ledger_stock 已是非自增'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ledger_stock_flow
SET @sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_stock_flow' AND COLUMN_NAME = 'id' AND EXTRA LIKE '%auto_increment%') > 0,
    'ALTER TABLE `ledger_stock_flow` MODIFY COLUMN `id` bigint NOT NULL', 'SELECT ''ledger_stock_flow 已是非自增'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ledger_tip_order
SET @sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_tip_order' AND COLUMN_NAME = 'id' AND EXTRA LIKE '%auto_increment%') > 0,
    'ALTER TABLE `ledger_tip_order` MODIFY COLUMN `id` bigint NOT NULL', 'SELECT ''ledger_tip_order 已是非自增'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ledger_transaction
SET @sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_transaction' AND COLUMN_NAME = 'id' AND EXTRA LIKE '%auto_increment%') > 0,
    'ALTER TABLE `ledger_transaction` MODIFY COLUMN `id` bigint NOT NULL', 'SELECT ''ledger_transaction 已是非自增'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ledger_warehouse
SET @sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_warehouse' AND COLUMN_NAME = 'id' AND EXTRA LIKE '%auto_increment%') > 0,
    'ALTER TABLE `ledger_warehouse` MODIFY COLUMN `id` bigint NOT NULL', 'SELECT ''ledger_warehouse 已是非自增'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ledger_warehouse_config
SET @sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_warehouse_config' AND COLUMN_NAME = 'id' AND EXTRA LIKE '%auto_increment%') > 0,
    'ALTER TABLE `ledger_warehouse_config` MODIFY COLUMN `id` bigint NOT NULL', 'SELECT ''ledger_warehouse_config 已是非自增'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ledger_warehouse_member
SET @sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_warehouse_member' AND COLUMN_NAME = 'id' AND EXTRA LIKE '%auto_increment%') > 0,
    'ALTER TABLE `ledger_warehouse_member` MODIFY COLUMN `id` bigint NOT NULL', 'SELECT ''ledger_warehouse_member 已是非自增'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

