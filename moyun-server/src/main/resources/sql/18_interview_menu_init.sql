-- =============================================
-- 面试空间模块 - 菜单和权限初始化脚本
-- 版本：1.0
-- 日期：2026-06-18
-- 说明：为墨韵智库面试空间模块添加后台管理菜单和权限
-- 依赖：需要先执行 04_cms_menu_init.sql（内容管理目录已存在）
-- =============================================

-- 1. 获取"内容管理"目录的 menu_id
SET @cms_menu_id = (SELECT menu_id FROM sys_menu WHERE menu_name = '内容管理' AND menu_type = 'M' LIMIT 1);

-- 如果"内容管理"目录不存在，则创建
INSERT IGNORE INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '内容管理', 0, 10, 'cms', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'documentation', 'admin', NOW(), '', NULL, '内容管理目录'
);

-- 重新获取 menu_id（如果刚刚插入）
SET @cms_menu_id = (SELECT menu_id FROM sys_menu WHERE menu_name = '内容管理' AND menu_type = 'M' LIMIT 1);

-- =============================================
-- 2. 插入二级菜单：面试管理（目录）
-- =============================================
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '面试管理', @cms_menu_id, 8, 'interview', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'education', 'admin', NOW(), '', NULL, '面试管理目录'
);

SET @interview_menu_id = LAST_INSERT_ID();

-- =============================================
-- 3. 题目管理
-- =============================================
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '题目管理', @interview_menu_id, 1, 'question', 'cms/interview/question/index', NULL, 1, 0, 'C', '0', '0', 'cms:interview:list', 'list', 'admin', NOW(), '', NULL, '面试题目管理菜单'
);

SET @question_menu_id = LAST_INSERT_ID();

INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES
('题目查询', @question_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:query', '#', 'admin', NOW(), '', NULL, ''),
('题目新增', @question_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:add', '#', 'admin', NOW(), '', NULL, ''),
('题目修改', @question_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:edit', '#', 'admin', NOW(), '', NULL, ''),
('题目删除', @question_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:remove', '#', 'admin', NOW(), '', NULL, '');

-- =============================================
-- 4. 分类管理
-- =============================================
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '面试分类', @interview_menu_id, 2, 'category', 'cms/interview/category/index', NULL, 1, 0, 'C', '0', '0', 'cms:interview:list', 'tree-table', 'admin', NOW(), '', NULL, '面试分类管理菜单'
);

SET @category_menu_id = LAST_INSERT_ID();

INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES
('分类查询', @category_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:query', '#', 'admin', NOW(), '', NULL, ''),
('分类新增', @category_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:add', '#', 'admin', NOW(), '', NULL, ''),
('分类修改', @category_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:edit', '#', 'admin', NOW(), '', NULL, ''),
('分类删除', @category_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:remove', '#', 'admin', NOW(), '', NULL, '');

-- =============================================
-- 5. 面经管理
-- =============================================
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '面经管理', @interview_menu_id, 3, 'experience', 'cms/interview/experience/index', NULL, 1, 0, 'C', '0', '0', 'cms:interview:list', 'documentation', 'admin', NOW(), '', NULL, '面试面经管理菜单'
);

SET @experience_menu_id = LAST_INSERT_ID();

INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES
('面经查询', @experience_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:query', '#', 'admin', NOW(), '', NULL, ''),
('面经审核', @experience_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:edit', '#', 'admin', NOW(), '', NULL, ''),
('面经置顶', @experience_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:edit', '#', 'admin', NOW(), '', NULL, ''),
('面经删除', @experience_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:remove', '#', 'admin', NOW(), '', NULL, '');

-- =============================================
-- 6. 评论管理
-- =============================================
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '面试评论', @interview_menu_id, 4, 'comment', 'cms/interview/comment/index', NULL, 1, 0, 'C', '0', '0', 'cms:interview:list', 'message', 'admin', NOW(), '', NULL, '面试评论管理菜单'
);

SET @comment_menu_id = LAST_INSERT_ID();

INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES
('评论查询', @comment_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:query', '#', 'admin', NOW(), '', NULL, ''),
('评论审核', @comment_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:edit', '#', 'admin', NOW(), '', NULL, ''),
('评论删除', @comment_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:remove', '#', 'admin', NOW(), '', NULL, '');

-- =============================================
-- 7. 简历模板管理
-- =============================================
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '简历模板', @interview_menu_id, 5, 'resume', 'cms/interview/resume/index', NULL, 1, 0, 'C', '0', '0', 'cms:interview:list', 'example', 'admin', NOW(), '', NULL, '简历模板管理菜单'
);

SET @resume_menu_id = LAST_INSERT_ID();

INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES
('模板查询', @resume_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:query', '#', 'admin', NOW(), '', NULL, ''),
('模板新增', @resume_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:add', '#', 'admin', NOW(), '', NULL, ''),
('模板修改', @resume_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:edit', '#', 'admin', NOW(), '', NULL, ''),
('模板删除', @resume_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:remove', '#', 'admin', NOW(), '', NULL, '');

-- =============================================
-- 8. 公司标签管理
-- =============================================
INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES (
    '公司管理', @interview_menu_id, 6, 'company', 'cms/interview/company/index', NULL, 1, 0, 'C', '0', '0', 'cms:interview:list', 'peoples', 'admin', NOW(), '', NULL, '面试公司管理菜单'
);

SET @company_menu_id = LAST_INSERT_ID();

INSERT INTO `sys_menu`(
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES
('公司查询', @company_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:query', '#', 'admin', NOW(), '', NULL, ''),
('公司新增', @company_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:add', '#', 'admin', NOW(), '', NULL, ''),
('公司修改', @company_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:edit', '#', 'admin', NOW(), '', NULL, ''),
('公司删除', @company_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:interview:remove', '#', 'admin', NOW(), '', NULL, '');

-- =============================================
-- 为管理员角色分配面试空间菜单权限
-- =============================================

-- 获取管理员角色的ID（通常为1）
SET @admin_role_id = 1;

-- 删除旧的面试空间权限（如果存在），避免重复
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (
    SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` IN (
        '面试管理', '题目管理', '面试分类', '面经管理', '面试评论', '简历模板', '公司管理'
    ) OR `parent_id` IN (
        SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` IN (
            '面试管理', '题目管理', '面试分类', '面经管理', '面试评论', '简历模板', '公司管理'
        )
    )
);

-- 重新为管理员角色分配面试空间权限
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @admin_role_id, `menu_id` FROM `sys_menu`
WHERE `menu_name` IN (
    '面试管理', '题目管理', '面试分类', '面经管理', '面试评论', '简历模板', '公司管理'
) OR `parent_id` IN (
    SELECT `menu_id` FROM `sys_menu` WHERE `menu_name` IN (
        '面试管理', '题目管理', '面试分类', '面经管理', '面试评论', '简历模板', '公司管理'
    )
);

-- =============================================
-- 脚本执行完成
-- =============================================

SELECT '面试空间菜单和权限初始化脚本执行完成！' AS message;
SELECT '注意：本脚本依赖"内容管理"一级菜单已存在，请先执行 04_cms_menu_init.sql' AS note;