-- =============================================
-- V19: 全局用户成长体系建表 + 初始化数据
-- 包含：成长值总表、成长事件流水、用户统计聚合、成长规则、成就定义、用户徽章
-- @author moyun
-- =============================================

-- ----------------------------
-- 1. 用户成长值总表
-- ----------------------------
DROP TABLE IF EXISTS `portal_user_growth`;
CREATE TABLE `portal_user_growth` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`       BIGINT UNSIGNED NOT NULL COMMENT '门户用户ID（portal_user.id）',
    `growth_value`  INT UNSIGNED    DEFAULT 0 COMMENT '成长值（累计，只增不减）',
    `level`         INT             DEFAULT 1 COMMENT '当前等级',
    `title`         VARCHAR(50)     DEFAULT '初出茅庐' COMMENT '当前头衔',
    `season_value`  INT UNSIGNED    DEFAULT 0 COMMENT '本季成长值（赛季排名用）',
    `create_by`     VARCHAR(64)     DEFAULT '' COMMENT '创建者',
    `create_time`   DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`     VARCHAR(64)     DEFAULT '' COMMENT '更新者',
    `update_time`   DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`        VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user` (`user_id`),
    KEY `idx_season` (`season_value` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户成长值总表';

-- ----------------------------
-- 2. 成长事件流水表
-- ----------------------------
DROP TABLE IF EXISTS `portal_growth_log`;
CREATE TABLE `portal_growth_log` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`        BIGINT UNSIGNED NOT NULL COMMENT '获得成长值的用户ID',
    `target_user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '目标用户ID（如被点赞的内容作者）',
    `module`         VARCHAR(32)     NOT NULL COMMENT '来源模块: article/reading/interview/all',
    `action`         VARCHAR(64)     NOT NULL COMMENT '行为: publish_article/solve_question/finish_book/...',
    `entity_type`    VARCHAR(32)     DEFAULT NULL COMMENT '实体类型: article/book/question/note/experience',
    `entity_id`      BIGINT          DEFAULT NULL COMMENT '实体ID',
    `growth_delta`   INT             NOT NULL COMMENT '成长值变化（正数增加，负数减少）',
    `description`    VARCHAR(255)    DEFAULT NULL COMMENT '描述',
    `create_by`      VARCHAR(64)     DEFAULT '' COMMENT '创建者',
    `create_time`    DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`      VARCHAR(64)     DEFAULT '' COMMENT '更新者',
    `update_time`    DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`         VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_user_time` (`user_id`, `create_time`),
    KEY `idx_module_action` (`module`, `action`),
    KEY `idx_entity` (`entity_type`, `entity_id`),
    KEY `idx_target_user` (`target_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成长事件流水表';

-- ----------------------------
-- 3. 用户统计聚合表
-- ----------------------------
DROP TABLE IF EXISTS `portal_user_stats`;
CREATE TABLE `portal_user_stats` (
    `id`                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`               BIGINT UNSIGNED NOT NULL COMMENT '门户用户ID',
    `article_count`         INT             DEFAULT 0 COMMENT '发布文章数',
    `article_view_sum`      BIGINT          DEFAULT 0 COMMENT '文章总浏览量',
    `article_like_sum`      BIGINT          DEFAULT 0 COMMENT '文章总获赞数',
    `article_bookmark_sum`  BIGINT          DEFAULT 0 COMMENT '文章总收藏数',
    `article_word_sum`      BIGINT          DEFAULT 0 COMMENT '累计创作字数',
    `book_finished`         INT             DEFAULT 0 COMMENT '读完的书',
    `booklist_count`        INT             DEFAULT 0 COMMENT '创建书单数',
    `quote_count`           INT             DEFAULT 0 COMMENT '发布金句数',
    `reading_minutes`       BIGINT          DEFAULT 0 COMMENT '累计阅读时长(分钟)',
    `question_solved`       INT             DEFAULT 0 COMMENT '解题数',
    `note_count`            INT             DEFAULT 0 COMMENT '笔记数',
    `experience_count`      INT             DEFAULT 0 COMMENT '面经数',
    `note_adopted`          INT             DEFAULT 0 COMMENT '笔记被精选数',
    `follower_count`        INT             DEFAULT 0 COMMENT '粉丝数',
    `following_count`       INT             DEFAULT 0 COMMENT '关注数',
    `comment_count`         INT             DEFAULT 0 COMMENT '跨模块评论总数',
    `total_like_received`   BIGINT          DEFAULT 0 COMMENT '跨模块总获赞',
    `checkin_streak`        INT             DEFAULT 0 COMMENT '连续签到天数',
    `last_checkin_date`     DATE            DEFAULT NULL COMMENT '最后签到日期',
    `create_by`             VARCHAR(64)     DEFAULT '' COMMENT '创建者',
    `create_time`           DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`             VARCHAR(64)     DEFAULT '' COMMENT '更新者',
    `update_time`           DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`                VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门户用户统计聚合表';

-- ----------------------------
-- 4. 成长规则配置表
-- ----------------------------
DROP TABLE IF EXISTS `portal_growth_rule`;
CREATE TABLE `portal_growth_rule` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `module`        VARCHAR(32)     NOT NULL COMMENT '模块: article/reading/interview/all',
    `action`        VARCHAR(64)     NOT NULL COMMENT '行为编码',
    `growth_delta`  INT             NOT NULL COMMENT '成长值',
    `daily_limit`   INT             DEFAULT 0 COMMENT '每日上限（0=不限）',
    `description`   VARCHAR(255)    DEFAULT NULL COMMENT '描述',
    `status`        CHAR(1)         DEFAULT '0' COMMENT '状态（0启用 1停用）',
    `sort`          INT             DEFAULT 0 COMMENT '排序',
    `create_by`     VARCHAR(64)     DEFAULT '' COMMENT '创建者',
    `create_time`   DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`     VARCHAR(64)     DEFAULT '' COMMENT '更新者',
    `update_time`   DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`        VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_module_action` (`module`, `action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成长规则配置表';

-- ----------------------------
-- 5. 成就定义表
-- ----------------------------
DROP TABLE IF EXISTS `portal_achievement`;
CREATE TABLE `portal_achievement` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code`           VARCHAR(64)     NOT NULL COMMENT '成就编码',
    `name`           VARCHAR(100)    NOT NULL COMMENT '成就名称',
    `description`    VARCHAR(255)    DEFAULT NULL COMMENT '成就描述',
    `icon`           VARCHAR(500)    DEFAULT NULL COMMENT '图标URL',
    `module`         VARCHAR(32)     DEFAULT NULL COMMENT '所属模块: article/reading/interview/all',
    `condition_json` TEXT            DEFAULT NULL COMMENT '达成条件JSON',
    `growth_reward`  INT             DEFAULT 0 COMMENT '达成奖励成长值',
    `sort`           INT             DEFAULT 0 COMMENT '排序',
    `status`         CHAR(1)         DEFAULT '0' COMMENT '状态（0启用 1停用）',
    `create_by`      VARCHAR(64)     DEFAULT '' COMMENT '创建者',
    `create_time`    DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`      VARCHAR(64)     DEFAULT '' COMMENT '更新者',
    `update_time`    DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`         VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成就定义表';

-- ----------------------------
-- 6. 用户徽章记录表
-- ----------------------------
DROP TABLE IF EXISTS `portal_user_badge`;
CREATE TABLE `portal_user_badge` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`        BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `achievement_id` BIGINT UNSIGNED NOT NULL COMMENT '成就ID',
    `create_by`      VARCHAR(64)     DEFAULT '' COMMENT '创建者',
    `create_time`    DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '获得时间',
    `update_by`      VARCHAR(64)     DEFAULT '' COMMENT '更新者',
    `update_time`    DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`         VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_achievement` (`user_id`, `achievement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户徽章记录表';

-- =============================================
-- 初始化数据：成长规则
-- =============================================
INSERT INTO `portal_growth_rule` (`module`, `action`, `growth_delta`, `daily_limit`, `description`, `status`, `sort`) VALUES
-- 文章模块
('article', 'publish_article',      50,  3,  '发布文章',           '0', 1),
('article', 'receive_like',          2,  0,  '文章被点赞',         '0', 2),
('article', 'receive_bookmark',      3,  0,  '文章被收藏',         '0', 3),
('article', 'receive_follow',        5,  0,  '被关注',             '0', 4),
('article', 'article_featured',    100,  0,  '文章被精选',         '0', 5),
('article', 'receive_comment',       2,  0,  '文章被评论',         '0', 6),
-- 读书空间
('reading', 'finish_book',          20,  1,  '完成阅读一本书',     '0', 10),
('reading', 'write_quote',          15,  0,  '发布金句',           '0', 11),
('reading', 'create_booklist',      20,  0,  '创建书单',           '0', 12),
('reading', 'quote_liked',           5,  0,  '金句被点赞',         '0', 13),
('reading', 'booklist_liked',        5,  0,  '书单被点赞',         '0', 14),
('reading', 'booklist_bookmarked',  10,  0,  '书单被收藏',         '0', 15),
-- 面试空间
('interview', 'solve_question',     10, 20,  '解题',               '0', 20),
('interview', 'write_note',         15,  0,  '写笔记',             '0', 21),
('interview', 'note_adopted',       50,  0,  '笔记被精选',         '0', 22),
('interview', 'publish_experience', 30,  0,  '发布面经',           '0', 23),
('interview', 'experience_liked',    2,  0,  '面经被点赞',         '0', 24),
('interview', 'experience_bookmarked', 3, 0, '面经被收藏',         '0', 25),
-- 通用
('all', 'daily_checkin',             1,  1,  '每日签到',           '0', 30),
('all', 'daily_login',               1,  1,  '每日登录',           '0', 31);

-- =============================================
-- 初始化数据：成就定义
-- =============================================
INSERT INTO `portal_achievement` (`code`, `name`, `description`, `icon`, `module`, `condition_json`, `growth_reward`, `sort`, `status`) VALUES
-- 文章模块成就
('first_article',        '初露锋芒',     '发布第一篇文章',           NULL, 'article',  '{"action":"publish_article","count":1}',     20,  1,  '0'),
('article_10',           '勤勉作者',     '发布10篇文章',             NULL, 'article',  '{"action":"publish_article","count":10}',    50,  2,  '0'),
('article_50',           '高产作者',     '发布50篇文章',             NULL, 'article',  '{"action":"publish_article","count":50}',    200, 3,  '0'),
('article_featured',     '精华创作者',   '文章被精选',               NULL, 'article',  '{"action":"article_featured","count":1}',   100, 4,  '0'),
('article_100_likes',    '人气作者',     '单篇文章获赞100',          NULL, 'article',  '{"action":"receive_like","count":100}',     50,  5,  '0'),
-- 读书空间成就
('first_book',           '开卷有益',     '完成阅读第一本书',         NULL, 'reading',  '{"action":"finish_book","count":1}',        20,  10, '0'),
('book_worm_10',         '书虫',         '完成阅读10本书',           NULL, 'reading',  '{"action":"finish_book","count":10}',       100, 11, '0'),
('book_worm_50',         '阅读达人',     '完成阅读50本书',           NULL, 'reading',  '{"action":"finish_book","count":50}',       300, 12, '0'),
('first_booklist',       '书单策划',     '创建第一个书单',           NULL, 'reading',  '{"action":"create_booklist","count":1}',    20,  13, '0'),
('quote_master',         '金句达人',     '发布20条金句',             NULL, 'reading',  '{"action":"write_quote","count":20}',       50,  14, '0'),
-- 面试空间成就
('first_solve',          '初试身手',     '解答第一道面试题',         NULL, 'interview','{"action":"solve_question","count":1}',      10,  20, '0'),
('solve_50',             '刷题能手',     '解答50道面试题',           NULL, 'interview','{"action":"solve_question","count":50}',     100, 21, '0'),
('solve_200',            '面试达人',     '解答200道面试题',          NULL, 'interview','{"action":"solve_question","count":200}',    300, 22, '0'),
('first_note',           '笔记新手',     '撰写第一篇笔记',           NULL, 'interview','{"action":"write_note","count":1}',          15,  23, '0'),
('note_adopted',         '知识贡献者',   '笔记被精选',               NULL, 'interview','{"action":"note_adopted","count":1}',        50,  24, '0'),
('first_experience',     '面经分享者',   '发布第一篇面经',           NULL, 'interview','{"action":"publish_experience","count":1}',  30,  25, '0'),
('experience_10',        '面经达人',     '发布10篇面经',             NULL, 'interview','{"action":"publish_experience","count":10}', 100, 26, '0'),
-- 通用成就
('checkin_7',            '坚持一周',     '连续签到7天',              NULL, 'all',      '{"action":"daily_checkin","count":7}',       10,  30, '0'),
('checkin_30',           '坚持一月',     '连续签到30天',             NULL, 'all',      '{"action":"daily_checkin","count":30}',      50,  31, '0'),
('level_5',              '渐入佳境',     '达到5级',                  NULL, 'all',      '{"action":"level","count":5}',               0,   32, '0'),
('level_8',              '登峰造极',     '达到8级',                  NULL, 'all',      '{"action":"level","count":8}',               0,   33, '0');

-- =============================================
-- 等级头衔映射（供 Service 层使用，不建表，硬编码在 PortalGrowthServiceImpl 中）
-- =============================================
-- 等级 | 成长值区间      | 头衔
-- 1    | 0 - 99          | 初出茅庐
-- 2    | 100 - 299       | 小试牛刀
-- 3    | 300 - 699       | 锋芒初露
-- 4    | 700 - 1499      | 登堂入室
-- 5    | 1500 - 2999     | 渐入佳境
-- 6    | 3000 - 5999     | 炉火纯青
-- 7    | 6000 - 9999     | 游刃有余
-- 8    | 10000 - 19999   | 登峰造极
-- 9    | 20000+          | 一代宗师
