-- =============================================================================
-- 2026-10-21 记账后台管理边界整改：用户数据授权 + 访问审计 + 预设后台化
-- -----------------------------------------------------------------------------
-- 背景：后台此前可直接查看用户流水（未授权）、无访问审计；
--       商品类目 / 售卖渠道 / 进货渠道三张预设表有数据但无后台入口；
--       功能入口无路由字段，无法校验死链。
--
-- 本脚本面向存量库，可重复执行（幂等）：
--   · 建表用 IF NOT EXISTS；
--   · 加列前查 information_schema；
--   · 种子数据逐条 NOT EXISTS；
--   · 菜单按 perms 反查父菜单，不写死 menu_id/parent_id。
-- 全新库请使用 init-sql（本脚本的等价内容已同步进去）。
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 一、用户数据授权（C 档用户数据的唯一准入凭证；后台只读）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `portal_data_auth` (
    `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '授权ID',
    `user_id`       bigint       NOT NULL COMMENT '授权人 portal_user.id',
    `biz_domain`    varchar(20)  NOT NULL DEFAULT 'ledger' COMMENT '业务域：ledger/portal/cms',
    `scopes`        varchar(500) NOT NULL COMMENT '授权范围，逗号分隔：transaction,account,budget,inventory,ai,profile',
    `purpose`       varchar(200) NOT NULL COMMENT '用途（用户可见，后台展示）',
    `channel`       varchar(20)  NOT NULL DEFAULT 'app' COMMENT '发起渠道：app/web',
    `grant_type`    varchar(20)  NOT NULL DEFAULT 'user_initiated' COMMENT '授权方式：user_initiated/requested',
    `doc_version`   varchar(20)  NOT NULL DEFAULT '' COMMENT '隐私政策版本',
    `granted_at`    datetime     NOT NULL COMMENT '授权时间',
    `expire_at`     datetime     DEFAULT NULL COMMENT '到期时间（NULL=长期，可随时撤回）',
    `revoked_at`    datetime     DEFAULT NULL COMMENT '撤回时间（NULL=未撤回）',
    `revoke_reason` varchar(200) DEFAULT NULL COMMENT '撤回原因',
    `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_domain` (`user_id`,`biz_domain`,`revoked_at`),
    KEY `idx_expire` (`expire_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户数据授权（个体数据查看的唯一准入凭证）';

-- ---------------------------------------------------------------------------
-- 二、敏感数据访问日志（只增不改；允许/拒绝都记）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_data_access_log` (
    `id`             bigint      NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `operator_id`    bigint      NOT NULL COMMENT '操作人 sys_user.user_id',
    `operator_name`  varchar(64) NOT NULL COMMENT '操作人账号',
    `target_user_id` bigint      NOT NULL COMMENT '被查看用户 portal_user.id',
    `biz_domain`     varchar(20) NOT NULL DEFAULT 'ledger' COMMENT '业务域',
    `scope`          varchar(50) NOT NULL COMMENT '访问的授权范围',
    `auth_id`        bigint      DEFAULT NULL COMMENT '关联授权ID（拒绝时为空）',
    `action`         varchar(20) NOT NULL DEFAULT 'query' COMMENT '动作：query/export',
    `query_desc`     varchar(500) DEFAULT NULL COMMENT '查询条件与条数（不含字段值）',
    `result`         varchar(20) NOT NULL COMMENT '结果：allowed/denied',
    `client_ip`      varchar(64) DEFAULT NULL COMMENT '客户端IP',
    `access_time`    datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
    PRIMARY KEY (`id`),
    KEY `idx_target_time` (`target_user_id`,`access_time`),
    KEY `idx_operator_time` (`operator_id`,`access_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='敏感数据访问日志（只增不改）';

-- ---------------------------------------------------------------------------
-- 三、功能入口增加 route 列（App 路由白名单，避免死链）
-- ---------------------------------------------------------------------------
SET @has_route := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ledger_app_feature_config' AND COLUMN_NAME = 'route');
SET @sql_route := IF(@has_route = 0,
    'ALTER TABLE `ledger_app_feature_config` ADD COLUMN `route` varchar(200) DEFAULT NULL COMMENT ''App 路由（须在后台路由白名单内；空=非跳转型入口）'' AFTER `badge`',
    'DO 0');
PREPARE stmt FROM @sql_route; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 四、预设表补齐（存量库若由历史增量脚本建过表，这里只补种子）
-- ---------------------------------------------------------------------------
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

CREATE TABLE IF NOT EXISTS `ledger_source_channel` (
    `id`          bigint       NOT NULL COMMENT '主键（雪花ID）',
    `user_id`     bigint       NOT NULL COMMENT '归属用户（0=系统预设）',
    `name`        varchar(50)  NOT NULL COMMENT '渠道名称',
    `icon`        varchar(255) DEFAULT NULL COMMENT '图标（emoji 或图片地址）',
    `sort_order`  int          NOT NULL DEFAULT 0 COMMENT '排序',
    `is_system`   tinyint      NOT NULL DEFAULT 0 COMMENT '是否系统预设：1=是 0=用户自建',
    `status`      tinyint      NOT NULL DEFAULT 1 COMMENT '状态：1=启用 0=停用',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_src_channel_user` (`user_id`),
    KEY `idx_src_channel_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='进货渠道';

-- 商品类目种子（6 个一级 + 12 个二级）
INSERT INTO `ledger_product_category` (`name`,`user_id`,`parent_id`,`icon`,`sort_order`,`is_system`,`status`)
SELECT '食品', 0, 0, '🍱', 10, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ledger_product_category` WHERE `user_id`=0 AND `parent_id`=0 AND `name`='食品');
INSERT INTO `ledger_product_category` (`name`,`user_id`,`parent_id`,`icon`,`sort_order`,`is_system`,`status`)
SELECT '饮料', 0, 0, '🥤', 20, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ledger_product_category` WHERE `user_id`=0 AND `parent_id`=0 AND `name`='饮料');
INSERT INTO `ledger_product_category` (`name`,`user_id`,`parent_id`,`icon`,`sort_order`,`is_system`,`status`)
SELECT '日用品', 0, 0, '🧴', 30, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ledger_product_category` WHERE `user_id`=0 AND `parent_id`=0 AND `name`='日用品');
INSERT INTO `ledger_product_category` (`name`,`user_id`,`parent_id`,`icon`,`sort_order`,`is_system`,`status`)
SELECT '电子产品', 0, 0, '📱', 40, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ledger_product_category` WHERE `user_id`=0 AND `parent_id`=0 AND `name`='电子产品');
INSERT INTO `ledger_product_category` (`name`,`user_id`,`parent_id`,`icon`,`sort_order`,`is_system`,`status`)
SELECT '服装鞋帽', 0, 0, '👕', 50, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ledger_product_category` WHERE `user_id`=0 AND `parent_id`=0 AND `name`='服装鞋帽');
INSERT INTO `ledger_product_category` (`name`,`user_id`,`parent_id`,`icon`,`sort_order`,`is_system`,`status`)
SELECT '其他', 0, 0, '📦', 90, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ledger_product_category` WHERE `user_id`=0 AND `parent_id`=0 AND `name`='其他');

