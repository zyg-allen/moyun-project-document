-- =====================================================
-- 打赏功能 DDL 脚本（任务 2.2）
-- 支持对文章/专栏打赏，复用为付费阅读购买记录（target_type='article_paid'）
-- 幂等设计：CREATE TABLE IF NOT EXISTS
-- 执行顺序：本脚本在 57_column_init.sql 之后执行
-- =====================================================

-- 打赏订单表（同时复用为付费阅读购买记录）
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打赏订单（复用为付费阅读购买记录，target_type=article_paid）';
