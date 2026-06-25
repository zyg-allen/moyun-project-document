-- ===================================================================
-- 38_init_report_feedback_menu.sql
-- 举报与反馈模块初始化
--
-- 内容：
--   1. 创建 portal_report  举报记录表
--   2. 创建 portal_feedback 意见反馈表
--   3. 内容管理菜单下补充：帮助分类、帮助文章、举报管理、反馈管理
--   4. 为超级管理员（role_id=1）分配权限
--
-- 说明：
--   - 通知公告（sys_notice, menu_id=107, system/notice/index）已存在于 01_moyun_init.sql，无需新增
--   - 通知中心（system/notification）已由 36_init_notification_center_menu.sql 创建
--   - 帮助中心 controller/vue 页面已存在，但缺 sys_menu 入口，本脚本补齐
--   - 本脚本可重复执行（IF NOT EXISTS 守护）
-- ===================================================================

SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS=0;

-- =====================================================
-- 一、举报记录表 portal_report
-- =====================================================
CREATE TABLE IF NOT EXISTS `portal_report` (
  `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '举报ID',
  `report_type`   varchar(32)  NOT NULL                COMMENT '举报类型：spam/inappropriate/infringement/fraud/other',
  `target_url`    varchar(500) DEFAULT NULL             COMMENT '举报目标URL',
  `description`   varchar(1000) NOT NULL                COMMENT '问题描述',
  `contact`       varchar(100) DEFAULT NULL             COMMENT '联系方式（可选）',
  `images`        varchar(1000) DEFAULT NULL            COMMENT '图片证据（JSON数组，最多3张）',
  `user_id`       bigint       DEFAULT NULL             COMMENT '举报人用户ID',
  `username`      varchar(64)  DEFAULT NULL             COMMENT '举报人用户名（冗余）',
  `ip`            varchar(128) DEFAULT NULL             COMMENT '举报人IP',
  `status`        varchar(20)  DEFAULT 'pending'        COMMENT '处理状态：pending/processing/resolved/rejected',
  `handler`       varchar(64)  DEFAULT NULL             COMMENT '处理人',
  `handle_result` varchar(1000) DEFAULT NULL            COMMENT '处理结果说明',
  `handle_time`   datetime     DEFAULT NULL             COMMENT '处理时间',
  `create_time`   datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark`        varchar(500) DEFAULT NULL             COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_report_type` (`report_type`),
  KEY `idx_status` (`status`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户举报记录表';

-- =====================================================
-- 二、意见反馈表 portal_feedback
-- =====================================================
CREATE TABLE IF NOT EXISTS `portal_feedback` (
  `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '反馈ID',
  `feedback_type` varchar(32)  NOT NULL                COMMENT '反馈类型：suggestion/bug/experience/other',
  `subject`       varchar(200) DEFAULT NULL             COMMENT '反馈主题',
  `description`   varchar(2000) NOT NULL                COMMENT '反馈详细描述',
  `contact`       varchar(100) DEFAULT NULL             COMMENT '联系方式（可选）',
  `user_id`       bigint       DEFAULT NULL             COMMENT '反馈人用户ID',
  `username`      varchar(64)  DEFAULT NULL             COMMENT '反馈人用户名（冗余）',
  `ip`            varchar(128) DEFAULT NULL             COMMENT '反馈人IP',
  `status`        varchar(20)  DEFAULT 'pending'        COMMENT '处理状态：pending/processing/resolved/rejected',
  `handler`       varchar(64)  DEFAULT NULL             COMMENT '处理人',
  `handle_result` varchar(1000) DEFAULT NULL            COMMENT '处理结果说明',
  `handle_time`   datetime     DEFAULT NULL             COMMENT '处理时间',
  `create_time`   datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark`        varchar(500) DEFAULT NULL             COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_feedback_type` (`feedback_type`),
  KEY `idx_status` (`status`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户意见反馈表';

-- =====================================================
-- 三、补充菜单：内容管理 → 帮助分类 / 帮助文章 / 举报管理 / 反馈管理
-- =====================================================

-- 取"内容管理"父菜单ID
SELECT @cmsParentId := menu_id FROM sys_menu WHERE menu_name = '内容管理' AND parent_id = 0 LIMIT 1;
SET @cmsParentId = IFNULL(@cmsParentId, 2000);

-- 3.1 帮助分类菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '帮助分类', @cmsParentId, 8, 'help-category', 'cms/help-category/index', NULL, 1, 0,
       'C', '0', '0', 'cms:help-category:list', 'tree', 'admin', NOW(), '帮助中心分类管理'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:help-category:list');
SELECT @helpCategoryMenuId := menu_id FROM sys_menu WHERE perms = 'cms:help-category:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '分类查询', @helpCategoryMenuId, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:help-category:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:help-category:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '分类新增', @helpCategoryMenuId, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:help-category:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:help-category:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '分类修改', @helpCategoryMenuId, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:help-category:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:help-category:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '分类删除', @helpCategoryMenuId, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:help-category:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:help-category:remove');

-- 3.2 帮助文章菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '帮助文章', @cmsParentId, 9, 'help-article', 'cms/help-article/index', NULL, 1, 0,
       'C', '0', '0', 'cms:help-article:list', 'documentation', 'admin', NOW(), '帮助中心文章管理'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:help-article:list');
SELECT @helpArticleMenuId := menu_id FROM sys_menu WHERE perms = 'cms:help-article:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文章查询', @helpArticleMenuId, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:help-article:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:help-article:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文章新增', @helpArticleMenuId, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:help-article:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:help-article:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文章修改', @helpArticleMenuId, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:help-article:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:help-article:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '文章删除', @helpArticleMenuId, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:help-article:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:help-article:remove');

-- 3.3 举报管理菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '举报管理', @cmsParentId, 10, 'report', 'cms/report/index', NULL, 1, 0,
       'C', '0', '0', 'cms:report:list', 'warning', 'admin', NOW(), '用户举报记录管理'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:report:list');
SELECT @reportMenuId := menu_id FROM sys_menu WHERE perms = 'cms:report:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '举报查询', @reportMenuId, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:report:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:report:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '处理举报', @reportMenuId, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:report:handle', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:report:handle');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '删除举报', @reportMenuId, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:report:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:report:remove');