INSERT INTO `ledger_product_category` (`name`,`user_id`,`parent_id`,`icon`,`sort_order`,`is_system`,`status`)
SELECT '零食', 0, p.`id`, '🍿', 10, 1, 1 FROM `ledger_product_category` p
WHERE p.`user_id`=0 AND p.`parent_id`=0 AND p.`name`='食品'
  AND NOT EXISTS (SELECT 1 FROM `ledger_product_category` x WHERE x.`user_id`=0 AND x.`parent_id`=p.`id` AND x.`name`='零食');
INSERT INTO `ledger_product_category` (`name`,`user_id`,`parent_id`,`icon`,`sort_order`,`is_system`,`status`)
SELECT '生鲜', 0, p.`id`, '🥬', 20, 1, 1 FROM `ledger_product_category` p
WHERE p.`user_id`=0 AND p.`parent_id`=0 AND p.`name`='食品'
  AND NOT EXISTS (SELECT 1 FROM `ledger_product_category` x WHERE x.`user_id`=0 AND x.`parent_id`=p.`id` AND x.`name`='生鲜');
INSERT INTO `ledger_product_category` (`name`,`user_id`,`parent_id`,`icon`,`sort_order`,`is_system`,`status`)
SELECT '米面粮油', 0, p.`id`, '🍚', 30, 1, 1 FROM `ledger_product_category` p
WHERE p.`user_id`=0 AND p.`parent_id`=0 AND p.`name`='食品'
  AND NOT EXISTS (SELECT 1 FROM `ledger_product_category` x WHERE x.`user_id`=0 AND x.`parent_id`=p.`id` AND x.`name`='米面粮油');
INSERT INTO `ledger_product_category` (`name`,`user_id`,`parent_id`,`icon`,`sort_order`,`is_system`,`status`)
SELECT '碳酸饮料', 0, p.`id`, '🥤', 10, 1, 1 FROM `ledger_product_category` p
WHERE p.`user_id`=0 AND p.`parent_id`=0 AND p.`name`='饮料'
  AND NOT EXISTS (SELECT 1 FROM `ledger_product_category` x WHERE x.`user_id`=0 AND x.`parent_id`=p.`id` AND x.`name`='碳酸饮料');
