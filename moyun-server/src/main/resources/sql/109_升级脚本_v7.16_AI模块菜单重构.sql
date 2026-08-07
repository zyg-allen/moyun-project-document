-- =============================================================================
-- 109_升级脚本_v7.16_AI模块菜单重构.sql
--
-- 目标：将 AI 模块从扁平 12 菜单重构为嵌套结构（3 个 M 目录 + 业务分组）
--   重构前（54脚本注册，全部平铺在"智能AI"下）：
--     1.智能体管理 2.知识库管理 3.知识文库 4.模型配置 5.工具管理 6.工作流管理
--     7.领域词典 8.数据源管理 9.Token统计 10.数据分析 11.AI数据大屏 12.架构图生成
--
--   重构后（按业务关联性分组，3 个 M 目录嵌套）：
--     智能AI (ai, order=11)
--     ├─ 智能体管理 (order=1)
--     ├─ 知识中心 (order=2, M目录)        ← 新建，整合知识库管理+知识文库
--     │  ├─ 知识库管理 (原order=2)
--     │  └─ 知识文库   (原order=3)
--     ├─ 工作流管理 (order=3, 原order=6)
--     ├─ AI基础配置 (order=4, M目录)      ← 新建，整合模型配置+工具管理+数据源管理
--     │  ├─ 模型配置   (原order=4)
--     │  ├─ 工具管理   (原order=5)
--     │  └─ 数据源管理 (原order=8)
--     ├─ 领域词典 (order=5, 原order=7)
--     ├─ AI数据分析 (order=6, 原order=10, 改名)
--     ├─ 架构图生成 (order=7, 原order=12)
--     └─ 运营监控 (order=9, M目录)        ← 新建，整合概览大屏+Token统计
--        ├─ 概览大屏 (原AI数据大屏, 原order=11, 改名)
--        └─ Token统计 (原order=9)
--
-- 同时修复：
--   1. 旧知识库管理/知识文库菜单改为隐藏（visible=1），由知识中心 Tab 容器承载
--   2. AI数据大屏改名为"概览大屏"
--   3. 数据分析改名为"AI数据分析"
--   4. 超级管理员角色自动关联新增的 M 目录菜单
-- 幂等：可重复执行，所有操作均带 NOT EXISTS 或条件判断
-- =============================================================================

-- 获取智能AI一级目录ID
SELECT @ai_parent_id := menu_id FROM sys_menu WHERE menu_name = '智能AI' AND parent_id = 0 LIMIT 1;

-- =============================================================================
-- 一、创建 3 个 M 目录
-- =============================================================================

-- 1. 知识中心 M 目录
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '知识中心', @ai_parent_id, 2, 'knowledge-center', 'ai/knowledge-center/index', NULL, 1, 0, 'M', '0', '0', '', 'documentation', 'admin', NOW(), '知识中心目录（知识库管理+知识文库 Tab）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name = '知识中心' AND parent_id = @ai_parent_id);
SELECT @kc_dir_id := menu_id FROM sys_menu WHERE menu_name = '知识中心' AND parent_id = @ai_parent_id LIMIT 1;

-- 2. AI基础配置 M 目录
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'AI基础配置', @ai_parent_id, 4, 'ai-config', NULL, NULL, 1, 0, 'M', '0', '0', '', 'system', 'admin', NOW(), 'AI基础配置目录（模型配置+工具管理+数据源管理）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name = 'AI基础配置' AND parent_id = @ai_parent_id);
SELECT @cfg_dir_id := menu_id FROM sys_menu WHERE menu_name = 'AI基础配置' AND parent_id = @ai_parent_id LIMIT 1;

-- 3. 运营监控 M 目录
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '运营监控', @ai_parent_id, 9, 'ai-monitor', NULL, NULL, 1, 0, 'M', '0', '0', '', 'monitor', 'admin', NOW(), '运营监控目录（概览大屏+Token统计）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name = '运营监控' AND parent_id = @ai_parent_id LIMIT 1);
SELECT @mon_dir_id := menu_id FROM sys_menu WHERE menu_name = '运营监控' AND parent_id = @ai_parent_id LIMIT 1;

-- =============================================================================
-- 二、调整子菜单 parent_id 和 order_num
-- =============================================================================

-- 知识库管理 → 挂到知识中心目录下，设为隐藏（由 Tab 容器承载）
UPDATE sys_menu SET parent_id = @kc_dir_id, order_num = 1, visible = '1',
       remark = CONCAT(IFNULL(remark, ''), ' [v7.16 已整合到知识中心 Tab 容器]')
