-- =============================================
-- 墨韵智库 - 通知表合并：sys_notification + sys_notification_read
-- 执行顺序: 34 (在 33_alter_article_status_enum.sql 之后执行)
-- 背景:
--   原有两套通知表：
--   1. portal_notification（per-user 模型，门户用户通知）
--   2. sys_notice（全局广播模型，RuoYi 内置公告）
--   两者模型冲突：portal_notification 的 is_read 无法表达广播通知的 per-user 已读状态。
--
-- 合并方案：采用主流平台（掘金/知乎/GitHub）的两表结构
--   1. sys_notification（通知主体表）：存内容，scope=user 个人 / scope=all 广播
--   2. sys_notification_read（用户已读关系表）：存已读记录，NOT EXISTS 计算未读
--
-- 废弃旧表：
--   - portal_notification：数据迁移后 DROP
--   - sys_notice：数据迁移后 DROP（保留 RuoYi 菜单/权限/字典，Controller 内部改用新表）
--
-- 兼容: MySQL 8+（可重复执行）
-- =============================================

-- =============================================
-- 1. 创建通知主体表 sys_notification
-- =============================================
CREATE TABLE IF NOT EXISTS `sys_notification` (
  `id`            bigint        NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `type`          varchar(50)   NOT NULL                COMMENT '类型：system/comment/like/follow/order/notice/announcement',
  `title`         varchar(200)  DEFAULT NULL            COMMENT '通知标题',
  `content`       text                                  COMMENT '通知内容',
  `data`          json          DEFAULT NULL            COMMENT '通知数据（JSON格式）',
  `scope`         varchar(20)   NOT NULL DEFAULT 'user' COMMENT '范围：user=个人通知 / all=全局广播',
  `user_id`       bigint        DEFAULT NULL            COMMENT '接收用户ID（scope=user 时必填，scope=all 时为 NULL）',
  `notice_type`   char(1)       DEFAULT NULL            COMMENT '通知/公告分类：1=通知 / 2=公告（兼容 sys_notice 字典 sys_notice_type）',
  `status`        char(1)       DEFAULT '0'              COMMENT '状态：0=正常 / 1=关闭（兼容 sys_notice 字典 sys_notice_status）',
  `create_by`     varchar(64)   DEFAULT ''              COMMENT '创建者',
  `create_time`   datetime      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     varchar(64)   DEFAULT ''              COMMENT '更新者',
  `update_time`   datetime      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark`        varchar(500)  DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_type` (`type`),
  KEY `idx_scope` (`scope`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统通知主体表（合并 portal_notification + sys_notice）';

-- =============================================
-- 2. 创建用户已读关系表 sys_notification_read
--    用于记录用户已读通知（广播通知的 per-user 已读状态）
-- =============================================
CREATE TABLE IF NOT EXISTS `sys_notification_read` (
  `id`              bigint   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `notification_id` bigint   NOT NULL                COMMENT '通知ID（关联 sys_notification.id）',
  `user_id`         bigint   NOT NULL                COMMENT '用户ID',
  `read_time`       datetime DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
  `create_time`     datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notif_user` (`notification_id`, `user_id`) COMMENT '唯一索引：防止同一用户对同一通知重复标记已读',
  KEY `idx_user_id` (`user_id`),
  KEY `idx_notification_id` (`notification_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统通知用户已读关系表';

-- =============================================
-- 3. 迁移 portal_notification 历史数据到 sys_notification
--    原 per-user 通知 → scope=user，保留 user_id
--    原 is_read=true 的记录同步写入 sys_notification_read
-- =============================================
INSERT INTO `sys_notification` (`id`, `type`, `title`, `content`, `data`, `scope`, `user_id`, `notice_type`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT
    `id`,
    `type`,
    `title`,
    `content`,
    `data`,
    'user' AS scope,
    `user_id`,
    NULL AS notice_type,
    '0' AS status,
    `create_by`,
    `create_time`,
    `update_by`,
    `update_time`,
    `remark`
FROM `portal_notification` a
ON DUPLICATE KEY UPDATE id = a.id;

-- 迁移已读记录：portal_notification.is_read=1 的记录写入 sys_notification_read
INSERT IGNORE INTO `sys_notification_read` (`notification_id`, `user_id`, `read_time`, `create_time`)
SELECT
    n.`id`,
    n.`user_id`,
    n.`update_time` AS read_time,
    n.`update_time` AS create_time
FROM `portal_notification` n
WHERE n.`is_read` = 1
ON DUPLICATE KEY UPDATE notification_id = notification_id;

-- =============================================
-- 4. 迁移 sys_notice 历史数据到 sys_notification
--    原全局公告 → scope=all，user_id=NULL
--    notice_type 保留（1=通知 / 2=公告）
--    status 保留（0=正常 / 1=关闭）
-- =============================================
INSERT INTO `sys_notification` (`type`, `title`, `content`, `scope`, `user_id`, `notice_type`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT
    CASE WHEN n.`notice_type` = '1' THEN 'notice' ELSE 'announcement' END AS type,
    n.`notice_title` AS title,
    CAST(n.`notice_content` AS CHAR) AS content,
    'all' AS scope,
    NULL AS user_id,
    n.`notice_type`,
    n.`status`,
    n.`create_by`,
    n.`create_time`,
    n.`update_by`,
    n.`update_time`,
    n.`remark`
FROM `sys_notice` n
ON DUPLICATE KEY UPDATE id = id;

-- =============================================
-- 5. 废弃旧表（保留表结构，仅重命名备份，便于回滚）
--    注意：不直接 DROP，避免误操作导致数据丢失
--    确认新表运行正常后，可手动执行 DROP TABLE portal_notification_bak / sys_notice_bak
-- =============================================
-- 备份并移除原表（重命名为 _bak 后缀）
RENAME TABLE `portal_notification` TO `portal_notification_bak`;
RENAME TABLE `sys_notice` TO `sys_notice_bak`;

-- =============================================
-- 6. 校验结果
-- =============================================
SELECT '=== sys_notification 迁移结果 ===' AS message;
SELECT
    scope,
    COUNT(*) AS count
FROM `sys_notification`
GROUP BY scope;

SELECT '=== sys_notification_read 迁移结果 ===' AS message;
SELECT COUNT(*) AS total_read_records FROM `sys_notification_read`;

SELECT '=== 旧表已备份 ===' AS message;
SELECT TABLE_NAME FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('portal_notification_bak', 'sys_notice_bak');

SELECT '通知表合并完成！新表：sys_notification + sys_notification_read' AS message;