INSERT INTO `ledger_product_category` (`name`,`user_id`,`parent_id`,`icon`,`sort_order`,`is_system`,`status`)
SELECT '茶饮咖啡', 0, p.`id`, '☕', 20, 1, 1 FROM `ledger_product_category` p
WHERE p.`user_id`=0 AND p.`parent_id`=0 AND p.`name`='饮料'
  AND NOT EXISTS (SELECT 1 FROM `ledger_product_category` x WHERE x.`user_id`=0 AND x.`parent_id`=p.`id` AND x.`name`='茶饮咖啡');
INSERT INTO `ledger_product_category` (`name`,`user_id`,`parent_id`,`icon`,`sort_order`,`is_system`,`status`)
SELECT '酒水', 0, p.`id`, '🍺', 30, 1, 1 FROM `ledger_product_category` p
WHERE p.`user_id`=0 AND p.`parent_id`=0 AND p.`name`='饮料'
  AND NOT EXISTS (SELECT 1 FROM `ledger_product_category` x WHERE x.`user_id`=0 AND x.`parent_id`=p.`id` AND x.`name`='酒水');
INSERT INTO `ledger_product_category` (`name`,`user_id`,`parent_id`,`icon`,`sort_order`,`is_system`,`status`)
SELECT '洗护', 0, p.`id`, '🧼', 10, 1, 1 FROM `ledger_product_category` p
WHERE p.`user_id`=0 AND p.`parent_id`=0 AND p.`name`='日用品'
  AND NOT EXISTS (SELECT 1 FROM `ledger_product_category` x WHERE x.`user_id`=0 AND x.`parent_id`=p.`id` AND x.`name`='洗护');
INSERT INTO `ledger_product_category` (`name`,`user_id`,`parent_id`,`icon`,`sort_order`,`is_system`,`status`)
SELECT '厨房用品', 0, p.`id`, '🍳', 20, 1, 1 FROM `ledger_product_category` p
WHERE p.`user_id`=0 AND p.`parent_id`=0 AND p.`name`='日用品'
  AND NOT EXISTS (SELECT 1 FROM `ledger_product_category` x WHERE x.`user_id`=0 AND x.`parent_id`=p.`id` AND x.`name`='厨房用品');
INSERT INTO `ledger_product_category` (`name`,`user_id`,`parent_id`,`icon`,`sort_order`,`is_system`,`status`)
SELECT '数码配件', 0, p.`id`, '🔌', 10, 1, 1 FROM `ledger_product_category` p
WHERE p.`user_id`=0 AND p.`parent_id`=0 AND p.`name`='电子产品'
  AND NOT EXISTS (SELECT 1 FROM `ledger_product_category` x WHERE x.`user_id`=0 AND x.`parent_id`=p.`id` AND x.`name`='数码配件');
INSERT INTO `ledger_product_category` (`name`,`user_id`,`parent_id`,`icon`,`sort_order`,`is_system`,`status`)
SELECT '手机平板', 0, p.`id`, '📱', 20, 1, 1 FROM `ledger_product_category` p
WHERE p.`user_id`=0 AND p.`parent_id`=0 AND p.`name`='电子产品'
  AND NOT EXISTS (SELECT 1 FROM `ledger_product_category` x WHERE x.`user_id`=0 AND x.`parent_id`=p.`id` AND x.`name`='手机平板');
INSERT INTO `ledger_product_category` (`name`,`user_id`,`parent_id`,`icon`,`sort_order`,`is_system`,`status`)
SELECT '上装', 0, p.`id`, '👔', 10, 1, 1 FROM `ledger_product_category` p
WHERE p.`user_id`=0 AND p.`parent_id`=0 AND p.`name`='服装鞋帽'
  AND NOT EXISTS (SELECT 1 FROM `ledger_product_category` x WHERE x.`user_id`=0 AND x.`parent_id`=p.`id` AND x.`name`='上装');
INSERT INTO `ledger_product_category` (`name`,`user_id`,`parent_id`,`icon`,`sort_order`,`is_system`,`status`)
SELECT '鞋靴', 0, p.`id`, '👟', 20, 1, 1 FROM `ledger_product_category` p
WHERE p.`user_id`=0 AND p.`parent_id`=0 AND p.`name`='服装鞋帽'
  AND NOT EXISTS (SELECT 1 FROM `ledger_product_category` x WHERE x.`user_id`=0 AND x.`parent_id`=p.`id` AND x.`name`='鞋靴');