WHERE perms = 'cms:ai:knowledge-base:list' AND parent_id = @ai_parent_id;

-- 知识文库 → 挂到知识中心目录下，设为隐藏（由 Tab 容器承载）
UPDATE sys_menu SET parent_id = @kc_dir_id, order_num = 2, visible = '1',
       remark = CONCAT(IFNULL(remark, ''), ' [v7.16 已整合到知识中心 Tab 容器]')
WHERE perms = 'cms:ai:knowledge-library:list' AND parent_id = @ai_parent_id;

-- 模型配置 → 挂到 AI基础配置目录下
UPDATE sys_menu SET parent_id = @cfg_dir_id, order_num = 1
WHERE perms = 'cms:ai:model-config:list' AND parent_id = @ai_parent_id;

-- 工具管理 → 挂到 AI基础配置目录下
UPDATE sys_menu SET parent_id = @cfg_dir_id, order_num = 2
WHERE perms = 'cms:ai:tool:list' AND parent_id = @ai_parent_id;

-- 数据源管理 → 挂到 AI基础配置目录下
UPDATE sys_menu SET parent_id = @cfg_dir_id, order_num = 3
WHERE perms = 'cms:ai:datasource:list' AND parent_id = @ai_parent_id;

-- 工作流管理 → order=3
UPDATE sys_menu SET order_num = 3
WHERE perms = 'cms:ai:workflow:list' AND parent_id = @ai_parent_id;

-- 领域词典 → order=5
UPDATE sys_menu SET order_num = 5
WHERE perms = 'cms:ai:domain-dictionary:list' AND parent_id = @ai_parent_id;

-- 数据分析 → order=6，改名为 AI数据分析
UPDATE sys_menu SET order_num = 6, menu_name = 'AI数据分析'
WHERE perms = 'cms:ai:data-analysis:list' AND parent_id = @ai_parent_id AND menu_name != 'AI数据分析';

-- 架构图生成 → order=7
UPDATE sys_menu SET order_num = 7
WHERE perms = 'cms:ai:diagram:list' AND parent_id = @ai_parent_id;

-- AI数据大屏 → 挂到运营监控目录下，改名为 概览大屏
UPDATE sys_menu SET parent_id = @mon_dir_id, order_num = 1, menu_name = '概览大屏'
WHERE perms = 'cms:ai:dashboard:list' AND parent_id = @ai_parent_id AND menu_name != '概览大屏';

-- Token统计 → 挂到运营监控目录下
UPDATE sys_menu SET parent_id = @mon_dir_id, order_num = 2
WHERE perms = 'cms:ai:token-usage:list' AND parent_id = @ai_parent_id;

-- 智能体管理 → order=1（保持顶级）
UPDATE sys_menu SET order_num = 1
WHERE perms = 'cms:ai:agent:list' AND parent_id = @ai_parent_id;

-- =============================================================================
-- 三、dashboard 和 diagram 走动态路由（Layout 包裹），不需要修改 path
--   dashboard CSS 已改为 min-height: calc(100vh - 84px) 撑满 Layout 内容区
--   diagram 页面自身已有 height: 100% 处理
-- =============================================================================

-- =============================================================================
-- 四、超级管理员角色自动关联新增的 M 目录菜单
-- =============================================================================
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id
FROM sys_menu m
WHERE m.menu_name IN ('知识中心', 'AI基础配置', '运营监控')
  AND m.parent_id = @ai_parent_id
  AND m.menu_type = 'M'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id
  );

-- =============================================================================
-- 五、校验输出
-- =============================================================================
SELECT '=== AI 模块菜单结构（重构后）===' AS info;
SELECT
    CASE
        WHEN p.menu_type = 'M' AND p.parent_id = @ai_parent_id THEN CONCAT('■ ', p.menu_name)
        WHEN c.menu_type = 'M' THEN CONCAT('  └─ ■ ', c.menu_name, ' (M目录)')
        ELSE CONCAT('     ├─ ', c.menu_name)
    END AS 菜单层级,
    c.perms AS 权限标识,
    c.order_num AS 排序,
    c.visible AS 可见,
    c.path AS 路由路径
FROM sys_menu p
LEFT JOIN sys_menu c ON c.parent_id = p.menu_id AND c.menu_type IN ('C', 'M')
WHERE p.menu_id = @ai_parent_id
ORDER BY c.order_num;

SELECT '=== 重构完成 ===' AS info;
