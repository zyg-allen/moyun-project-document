-- =============================================
-- v4.0 Bug 修复脚本
-- 1. 友情链接 status 值统一为 0/1（原 active/inactive）
-- 2. 删除多余通知菜单及其代码：通知公告(107) + 通知中心(system:notification:*)
--    （保留 cms/notification 通知管理作为唯一通知入口）
-- 3. 评论管理菜单 path 修正 + 权限标识统一为 cms:comment:edit
-- 作者：moyun  日期：2026-06-25
-- =============================================

-- =============================================
-- Bug2: 友情链接 status 值统一为 0/1
-- =============================================

-- 2.1 修改列默认值与注释
ALTER TABLE `portal_friend_link`
    MODIFY COLUMN `status` varchar(20) DEFAULT '0' COMMENT '状态：0正常 1停用';

-- 2.2 迁移历史数据（active→0, inactive→1，幂等）
UPDATE `portal_friend_link` SET `status` = '0' WHERE `status` = 'active';
UPDATE `portal_friend_link` SET `status` = '1' WHERE `status` = 'inactive';

-- =============================================
-- Bug3: 删除多余通知菜单（保留 cms/notification 通知管理）
--   说明：对应后端 SysNoticeController、SysNotificationController
--        及前端 views/system/notice、views/system/notification 已删除
-- =============================================

-- 3.1 先回收"通知公告"菜单（menu_id=107）及其按钮（1035-1038）的角色授权
DELETE FROM `sys_role_menu`
WHERE `menu_id` IN (107, 1035, 1036, 1037, 1038);

-- 3.2 删除"通知公告"菜单及其按钮
DELETE FROM `sys_menu`
WHERE `menu_id` = 107 OR `parent_id` = 107;

-- 3.3 先回收"通知中心"菜单及其按钮的角色授权（按 perms 匹配，因 menu_id 自增）
DELETE FROM `sys_role_menu`
WHERE `menu_id` IN (
    SELECT menu_id FROM (
        SELECT `menu_id` FROM `sys_menu`
        WHERE `perms` LIKE 'system:notification:%'
           OR (`menu_name` = '通知中心' AND `parent_id` = 1)
    ) t
);

-- 3.4 删除"通知中心"菜单及其按钮
DELETE FROM `sys_menu`
WHERE `perms` LIKE 'system:notification:%'
   OR (`menu_name` = '通知中心' AND `parent_id` = 1);

-- =============================================
-- Bug4: 评论管理菜单修复 + 权限标识统一
-- =============================================

-- 4.1 修正评论管理菜单 path（如果被错误改为 'cms/comment'，恢复为 'comment'）
UPDATE `sys_menu`
SET `path` = 'comment', `update_by` = 'admin', `update_time` = NOW()
WHERE `menu_name` = '评论管理'
  AND `path` != 'comment'
  AND `parent_id` IN (
      SELECT menu_id FROM (
          SELECT `menu_id` FROM `sys_menu`
          WHERE `menu_name` = '内容管理' AND `parent_id` = 0
      ) t
  );

-- 4.2 权限标识统一：cms:comment:audit → cms:comment:edit（与前后端代码一致）
UPDATE `sys_menu`
SET `perms` = 'cms:comment:edit', `update_by` = 'admin', `update_time` = NOW()
WHERE `perms` = 'cms:comment:audit';

-- 4.3 确保评论管理菜单及其按钮可见（防止脚本未执行或被禁用）
-- 子查询查找"内容管理"父菜单ID
SET @cmsParentId = (SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` = '内容管理' AND `parent_id` = 0 LIMIT 1);
SET @cmsParentId = IFNULL(@cmsParentId, 2000);

-- 评论管理主菜单不存在则插入
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '评论管理', @cmsParentId, 5, 'comment', 'cms/comment/index', '', 1, 0, 'C', '0', '0', 'cms:comment:list', 'message', 'admin', NOW(), '评论管理菜单'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu`
    WHERE `menu_name` = '评论管理' AND `parent_id` = @cmsParentId
);

-- 获取评论管理菜单ID（用于按钮插入）
SET @commentMenuId = (SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` = '评论管理' AND `parent_id` = @cmsParentId LIMIT 1);

-- 评论按钮权限（query/edit/remove）
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '评论查询', @commentMenuId, 1, '#', '', '', 1, 0, 'F', '0', '0', 'cms:comment:query', '#', 'admin', NOW(), ''
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:comment:query');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '评论编辑', @commentMenuId, 2, '#', '', '', 1, 0, 'F', '0', '0', 'cms:comment:edit', '#', 'admin', NOW(), ''
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:comment:edit');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '评论删除', @commentMenuId, 3, '#', '', '', 1, 0, 'F', '0', '0', 'cms:comment:remove', '#', 'admin', NOW(), ''
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'cms:comment:remove');

-- 4.4 为 admin 角色（role_id=1）授权评论管理菜单及按钮
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `menu_id` FROM `sys_menu`
WHERE `perms` LIKE 'cms:comment:%'
  AND `menu_id` NOT IN (SELECT `menu_id` FROM `sys_role_menu` WHERE `role_id` = 1);

-- =============================================
-- 5. 修复 portal_report.description 列长与实体校验不一致
--    实体 PortalReport.java @Size(max=2000)，原建表为 varchar(1000)
--    统一为 varchar(2000)，避免 1001-2000 字的举报描述写入失败
-- =============================================
ALTER TABLE `portal_report` MODIFY COLUMN `description` varchar(2000) NOT NULL COMMENT '问题描述';

-- =============================================
-- 验证查询（执行后可手动运行核对）
-- =============================================
-- SELECT menu_id, menu_name, parent_id, path, component, status, visible, perms
-- FROM sys_menu
-- WHERE perms LIKE 'cms:comment:%' OR perms LIKE '%notification%' OR perms LIKE '%notice%'
-- ORDER BY parent_id, order_num;
