-- =====================================================================
-- 墨韵智库 v8.2 · 通用导入模板配置 + 题库导入导出权限升级
-- =====================================================================
-- 用途：
--   1. 新建 portal_import_template_config 表：动态导入模板字段配置
--      运营可在后台维护各业务（题库/标签/文章/面经/笔记等）的导入模板字段，
--      无需改代码即可调整列名、说明、示例、必填、字典、下拉值
--   2. 新增题库导入导出按钮权限：cms:interview:import / cms:interview:export
--   3. 题库默认模板字段配置初始化（businessKey=interview_question）
--   4. 后续标签/文章/面经/笔记等业务复用同表结构，仅 businessKey 不同
--
-- 设计原则：
--   - 动态模板优先：导入接口优先读此表生成 Excel 模板，未命中则回退 @Excel 注解
--   - 失败行可重导：导入返回 ImportResult（成功/失败统计 + 失败明细），失败行可导出修正后重导
--   - 字段配置驱动：列顺序、必填标记、下拉值、列宽均由配置决定，前端无需硬编码
--
-- 适配：MySQL 8.0+（utf8mb4 / utf8mb4_0900_ai_ci）
-- 幂等：所有 DDL/DML 使用 IF NOT EXISTS / NOT EXISTS 校验，可重复执行
-- =====================================================================

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

SELECT '================================================' AS info;
SELECT '墨韵智库 v8.2 通用导入模板配置 + 题库导入导出权限升级开始' AS info;
SELECT CONCAT('数据库: ', CONVERT(DATABASE() USING utf8mb4) COLLATE utf8mb4_0900_ai_ci, ' | 时间: ', NOW()) AS info;
SELECT '================================================' AS info;


