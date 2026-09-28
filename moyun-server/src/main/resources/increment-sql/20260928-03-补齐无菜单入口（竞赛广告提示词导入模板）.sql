-- =============================================================================
-- 补齐「有页面 / 有接口 / 有权限码，但 sys_menu 中从无菜单」的后台管理入口
-- -----------------------------------------------------------------------------
-- 背景（v13.26 调研结论）：以下 4 个模块的 视图页 + 前端 API + 后端 Controller + @PreAuthorize
-- 权限码 全部齐备，但 sys_menu 里没有任何菜单行 —— 除超级管理员（SysUser.isAdmin 绕过
-- sys_role_menu）外，任何角色都无法进入这些页面；页面内的按钮因权限码未下发而全部隐藏。
--
--   模块            页面组件                        后端 Controller              权限前缀
--   ----------------------------------------------------------------------------------------
--   竞赛管理        cms/contest/index               CmsContestController        cms:contest:*
--   广告位管理      cms/ad/index                    CmsAdSlotController         cms:ad:*
--   写作提示词      cms/prompt/index                CmsWritingPromptController  cms:writing-prompt:*
--   导入模板配置    cms/importTemplate/index        CmsImportTemplateController cms:importTemplate:*
--
-- 归类依据：门户前台五大主线 / 后台现有分组口径
--   竞赛管理、广告位管理、写作提示词 → 门户管理 / 内容管理（均服务于前台展示内容）
--   导入模板配置 → 系统设置 / 系统工具（运营配置类，与业务内容无关）
--
-- 幂等性：INSERT ... ON DUPLICATE KEY UPDATE（menu_id 主键）+ INSERT IGNORE 角色绑定；
--         可重复执行（判定标准：连跑两次均 exit 0）。
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. 竞赛管理（对齐门户前台 /contest 竞赛页）
-- ---------------------------------------------------------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (5630, '竞赛管理', 5068, 14, 'contest', 'cms/contest/index', 1, 0, 'C', '0', '0', 'cms:contest:list', 'star', 'admin', NOW(), '对齐门户前台竞赛页；页面与接口早已存在但无菜单')
ON DUPLICATE KEY UPDATE menu_name='竞赛管理', parent_id=5068, order_num=14, path='contest',
  component='cms/contest/index', menu_type='C', visible='0', status='0',
  perms='cms:contest:list', icon='star', remark='对齐门户前台竞赛页；页面与接口早已存在但无菜单';

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (5631, '竞赛查询', 5630, 1, '', NULL, 1, 0, 'F', '0', '0', 'cms:contest:query', '#', 'admin', NOW(), NULL),
       (5632, '竞赛新增', 5630, 2, '', NULL, 1, 0, 'F', '0', '0', 'cms:contest:add', '#', 'admin', NOW(), NULL),
       (5633, '竞赛修改', 5630, 3, '', NULL, 1, 0, 'F', '0', '0', 'cms:contest:edit', '#', 'admin', NOW(), NULL),
       (5634, '竞赛删除', 5630, 4, '', NULL, 1, 0, 'F', '0', '0', 'cms:contest:remove', '#', 'admin', NOW(), NULL)
ON DUPLICATE KEY UPDATE parent_id=5630, menu_type='F', visible='0', status='0',
  perms=VALUES(perms), menu_name=VALUES(menu_name);

-- ---------------------------------------------------------------------------
-- 2. 广告位管理（门户前台广告位配置）
-- ---------------------------------------------------------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (5635, '广告位管理', 5068, 15, 'ad', 'cms/ad/index', 1, 0, 'C', '0', '0', 'cms:ad:list', 'component', 'admin', NOW(), '门户前台广告位配置；页面与接口早已存在但无菜单')
ON DUPLICATE KEY UPDATE menu_name='广告位管理', parent_id=5068, order_num=15, path='ad',
  component='cms/ad/index', menu_type='C', visible='0', status='0',
  perms='cms:ad:list', icon='component', remark='门户前台广告位配置；页面与接口早已存在但无菜单';

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (5636, '广告查询', 5635, 1, '', NULL, 1, 0, 'F', '0', '0', 'cms:ad:query', '#', 'admin', NOW(), NULL),
       (5637, '广告新增', 5635, 2, '', NULL, 1, 0, 'F', '0', '0', 'cms:ad:add', '#', 'admin', NOW(), NULL),
       (5638, '广告修改', 5635, 3, '', NULL, 1, 0, 'F', '0', '0', 'cms:ad:edit', '#', 'admin', NOW(), NULL),
       (5639, '广告删除', 5635, 4, '', NULL, 1, 0, 'F', '0', '0', 'cms:ad:remove', '#', 'admin', NOW(), NULL)
