-- ============================================================
-- 邀请码：把仓库分享给别人，对方未注册时走注册后自动加入
--
-- 一个码可邀请多人（合伙场景），有有效期与人数上限，可撤销。
-- 关联 ledger_warehouse_member：接受邀请即插入一行成员记录。
-- ============================================================

CREATE TABLE IF NOT EXISTS `ledger_invite` (
  `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code`         varchar(16)  NOT NULL COMMENT '邀请码（6 位，去易混字符）',
  `type`         varchar(20)  NOT NULL DEFAULT 'warehouse' COMMENT 'warehouse=加入仓库',
  `warehouse_id` bigint       NOT NULL DEFAULT 0 COMMENT '目标仓库',
  `role`         varchar(20)  NOT NULL DEFAULT 'member' COMMENT '加入后角色',
  `created_by`   bigint       NOT NULL COMMENT '邀请人',
  `expire_at`    datetime     DEFAULT NULL COMMENT '过期时间',
  `max_use`      int          NOT NULL DEFAULT 10 COMMENT '可用次数上限',
  `used_count`   int          NOT NULL DEFAULT 0 COMMENT '已使用次数',
  `status`       tinyint      NOT NULL DEFAULT 1 COMMENT '1 有效 0 已撤销',
  `create_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_warehouse` (`warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='邀请码';
