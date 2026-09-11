-- ============================================================
-- AI 执行日志管理页（v11.60 P1-1：可观测性补齐）
-- 依据：《AI底座企业级评估-代码实测结论与改进清单》P1-1
--   ai_execute_log 后端全字段落库但无管理端查询页（数据在采没人看）
-- 内容：
--   1) 菜单：AI基础配置 5238 → AI执行日志 5472（admin 前端 ai/execute-log/index.vue）
--   2) 后端接口：/cms/ai/execute-log（list/summary/scene-options/{id}/{ids}，见 AiExecuteLogController）
-- 幂等：菜单 DELETE+INSERT（5472-5474）
-- ============================================================

-- ------------------------------------------------------------
-- 1. 菜单：AI执行日志（挂在 AI基础配置 5238 下，order 12 排在内容安全检测 5470 之后）
-- ------------------------------------------------------------
DELETE FROM `moyun-db`.sys_menu WHERE menu_id BETWEEN 5472 AND 5474;
DELETE FROM `moyun-db`.sys_role_menu WHERE role_id = 1 AND menu_id BETWEEN 5472 AND 5474;

INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5472, 'AI执行日志', 5238, 12, 'execute-log', 'ai/execute-log/index', null, '', 1, 0, 'C', '0', '0', 'cms:ai:execute-log:list', 'log', 'admin', NOW(), '', null, '统一网关全量调用日志：场景/模型/Token/成本/耗时筛选与详情（v11.60 P1-1 可观测性）', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5473, '日志查询', 5472, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:execute-log:query', '#', 'admin', NOW(), '', null, '', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5474, '日志删除', 5472, 2, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:execute-log:remove', '#', 'admin', NOW(), '', null, '过期数据清理（物理删除，日志只增不改）', '0');

-- 管理员角色授权
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id) VALUES (1, 5472), (1, 5473), (1, 5474);

-- ------------------------------------------------------------
-- 2. 验证
-- ------------------------------------------------------------
-- SELECT menu_id, menu_name, parent_id, perms FROM `moyun-db`.sys_menu WHERE menu_id BETWEEN 5472 AND 5474;
-- 执行任意 AI 场景调用后：
-- SELECT scene_code, model_used, token_used, cost_yuan, status, elapsed_ms, create_time FROM `moyun-db`.ai_execute_log ORDER BY id DESC LIMIT 10;