ON DUPLICATE KEY UPDATE parent_id=5635, menu_type='F', visible='0', status='0',
  perms=VALUES(perms), menu_name=VALUES(menu_name);

-- ---------------------------------------------------------------------------
-- 3. 写作提示词（AI 生成选题提示词，服务于文章创作）
-- ---------------------------------------------------------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (5640, '写作提示词', 5068, 16, 'prompt', 'cms/prompt/index', 1, 0, 'C', '0', '0', 'cms:writing-prompt:list', 'edit', 'admin', NOW(), 'AI 写作选题提示词；页面与接口早已存在但无菜单')
ON DUPLICATE KEY UPDATE menu_name='写作提示词', parent_id=5068, order_num=16, path='prompt',
  component='cms/prompt/index', menu_type='C', visible='0', status='0',
  perms='cms:writing-prompt:list', icon='edit', remark='AI 写作选题提示词；页面与接口早已存在但无菜单';

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (5641, '提示词查询', 5640, 1, '', NULL, 1, 0, 'F', '0', '0', 'cms:writing-prompt:query', '#', 'admin', NOW(), NULL),
       (5642, '提示词新增', 5640, 2, '', NULL, 1, 0, 'F', '0', '0', 'cms:writing-prompt:add', '#', 'admin', NOW(), NULL),
       (5643, '提示词修改', 5640, 3, '', NULL, 1, 0, 'F', '0', '0', 'cms:writing-prompt:edit', '#', 'admin', NOW(), NULL),
       (5644, '提示词删除', 5640, 4, '', NULL, 1, 0, 'F', '0', '0', 'cms:writing-prompt:remove', '#', 'admin', NOW(), NULL)
ON DUPLICATE KEY UPDATE parent_id=5640, menu_type='F', visible='0', status='0',
  perms=VALUES(perms), menu_name=VALUES(menu_name);

-- ---------------------------------------------------------------------------
-- 4. 导入模板配置（批量导入的字段配置；运营工具类）
-- ---------------------------------------------------------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (5645, '导入模板配置', 3, 4, 'import-template', 'cms/importTemplate/index', 1, 0, 'C', '0', '0', 'cms:importTemplate:list', 'table', 'admin', NOW(), '批量导入字段配置；页面与接口早已存在但无菜单')
ON DUPLICATE KEY UPDATE menu_name='导入模板配置', parent_id=3, order_num=4, path='import-template',
  component='cms/importTemplate/index', menu_type='C', visible='0', status='0',
  perms='cms:importTemplate:list', icon='table', remark='批量导入字段配置；页面与接口早已存在但无菜单';

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (5646, '模板编辑', 5645, 1, '', NULL, 1, 0, 'F', '0', '0', 'cms:importTemplate:edit', '#', 'admin', NOW(), NULL)
ON DUPLICATE KEY UPDATE parent_id=5645, menu_type='F', visible='0', status='0',
  perms='cms:importTemplate:edit', menu_name='模板编辑';

-- ---------------------------------------------------------------------------
-- 5. 角色绑定自愈：把新菜单补授给超级管理员，并清理悬空绑定
-- ---------------------------------------------------------------------------
DELETE rm FROM sys_role_menu rm LEFT JOIN sys_menu m ON m.menu_id = rm.menu_id WHERE m.menu_id IS NULL;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id, create_by, create_time, remark)
SELECT 1, m.menu_id, 'admin', NOW(), 'v13.26 补齐无菜单入口默认授权'
  FROM sys_menu m
 WHERE m.menu_id IN (5630, 5631, 5632, 5633, 5634, 5635, 5636, 5637, 5638, 5639,
                     5640, 5641, 5642, 5643, 5644, 5645, 5646);

-- ---------------------------------------------------------------------------
-- 6. 复核 SQL（可人工执行验证）
-- ---------------------------------------------------------------------------
-- SELECT m.menu_id, p.menu_name AS parent_name, m.menu_name, m.path, m.component, m.perms
--   FROM sys_menu m LEFT JOIN sys_menu p ON p.menu_id = m.parent_id
--  WHERE m.menu_id BETWEEN 5630 AND 5646 ORDER BY m.menu_id;
