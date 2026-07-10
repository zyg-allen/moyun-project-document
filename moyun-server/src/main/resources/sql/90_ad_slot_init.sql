-- =====================================================
-- 自研广告位模块初始化脚本（MVP 阶段）
-- 用于详情页底部展示广告卡片，流量起来后可接广告联盟
-- 挂载在"内容管理"一级菜单下
-- 幂等设计：CREATE TABLE IF NOT EXISTS + INSERT ... WHERE NOT EXISTS
-- 执行顺序：本脚本在 88_message_admin_menu.sql 之后执行
-- =====================================================

-- =============================================
-- 一、广告位表
-- =============================================
CREATE TABLE IF NOT EXISTS `portal_ad_slot` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '广告位ID',
  `slot_key` varchar(64) NOT NULL COMMENT '广告位标识，如 article_detail_bottom',
  `title` varchar(100) NOT NULL COMMENT '广告标题',
  `image` varchar(500) DEFAULT NULL COMMENT '广告图片URL',
  `link` varchar(500) DEFAULT NULL COMMENT '点击跳转链接',
  `content` varchar(500) DEFAULT NULL COMMENT '广告文案',
  `sort` int DEFAULT '0' COMMENT '排序',
  `status` varchar(1) DEFAULT '0' COMMENT '状态：0=启用 1=停用',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_slot_key` (`slot_key`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户自研广告位表';

-- =============================================
-- 二、后台菜单（挂在"内容管理"一级菜单下）
-- =============================================
-- 取"内容管理"父菜单ID
SELECT @cms_parent_id := menu_id FROM sys_menu WHERE menu_name = '内容管理' AND parent_id = 0 LIMIT 1;
SET @cms_parent_id = IFNULL(@cms_parent_id, 0);

-- 广告位管理菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '广告位管理', @cms_parent_id, 14, 'ad', 'cms/ad/index', NULL, 1, 0,
       'C', '0', '0', 'portal:ad:list', 'image', 'admin', NOW(), '自研广告位 MVP，详情页底部广告卡片'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:ad:list');
SELECT @ad_menu_id := menu_id FROM sys_menu WHERE perms = 'portal:ad:list' LIMIT 1;

-- 按钮权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '广告位查询', @ad_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:ad:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:ad:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '广告位新增', @ad_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:ad:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:ad:add');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '广告位修改', @ad_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:ad:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:ad:edit');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '广告位删除', @ad_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:ad:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:ad:remove');

-- =============================================
-- 三、为管理员角色分配广告位菜单权限
-- =============================================
SET @admin_role_id = 1;

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @admin_role_id, `menu_id` FROM `sys_menu`
WHERE `perms` IN (
    'portal:ad:list', 'portal:ad:query', 'portal:ad:add', 'portal:ad:edit', 'portal:ad:remove'
)
AND `menu_id` NOT IN (SELECT `menu_id` FROM `sys_role_menu` WHERE `role_id` = @admin_role_id);

-- =============================================
-- 校验结果
-- =============================================
SELECT '自研广告位模块初始化脚本执行完成！' AS message;