-- =====================================================================
-- 一、导入模板字段配置表 portal_import_template_config
-- =====================================================================
-- 设计：
--   - business_key：业务标识（interview_question / interview_experience / article / tag / note 等）
--   - field_name：实体 Java 属性名，用于反射取值/赋值
--   - column_name：Excel 表头列名（中文显示）
--   - description：模板第2行字段说明（如"必填，不超过500字"）
--   - example_value：模板第3行示例数据
--   - required：1=必填 0=可选（表头追加 " *" 标记）
--   - field_type：string / number / date / dict
--   - dict_type：字典 type（field_type=dict 时生效）
--   - combo_values：下拉可选值（逗号分隔，优先于 dict_type）
--   - column_width：Excel 列宽（默认 20）
--   - sort：列顺序（升序）
--   - status：0=启用 1=停用
CREATE TABLE IF NOT EXISTS portal_import_template_config (
  id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  business_key    VARCHAR(64) NOT NULL                COMMENT '业务标识（interview_question/interview_experience/article/tag/note）',
  field_name      VARCHAR(64) NOT NULL                COMMENT '实体字段名（Java 属性名，如 title/difficulty）',
  column_name     VARCHAR(64) NOT NULL                COMMENT 'Excel 列名（中文表头，如"题目标题"）',
  description     VARCHAR(255) DEFAULT NULL            COMMENT '字段说明（模板第2行说明文字）',
  example_value   VARCHAR(255) DEFAULT NULL            COMMENT '示例值（模板第3行示例数据）',
  required        TINYINT      DEFAULT 0               COMMENT '是否必填：1=必填 0=可选',
  field_type      VARCHAR(20) DEFAULT 'string'        COMMENT '字段类型：string/number/date/dict',
  dict_type       VARCHAR(64) DEFAULT NULL             COMMENT '字典 type（field_type=dict 时生效）',
  combo_values    VARCHAR(500) DEFAULT NULL            COMMENT '下拉可选值（逗号分隔，优先于 dict_type）',
  column_width    INT          DEFAULT 20              COMMENT 'Excel 列宽',
  sort            INT          DEFAULT 0                COMMENT '列排序号（升序生成列）',
  status          CHAR(1)      DEFAULT '0'              COMMENT '状态：0=启用 1=停用',
  create_by       VARCHAR(64) DEFAULT ''               COMMENT '创建者',
  create_time     DATETIME     DEFAULT NULL            COMMENT '创建时间',
  update_by       VARCHAR(64) DEFAULT ''               COMMENT '更新者',
  update_time     DATETIME     DEFAULT NULL            COMMENT '更新时间',
  remark          VARCHAR(500) DEFAULT NULL            COMMENT '备注（运营维护说明）',
  PRIMARY KEY (id),
  KEY idx_business_key_status_sort (business_key, status, sort),
  KEY idx_business_key_field (business_key, field_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='导入模板字段配置（动态模板）';

SELECT CONCAT('portal_import_template_config 表创建完成，行数: ', (SELECT COUNT(*) FROM portal_import_template_config)) AS info;


-- =====================================================================
-- 二、题库导入导出按钮权限菜单
-- =====================================================================
-- 挂在"题库资源"菜单（perms=cms:interview:list 且 menu_type='C'）下作为功能按钮
SET @interview_question_btn_parent := (SELECT menu_id FROM sys_menu WHERE perms = 'cms:interview:list' AND menu_type = 'C' LIMIT 1);

-- 题库导入按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '题库导入', @interview_question_btn_parent, 5, '#', '', 1, 0, 'F', '0', '0', 'cms:interview:import', '#', 'admin', NOW(), '题库批量导入 Excel 数据'
FROM DUAL
WHERE @interview_question_btn_parent IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:interview:import');
UPDATE sys_menu SET parent_id = @interview_question_btn_parent
WHERE perms = 'cms:interview:import' AND parent_id IS NULL AND @interview_question_btn_parent IS NOT NULL;

-- 题库导出按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '题库导出', @interview_question_btn_parent, 6, '#', '', 1, 0, 'F', '0', '0', 'cms:interview:export', '#', 'admin', NOW(), '题库按筛选条件导出 Excel'
FROM DUAL
WHERE @interview_question_btn_parent IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:interview:export');
UPDATE sys_menu SET parent_id = @interview_question_btn_parent
WHERE perms = 'cms:interview:export' AND parent_id IS NULL AND @interview_question_btn_parent IS NOT NULL;

SELECT CONCAT('题库导入按钮 menu_id: ', IFNULL((SELECT menu_id FROM sys_menu WHERE perms = 'cms:interview:import'), '未创建')) AS info;
SELECT CONCAT('题库导出按钮 menu_id: ', IFNULL((SELECT menu_id FROM sys_menu WHERE perms = 'cms:interview:export'), '未创建')) AS info;


-- =====================================================================
-- 三、为管理员角色（role_id=1）分配新按钮权限
-- =====================================================================
INSERT INTO sys_role_menu (role_id, menu_id, create_by, create_time)
SELECT 1, m.menu_id, 'admin', NOW()
FROM sys_menu m
WHERE m.perms IN ('cms:interview:import', 'cms:interview:export')
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id
  );

SELECT CONCAT('admin 角色已分配题库导入导出按钮，新增: ', ROW_COUNT()) AS info;


-- =====================================================================
-- 四、题库默认导入模板字段配置（businessKey=interview_question）
-- =====================================================================
-- 对齐 PortalInterviewQuestion 实体字段，sort 升序对应 Excel 列顺序
-- 必填字段：title（题目标题）；其余可选
-- 枚举字段下拉：difficulty / question_type / status

-- 先清理旧配置（幂等：重复执行时清空再插入）
DELETE FROM portal_import_template_config WHERE business_key = 'interview_question';

INSERT INTO portal_import_template_config
(business_key, field_name, column_name, description, example_value, required, field_type, combo_values, column_width, sort, status, create_by, create_time, remark) VALUES
('interview_question', 'title',          '题目标题',     '必填，不超过500字。题目的核心提问，如"请实现一个 LRU 缓存"', '请实现一个 LRU 缓存', 1, 'string', NULL,                            40,  1, '0', 'admin', NOW(), '题目标题，主键索引列'),
('interview_question', 'description',    '题目描述',     '可选，题目背景与详细说明', '设计一个符合 LRU 缓存淘汰策略的数据结构', 0, 'string', NULL,                            50,  2, '0', 'admin', NOW(), '题目描述，富文本/纯文本'),
('interview_question', 'questionType',   '题目类型',     '可选，下拉：bagwen八股/algorithm算法/system_design系统设计/project项目/hr', 'algorithm', 0, 'dict', 'bagwen,algorithm,system_design,project,hr', 16, 3, '0', 'admin', NOW(), '题目类型，决定作答模式'),
('interview_question', 'difficulty',    '难度',         '可选，下拉：easy简单/medium中等/hard困难', 'medium', 0, 'dict', 'easy,medium,hard', 12,  4, '0', 'admin', NOW(), '难度，影响推荐排序'),
('interview_question', 'categoryId',     '分类ID',       '可选，对应 portal_interview_category.id，留空则不归类', '10', 0, 'number', NULL,                           10,  5, '0', 'admin', NOW(), '分类ID，可空'),
('interview_question', 'tags',           '标签',         '可选，多个用英文逗号分隔，会同步标签引用计数', 'LRU,哈希表,链表', 0, 'string', NULL,                        30,  6, '0', 'admin', NOW(), '标签，逗号分隔'),
('interview_question', 'companies',      '公司',         '可选，多个用英文逗号分隔', '字节跳动,阿里巴巴', 0, 'string', NULL,                              30,  7, '0', 'admin', NOW(), '关联公司，逗号分隔'),
('interview_question', 'hint',           '提示',         '可选，作答提示', '注意考虑 O(1) 时间复杂度', 0, 'string', NULL,                                 30,  8, '0', 'admin', NOW(), '作答提示'),
('interview_question', 'solution',      '参考答案',     '可选，代码题填参考代码；其他题型建议填 referenceAnswer', NULL, 0, 'string', NULL,                            50,  9, '0', 'admin', NOW(), '旧字段：参考代码片段'),
('interview_question', 'referenceAnswer','官方参考答案', '可选，Markdown 格式，推荐用于八股/设计/项目/HR 题型完整答案', '## 解题思路\n使用哈希表 + 双向链表...', 0, 'string', NULL, 50, 10, '0', 'admin', NOW(), '官方参考答案，Markdown'),
('interview_question', 'examinePoints',  '考察点',       '可选，JSON 数组字符串，如 ["TCP三次握手"]', '["LRU策略","双向链表"]', 0, 'string', NULL,                     30, 11, '0', 'admin', NOW(), '考察点，JSON 数组'),
('interview_question', 'answerOutline',  '答题大纲',     '可选，Markdown 结构化答题思路', '## 步骤1\n...\n## 步骤2\n...', 0, 'string', NULL, 40, 12, '0', 'admin', NOW(), '答题大纲，Markdown'),
('interview_question', 'scoringCriteria', '评分标准',    '可选，JSON 数组字符串，每项含 dimension/weight/description', NULL, 0, 'string', NULL,                       40, 13, '0', 'admin', NOW(), '评分标准，JSON 数组'),
('interview_question', 'prerequisiteIds','前置题目ID',   '可选，多个用英文逗号分隔', '12,15', 0, 'string', NULL,                                       20, 14, '0', 'admin', NOW(), '前置题目ID，逗号分隔'),
('interview_question', 'sort',           '排序',         '可选，数字越小越靠前，默认 0', '0', 0, 'number', NULL,                                                8, 15, '0', 'admin', NOW(), '排序号'),
('interview_question', 'status',         '状态',         '可选，下拉：draft草稿/published已发布/archived已归档，默认 published（导入默认即发布可见）', 'published', 0, 'dict', 'draft,published,archived', 14, 16, '0', 'admin', NOW(), '状态，影响前台展示');

SELECT CONCAT('题库默认导入模板配置完成，字段数: ',
  (SELECT COUNT(*) FROM portal_import_template_config WHERE business_key = 'interview_question')) AS info;


-- =====================================================================
-- 四-补、题库 status 历史数据兼容修复
-- =====================================================================
-- 背景：
--   PortalInterviewQuestion.status 历史注释为 active/inactive（启停语义），
--   实际前端与导入流程统一使用 draft/published/archived（内容生命周期语义）。
--   v8.2 起 buildQuestionQueryWrapper 默认查询 published（与实体注释、
--   导入默认值、前端筛选下拉一致），历史 active 数据需修正为 published 才能被默认列表查到。
-- 处理：
--   active → published（前台可见）
--   inactive → archived（不可见，与"停用"语义最接近）
--   draft / published / archived 保持不变
UPDATE portal_interview_question SET status = 'published', update_time = NOW()
WHERE status = 'active' AND del_flag = '0';

UPDATE portal_interview_question SET status = 'archived', update_time = NOW()
WHERE status = 'inactive' AND del_flag = '0';

SELECT CONCAT('题库 status 历史数据修复完成：active→published ',
  ROW_COUNT(), ' 行；inactive→archived ',
  (SELECT COUNT(*) FROM portal_interview_question WHERE status = 'inactive' AND del_flag = '0'),
  ' 行剩余（如未影响则不变）') AS info;

-- 校验修复后状态分布
SELECT status, COUNT(*) AS cnt
FROM portal_interview_question
WHERE del_flag = '0'
GROUP BY status
ORDER BY status;


-- =====================================================================
-- 四-补2、portal_interview_submission 补精选笔记字段
-- =====================================================================
-- 背景：
--   PortalInterviewSubmissionMapper.selectFeaturedByQuestion 使用了 is_featured / featured_time 两列，
--   但 init_v7.8.sql 建表语句未包含这两列，导致"精选笔记"接口抛
--   java.sql.SQLSyntaxErrorException: Unknown column 'is_featured'，前端报"操作失败"。
--   实体 PortalInterviewSubmission 已定义 isFeatured / featuredTime 字段，仅需补列。
-- 策略：幂等 ADD COLUMN（COLUMN 已存在时跳过）
SET @col := 'is_featured';
SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE portal_interview_submission ADD COLUMN is_featured TINYINT(1) DEFAULT 0 COMMENT ''是否精选笔记：1=是 0=否'' AFTER note',
    'SELECT ''is_featured 已存在，跳过'' AS info')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_interview_submission' AND COLUMN_NAME = @col
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := 'featured_time';
SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE portal_interview_submission ADD COLUMN featured_time DATETIME DEFAULT NULL COMMENT ''精选时间'' AFTER is_featured',
    'SELECT ''featured_time 已存在，跳过'' AS info')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_interview_submission' AND COLUMN_NAME = @col
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 索引（用于精选笔记查询：WHERE question_id=? AND is_featured=1 ORDER BY featured_time DESC）
SET @idx := 'idx_submission_featured';
SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE portal_interview_submission ADD INDEX idx_submission_featured (question_id, is_featured, featured_time)',
    'SELECT ''idx_submission_featured 已存在，跳过'' AS info')
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_interview_submission' AND INDEX_NAME = @idx
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT CONCAT('portal_interview_submission 精选笔记字段补齐：is_featured / featured_time / idx_submission_featured') AS info;


