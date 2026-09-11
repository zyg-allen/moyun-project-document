-- ============================================================
-- 系统文件管理菜单（sys_file 后台管理入口）
-- 背景：sys_file 表 + 后端 SysFileController(/system/file) + 前端页面
--   (views/system/file/index.vue) 与 API(api/system/file.js) 均已存在，
--   但 sys_menu 无菜单记录，管理端无入口可见。
-- 内容：
--   1) 菜单：基础管理 5243 → 文件管理 5475（C，system/file/index）
--   2) 按钮：文件查询 5476 / 文件上传 5477 / 文件删除 5478（F 权限位）
-- 幂等：菜单 DELETE+INSERT（5475-5478）
-- 权限标识与后端 @PreAuthorize 对齐：
--   system:file:list / system:file:query / system:file:add / system:file:remove
-- ============================================================

-- ------------------------------------------------------------
-- 1. 菜单：文件管理（挂在 基础管理 5243 下，order 6 排在岗位管理之后）
-- ------------------------------------------------------------
DELETE FROM `moyun-db`.sys_menu WHERE menu_id BETWEEN 5475 AND 5478;
DELETE FROM `moyun-db`.sys_role_menu WHERE role_id = 1 AND menu_id BETWEEN 5475 AND 5478;

INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5475, '文件管理', 5243, 6, 'file', 'system/file/index', null, '', 1, 0, 'C', '0', '0', 'system:file:list', 'upload', 'admin', NOW(), '', null, '系统文件管理：MinIO/本地存储的文件列表、上传、预览、下载、删除与存储模式切换', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5476, '文件查询', 5475, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'system:file:query', '#', 'admin', NOW(), '', null, '文件详情查看（GET /system/file/{id}）', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5477, '文件上传', 5475, 2, '', null, null, '', 1, 0, 'F', '0', '0', 'system:file:add', '#', 'admin', NOW(), '', null, '文件上传与存储模式切换（POST /system/file/upload、PUT /storage/switch）', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5478, '文件删除', 5475, 3, '', null, null, '', 1, 0, 'F', '0', '0', 'system:file:remove', '#', 'admin', NOW(), '', null, '单删/批删/按URL删（删除同时清理存储与记录，不可恢复）', '0');

-- 管理员角色授权
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id) VALUES (1, 5475), (1, 5476), (1, 5477), (1, 5478);

-- ------------------------------------------------------------
-- 2. 验证
-- ------------------------------------------------------------
-- SELECT menu_id, menu_name, parent_id, perms FROM `moyun-db`.sys_menu WHERE menu_id BETWEEN 5475 AND 5478;
-- 管理端重新登录后：系统设置 → 基础管理 → 文件管理
-- 已有上传数据可查：SELECT file_name, file_type, storage_type, file_url FROM `moyun-db`.sys_file ORDER BY id DESC LIMIT 10;
