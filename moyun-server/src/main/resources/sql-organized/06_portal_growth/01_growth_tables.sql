-- ============================================
-- 墨韵智库 - 成长体系模块建表 DDL（最终合并版）
-- ============================================
-- 合并来源脚本：
--   19_growth_system_init.sql  （成长体系基础建表：portal_user_growth/portal_growth_log/portal_growth_rule/portal_achievement/portal_user_badge/portal_user_stats）
--   73_checkin_calendar_init.sql （扩展 portal_user_growth：新增 supplement_card_count、last_card_grant_month 字段）
--   74_task_shop_init.sql      （扩展 portal_user_growth：新增 points 字段；新建 portal_task/portal_user_task/portal_shop_item/portal_shop_exchange）
-- 涉及表：
--   portal_user_growth（19 建表 + 73/74 扩展字段）
--   portal_growth_log（19 建表）
--   portal_growth_rule（19 建表）
--   portal_achievement（19 建表）
--   portal_user_badge（19 建表）
--   portal_user_stats（19 建表）
--   portal_task（74 建表）
--   portal_user_task（74 建表）
--   portal_shop_item（74 建表）
--   portal_shop_exchange（74 建表）
-- 说明：
--   - 所有 ALTER TABLE 已合并进 CREATE TABLE IF NOT EXISTS
--   - 不包含 INSERT 数据语句（成长规则/成就定义/任务/商品等种子数据见各自的数据脚本）
--   - 不包含后台菜单注入语句
-- @author moyun
-- ============================================

-- ----------------------------
-- 1. 用户成长值总表
--    合并：19 建表 + 73（supplement_card_count、last_card_grant_month）+ 74（points）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_user_growth` (
    `id`                     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`                BIGINT UNSIGNED NOT NULL COMMENT '门户用户ID（portal_user.id）',
    `growth_value`           INT UNSIGNED    DEFAULT 0 COMMENT '成长值（累计，只增不减）',
    `level`                  INT             DEFAULT 1 COMMENT '当前等级',
    `title`                  VARCHAR(50)     DEFAULT '初出茅庐' COMMENT '当前头衔',
    `season_value`           INT UNSIGNED    DEFAULT 0 COMMENT '本季成长值（赛季排名用）',
    `points`                 BIGINT          NOT NULL DEFAULT 0 COMMENT '积分余额（可消耗，与成长值解耦）',
    `supplement_card_count`  INT             NOT NULL DEFAULT 0 COMMENT '补签卡数量（每月赠送1张，补签消耗）',
    `last_card_grant_month`  VARCHAR(7)      DEFAULT NULL COMMENT '最后赠送补签卡月份（YYYY-MM，幂等控制）',
    `create_by`              VARCHAR(64)     DEFAULT '' COMMENT '创建者',
    `create_time`            DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`              VARCHAR(64)     DEFAULT '' COMMENT '更新者',
    `update_time`            DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`                 VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user` (`user_id`),
    KEY `idx_season` (`season_value` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户成长值总表';

-- ----------------------------
-- 2. 成长事件流水表
--    合并：19 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_growth_log` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成长事件流水表';

-- ----------------------------
-- 3. 用户统计聚合表
--    合并：19 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_user_stats` (
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
    -- v5.9 阶段0：面试画像驱动抽题相关字段
    `mock_interview_count`  INT             DEFAULT 0 COMMENT '模拟面试次数',
    `avg_mock_score`        INT             DEFAULT 0 COMMENT '模拟面试平均分',
    `weak_tags`             TEXT            DEFAULT NULL COMMENT '薄弱知识点 JSON 数组（如 [{"tagId":1,"tagName":"Spring","failRate":0.6}]）',
    `weak_tags_updated_time` DATETIME       DEFAULT NULL COMMENT '薄弱点最后计算时间',
    `create_by`             VARCHAR(64)     DEFAULT '' COMMENT '创建者',
    `create_time`           DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`             VARCHAR(64)     DEFAULT '' COMMENT '更新者',
    `update_time`           DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`                VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='门户用户统计聚合表';

-- ----------------------------
-- 4. 成长规则配置表
--    合并：19 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_growth_rule` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成长规则配置表';

-- ----------------------------
-- 5. 成就定义表
--    合并：19 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_achievement` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成就定义表';

-- ----------------------------
-- 6. 用户徽章记录表
--    合并：19 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_user_badge` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户徽章记录表';

