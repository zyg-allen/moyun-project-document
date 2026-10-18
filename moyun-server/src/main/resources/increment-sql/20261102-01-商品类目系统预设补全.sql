-- ============================================================
-- 商品类目：系统预设补全（一级图标改 emoji + 补二级）
--
-- 原预设的 icon 存的是 'food'/'drink' 这类 key，但前端按 emoji/图片渲染，
-- 结果页面上显示成「food」「drink」文字。这里统一改成 emoji。
--
-- 幂等：全部用 NOT EXISTS 守卫，重复执行不会插重复行。
-- ============================================================

-- ① 一级图标改为 emoji
UPDATE `ledger_product_category` SET `icon` = '🍱' WHERE `user_id`=0 AND `name`='食品';
UPDATE `ledger_product_category` SET `icon` = '🥤' WHERE `user_id`=0 AND `name`='饮料';
UPDATE `ledger_product_category` SET `icon` = '🧴' WHERE `user_id`=0 AND `name`='日用品';
UPDATE `ledger_product_category` SET `icon` = '📱' WHERE `user_id`=0 AND `name`='电子产品';
UPDATE `ledger_product_category` SET `icon` = '👕' WHERE `user_id`=0 AND `name`='服装鞋帽';
UPDATE `ledger_product_category` SET `icon` = '📦' WHERE `user_id`=0 AND `name`='其他';

-- ② 补二级（挂到对应一级下）
SET @p := (SELECT `id` FROM `ledger_product_category` WHERE `user_id`=0 AND `parent_id`=0 AND `name`='食品' LIMIT 1);
INSERT INTO `ledger_product_category` (`user_id`,`parent_id`,`name`,`icon`,`sort_order`,`is_system`)
SELECT 0, @p, '零食', '🍿', 10, 1 FROM DUAL
WHERE @p IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `ledger_product_category` WHERE `user_id`=0 AND `parent_id`=@p AND `name`='零食');
INSERT INTO `ledger_product_category` (`user_id`,`parent_id`,`name`,`icon`,`sort_order`,`is_system`)
SELECT 0, @p, '生鲜', '🥬', 20, 1 FROM DUAL
WHERE @p IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `ledger_product_category` WHERE `user_id`=0 AND `parent_id`=@p AND `name`='生鲜');
INSERT INTO `ledger_product_category` (`user_id`,`parent_id`,`name`,`icon`,`sort_order`,`is_system`)
SELECT 0, @p, '米面粮油', '🍚', 30, 1 FROM DUAL
WHERE @p IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `ledger_product_category` WHERE `user_id`=0 AND `parent_id`=@p AND `name`='米面粮油');

SET @p := (SELECT `id` FROM `ledger_product_category` WHERE `user_id`=0 AND `parent_id`=0 AND `name`='饮料' LIMIT 1);
INSERT INTO `ledger_product_category` (`user_id`,`parent_id`,`name`,`icon`,`sort_order`,`is_system`)
SELECT 0, @p, '碳酸饮料', '🥤', 10, 1 FROM DUAL
WHERE @p IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `ledger_product_category` WHERE `user_id`=0 AND `parent_id`=@p AND `name`='碳酸饮料');
INSERT INTO `ledger_product_category` (`user_id`,`parent_id`,`name`,`icon`,`sort_order`,`is_system`)
SELECT 0, @p, '茶饮咖啡', '☕', 20, 1 FROM DUAL
WHERE @p IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `ledger_product_category` WHERE `user_id`=0 AND `parent_id`=@p AND `name`='茶饮咖啡');
INSERT INTO `ledger_product_category` (`user_id`,`parent_id`,`name`,`icon`,`sort_order`,`is_system`)
SELECT 0, @p, '酒水', '🍺', 30, 1 FROM DUAL
WHERE @p IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `ledger_product_category` WHERE `user_id`=0 AND `parent_id`=@p AND `name`='酒水');

SET @p := (SELECT `id` FROM `ledger_product_category` WHERE `user_id`=0 AND `parent_id`=0 AND `name`='日用品' LIMIT 1);
INSERT INTO `ledger_product_category` (`user_id`,`parent_id`,`name`,`icon`,`sort_order`,`is_system`)
SELECT 0, @p, '洗护', '🧼', 10, 1 FROM DUAL
WHERE @p IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `ledger_product_category` WHERE `user_id`=0 AND `parent_id`=@p AND `name`='洗护');
INSERT INTO `ledger_product_category` (`user_id`,`parent_id`,`name`,`icon`,`sort_order`,`is_system`)
SELECT 0, @p, '厨房用品', '🍳', 20, 1 FROM DUAL
WHERE @p IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `ledger_product_category` WHERE `user_id`=0 AND `parent_id`=@p AND `name`='厨房用品');

SET @p := (SELECT `id` FROM `ledger_product_category` WHERE `user_id`=0 AND `parent_id`=0 AND `name`='电子产品' LIMIT 1);
INSERT INTO `ledger_product_category` (`user_id`,`parent_id`,`name`,`icon`,`sort_order`,`is_system`)
SELECT 0, @p, '数码配件', '🔌', 10, 1 FROM DUAL
WHERE @p IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `ledger_product_category` WHERE `user_id`=0 AND `parent_id`=@p AND `name`='数码配件');
INSERT INTO `ledger_product_category` (`user_id`,`parent_id`,`name`,`icon`,`sort_order`,`is_system`)
SELECT 0, @p, '手机平板', '📱', 20, 1 FROM DUAL
WHERE @p IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `ledger_product_category` WHERE `user_id`=0 AND `parent_id`=@p AND `name`='手机平板');

SET @p := (SELECT `id` FROM `ledger_product_category` WHERE `user_id`=0 AND `parent_id`=0 AND `name`='服装鞋帽' LIMIT 1);
INSERT INTO `ledger_product_category` (`user_id`,`parent_id`,`name`,`icon`,`sort_order`,`is_system`)
SELECT 0, @p, '上装', '👔', 10, 1 FROM DUAL
WHERE @p IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `ledger_product_category` WHERE `user_id`=0 AND `parent_id`=@p AND `name`='上装');
INSERT INTO `ledger_product_category` (`user_id`,`parent_id`,`name`,`icon`,`sort_order`,`is_system`)
SELECT 0, @p, '鞋靴', '👟', 20, 1 FROM DUAL
WHERE @p IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `ledger_product_category` WHERE `user_id`=0 AND `parent_id`=@p AND `name`='鞋靴');