-- =====================================================================
-- 五、导入模板配置管理菜单（运营维护入口）
-- =====================================================================
-- 挂在"系统管理"或"内容管理"一级菜单下，运营在此维护各业务 Excel 导入模板字段
-- 注：菜单 component 路径 cms/importTemplate/index，对应前端页面需同步创建

-- 找一个合适的父菜单（内容管理 / CMS 内容管理），如果没有则挂到"系统管理"
SET @import_tpl_parent_id := (SELECT menu_id FROM sys_menu WHERE menu_name = '内容管理' AND parent_id = 0 AND menu_type = 'M' LIMIT 1);
SET @import_tpl_parent_id := IFNULL(@import_tpl_parent_id, (SELECT menu_id FROM sys_menu WHERE menu_name = '系统管理' AND parent_id = 0 AND menu_type = 'M' LIMIT 1));

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '导入模板配置', @import_tpl_parent_id, 99, 'importTemplate', 'cms/importTemplate/index', 1, 0, 'C', '0', '0', 'cms:importTemplate:list', 'edit', 'admin', NOW(), '运营维护各业务 Excel 导入模板字段（动态模板）'
FROM DUAL
WHERE @import_tpl_parent_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:importTemplate:list' AND menu_type = 'C');
UPDATE sys_menu SET parent_id = @import_tpl_parent_id
WHERE perms = 'cms:importTemplate:list' AND menu_type = 'C' AND parent_id IS NULL AND @import_tpl_parent_id IS NOT NULL;

