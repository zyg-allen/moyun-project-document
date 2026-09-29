-- =============================================================================
-- 新增后台菜单：简历解析配置（含 4 个按钮权限）
-- -----------------------------------------------------------------------------
-- 背景（v13.38 简历解析重构）：
--   规则解析引擎（ResumeRuleParser）的词表配置化 —— 章节标题词典 / 技能词域 /
--   学历词 / 岗位词 落表 portal_resume_parse_config，需后台可维护页面。
--
-- 页面：/cms/interview/resumeParseConfig/index
--   → 前端 views/cms/interview/resumeParseConfig/index.vue
--   → 后端 CmsResumeParseConfigController（@RequestMapping /cms/interview/resumeParseConfig）
--   → 权限：cms:interview:resumeParseConfig:list|query|create|update|remove
--
-- 挂载：父 26「面试管理」，order_num=7（与「岗位模板」「面试配置」同级）
--
-- ⚠️ menu_id 分配说明（重要，勿随意改动）：
--   既有 sys_menu 的 id 空间为 1..396 **密集占满**，唯一连续空闲块是 **397..406**（长度 10），
--   故本菜单取 397、按钮取 398..401；并把 AUTO_INCREMENT 推到 402，避免后续自增撞车。
--   副作用：满足既有惯例「子 id > 父 id」（如父 26 → 子 27..32、397）。
--
-- 幂等性：INSERT ... WHERE NOT EXISTS（按 menu_id），可重复执行。
-- =============================================================================

INSERT INTO `sys_menu` (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,
                        `route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,
                        `create_by`,`create_time`,`update_by`,`update_time`,`remark`,`del_flag`)
SELECT 397,'简历解析配置',26,7,'resumeParseConfig','cms/interview/resumeParseConfig/index',NULL,
       '',1,0,'C','0','0','cms:interview:resumeParseConfig:list','form',
       'admin',NOW(),'',NULL,
       '简历解析配置：规则解析词表（章节标题词典/技能词域/学历词/岗位词）；表为空时引擎用内置默认词典兜底','0'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 397);

INSERT INTO `sys_menu` (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,
                        `route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,
                        `create_by`,`create_time`,`update_by`,`update_time`,`remark`,`del_flag`)
SELECT 398,'解析配置查询',397,1,'',NULL,NULL,'',1,0,'F','0','0','cms:interview:resumeParseConfig:query','#',
       'admin',NOW(),'',NULL,'','0'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 398);

INSERT INTO `sys_menu` (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,
                        `route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,
                        `create_by`,`create_time`,`update_by`,`update_time`,`remark`,`del_flag`)
SELECT 399,'解析配置新增',397,2,'',NULL,NULL,'',1,0,'F','0','0','cms:interview:resumeParseConfig:create','#',
       'admin',NOW(),'',NULL,'','0'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 399);

INSERT INTO `sys_menu` (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,
                        `route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,
                        `create_by`,`create_time`,`update_by`,`update_time`,`remark`,`del_flag`)
SELECT 400,'解析配置修改',397,3,'',NULL,NULL,'',1,0,'F','0','0','cms:interview:resumeParseConfig:update','#',
       'admin',NOW(),'',NULL,'','0'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 400);

INSERT INTO `sys_menu` (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,
                        `route_name`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,
                        `create_by`,`create_time`,`update_by`,`update_time`,`remark`,`del_flag`)
SELECT 401,'解析配置删除',397,4,'',NULL,NULL,'',1,0,'F','0','0','cms:interview:resumeParseConfig:remove','#',
       'admin',NOW(),'',NULL,'','0'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 401);

-- AUTO_INCREMENT 推到空闲块之后（重复执行安全：仅当当前值更小才抬高）
SET @cur := (SELECT AUTO_INCREMENT FROM information_schema.TABLES
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_menu');
SET @ddl := IF(@cur < 402, 'ALTER TABLE `sys_menu` AUTO_INCREMENT = 402', 'SELECT ''skip: AUTO_INCREMENT 已 >= 402'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 授权给超级管理员（role_id=1）
-- ---------------------------------------------------------------------------
-- ⚠️ 必要步骤，勿省：本项目超管**不使用 `*:*:*` 通配**，权限完全来自 sys_role_menu
--    （见 PermissionService.hasPermissions：需 permissions 含 ALL_PERMISSION 或精确匹配）。
--    只建菜单不授权 → 后台侧边栏不显示该菜单、@PreAuthorize 直接 403。
--    此坑在本次落地时已实际踩到（菜单建好但 role_menu 无记录 → 页面进不去）。
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 1, m.menu_id FROM `sys_menu` m
 WHERE m.menu_id BETWEEN 397 AND 401
   AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id);

-- ---------------------------------------------------------------------------
-- 复核 SQL
-- ---------------------------------------------------------------------------
-- SELECT menu_id, menu_name, parent_id, order_num, path, component, menu_type, perms
--   FROM sys_menu WHERE menu_id BETWEEN 397 AND 401 ORDER BY menu_id;
-- SELECT COUNT(*) FROM sys_role_menu WHERE menu_id BETWEEN 397 AND 401;   -- 期望 5
-- 其它角色如需访问，请在后台「角色管理」中勾选授权。