-- 售卖渠道种子
INSERT INTO `ledger_sale_channel` (`name`,`user_id`,`icon`,`sort_order`,`is_system`,`status`)
SELECT '抖音', 0, 'douyin', 10, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ledger_sale_channel` WHERE `user_id`=0 AND `name`='抖音');
INSERT INTO `ledger_sale_channel` (`name`,`user_id`,`icon`,`sort_order`,`is_system`,`status`)
SELECT '淘宝', 0, 'taobao', 20, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ledger_sale_channel` WHERE `user_id`=0 AND `name`='淘宝');
INSERT INTO `ledger_sale_channel` (`name`,`user_id`,`icon`,`sort_order`,`is_system`,`status`)
SELECT '拼多多', 0, 'pdd', 30, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ledger_sale_channel` WHERE `user_id`=0 AND `name`='拼多多');
INSERT INTO `ledger_sale_channel` (`name`,`user_id`,`icon`,`sort_order`,`is_system`,`status`)
SELECT '闲鱼', 0, 'xianyu', 40, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ledger_sale_channel` WHERE `user_id`=0 AND `name`='闲鱼');
INSERT INTO `ledger_sale_channel` (`name`,`user_id`,`icon`,`sort_order`,`is_system`,`status`)
SELECT '小红书', 0, 'xhs', 50, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ledger_sale_channel` WHERE `user_id`=0 AND `name`='小红书');
INSERT INTO `ledger_sale_channel` (`name`,`user_id`,`icon`,`sort_order`,`is_system`,`status`)
SELECT '微信', 0, 'wechat', 60, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ledger_sale_channel` WHERE `user_id`=0 AND `name`='微信');
INSERT INTO `ledger_sale_channel` (`name`,`user_id`,`icon`,`sort_order`,`is_system`,`status`)
SELECT '线下', 0, 'offline', 70, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ledger_sale_channel` WHERE `user_id`=0 AND `name`='线下');

-- 进货渠道种子（显式 id，避免与雪花主键策略冲突）
INSERT INTO `ledger_source_channel` (`name`,`id`,`user_id`,`sort_order`,`is_system`,`status`)
SELECT '拼多多', 9001, 0, 1, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ledger_source_channel` WHERE `user_id`=0 AND `name`='拼多多');
INSERT INTO `ledger_source_channel` (`name`,`id`,`user_id`,`sort_order`,`is_system`,`status`)
SELECT '闲鱼', 9002, 0, 2, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ledger_source_channel` WHERE `user_id`=0 AND `name`='闲鱼');
INSERT INTO `ledger_source_channel` (`name`,`id`,`user_id`,`sort_order`,`is_system`,`status`)
SELECT '淘宝', 9003, 0, 3, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ledger_source_channel` WHERE `user_id`=0 AND `name`='淘宝');
INSERT INTO `ledger_source_channel` (`name`,`id`,`user_id`,`sort_order`,`is_system`,`status`)
SELECT '批发市场', 9004, 0, 4, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ledger_source_channel` WHERE `user_id`=0 AND `name`='批发市场');
INSERT INTO `ledger_source_channel` (`name`,`id`,`user_id`,`sort_order`,`is_system`,`status`)
SELECT '朋友赠', 9005, 0, 5, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ledger_source_channel` WHERE `user_id`=0 AND `name`='朋友赠');

-- ---------------------------------------------------------------------------
-- 五、功能入口路由绑定 + 死链收敛（幂等）
-- ---------------------------------------------------------------------------
UPDATE `ledger_app_feature_config` SET `route`='/pages/mine/categories/index' WHERE `feature_key`='category';
UPDATE `ledger_app_feature_config` SET `route`='/pages/mine/settings/index' WHERE `feature_key`='setting';
UPDATE `ledger_app_feature_config` SET `route`='/pages/mine/savings/index' WHERE `feature_key`='savings';
UPDATE `ledger_app_feature_config` SET `route`='/pages/mine/schedule/index' WHERE `feature_key`='schedule';
UPDATE `ledger_app_feature_config` SET `route`='/pages/mine/feedback/index' WHERE `feature_key`='feedback';
UPDATE `ledger_app_feature_config` SET `route`='/pages/mine/memo/index' WHERE `feature_key`='memo';
UPDATE `ledger_app_feature_config` SET `route`='/pages/record/list' WHERE `feature_key`='list';
UPDATE `ledger_app_feature_config` SET `route`='/pages/warehouse/index' WHERE `feature_key`='warehouse';
UPDATE `ledger_app_feature_config` SET `route`='/pages/mine/warehouse-settings/index' WHERE `feature_key`='warehouseSetting';
UPDATE `ledger_app_feature_config` SET `route`='/pages/mine/product-categories/index' WHERE `feature_key`='productCategory';
UPDATE `ledger_app_feature_config` SET `visible`=0, `route`=NULL WHERE `feature_key` IN ('tip','personalize','catIcon');

