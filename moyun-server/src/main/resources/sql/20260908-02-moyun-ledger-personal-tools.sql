-- =====================================================================
-- 记账模块（个人资产管理）个人工具集扩展：我的页面四大新功能
-- 墨韵智库 · moyun
-- 执行库：moyun-db（与既有业务同库）
-- 幂等：建表 IF NOT EXISTS，可重复执行
-- 设计文档：docs/11-记账模块需求分析/记账模块设计方案V1.3.md
-- 说明：
--   1) 金额一律 DECIMAL(18,2) "元"存储（与 20260904-03 金额单位统一方案一致）
--   2) 实体类不继承 BaseEntity（表无 create_by/update_by/del_flag）
--   3) user_id 口径 = portal_user.id（门户用户主键）
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. 存钱计划表
--    method: 52week=52周存钱法(第n周存10n元,累计13780) / fixed=固定周期存钱
--            monthly=每月固定存 / custom=自定义递增规则
--    status: 1=进行中 2=成功 3=失败 0=已删除
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ledger_saving_plan` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '计划ID',
  `user_id`        BIGINT        NOT NULL COMMENT '门户用户ID（portal_user.id）',
  `name`           VARCHAR(100)  NOT NULL COMMENT '计划名称，如"买房基金"',
  `method`         VARCHAR(20)   NOT NULL COMMENT '存钱方式：52week/fixed/monthly/custom',
  `target_amount`  DECIMAL(18,2) NOT NULL COMMENT '目标金额（元）',
  `current_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '当前已存金额（元）',
  `period_amount`  DECIMAL(18,2) NULL COMMENT '每期金额（元；fixed/monthly 用）',
  `period_count`   INT           NULL COMMENT '总期数（custom 自定义规则期数）',
  `increase_step`  DECIMAL(18,2) NULL COMMENT '每期递增金额（元；custom 自定义递增规则用）',
  `start_date`     DATE          NOT NULL COMMENT '开始日期',
  `end_date`       DATE          NULL COMMENT '结束日期（可选）',
  `remark`         VARCHAR(200)  NULL COMMENT '备注',
  `status`         TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：1=进行中 2=成功 3=失败 0=已删除',
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='记账-存钱计划';

-- ---------------------------------------------------------------------
-- 2. 存钱流水表（每个计划的期次存入记录）
--    status: 0=待存 1=成功 2=失败（如余额不足）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ledger_saving_record` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '流水ID',
  `plan_id`       BIGINT        NOT NULL COMMENT '计划ID（ledger_saving_plan.id）',
  `user_id`       BIGINT        NOT NULL COMMENT '门户用户ID（冗余，数据隔离校验用）',
  `period_index`  INT           NOT NULL COMMENT '期次序号（第几期/第几周）',
  `target_amount` DECIMAL(18,2) NOT NULL COMMENT '本期应存金额（元）',
  `amount`        DECIMAL(18,2) NULL COMMENT '实际存入金额（元；成功时记录）',
  `status`        TINYINT       NOT NULL DEFAULT 0 COMMENT '状态：0=待存 1=成功 2=失败',
  `fail_reason`   VARCHAR(200)  NULL COMMENT '失败原因（如余额不足）',
  `remark`        VARCHAR(200)  NULL COMMENT '备注',
  `record_date`   DATE          NULL COMMENT '存入/失败日期',
  `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_plan` (`plan_id`, `period_index`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='记账-存钱流水';

-- ---------------------------------------------------------------------
-- 3. 备忘录表（首页待办事项与备忘录模块共用）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ledger_memo` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '备忘录ID',
  `user_id`     BIGINT       NOT NULL COMMENT '门户用户ID（portal_user.id）',
  `content`     VARCHAR(500) NOT NULL COMMENT '待办内容',
  `done`        TINYINT      NOT NULL DEFAULT 0 COMMENT '完成状态：1=已完成 0=未完成',
  `todo_date`   DATE         NULL COMMENT '创建日期',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_done` (`user_id`, `done`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='记账-备忘录（待办事项）';

-- ---------------------------------------------------------------------
-- 4. 打赏记录表（演示性质，模拟支付成功即落库）
--    target: developer=开发者 platform=平台
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ledger_tip_order` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '打赏单ID',
  `user_id`     BIGINT        NOT NULL COMMENT '门户用户ID（portal_user.id）',
  `amount`      DECIMAL(18,2) NOT NULL COMMENT '打赏金额（元）',
  `target`      VARCHAR(20)   NOT NULL DEFAULT 'developer' COMMENT '打赏对象：developer=开发者 platform=平台',
  `reason`      VARCHAR(200)  NULL COMMENT '打赏理由（可选）',
  `pay_way`     VARCHAR(20)   NOT NULL DEFAULT 'wechat' COMMENT '支付方式：wechat/alipay',
  `status`      TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：1=成功 0=已撤销',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '打赏时间',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='记账-打赏记录';

