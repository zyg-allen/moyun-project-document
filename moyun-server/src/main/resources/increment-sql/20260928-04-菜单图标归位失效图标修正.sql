-- =============================================================================
-- 菜单图标归位：把使用了「SVG 精灵中不存在的图标名」的菜单改为真实存在的图标
-- -----------------------------------------------------------------------------
-- 背景：后台侧边栏渲染为 <svg><use xlink:href="#icon-{name}"/></svg>
--       （见 layout/components/Sidebar/SidebarItem.vue → components/SvgIcon/index.vue）。
--       图标名不存在时 <use> 静默不渲染 → 菜单左侧出现空白，无报错、难察觉。
--       全量比对 sys_menu.icon 与 src/assets/icons/svg/ 后，以下 7 条为失效图标。
--
-- 归位映射（新图标均在 SVG 精灵中存在）：
--   5114 举报管理      warning  → eye           （审查/查看类语义）
--   5253 语音面试      microphone → rate        （语音/评分类语义）
--   5303 用户银行卡    card     → money         （资金卡片语义）
--   5470 内容安全检测  shield   → eye-open      （内容审查语义）
--   5501 VIP管理       crown    → star          （会员权益语义）
--   5506 用户会员卡    idcard   → user          （用户卡片语义）
--   5613 成就管理      trophy   → star          （成就/荣誉语义）
--
-- 幂等性：纯 UPDATE，可重复执行。
-- =============================================================================

UPDATE sys_menu SET icon = 'eye'      WHERE menu_id = 5114 AND icon = 'warning';
UPDATE sys_menu SET icon = 'rate'     WHERE menu_id = 5253 AND icon = 'microphone';
UPDATE sys_menu SET icon = 'money'    WHERE menu_id = 5303 AND icon = 'card';
UPDATE sys_menu SET icon = 'eye-open' WHERE menu_id = 5470 AND icon = 'shield';
UPDATE sys_menu SET icon = 'star'     WHERE menu_id = 5501 AND icon = 'crown';
UPDATE sys_menu SET icon = 'user'     WHERE menu_id = 5506 AND icon = 'idcard';
UPDATE sys_menu SET icon = 'star'     WHERE menu_id = 5613 AND icon = 'trophy';

-- ---------------------------------------------------------------------------
-- 复核方式（SVG 精灵文件不在数据库内，需在应用侧比对）：
--   1) 导出菜单使用的图标：
--      SELECT DISTINCT icon FROM sys_menu WHERE icon IS NOT NULL AND icon <> '' AND icon <> '#';
--   2) 列出精灵可用图标（PowerShell，在 moyun-admin-vue 目录执行）：
--      (Get-ChildItem src/assets/icons/svg).BaseName
--   3) 两者取差集应为空；CI/守卫可把该比对固化为用例，避免再次引入失效图标名。
-- ---------------------------------------------------------------------------
