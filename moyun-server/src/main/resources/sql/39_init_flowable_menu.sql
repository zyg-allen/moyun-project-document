-- ===================================================================
-- 39_init_flowable_menu.sql
-- 流程管理菜单补充与修复
--
-- 背景：
--   01_moyun_init.sql 中流程管理(menu_id=4)下仅有两条菜单：
--     118 流程定义  component=flowable/definition/index     ✓ 存在
--     119 流程任务  component=flowable/task/index            ✗ 组件不存在（页面在 task/todo、task/finished 等子目录）
--   实际前端 views/flowable/ 已存在以下子页面，但缺菜单入口：
--     task/todo         待办任务
--     task/finished     已办任务
--     task/myProcess    我的流程
--     task/form         表单管理
--     expression        流程表达式
--     listener          流程监听器
--
-- 本脚本：
--   1. 禁用损坏的"流程任务"菜单（menu_id=119，保留 ID 但置 visible=1 隐藏 + status=1 停用）
--   2. 新增 6 条子菜单挂在"流程管理"下
--   3. 为超级管理员（role_id=1）分配权限
--
-- 说明：本脚本可重复执行（IF NOT EXISTS 守护）
-- ===================================================================

-- 1. 禁用损坏的"流程任务"菜单（保留 ID，避免外键断裂，仅隐藏停用）
UPDATE sys_menu
SET visible = '1', status = '1', update_by = 'admin', update_time = NOW(),
    remark = '已废弃：组件 flowable/task/index 不存在，由待办/已办/我的流程替代'
WHERE menu_id = 119;

-- 取"流程管理"父菜单ID（通常 menu_id=4，但用 name 查更稳健）
SELECT @flowParentId := menu_id FROM sys_menu WHERE menu_name = '流程管理' AND parent_id = 0 LIMIT 1;
SET @flowParentId = IFNULL(@flowParentId, 4);

-- =====================================================
-- 2.1 待办任务
-- =====================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '待办任务', @flowParentId, 2, 'todo', 'flowable/task/todo/index', NULL, 1, 0,
       'C', '0', '0', 'flowable:task:todoList', 'list', 'admin', NOW(), '我的待办流程任务'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'flowable:task:todoList');

-- =====================================================
-- 2.2 已办任务
-- =====================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '已办任务', @flowParentId, 3, 'finished', 'flowable/task/finished/index', NULL, 1, 0,
       'C', '0', '0', 'flowable:task:finishedList', 'checkbox', 'admin', NOW(), '我已处理的流程任务'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'flowable:task:finishedList');

-- =====================================================
-- 2.3 我的流程
-- =====================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '我的流程', @flowParentId, 4, 'myProcess', 'flowable/task/myProcess/index', NULL, 1, 0,
       'C', '0', '0', 'flowable:task:myProcess', 'people', 'admin', NOW(), '我发起的流程实例'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'flowable:task:myProcess');

-- =====================================================
-- 2.4 表单管理
-- =====================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '表单管理', @flowParentId, 5, 'form', 'flowable/task/form/index', NULL, 1, 0,
       'C', '0', '0', 'flowable:form:list', 'form', 'admin', NOW(), '流程表单管理'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'flowable:form:list');

-- =====================================================
-- 2.5 流程表达式
-- =====================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '流程表达式', @flowParentId, 6, 'expression', 'flowable/expression/index', NULL, 1, 0,
       'C', '0', '0', 'flowable:expression:list', 'edit', 'admin', NOW(), '流程表达式管理'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'flowable:expression:list');

-- =====================================================
-- 2.6 流程监听器
-- =====================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '流程监听器', @flowParentId, 7, 'listener', 'flowable/listener/index', NULL, 1, 0,
       'C', '0', '0', 'flowable:listener:list', 'guide', 'admin', NOW(), '流程监听器管理'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'flowable:listener:list');

-- =====================================================
-- 3. 为超级管理员（role_id=1）分配新增菜单权限
-- =====================================================
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu
WHERE perms IN (
    'flowable:task:todoList',
    'flowable:task:finishedList',
    'flowable:task:myProcess',
    'flowable:form:list',
    'flowable:expression:list',
    'flowable:listener:list'
)
AND menu_id NOT IN (SELECT menu_id FROM sys_role_menu WHERE role_id = 1);

-- =====================================================
-- 4. 校验结果
-- =====================================================
SELECT '流程管理菜单（修复后）:' AS info;
SELECT menu_id, menu_name, parent_id, order_num, path, component, perms, visible, status
FROM sys_menu
WHERE parent_id = @flowParentId
ORDER BY order_num, menu_id;
