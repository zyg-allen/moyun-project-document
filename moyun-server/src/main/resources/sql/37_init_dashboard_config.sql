-- ===================================================================
-- 37_init_dashboard_config.sql
-- 后台运营首页配置初始化
--
-- 内容：
--   1. 新增运营首页相关系统配置项（缓存时长、模块开关等）
--   2. 新增 dashboard 查询/刷新缓存权限点（挂在首页菜单下，所有管理员默认可见）
--   3. 预置模拟数据说明（运行时 Service 会自动回退到模拟数据，无需 DML 插入）
--
-- 说明：
--   - 首页路由 /index 已存在，无需新增菜单
--   - 接口 /system/dashboard/* 仅登录即可访问，权限点仅用于精细化控制
--   - 缓存策略：Redis 统一 5 分钟（dashboard:* 前缀），排行榜使用 ZSet
-- ===================================================================

-- 1. 新增运营首页系统配置项（如果不存在）
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '运营首页-缓存时长(秒)', 'dashboard.cache.ttl', '300', 'Y', 'admin', NOW(),
       '运营首页Redis缓存有效期，单位秒，默认300秒（5分钟）'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'dashboard.cache.ttl');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '运营首页-待办显示数量', 'dashboard.todo.limit', '8', 'Y', 'admin', NOW(),
       '运营首页待办任务列表最大显示条数，默认8条'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'dashboard.todo.limit');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '运营首页-栏目排行数量', 'dashboard.category.rank.limit', '10', 'Y', 'admin', NOW(),
       '运营首页栏目排行榜显示数量，默认Top10'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'dashboard.category.rank.limit');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '运营首页-热门文章数量', 'dashboard.hot.article.limit', '5', 'Y', 'admin', NOW(),
       '运营首页热门文章排行榜显示数量，默认Top5'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'dashboard.hot.article.limit');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '运营首页-趋势统计天数', 'dashboard.trend.days', '7', 'Y', 'admin', NOW(),
       '运营首页登录/发布趋势图统计天数，默认近7天'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'dashboard.trend.days');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '运营首页-启用模拟数据', 'dashboard.mock.enabled', 'true', 'Y', 'admin', NOW(),
       '查询失败时是否回退到模拟数据，true=启用（保证首页可用），false=返回空数据'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'dashboard.mock.enabled');

-- 2. 为首页菜单添加 dashboard 权限点（按钮型）
--    首页菜单 path='index'，通常 menu_id 较小，先查出
SELECT @indexMenuId := menu_id FROM sys_menu WHERE path = 'index' AND menu_type = 'C' LIMIT 1;
SET @indexMenuId = IFNULL(@indexMenuId, 1);

-- 查询权限点
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '首页数据查询', @indexMenuId, 1, '#', '', 1, 0,
       'F', '0', '0', 'system:dashboard:query', '#', 'admin', NOW(), '运营首页数据查询权限'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE perms = 'system:dashboard:query'
);

-- 刷新缓存权限点
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '首页缓存刷新', @indexMenuId, 2, '#', '', 1, 0,
       'F', '0', '0', 'system:dashboard:refresh', '#', 'admin', NOW(), '运营首页缓存刷新权限'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE perms = 'system:dashboard:refresh'
);

-- 3. 为超级管理员角色（role_id=1）分配权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu
WHERE perms LIKE 'system:dashboard:%'
  AND menu_id NOT IN (SELECT menu_id FROM sys_role_menu WHERE role_id = 1);

-- 4. 校验结果
SELECT '运营首页配置项:' AS info;
SELECT config_key, config_value, remark FROM sys_config WHERE config_key LIKE 'dashboard.%' ORDER BY config_id;

SELECT '运营首页权限点:' AS info;
SELECT menu_id, menu_name, perms, menu_type FROM sys_menu WHERE perms LIKE 'system:dashboard:%' ORDER BY menu_id;
