-- =====================================================
-- 话题讨论模块 DDL 脚本（任务 4.x 社区深化）
-- 新设计：话题 + 观点（楼层）+ 观点点赞 + 多态评论 + 评论点赞
-- 与旧版 71_topic_init.sql 的 tag 聚合型话题完全不同，需要 DROP 重建
-- 幂等设计：可重复执行
-- =====================================================

-- =============================================
-- 2.1 清理旧版话题表（71_topic_init.sql 创建的 tag 聚合型，新设计完全不同）
-- =============================================
DROP TABLE IF EXISTS portal_topic_follow;
DROP TABLE IF EXISTS portal_topic_like;
DROP TABLE IF EXISTS portal_topic;
-- 清理旧版后台菜单
DELETE FROM sys_role_menu WHERE menu_id IN (
    SELECT menu_id FROM (SELECT * FROM sys_menu) tmp WHERE menu_name IN ('话题管理', '话题列表') OR perms LIKE 'portal:topic:%'
);
DELETE FROM sys_menu WHERE menu_name IN ('话题管理', '话题列表') OR perms LIKE 'portal:topic:%';

-- =============================================
-- 2.2 创建 6 张新表（5 张主表 + 1 张话题点赞表，话题点赞表为 toggleTopicLike 必需）
-- =============================================