-- 3.4 反馈管理菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '反馈管理', @cmsParentId, 11, 'feedback', 'cms/feedback/index', NULL, 1, 0,
       'C', '0', '0', 'cms:feedback:list', 'message', 'admin', NOW(), '用户意见反馈管理'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:feedback:list');
SELECT @feedbackMenuId := menu_id FROM sys_menu WHERE perms = 'cms:feedback:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '反馈查询', @feedbackMenuId, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:feedback:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:feedback:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '处理反馈', @feedbackMenuId, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:feedback:handle', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:feedback:handle');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '删除反馈', @feedbackMenuId, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:feedback:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:feedback:remove');

-- =====================================================
-- 四、为超级管理员（role_id=1）批量分配新菜单权限
-- =====================================================
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu
WHERE perms IN (
    'cms:help-category:list', 'cms:help-category:query', 'cms:help-category:add',
    'cms:help-category:edit', 'cms:help-category:remove',
    'cms:help-article:list', 'cms:help-article:query', 'cms:help-article:add',
    'cms:help-article:edit', 'cms:help-article:remove',
    'cms:report:list', 'cms:report:query', 'cms:report:handle', 'cms:report:remove',
    'cms:feedback:list', 'cms:feedback:query', 'cms:feedback:handle', 'cms:feedback:remove'
)
AND menu_id NOT IN (SELECT menu_id FROM sys_role_menu WHERE role_id = 1);

SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;

-- =====================================================
-- 五、校验结果
-- =====================================================
SELECT '举报/反馈/帮助中心 菜单:' AS info;
SELECT menu_id, menu_name, parent_id, path, component, perms, menu_type
FROM sys_menu
WHERE perms LIKE 'cms:help-category:%'
   OR perms LIKE 'cms:help-article:%'
   OR perms LIKE 'cms:report:%'
   OR perms LIKE 'cms:feedback:%'
ORDER BY menu_id;
