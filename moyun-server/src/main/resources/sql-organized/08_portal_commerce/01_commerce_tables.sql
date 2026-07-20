-- ============================================
-- 墨韵智库 - 商业化模块建表 DDL（最终合并版）
-- ============================================
-- 合并来源脚本：
--   58_tip_init.sql                 （打赏订单建表：portal_tip_order）
--   60_contest_init.sql             （创作挑战/征文建表：portal_writing_contest/portal_contest_submission/portal_contest_vote）
--   61_writing_prompt_init.sql      （每日写作 prompt 建表：portal_writing_prompt）
--   63_creator_certification_init.sql （创作者认证建表：portal_creator_certification）
--   76_creator_settlement_init.sql  （创作者分成结算建表：portal_creator_settlement）
--   90_ad_slot_init.sql             （自研广告位建表：portal_ad_slot）
-- 涉及表：
--   portal_tip_order（58 建表）
--   portal_writing_contest（60 建表）
--   portal_contest_submission（60 建表）
--   portal_contest_vote（60 建表）
--   portal_writing_prompt（61 建表）
--   portal_creator_certification（63 建表）
--   portal_creator_settlement（76 建表）
--   portal_ad_slot（90 建表）
-- 说明：
--   - 所有表均无后续 ALTER 扩展，直接采用原始 CREATE TABLE IF NOT EXISTS
--   - 不包含 INSERT 数据语句（写作 prompt 种子数据见 61 数据脚本）
--   - 不包含后台菜单注入语句
-- @author moyun
-- ============================================

-- ----------------------------
-- 1. 打赏订单表（同时复用为付费阅读购买记录）
--    合并：58 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_tip_order` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`      BIGINT        NOT NULL                COMMENT '打赏者用户ID',
    `author_id`    BIGINT        NOT NULL                COMMENT '被打赏者用户ID',
    `target_type`  VARCHAR(32)   NOT NULL                COMMENT '打赏对象类型 article/column/article_paid',
    `target_id`    BIGINT        NOT NULL                COMMENT '打赏对象ID',
    `amount`       DECIMAL(10,2) NOT NULL                COMMENT '打赏金额',
    `message`      VARCHAR(200)  DEFAULT NULL            COMMENT '打赏留言',
    `status`       VARCHAR(16)   NOT NULL DEFAULT 'pending' COMMENT '状态 pending/paid/refunded',
    `pay_method`   VARCHAR(32)   DEFAULT NULL            COMMENT '支付方式',
    `paid_time`    DATETIME      DEFAULT NULL            COMMENT '支付时间',
    `created_time` DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_author` (`author_id`),
    KEY `idx_target` (`target_type`, `target_id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='打赏订单（复用为付费阅读购买记录，target_type=article_paid）';

-- ----------------------------
-- 2. 创作挑战/征文活动
--    合并：60 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_writing_contest` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title`          VARCHAR(128)  NOT NULL                COMMENT '活动标题',
    `description`    TEXT                                  COMMENT '活动描述',
    `theme`          VARCHAR(128)  DEFAULT NULL            COMMENT '征文主题',
    `cover`          VARCHAR(500)  DEFAULT NULL            COMMENT '封面',
    `start_time`     DATETIME      DEFAULT NULL            COMMENT '活动开始时间',
    `end_time`       DATETIME      DEFAULT NULL            COMMENT '投稿截止时间',
    `vote_end_time`  DATETIME      DEFAULT NULL            COMMENT '投票截止时间',
    `prize`          VARCHAR(500)  DEFAULT NULL            COMMENT '奖品说明',
    `status`         VARCHAR(16)   NOT NULL DEFAULT 'draft' COMMENT 'draft/collecting/voting/ended',
    `created_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_start_time` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='创作挑战/征文活动';