-- 1. 话题主表
CREATE TABLE portal_topic (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    title           VARCHAR(128) NOT NULL                COMMENT '话题标题',
    description     VARCHAR(500) DEFAULT NULL            COMMENT '话题描述/导语',
    cover           VARCHAR(500) DEFAULT NULL            COMMENT '封面图 URL',
    creator_id      BIGINT       NOT NULL                COMMENT '发起人 portal_user.id（必须是认证创作者）',
    status          VARCHAR(20)  NOT NULL DEFAULT 'active' COMMENT '状态：active 活跃/archived 归档/deleted 删除',
    pinned          TINYINT      NOT NULL DEFAULT 0     COMMENT '是否置顶：0 否/1 是',
    view_count      INT          NOT NULL DEFAULT 0     COMMENT '浏览数',
    post_count      INT          NOT NULL DEFAULT 0     COMMENT '观点数',
    like_count      INT          NOT NULL DEFAULT 0     COMMENT '话题被赞数',
    is_featured     TINYINT      NOT NULL DEFAULT 0     COMMENT '是否精选：0 否/1 是',
    comment_count   INT          NOT NULL DEFAULT 0     COMMENT '评论数（一级评论）',
    last_post_time  DATETIME     DEFAULT NULL            COMMENT '最后观点时间',
    last_poster_id  BIGINT       DEFAULT NULL            COMMENT '最后观点用户',
    created_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time    DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_creator_time (creator_id, created_time),
    KEY idx_status_pinned_last (status, pinned, last_post_time),
    KEY idx_last_post (last_post_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf4mb4 COMMENT='话题主表';

-- 2. 话题观点表（楼层）
CREATE TABLE portal_topic_post (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    topic_id        BIGINT       NOT NULL                COMMENT '所属话题',
    user_id         BIGINT       NOT NULL                COMMENT '发布者 portal_user.id',
    content         TEXT         NOT NULL                COMMENT '观点内容（Markdown）',
    images          JSON         DEFAULT NULL            COMMENT '图片 URL 列表，最多 9 张',
    parent_post_id  BIGINT       DEFAULT NULL            COMMENT '父观点 ID（楼中楼，NULL 为一级观点）',
    reply_to_user_id BIGINT      DEFAULT NULL            COMMENT '回复的用户 ID',
    floor           INT          NOT NULL DEFAULT 0     COMMENT '楼层号',
    like_count      INT          NOT NULL DEFAULT 0,
    comment_count   INT          NOT NULL DEFAULT 0,
    is_deleted      TINYINT      NOT NULL DEFAULT 0     COMMENT '软删：0 否/1 是',
    created_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time    DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_topic_floor (topic_id, floor),
    KEY idx_topic_time (topic_id, created_time),
    KEY idx_user_time (user_id, created_time),
    KEY idx_parent (parent_post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf4mb4 COMMENT='话题观点（楼层）';

-- 3. 观点点赞表
CREATE TABLE portal_topic_post_like (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    post_id     BIGINT   NOT NULL,
    user_id     BIGINT   NOT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_post_user (post_id, user_id),
    KEY idx_user_time (user_id, created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf4mb4 COMMENT='话题观点点赞';

-- 3.1 话题点赞表（toggleTopicLike 必需，存储话题维度的点赞关系）
CREATE TABLE portal_topic_like (
    id           BIGINT   NOT NULL AUTO_INCREMENT,
    topic_id     BIGINT   NOT NULL,
    user_id      BIGINT   NOT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_topic_user (topic_id, user_id),
    KEY idx_user_time (user_id, created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf4mb4 COMMENT='话题点赞';

-- 4. 话题评论表（多态：可评论话题或观点）
CREATE TABLE portal_topic_comment (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    target_type       VARCHAR(20)  NOT NULL                COMMENT '目标类型：topic 话题评论 / post 观点评论',
    target_id         BIGINT       NOT NULL                COMMENT '目标 ID',
    author_id         BIGINT       NOT NULL                COMMENT '评论者 portal_user.id',
    content           VARCHAR(2000) NOT NULL               COMMENT '评论内容',
    parent_id         BIGINT       NOT NULL DEFAULT 0     COMMENT '父评论 ID（0=一级评论）',
    root_id           BIGINT       NOT NULL DEFAULT 0     COMMENT '根评论 ID（一级评论 root_id=0）',
    reply_to          BIGINT       DEFAULT NULL            COMMENT '被回复的用户 ID',
    reply_to_content  VARCHAR(200) DEFAULT ''              COMMENT '被回复内容摘要',
    like_count        INT          NOT NULL DEFAULT 0,
    reply_count       INT          NOT NULL DEFAULT 0     COMMENT '回复数（仅一级评论维护）',
    is_deleted        TINYINT      NOT NULL DEFAULT 0     COMMENT '软删',
    created_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time      DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_target_type_id_parent (target_type, target_id, parent_id, created_time),
    KEY idx_root (root_id, created_time),
    KEY idx_author_time (author_id, created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf4mb4 COMMENT='话题评论（多态）';

-- 5. 评论点赞表
CREATE TABLE portal_topic_comment_like (
    id           BIGINT   NOT NULL AUTO_INCREMENT,
    comment_id   BIGINT   NOT NULL,
    user_id      BIGINT   NOT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_comment_user (comment_id, user_id),
    KEY idx_user_time (user_id, created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf4mb4 COMMENT='话题评论点赞';

-- =============================================
-- 2.3 portal_user 表扩展：新增 is_certified_creator 字段
-- 幂等：使用 INFORMATION_SCHEMA 检查列是否存在
-- =============================================
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_user' AND COLUMN_NAME = 'is_certified_creator');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE portal_user ADD COLUMN is_certified_creator TINYINT NOT NULL DEFAULT 0 COMMENT ''是否认证创作者：0 否/1 是'' AFTER role',
    'SELECT ''column is_certified_creator already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =============================================
-- 2.4 删除「社区互动」一级分类及其所有子分类
-- 幂等：直接 DELETE，不存在时无影响
-- 用子查询查出社区互动的 ID，避免硬编码 ID（不依赖 parent_id=7）
-- =============================================
-- 删除社区互动的二级分类
DELETE FROM portal_category WHERE parent_id IN (
    SELECT id FROM (SELECT id FROM portal_category WHERE name = '社区互动' AND parent_id = 0) tmp
);
-- 删除社区互动一级分类本身
DELETE FROM portal_category WHERE name = '社区互动' AND parent_id = 0;

-- =============================================
-- 2.5 后台菜单初始化：互动管理一级目录
-- 路径：/cms/topic，组件：cms/topic/index
-- 参考 63_creator_certification_init.sql 的菜单初始化风格
-- =============================================

-- 0. 清理旧的菜单数据（避免重复执行时报错）
DELETE FROM sys_role_menu WHERE menu_id IN (
    SELECT menu_id FROM (SELECT * FROM sys_menu) tmp WHERE menu_name IN ('互动管理', '话题管理', '观点管理', '评论管理') OR perms LIKE 'cms:topic:%'
);
DELETE FROM sys_menu WHERE menu_name IN ('互动管理', '话题管理', '观点管理', '评论管理') OR perms LIKE 'cms:topic:%';

-- 1. 插入一级目录：互动管理
INSERT INTO sys_menu(
    menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark
) VALUES (
    '互动管理', 0, 15, 'interaction', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'message-square', 'admin', NOW(), '', NULL, '互动管理目录'
);

SET @interaction_dir_id = LAST_INSERT_ID();

-- 2.1 二级菜单：话题管理
INSERT INTO sys_menu(
    menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark
) VALUES (
    '话题管理', @interaction_dir_id, 1, 'topic', 'cms/topic/index', NULL, 1, 0, 'C', '0', '0', 'cms:topic:list', 'message-square', 'admin', NOW(), '', NULL, '话题管理菜单'
);

SET @topic_menu_id = LAST_INSERT_ID();

INSERT INTO sys_menu(
    menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark
) VALUES
('话题查询', @topic_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:topic:query', '#', 'admin', NOW(), '', NULL, ''),
('话题新增', @topic_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:topic:add', '#', 'admin', NOW(), '', NULL, ''),
('话题修改', @topic_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:topic:edit', '#', 'admin', NOW(), '', NULL, ''),
('话题删除', @topic_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:topic:remove', '#', 'admin', NOW(), '', NULL, '');

-- 2.2 二级菜单：观点管理
INSERT INTO sys_menu(
    menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark
) VALUES (
    '观点管理', @interaction_dir_id, 2, 'post', 'cms/topic/post', NULL, 1, 0, 'C', '0', '0', 'cms:topic:post', 'edit', 'admin', NOW(), '', NULL, '观点管理菜单'
);

SET @post_menu_id = LAST_INSERT_ID();

INSERT INTO sys_menu(
    menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark
) VALUES
('观点查询', @post_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:topic:post:query', '#', 'admin', NOW(), '', NULL, ''),
('观点新增', @post_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:topic:post:add', '#', 'admin', NOW(), '', NULL, ''),
('观点修改', @post_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:topic:post:edit', '#', 'admin', NOW(), '', NULL, ''),
('观点删除', @post_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:topic:post:remove', '#', 'admin', NOW(), '', NULL, '');

-- 2.3 二级菜单：评论管理
INSERT INTO sys_menu(
    menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark
) VALUES (
    '评论管理', @interaction_dir_id, 3, 'comment', 'cms/topic/comment', NULL, 1, 0, 'C', '0', '0', 'cms:topic:comment', 'message', 'admin', NOW(), '', NULL, '评论管理菜单'
);

SET @comment_menu_id = LAST_INSERT_ID();

INSERT INTO sys_menu(
    menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark
) VALUES
('评论查询', @comment_menu_id, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:topic:comment:query', '#', 'admin', NOW(), '', NULL, ''),
('评论新增', @comment_menu_id, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:topic:comment:add', '#', 'admin', NOW(), '', NULL, ''),
('评论修改', @comment_menu_id, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:topic:comment:edit', '#', 'admin', NOW(), '', NULL, ''),
('评论删除', @comment_menu_id, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'cms:topic:comment:remove', '#', 'admin', NOW(), '', NULL, '');

-- 3. 为管理员角色分配互动管理菜单权限
SET @admin_role_id = 1;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @admin_role_id, menu_id FROM sys_menu
WHERE menu_name IN ('互动管理', '话题管理', '观点管理', '评论管理')
   OR parent_id IN (
       SELECT menu_id FROM (SELECT * FROM sys_menu) tmp
       WHERE menu_name IN ('话题管理', '观点管理', '评论管理')
   );

-- =============================================
-- 2.6 新增成长规则
-- 参考 portal_growth_rule 表结构，实际字段名为 growth_delta（非 growth_value）
-- daily_limit 与 19_growth_system_init.sql 保持一致：0 表示不限
-- uk_module_action 唯一索引保证幂等，使用 INSERT IGNORE
-- =============================================
INSERT IGNORE INTO portal_growth_rule (module, action, growth_delta, daily_limit, description, status, create_by, create_time) VALUES
('topic', 'create_topic',           10, 0, '发起话题',           '0', 'admin', NOW()),
('topic', 'post_opinion',            2, 10,   '发表观点',           '0', 'admin', NOW()),
('topic', 'receive_topic_like',      2, 0, '话题被点赞',         '0', 'admin', NOW()),
('topic', 'receive_post_like',       2, 0, '观点被点赞',         '0', 'admin', NOW()),
('topic', 'receive_topic_comment',   2, 0, '话题被评论',         '0', 'admin', NOW()),
('topic', 'receive_post_comment',    2, 0, '观点被评论',         '0', 'admin', NOW()),
('topic', 'receive_comment_like',    2, 0, '评论被点赞',         '0', 'admin', NOW()),
('topic', 'topic_featured',         50, 0, '话题被精选',         '0', 'admin', NOW());

SELECT '话题讨论模块初始化脚本执行完成！' AS message;
