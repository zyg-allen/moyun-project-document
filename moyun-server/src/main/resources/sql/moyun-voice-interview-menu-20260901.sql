-- =============================================================================
-- V10.3 语音面试：Admin 管理菜单归档（增量）
-- 背景：Admin 前端路由已内置 cms:voiceInterview 视图（views/cms/voiceInterview），
--       但 sys_menu 无对应菜单/权限标识，非超级管理员角色无法分配权限。
-- 位置：挂在「门户管理(5241) → 面试管理(5192)」目录下，order_num=4。
-- 幂等：先 DELETE 再 INSERT，可重复执行。
-- =============================================================================

DELETE FROM `moyun-db`.sys_menu WHERE menu_id IN (5253, 5254, 5255, 5256);

-- 语音面试管理（页面菜单）
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5253, '语音面试', 5192, 4, 'voiceInterview', 'cms/voiceInterview/index', null, '', 1, 0, 'C', '0', '0', 'cms:voiceInterview:list', 'microphone', 'admin', NOW(), '', null, 'AI语音面试会话管理：列表/详情/评分报告查看', '0');

-- 按钮权限
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5254, '语音面试查询', 5253, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:voiceInterview:query', '#', 'admin', NOW(), '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5255, '语音面试删除', 5253, 2, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:voiceInterview:remove', '#', 'admin', NOW(), '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5256, '语音面试导出', 5253, 3, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:voiceInterview:export', '#', 'admin', NOW(), '', null, '', '0');