-- ---------------------------------------------------------------------------
-- 六、后台菜单（按 perms 反查父菜单，不写死 id）
-- ---------------------------------------------------------------------------
UPDATE `sys_menu` SET `menu_name`='用户数据',
    `remark`='用户维度聚合 + 授权后流水（默认脱敏，查看留痕）'
WHERE `perms`='cms:ledgerUsers:list';

INSERT INTO `sys_menu` (`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`,`del_flag`)
SELECT '商品类目', p.`menu_id`, 5, 'product-category', 'cms/ledger/productCategory/index', NULL, '', 1, 0, 'C', '0', '0', 'cms:ledgerProductCategory:list', 'tree-table', 'admin', NOW(), '', NULL, '商品类目系统预设（两级树，用户自建只读）', '0'
FROM `sys_menu` p
WHERE p.`path`='ledger-app' AND p.`menu_type`='M'
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` x WHERE x.`perms`='cms:ledgerProductCategory:list')
LIMIT 1;

INSERT INTO `sys_menu` (`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`,`del_flag`)
SELECT '渠道管理', p.`menu_id`, 6, 'channel', 'cms/ledger/channel/index', NULL, '', 1, 0, 'C', '0', '0', 'cms:ledgerChannel:list', 'link', 'admin', NOW(), '', NULL, '售卖/进货渠道系统预设（用户自建只读）', '0'
FROM `sys_menu` p
WHERE p.`path`='ledger-app' AND p.`menu_type`='M'
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` x WHERE x.`perms`='cms:ledgerChannel:list')
LIMIT 1;

INSERT INTO `sys_menu` (`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`,`del_flag`)
SELECT '授权与审计', p.`menu_id`, 7, 'auth', 'cms/ledger/auth/index', NULL, '', 1, 0, 'C', '0', '0', 'cms:ledgerAuth:list', 'lock', 'admin', NOW(), '', NULL, '用户数据授权记录 + 敏感数据访问日志（只读）', '0'
FROM `sys_menu` p
WHERE p.`path`='ledger-app' AND p.`menu_type`='M'
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` x WHERE x.`perms`='cms:ledgerAuth:list')
LIMIT 1;

INSERT INTO `sys_menu` (`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`update_by`,`update_time`,`remark`,`del_flag`)
SELECT '记账字典', p.`menu_id`, 8, 'dict', 'cms/ledger/dict/index', NULL, '', 1, 0, 'C', '0', '0', 'cms:ledgerDict:list', 'dict', 'admin', NOW(), '', NULL, '记账下拉字典一览（维护跳转系统字典，取值锁定）', '0'
FROM `sys_menu` p
WHERE p.`path`='ledger-app' AND p.`menu_type`='M'
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` x WHERE x.`perms`='cms:ledgerDict:list')
LIMIT 1;
-- 门户用户「查看联系方式」按钮（父菜单按 perms 反查）
INSERT INTO `sys_menu` (`menu_name`,`parent_id`,`order_num`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`remark`)
SELECT '查看联系方式', p.`menu_id`, 6, 'F', '0', '0', 'cms:user:contact', '#', 'admin', NOW(), '手机号/邮箱明文（留痕）'
FROM `sys_menu` p
WHERE p.`perms`='cms:user:list'
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` x WHERE x.`perms`='cms:user:contact')
LIMIT 1;
-- 按钮权限
INSERT INTO `sys_menu` (`menu_name`,`parent_id`,`order_num`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`)
SELECT '入口新增', p.`menu_id`, 2, 'F', '0', '0', 'cms:ledgerAppFeature:add', '#', 'admin', NOW()
FROM `sys_menu` p WHERE p.`perms`='cms:ledgerAppFeature:list'
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` x WHERE x.`perms`='cms:ledgerAppFeature:add') LIMIT 1;


INSERT INTO `sys_menu` (`menu_name`,`parent_id`,`order_num`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`)
SELECT '类目新增', p.`menu_id`, 2, 'F', '0', '0', 'cms:ledgerProductCategory:add', '#', 'admin', NOW()
FROM `sys_menu` p WHERE p.`perms`='cms:ledgerProductCategory:list'
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` x WHERE x.`perms`='cms:ledgerProductCategory:add') LIMIT 1;
INSERT INTO `sys_menu` (`menu_name`,`parent_id`,`order_num`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`)
SELECT '类目修改', p.`menu_id`, 3, 'F', '0', '0', 'cms:ledgerProductCategory:edit', '#', 'admin', NOW()
FROM `sys_menu` p WHERE p.`perms`='cms:ledgerProductCategory:list'
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` x WHERE x.`perms`='cms:ledgerProductCategory:edit') LIMIT 1;