-- ----------------------------
-- 7. 任务定义表
--    合并：74 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_task` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code`           VARCHAR(64)   NOT NULL                COMMENT '任务编码（唯一，用于埋点触发，如 daily_checkin）',
    `name`           VARCHAR(128)  NOT NULL                COMMENT '任务名称',
    `description`    VARCHAR(500)  DEFAULT NULL            COMMENT '任务描述',
    `task_type`      VARCHAR(32)   NOT NULL DEFAULT 'daily' COMMENT '任务类型 daily/once/achievement',
    `reward_points`  INT           NOT NULL DEFAULT 0      COMMENT '完成奖励积分',
    `target_count`   INT           NOT NULL DEFAULT 1      COMMENT '目标完成次数',
    `icon`           VARCHAR(500)  DEFAULT NULL            COMMENT '任务图标URL',
    `status`         VARCHAR(16)   NOT NULL DEFAULT 'active' COMMENT '状态 active/inactive',
    `create_by`      VARCHAR(64)   DEFAULT ''              COMMENT '创建者',
    `create_time`    DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`      VARCHAR(64)   DEFAULT ''              COMMENT '更新者',
    `update_time`    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`         VARCHAR(500)  DEFAULT NULL            COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_type_status` (`task_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务定义表';

-- ----------------------------
-- 8. 用户任务进度表
--    合并：74 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_user_task` (
    `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`         BIGINT      NOT NULL                COMMENT '用户ID',
    `task_id`         BIGINT      NOT NULL                COMMENT '任务ID',
    `progress`        INT         NOT NULL DEFAULT 0      COMMENT '当前进度',
    `completed`       TINYINT     NOT NULL DEFAULT 0      COMMENT '是否已完成 0/1',
    `claimed`         TINYINT     NOT NULL DEFAULT 0      COMMENT '是否已领取奖励 0/1',
    `completed_time`  DATETIME    DEFAULT NULL            COMMENT '完成时间',
    `create_by`       VARCHAR(64) DEFAULT ''             COMMENT '创建者',
    `create_time`     DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`       VARCHAR(64) DEFAULT ''             COMMENT '更新者',
    `update_time`     DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`          VARCHAR(500) DEFAULT NULL           COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_task` (`user_id`, `task_id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户任务进度表';

-- ----------------------------
-- 9. 积分商城商品表
--    合并：74 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_shop_item` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`          VARCHAR(128)  NOT NULL                COMMENT '商品名称',
    `description`   VARCHAR(500)  DEFAULT NULL            COMMENT '商品描述',
    `cover`         VARCHAR(500)  DEFAULT NULL            COMMENT '商品封面URL',
    `type`          VARCHAR(32)   NOT NULL DEFAULT 'virtual' COMMENT '商品类型 virtual/physical',
    `points_cost`   INT           NOT NULL DEFAULT 0      COMMENT '兑换所需积分',
    `stock`         INT           NOT NULL DEFAULT 0      COMMENT '库存（-1表示不限）',
    `status`        VARCHAR(16)   NOT NULL DEFAULT 'active' COMMENT '状态 active/inactive',
    `create_by`     VARCHAR(64)   DEFAULT ''              COMMENT '创建者',
    `create_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`     VARCHAR(64)   DEFAULT ''              COMMENT '更新者',
    `update_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`        VARCHAR(500)  DEFAULT NULL            COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_type_status` (`type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分商城商品表';

-- ----------------------------
-- 10. 积分兑换记录表
--     合并：74 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_shop_exchange` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`       BIGINT        NOT NULL                COMMENT '兑换用户ID',
    `item_id`       BIGINT        NOT NULL                COMMENT '商品ID',
    `points_cost`   INT           NOT NULL                COMMENT '消耗积分（冗余，便于查询）',
    `status`        VARCHAR(16)   NOT NULL DEFAULT 'pending' COMMENT '状态 pending/fulfilled/failed',
    `address`       VARCHAR(500)  DEFAULT NULL            COMMENT '收货地址（实物商品）',
    `exchange_time` DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '兑换时间',
    `create_by`     VARCHAR(64)   DEFAULT ''              COMMENT '创建者',
    `create_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`     VARCHAR(64)   DEFAULT ''              COMMENT '更新者',
    `update_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`        VARCHAR(500)  DEFAULT NULL            COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_item` (`item_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分兑换记录表';