-- ----------------------------
-- 3. 活动投稿
--    合并：60 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_contest_submission` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `contest_id`   BIGINT        NOT NULL                COMMENT '活动ID',
    `user_id`      BIGINT        NOT NULL                COMMENT '投稿用户ID',
    `article_id`   BIGINT        NOT NULL                COMMENT '投稿文章ID',
    `status`       VARCHAR(16)   NOT NULL DEFAULT 'pending' COMMENT 'pending/shortlisted/eliminated/winner',
    `vote_count`   INT           NOT NULL DEFAULT 0     COMMENT '投票数',
    `rank`         INT           DEFAULT NULL            COMMENT '排名',
    `remark`       VARCHAR(500)  DEFAULT NULL            COMMENT '备注（评审意见等）',
    `created_time` DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_contest_user` (`contest_id`, `user_id`),
    UNIQUE KEY `uk_contest_article` (`contest_id`, `article_id`),
    KEY `idx_contest` (`contest_id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动投稿';

-- ----------------------------
-- 4. 活动投稿投票记录（用于 toggle 去重：每用户对每条投稿仅能投一票）
--    合并：60 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_contest_vote` (
    `id`             BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `submission_id`   BIGINT   NOT NULL                COMMENT '投稿ID',
    `user_id`        BIGINT   NOT NULL                COMMENT '投票用户ID',
    `contest_id`     BIGINT   NOT NULL                COMMENT '活动ID（冗余便于按活动统计）',
    `created_time`   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '投票时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_submission_user` (`submission_id`, `user_id`),
    KEY `idx_submission` (`submission_id`),
    KEY `idx_contest` (`contest_id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动投稿投票记录';

-- ----------------------------
-- 5. 每日写作 prompt
--    合并：61 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_writing_prompt` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `prompt_date`  DATE          NOT NULL                COMMENT 'prompt 日期（唯一）',
    `title`        VARCHAR(128)  NOT NULL                COMMENT 'prompt 标题',
    `description`  TEXT                                  COMMENT 'prompt 描述',
    `category`     VARCHAR(32)   DEFAULT NULL            COMMENT '分类（如：生活/职场/情感/虚构/哲思）',
    `created_time` DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_prompt_date` (`prompt_date`),
    KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日写作 prompt';

-- ----------------------------
-- 6. 创作者认证申请表
--    合并：63 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_creator_certification` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`        BIGINT        NOT NULL                COMMENT '申请用户ID',
    `real_name`      VARCHAR(64)   NOT NULL                COMMENT '真实姓名',
    `cert_type`      VARCHAR(32)   NOT NULL                COMMENT '认证类型 identity/creator/expert',
    `cert_no`        VARCHAR(64)   DEFAULT NULL            COMMENT '证件号',
    `cert_image`     VARCHAR(500)  DEFAULT NULL            COMMENT '证件照URL',
    `intro`          TEXT                                  COMMENT '自我介绍',
    `works`          VARCHAR(500)  DEFAULT NULL            COMMENT '代表作链接',
    `status`         VARCHAR(16)   NOT NULL DEFAULT 'pending' COMMENT '审核状态 pending/approved/rejected',
    `auditor_id`     BIGINT        DEFAULT NULL            COMMENT '审核人ID',
    `audit_remark`   VARCHAR(500)  DEFAULT NULL            COMMENT '审核备注',
    `created_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `audited_time`   DATETIME      DEFAULT NULL            COMMENT '审核时间',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='创作者认证';

-- ----------------------------
-- 7. 创作者结算单表
--    合并：76 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_creator_settlement` (
    `id`                 BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `creator_id`         BIGINT        NOT NULL                COMMENT '创作者用户ID',
    `period`             VARCHAR(16)   NOT NULL                COMMENT '结算周期，格式 yyyy-MM，如 2026-07',
    `tip_income`         DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '打赏收入（当月已支付打赏总额）',
    `paid_read_income`   DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '付费阅读收入（当月已支付购买总额）',
    `column_income`      DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '专栏订阅收入（当月已支付订阅总额）',
    `total_income`       DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '总收入（三项之和）',
    `platform_fee`       DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '平台抽成（total_income * platform_fee_rate）',
    `creator_income`     DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '创作者实得（total_income - platform_fee）',
    `status`             VARCHAR(16)   NOT NULL DEFAULT 'pending' COMMENT '状态 pending/confirmed/paid',
    `paid_time`          DATETIME      DEFAULT NULL            COMMENT '打款时间',
    `create_time`        DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`        DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_creator_period` (`creator_id`, `period`),
    KEY `idx_creator` (`creator_id`),
    KEY `idx_period` (`period`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='创作者分成结算';

-- ----------------------------
-- 8. 门户自研广告位表
--    合并：90 建表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_ad_slot` (
    `id`          bigint        NOT NULL AUTO_INCREMENT COMMENT '广告位ID',
    `slot_key`    varchar(64)   NOT NULL                COMMENT '广告位标识，如 article_detail_bottom',
    `title`       varchar(100)  NOT NULL                COMMENT '广告标题',
    `image`       varchar(500)  DEFAULT NULL            COMMENT '广告图片URL',
    `link`        varchar(500)  DEFAULT NULL            COMMENT '点击跳转链接',
    `content`     varchar(500)  DEFAULT NULL            COMMENT '广告文案',
    `sort`        int           DEFAULT '0'             COMMENT '排序',
    `status`      varchar(1)    DEFAULT '0'             COMMENT '状态：0=启用 1=停用',
    `create_by`   varchar(64)   DEFAULT ''              COMMENT '创建者',
    `create_time` datetime      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   varchar(64)   DEFAULT ''              COMMENT '更新者',
    `update_time` datetime      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`      varchar(500)  DEFAULT NULL            COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_slot_key` (`slot_key`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户自研广告位表';

-- ============================================
-- 商业化基础表补齐（VIP / 订单 / 钱包 / 钱包流水）
--    合并来源：
--      03_portal_init.sql                  （portal_vip_package / portal_order / portal_wallet / portal_wallet_transaction 原始建表）
--      29_alter_all_tables_base_fields.sql（通过存储过程 AddColumnIfNotExists 幂等补审计字段：
--        - portal_wallet_transaction 补 create_by/update_by/update_time/remark
--        - portal_wallet 补 create_by/update_by/remark
--        - portal_vip_package、portal_order 已含全部审计字段，29 为幂等兜底）
--    说明：这 4 张表为商业化基础表（VIP/订单/钱包），之前整理时遗漏在 03_portal_base 之外，现归入 commerce 目录
-- ============================================

-- ----------------------------
-- 9. 门户VIP套餐表
--    合并：03 建表（已含全部审计字段，29 幂等兜底无新增）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_vip_package` (
  `id`             bigint       NOT NULL AUTO_INCREMENT  COMMENT '套餐ID',
  `name`           varchar(100) NOT NULL                 COMMENT '套餐名称',
  `price`          decimal(10,2) NOT NULL                COMMENT '价格',
  `original_price` decimal(10,2) DEFAULT NULL            COMMENT '原价',
  `duration`       int          NOT NULL                 COMMENT '有效期（天）',
  `description`    varchar(500) DEFAULT NULL             COMMENT '套餐描述',
  `features`       json         DEFAULT NULL             COMMENT '功能列表（JSON数组）',
  `popular`        tinyint(1)   DEFAULT '0'              COMMENT '是否热门',
  `sort`           int          DEFAULT '0'              COMMENT '排序',
  `status`         varchar(20)  DEFAULT 'active'         COMMENT '状态：active/inactive',
  `create_by`      varchar(64)  DEFAULT ''               COMMENT '创建者',
  `create_time`    datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`      varchar(64)  DEFAULT ''               COMMENT '更新者',
  `update_time`    datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark`         varchar(500) DEFAULT NULL             COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户VIP套餐表';

-- ----------------------------
-- 10. 门户订单表
--     合并：03 建表（已含全部审计字段，29 幂等兜底无新增）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_order` (
  `id`          bigint        NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no`    varchar(64)   NOT NULL                COMMENT '订单号',
  `user_id`     bigint        NOT NULL                COMMENT '用户ID（门户用户ID）',
  `type`        varchar(50)   NOT NULL                COMMENT '类型：vip/recharge/product',
  `product_id`  bigint        DEFAULT NULL            COMMENT '商品ID',
  `amount`      decimal(10,2) NOT NULL                COMMENT '金额',
  `status`      varchar(20)   DEFAULT 'pending'       COMMENT '状态：pending/paid/cancelled/refunded',
  `pay_method`  varchar(50)   DEFAULT NULL            COMMENT '支付方式：wechat/alipay',
  `paid_at`     datetime      DEFAULT NULL            COMMENT '支付时间',
  `create_by`   varchar(64)   DEFAULT ''              COMMENT '创建者',
  `create_time` datetime      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   varchar(64)   DEFAULT ''              COMMENT '更新者',
  `update_time` datetime      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark`      varchar(500)  DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_type` (`type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户订单表';

-- ----------------------------
-- 11. 门户钱包表
--     合并：03 建表 + 29 补 create_by/update_by/remark（03 建表已含这些字段，29 为幂等兜底，本 CREATE 无新增）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_wallet` (
  `id`              bigint        NOT NULL AUTO_INCREMENT COMMENT '钱包ID',
  `user_id`         bigint        NOT NULL                COMMENT '用户ID（门户用户ID）',
  `balance`         decimal(10,2) DEFAULT '0.00'         COMMENT '余额',
  `frozen_balance`  decimal(10,2) DEFAULT '0.00'         COMMENT '冻结余额',
  `total_recharge`  decimal(10,2) DEFAULT '0.00'         COMMENT '累计充值',
  `total_withdraw`  decimal(10,2) DEFAULT '0.00'         COMMENT '累计提现',
  `create_by`       varchar(64)   DEFAULT ''             COMMENT '创建者',
  `create_time`     datetime      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       varchar(64)   DEFAULT ''             COMMENT '更新者',
  `update_time`     datetime      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark`          varchar(500)  DEFAULT NULL           COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户钱包表';

-- ----------------------------
-- 12. 门户钱包交易记录表
--     合并：03 建表 + 29 补 create_by/update_by/update_time/remark（03 建表已含这些字段，29 为幂等兜底，本 CREATE 无新增）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `portal_wallet_transaction` (
  `id`             bigint        NOT NULL AUTO_INCREMENT COMMENT '交易ID',
  `user_id`        bigint        NOT NULL                COMMENT '用户ID（门户用户ID）',
  `type`           varchar(50)   NOT NULL                COMMENT '类型：recharge/consume/refund/withdraw',
  `amount`         decimal(10,2) NOT NULL                COMMENT '金额',
  `balance_before` decimal(10,2) NOT NULL                COMMENT '交易前余额',
  `balance_after`  decimal(10,2) NOT NULL                COMMENT '交易后余额',
  `description`    varchar(500)  DEFAULT NULL            COMMENT '描述',
  `order_id`       bigint        DEFAULT NULL            COMMENT '关联订单ID',
  `create_by`      varchar(64)   DEFAULT ''              COMMENT '创建者',
  `create_time`    datetime      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`      varchar(64)   DEFAULT ''              COMMENT '更新者',
  `update_time`    datetime      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark`         varchar(500)  DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门户钱包交易记录表';
