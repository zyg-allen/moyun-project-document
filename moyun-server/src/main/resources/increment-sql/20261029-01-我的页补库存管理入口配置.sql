-- ============================================================
-- 我的页宫格：补「库存管理」「仓库设置」两个入口配置
--
-- 背景：「我的」页宫格由后台配置（ledger_app_feature_config）覆盖内置清单，
-- 后台没有登记的 key 在 App 上直接不显示 —— 新加的这两个入口因此看不见。
--
-- 放在 main 组最前（sort_num 依次前移），visible=1。
-- 幂等：NOT EXISTS 守卫，重复执行不会插重复行。
-- ============================================================

-- ① 库存管理 ---------------------------------------------------------
INSERT INTO `ledger_app_feature_config`
  (`feature_key`, `feature_name`, `icon`, `icon_color`, `group_type`, `sort_num`, `visible`, `status`, `remark`)
SELECT 'warehouse', '库存管理', '📦', '#7fbf94', 'main', -10, 1, 'done', '仓库/货架/商品/库存'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ledger_app_feature_config` WHERE `feature_key` = 'warehouse');

-- ② 仓库设置 ---------------------------------------------------------
INSERT INTO `ledger_app_feature_config`
  (`feature_key`, `feature_name`, `icon`, `icon_color`, `group_type`, `sort_num`, `visible`, `status`, `remark`)
SELECT 'warehouseSetting', '仓库设置', '🏷️', '#7fbf94', 'main', -9, 1, 'done', '仓库开关/商品类目/参数'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ledger_app_feature_config` WHERE `feature_key` = 'warehouseSetting');