INSERT INTO `sys_menu` (`menu_name`,`parent_id`,`order_num`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`)
SELECT '渠道新增', p.`menu_id`, 2, 'F', '0', '0', 'cms:ledgerChannel:add', '#', 'admin', NOW()
FROM `sys_menu` p WHERE p.`perms`='cms:ledgerChannel:list'
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` x WHERE x.`perms`='cms:ledgerChannel:add') LIMIT 1;
INSERT INTO `sys_menu` (`menu_name`,`parent_id`,`order_num`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`)
SELECT '渠道修改', p.`menu_id`, 3, 'F', '0', '0', 'cms:ledgerChannel:edit', '#', 'admin', NOW()
FROM `sys_menu` p WHERE p.`perms`='cms:ledgerChannel:list'
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` x WHERE x.`perms`='cms:ledgerChannel:edit') LIMIT 1;

INSERT INTO `sys_menu` (`menu_name`,`parent_id`,`order_num`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`)
SELECT '访问日志查询', p.`menu_id`, 2, 'F', '0', '0', 'cms:ledgerAccessLog:list', '#', 'admin', NOW()
FROM `sys_menu` p WHERE p.`perms`='cms:ledgerAuth:list'
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` x WHERE x.`perms`='cms:ledgerAccessLog:list') LIMIT 1;

-- ---------------------------------------------------------------------------
-- 七、超级管理员授权（与 menu id 无关）
-- ---------------------------------------------------------------------------
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, m.`menu_id` FROM `sys_menu` m
WHERE m.`perms` LIKE 'cms:ledger%'
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.`role_id`=1 AND rm.`menu_id`=m.`menu_id`);

