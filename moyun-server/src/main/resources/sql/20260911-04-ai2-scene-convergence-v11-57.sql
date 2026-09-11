-- ============================================================
-- AI 场景业务收口（v11.57 P0-3 前半：sensitive_word / daily_topic）
-- 依据：《AI底座企业级评估-代码实测结论与改进清单》P0-3
-- 内容：
--   1) 官方账号 moyun_official（AI 生成话题的发起人，portal_topic.creator_id 非空）
--   2) 菜单：AI基础配置 5238 → 内容安全检测 5470（admin 前端 ai/safety/index.vue）
-- 幂等：官方账号 INSERT IGNORE（uk_username）；菜单 DELETE+INSERT（5470-5471）
-- ============================================================

-- ------------------------------------------------------------
-- 1. 官方账号（AI 生成话题发起人）
--    is_certified_creator=1 满足话题发起人约束；privacy_profile=0 不在作者列表曝光；
--    通知全关（系统账号无阅读诉求）
-- ------------------------------------------------------------
INSERT IGNORE INTO `moyun-db`.portal_user
    (username, nickname, bio, role, is_certified_creator, notify_like, notify_comment, notify_follow,
     notify_system, privacy_profile, privacy_bookmark, status, del_flag, create_by, remark)
VALUES ('moyun_official', '墨云官方', '墨云官方账号 · 每日话题由 AI 生成', 'admin', 1, 0, 0, 0, 0, 0, 0, '0', '0', 'admin', '系统账号：AI 生成话题专用，SQL 20260911-04 初始化');

-- ------------------------------------------------------------
-- 2. 菜单：内容安全检测（挂在 AI基础配置 5238 下）
-- ------------------------------------------------------------
DELETE FROM `moyun-db`.sys_menu WHERE menu_id BETWEEN 5470 AND 5471;
DELETE FROM `moyun-db`.sys_role_menu WHERE role_id = 1 AND menu_id BETWEEN 5470 AND 5471;

INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5470, '内容安全检测', 5238, 11, 'safety', 'ai/safety/index', null, '', 1, 0, 'C', '0', '0', 'cms:ai:safety:detect', 'shield', 'admin', NOW(), '', null, 'LLM 级文本敏感内容复核工具（经统一网关 sensitive_word 场景，限流/成本熔断自动生效）', '0');
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5471, '文本检测', 5470, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'cms:ai:safety:detect', '#', 'admin', NOW(), '', null, '', '0');

-- 管理员角色授权
INSERT INTO `moyun-db`.sys_role_menu (role_id, menu_id) VALUES (1, 5470), (1, 5471);

-- ------------------------------------------------------------
-- 3. 验证
-- ------------------------------------------------------------
-- SELECT id, username, nickname, is_certified_creator FROM `moyun-db`.portal_user WHERE username = 'moyun_official';
-- SELECT menu_id, menu_name, parent_id, perms FROM `moyun-db`.sys_menu WHERE menu_id BETWEEN 5470 AND 5471;
