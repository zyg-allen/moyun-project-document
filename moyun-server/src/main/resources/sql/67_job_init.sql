-- =====================================================
-- 职位列表 DDL 脚本（学习者成长闭环 - 求职信息）
-- 支持职位发布与展示
-- 幂等设计：CREATE TABLE IF NOT EXISTS + INSERT ... WHERE NOT EXISTS
-- 执行顺序：本脚本在 18_interview_menu_init.sql 之后执行
-- =====================================================

-- 职位
CREATE TABLE IF NOT EXISTS `portal_job` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `company_id`    BIGINT        DEFAULT NULL            COMMENT '公司ID（关联 portal_interview_company）',
    `title`         VARCHAR(128)  NOT NULL                COMMENT '职位名称',
    `city`          VARCHAR(64)   DEFAULT NULL            COMMENT '工作城市',
    `salary_min`    DECIMAL(10,2) DEFAULT NULL            COMMENT '薪资下限',
    `salary_max`    DECIMAL(10,2) DEFAULT NULL            COMMENT '薪资上限',
    `experience`    VARCHAR(32)   DEFAULT NULL            COMMENT '经验要求',
    `education`     VARCHAR(32)   DEFAULT NULL            COMMENT '学历要求',
    `description`   TEXT                                  COMMENT '职位描述',
    `requirement`   TEXT                                  COMMENT '任职要求',
    `status`        VARCHAR(16)   NOT NULL DEFAULT 'open' COMMENT '状态：open/closed',
    `created_time`  DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time`  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_company` (`company_id`),
    KEY `idx_status` (`status`),
    KEY `idx_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='职位';

-- =====================================================
-- 后台菜单初始化（挂载在"面试空间"一级菜单下）
-- =====================================================

-- 取"面试空间"父菜单ID
SELECT @interview_menu_id := menu_id FROM sys_menu WHERE menu_name = '面试空间' AND path = 'interview' AND parent_id = 0 LIMIT 1;
SET @interview_menu_id = IFNULL(@interview_menu_id, 0);

-- =============================================
-- 职位管理
-- =============================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '职位管理', @interview_menu_id, 7, 'job', 'portal/job/index', NULL, 1, 0,
       'C', '0', '0', 'portal:job:list', 'post', 'admin', NOW(), '职位管理菜单'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:job:list');
SELECT @job_menu_id := menu_id FROM sys_menu WHERE perms = 'portal:job:list' LIMIT 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '职位查询', @job_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:job:query', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:job:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '职位新增', @job_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:job:add', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:job:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '职位修改', @job_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:job:edit', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:job:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '职位删除', @job_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'portal:job:remove', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'portal:job:remove');

-- =============================================
-- 为管理员角色分配职位管理菜单权限
-- =============================================
SET @admin_role_id = 1;

-- 删除旧的职位管理权限（如果存在），避免重复
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (
    SELECT `menu_id` FROM `sys_menu` WHERE `perms` IN (
        'portal:job:list', 'portal:job:query', 'portal:job:add', 'portal:job:edit', 'portal:job:remove'
    )
);

-- 重新为管理员角色分配职位管理权限
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @admin_role_id, `menu_id` FROM `sys_menu`
WHERE `perms` IN (
    'portal:job:list', 'portal:job:query', 'portal:job:add', 'portal:job:edit', 'portal:job:remove'
);

-- =============================================
-- 脚本执行完成
-- =============================================
SELECT '职位列表初始化脚本执行完成！' AS message;