SET @import_tpl_menu_id := (SELECT menu_id FROM sys_menu WHERE perms = 'cms:importTemplate:list' AND menu_type = 'C' LIMIT 1);

-- 维护按钮（编辑字段配置）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '保存模板字段', @import_tpl_menu_id, 1, '#', '', 1, 0, 'F', '0', '0', 'cms:importTemplate:edit', '#', 'admin', NOW(), '保存导入模板字段配置'
FROM DUAL
WHERE @import_tpl_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'cms:importTemplate:edit');
UPDATE sys_menu SET parent_id = @import_tpl_menu_id
WHERE perms = 'cms:importTemplate:edit' AND parent_id IS NULL AND @import_tpl_menu_id IS NOT NULL;

-- 为管理员角色分配菜单权限
INSERT INTO sys_role_menu (role_id, menu_id, create_by, create_time)
SELECT 1, m.menu_id, 'admin', NOW()
FROM sys_menu m
WHERE m.perms IN ('cms:importTemplate:list', 'cms:importTemplate:edit')
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id
  );

SELECT CONCAT('导入模板配置菜单 menu_id: ', IFNULL(@import_tpl_menu_id, '未创建')) AS info;


-- =====================================================================
-- 六、校验总结
-- =====================================================================
SELECT '================================================' AS info;
SELECT '校验：portal_import_template_config 表' AS info;
SELECT business_key, COUNT(*) AS field_count, SUM(required) AS required_count
FROM portal_import_template_config
GROUP BY business_key;

SELECT '校验：题库导入导出按钮权限' AS info;
SELECT menu_id, menu_name, perms, menu_type, status
FROM sys_menu
WHERE perms IN ('cms:interview:import', 'cms:interview:export');

SELECT '校验：导入模板配置管理菜单' AS info;
SELECT menu_id, menu_name, perms, menu_type, parent_id
FROM sys_menu
WHERE perms LIKE 'cms:importTemplate:%';

SELECT '校验：admin 角色已分配权限' AS info;
SELECT m.perms, rm.role_id
FROM sys_menu m
JOIN sys_role_menu rm ON rm.menu_id = m.menu_id
WHERE m.perms IN ('cms:interview:import', 'cms:interview:export',
                  'cms:importTemplate:list', 'cms:importTemplate:edit') AND rm.role_id = 1;

SELECT '================================================' AS info;
SELECT '墨韵智库 v8.2 通用导入模板配置 + 题库导入导出权限升级完成' AS info;
SELECT CONCAT('完成时间: ', NOW()) AS info;
SELECT '================================================' AS info;