-- =============================================================================
-- 记账字典（2026-10-21）：App 硬编码下拉改为字典驱动（14 个类型 / 90 条取值）
-- 值锁定见 SysDictDataServiceImpl：记账字典禁止改 dict_value；交易类型与账户类型禁止新增取值。
-- 本段幂等：类型按 dict_type、数据按 (dict_type, dict_value) 判重。
-- =============================================================================
INSERT INTO `sys_dict_type` (`dict_name`,`dict_type`,`status`,`is_tree`,`create_by`,`create_time`,`remark`,`del_flag`)
SELECT t.dict_name, t.dict_type, '0', '0', 'admin', NOW(), t.remark, '0'
FROM (
  SELECT '交易类型' AS dict_name, 'ledger_txn_type' AS dict_type, '记账行为类型（参与统计口径，值锁定且不可新增）' AS remark
  UNION ALL SELECT '仓库类型', 'ledger_warehouse_type', '个人仓/家庭仓/合伙仓'
  UNION ALL SELECT '货架类型', 'ledger_shelf_type', '普通/冷藏/上锁/暂存'
  UNION ALL SELECT '入库原因', 'ledger_stock_in_reason', '采购/赠送/其他'
  UNION ALL SELECT '出库原因', 'ledger_stock_out_reason', '销售/自用/损耗/转出/赠送/其他'
  UNION ALL SELECT '商品来源', 'ledger_product_source', '采购/自产/自用转卖/赠品（决定成本口径）'
  UNION ALL SELECT '出库类型', 'ledger_out_type', '卖出/自己用/损耗'
  UNION ALL SELECT '包装类型', 'ledger_packaging_type', '无需/标准/防摔/特殊'
  UNION ALL SELECT '计量单位', 'ledger_product_unit', '商品计量单位（不做换算）'
  UNION ALL SELECT '保质期预设', 'ledger_shelf_life', '按天数折算'
  UNION ALL SELECT '二手成色', 'ledger_condition_grade', '二手商品成色档位'
  UNION ALL SELECT '存钱方式', 'ledger_saving_method', '52周/固定/每月/自定义递增'
  UNION ALL SELECT '定时记账周期', 'ledger_schedule_cycle', '每天/每周/每月/每N天'
  UNION ALL SELECT '备忘提醒规则', 'ledger_remind_rule', '准时/提前30分/1小时/2小时/1天/1天上午9点'
) t
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_type` x WHERE x.`dict_type` = t.dict_type);

DROP TEMPORARY TABLE IF EXISTS `tmp_ledger_dict`;
CREATE TEMPORARY TABLE `tmp_ledger_dict` (
  `dict_type` varchar(100) NOT NULL,
  `dict_sort` int NOT NULL DEFAULT 0,
  `dict_label` varchar(100) NOT NULL,
  `dict_value` varchar(100) NOT NULL
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `tmp_ledger_dict` (`dict_type`,`dict_sort`,`dict_label`,`dict_value`) VALUES
  ('ledger_txn_type',1,'收入','income'),
  ('ledger_txn_type',2,'支出','expense'),
  ('ledger_txn_type',3,'转账','transfer'),
  ('ledger_txn_type',4,'借出','lend'),
  ('ledger_txn_type',5,'收款','collect'),
  ('ledger_txn_type',6,'借款','borrow'),
  ('ledger_txn_type',7,'还款','repayment'),
  ('ledger_txn_type',8,'校准','adjust'),
  ('ledger_warehouse_type',1,'个人仓','personal'),
  ('ledger_warehouse_type',2,'家庭仓','family'),
  ('ledger_warehouse_type',3,'合伙仓','partner'),
  ('ledger_shelf_type',1,'普通货架','normal'),
  ('ledger_shelf_type',2,'冷藏/冷冻','refrigerated'),
  ('ledger_shelf_type',3,'保险柜/上锁','locked'),
  ('ledger_shelf_type',4,'临时暂存','temp'),
  ('ledger_stock_in_reason',1,'采购','purchase'),
  ('ledger_stock_in_reason',2,'赠送','gift'),
  ('ledger_stock_in_reason',3,'其他','other'),
  ('ledger_stock_out_reason',1,'销售','sale'),
  ('ledger_stock_out_reason',2,'自用','self_use'),
  ('ledger_stock_out_reason',3,'损耗','loss'),
  ('ledger_stock_out_reason',4,'转出','transfer'),
  ('ledger_stock_out_reason',5,'赠送','gift'),
  ('ledger_stock_out_reason',6,'其他','other'),
  ('ledger_product_source',1,'采购','purchase'),
  ('ledger_product_source',2,'自产','self_made'),
  ('ledger_product_source',3,'自用转卖','used_resale'),
  ('ledger_product_source',4,'赠品','gift'),
  ('ledger_out_type',1,'卖出','sale'),
  ('ledger_out_type',2,'自己用','self_use'),
  ('ledger_out_type',3,'损耗','loss'),
  ('ledger_packaging_type',1,'无需','none'),
  ('ledger_packaging_type',2,'标准','standard'),
  ('ledger_packaging_type',3,'防摔','fragile'),
  ('ledger_packaging_type',4,'特殊','custom'),
  ('ledger_product_unit',1,'件','件'),
  ('ledger_product_unit',2,'个','个'),
  ('ledger_product_unit',3,'只','只'),
  ('ledger_product_unit',4,'条','条'),
  ('ledger_product_unit',5,'张','张'),
  ('ledger_product_unit',6,'份','份'),
  ('ledger_product_unit',7,'支','支'),
  ('ledger_product_unit',8,'台','台'),
  ('ledger_product_unit',9,'套','套'),
  ('ledger_product_unit',10,'双','双'),
  ('ledger_product_unit',11,'斤','斤'),
  ('ledger_product_unit',12,'公斤','公斤'),
  ('ledger_product_unit',13,'克','克'),
  ('ledger_product_unit',14,'吨','吨'),
  ('ledger_product_unit',15,'毫升','毫升'),
  ('ledger_product_unit',16,'升','升'),
  ('ledger_product_unit',17,'米','米'),
  ('ledger_product_unit',18,'厘米','厘米'),
  ('ledger_product_unit',19,'平方米','平方米'),
  ('ledger_product_unit',20,'立方米','立方米'),
  ('ledger_product_unit',21,'箱','箱'),
  ('ledger_product_unit',22,'瓶','瓶'),
  ('ledger_product_unit',23,'包','包'),
  ('ledger_product_unit',24,'袋','袋'),
  ('ledger_product_unit',25,'盒','盒'),
  ('ledger_product_unit',26,'桶','桶'),
  ('ledger_product_unit',27,'打','打'),
  ('ledger_shelf_life',1,'无保质期（长期有效）','none'),
  ('ledger_shelf_life',2,'7 天','7d'),
  ('ledger_shelf_life',3,'1 个月','30d'),
  ('ledger_shelf_life',4,'3 个月','90d'),
  ('ledger_shelf_life',5,'6 个月','180d'),
  ('ledger_shelf_life',6,'12 个月（1 年）','365d'),
  ('ledger_shelf_life',7,'24 个月（2 年）','730d'),
  ('ledger_shelf_life',8,'36 个月（3 年）','1095d'),
  ('ledger_shelf_life',9,'自定义天数…','custom'),
  ('ledger_condition_grade',1,'全新','new'),
  ('ledger_condition_grade',2,'95新','95'),
  ('ledger_condition_grade',3,'9成新','90'),
  ('ledger_condition_grade',4,'8成新','80'),
  ('ledger_condition_grade',5,'7成新及以下','70'),
  ('ledger_saving_method',1,'52周存钱法','52week'),
  ('ledger_saving_method',2,'固定金额','fixed'),
  ('ledger_saving_method',3,'每月固定存','monthly'),
  ('ledger_saving_method',4,'自定义递增','custom'),
  ('ledger_schedule_cycle',1,'每天','daily'),
  ('ledger_schedule_cycle',2,'每周','weekly'),
  ('ledger_schedule_cycle',3,'每月','monthly'),
  ('ledger_schedule_cycle',4,'每N天','interval'),
  ('ledger_remind_rule',1,'准时提醒','on_time'),
  ('ledger_remind_rule',2,'提前 30 分钟','advance_30m'),
  ('ledger_remind_rule',3,'提前 1 小时','advance_1h'),
  ('ledger_remind_rule',4,'提前 2 小时','advance_2h'),
  ('ledger_remind_rule',5,'提前 1 天','advance_1d'),
  ('ledger_remind_rule',6,'提前 1 天上午 9 点','advance_1d_9am');

INSERT INTO `sys_dict_data` (`dict_sort`,`dict_label`,`dict_value`,`dict_type`,`status`,`parent_dict_id`,`is_first_level`,`create_by`,`create_time`,`remark`,`del_flag`)
SELECT t.dict_sort, t.dict_label, t.dict_value, t.dict_type, '0', 0, '0', 'admin', NOW(), '', '0'
FROM `tmp_ledger_dict` t
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` d
                  WHERE d.`dict_type` = t.dict_type AND d.`dict_value` = t.dict_value AND d.`del_flag` = '0');