-- ---------------------------------------------------------------------
-- 5. 定时记账任务表
--    cycle: daily=每天 / weekly=每周几 / monthly=每月几号 / interval=每N天一次
--    next_exec_date 下次执行日期由定时任务推进；end_date 到期后自动停用
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ledger_schedule_task` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `user_id`       BIGINT        NOT NULL COMMENT '门户用户ID（portal_user.id）',
  `name`          VARCHAR(100)  NOT NULL COMMENT '任务名称',
  `type`          VARCHAR(20)   NOT NULL COMMENT '记账类型：income/expense',
  `amount`        DECIMAL(18,2) NOT NULL COMMENT '金额（元）',
  `category_id`   BIGINT        NULL COMMENT '分类ID（ledger_category）',
  `account_id`    BIGINT        NULL COMMENT '关联资产账户ID（支出扣款/收入入账）',
  `description`   VARCHAR(200)  NULL COMMENT '备注',
  `cycle`         VARCHAR(20)   NOT NULL COMMENT '执行周期：daily/weekly/monthly/interval',
  `day_of_week`   TINYINT       NULL COMMENT '每周几（1-7，weekly 用）',
  `day_of_month`  TINYINT       NULL COMMENT '每月几号（1-28，monthly 用）',
  `interval_days` INT           NULL COMMENT '间隔天数（interval 用）',
  `exec_time`     VARCHAR(5)    NOT NULL DEFAULT '08:00' COMMENT '执行时间 HH:mm',
  `start_date`    DATE          NOT NULL COMMENT '开始日期',
  `end_date`      DATE          NULL COMMENT '结束日期（可选，到期自动停用）',
  `next_exec_date` DATE         NULL COMMENT '下次执行日期',
  `enabled`       TINYINT       NOT NULL DEFAULT 1 COMMENT '启用状态：1=启用 0=停用',
  `status`        TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：1=正常 0=已删除',
  `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_status` (`user_id`, `status`),
  KEY `idx_next_exec` (`enabled`, `status`, `next_exec_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='记账-定时记账任务';

-- ---------------------------------------------------------------------
-- 6. 定时记账执行日志表（成功生成流水 / 失败留痕，支持手动重试）
--    status: 1=成功 2=失败
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ledger_schedule_log` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `task_id`        BIGINT        NOT NULL COMMENT '任务ID（ledger_schedule_task.id）',
  `user_id`        BIGINT        NOT NULL COMMENT '门户用户ID（冗余）',
  `exec_date`      DATE          NOT NULL COMMENT '执行日期',
  `amount`         DECIMAL(18,2) NOT NULL COMMENT '金额（元）',
  `status`         TINYINT       NOT NULL COMMENT '状态：1=成功 2=失败',
  `fail_reason`    VARCHAR(200)  NULL COMMENT '失败原因（如余额不足）',
  `transaction_id` BIGINT        NULL COMMENT '生成的流水ID（ledger_transaction.id）',
  `retry_count`    INT           NOT NULL DEFAULT 0 COMMENT '重试次数',
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_task` (`task_id`, `exec_date`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='记账-定时记账执行日志';

-- ---------------------------------------------------------------------
-- v11.34 备忘录增强：事项/内容/时间/是否提醒/提醒方式/重要程度（增量 ALTER，不改动上方 CREATE）
-- 执行前请先执行上方 CREATE TABLE（已执行过则跳过，ALTER 部分单独执行）
-- ---------------------------------------------------------------------
ALTER TABLE `ledger_memo`
  ADD COLUMN `title`         VARCHAR(100) NULL COMMENT '事项标题' AFTER `user_id`,
  ADD COLUMN `event_time`    DATETIME     NULL COMMENT '事项时间（提醒基准时间）' AFTER `done`,
  ADD COLUMN `remind_enabled` TINYINT     NOT NULL DEFAULT 0 COMMENT '是否提醒：1=是 0=否' AFTER `event_time`,
  ADD COLUMN `remind_rule`   VARCHAR(20)  NULL COMMENT '提醒方式：on_time=准时 advance_30m=提前30分钟 advance_1h=提前1小时 advance_2h=提前2小时 advance_1d=提前1天 advance_1d_9am=提前一天上午9点' AFTER `remind_enabled`,
  ADD COLUMN `importance`    VARCHAR(10)  NOT NULL DEFAULT 'normal' COMMENT '重要程度：low=不重要 normal=一般 high=重要 urgent=紧急' AFTER `remind_rule`,
  ADD COLUMN `reminded`      TINYINT      NOT NULL DEFAULT 0 COMMENT '提醒是否已发送：1=已发 0=未发（防重复）' AFTER `importance`;

-- title 允许为空时列表回退显示 content；给存量数据补默认标题
UPDATE `ledger_memo` SET `title` = LEFT(`content`, 50) WHERE `title` IS NULL;
ALTER TABLE `ledger_memo` MODIFY COLUMN `title` VARCHAR(100) NOT NULL COMMENT '事项标题' AFTER `user_id`;

-- 提醒扫描索引（未完成+未提醒）
ALTER TABLE `ledger_memo` ADD INDEX `idx_user_remind` (`remind_enabled`, `done`, `reminded`);