DROP TEMPORARY TABLE IF EXISTS `tmp_ledger_dict`;

-- -----------------------------------------------------------------------------
-- 账户类型字典（树形）：资产/负债两棵根，各挂子类型
-- App 端 PortalLedgerAccountTypeController 读它，缺失时回退枚举；此处补齐初始化。
-- -----------------------------------------------------------------------------
INSERT INTO `sys_dict_type` (`dict_name`,`dict_type`,`status`,`is_tree`,`create_by`,`create_time`,`remark`,`del_flag`)
SELECT '资产负债账户分类', 'ledger_account_category', '0', '1', 'admin', NOW(), '资产/负债账户类型（树形，值为计算口径，不可新增）', '0'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_type` x WHERE x.`dict_type` = 'ledger_account_category');

INSERT INTO `sys_dict_data` (`dict_label`,`dict_value`,`dict_sort`,`dict_type`,`status`,`parent_dict_id`,`is_first_level`,`create_by`,`create_time`,`remark`,`del_flag`)
SELECT '资产', 'asset', 1, 'ledger_account_category', '0', 0, '1', 'admin', NOW(), '资产根节点', '0'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` d WHERE d.`dict_type`='ledger_account_category' AND d.`dict_value`='asset' AND d.`parent_dict_id`=0);
INSERT INTO `sys_dict_data` (`dict_label`,`dict_value`,`dict_sort`,`dict_type`,`status`,`parent_dict_id`,`is_first_level`,`create_by`,`create_time`,`remark`,`del_flag`)
SELECT '负债', 'liability', 2, 'ledger_account_category', '0', 0, '1', 'admin', NOW(), '负债根节点', '0'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` d WHERE d.`dict_type`='ledger_account_category' AND d.`dict_value`='liability' AND d.`parent_dict_id`=0);

INSERT INTO `sys_dict_data` (`dict_sort`,`dict_label`,`dict_value`,`dict_type`,`status`,`parent_dict_id`,`is_first_level`,`create_by`,`create_time`,`remark`,`del_flag`)
SELECT c.sort, c.label, c.val, 'ledger_account_category', '0', r.`dict_code`, '0', 'admin', NOW(), '', '0'
FROM (
  SELECT 'fund' AS val, '资金账户' AS label, 1 AS sort, 'asset' AS pv
  UNION ALL SELECT 'receivable', '应收款', 2, 'asset'
  UNION ALL SELECT 'investment', '投资', 3, 'asset'
  UNION ALL SELECT 'fixed', '固定资产', 4, 'asset'
  UNION ALL SELECT 'debt', '普通负债', 1, 'liability'
  UNION ALL SELECT 'credit', '赊账应付', 2, 'liability'
) c
JOIN `sys_dict_data` r ON r.`dict_type`='ledger_account_category' AND r.`dict_value`=c.pv AND r.`parent_dict_id`=0
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` d
                  WHERE d.`dict_type`='ledger_account_category' AND d.`dict_value`=c.val AND d.`parent_dict_id`=r.`dict_code`);
