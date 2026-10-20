-- ============================================================
-- 进货渠道（采购来源渠道：拼多多 / 闲鱼 / 批发市场 …）
--
-- 与「售卖渠道」对称：系统预设 (user_id=0) + 用户自建，可增删。
-- 用于追溯「这批货是从哪个平台买的」，与 source_type（采购/自产/…）互补：
--   source_type     来源类型（固定枚举，决定成本怎么算）
--   source_channel  进货渠道（自由维护，记录从哪来）
-- ============================================================
CREATE TABLE IF NOT EXISTS `ledger_source_channel` (
    `id`          BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    `user_id`     BIGINT       NOT NULL                COMMENT '归属用户（0=系统预设）',
    `name`        VARCHAR(50)  NOT NULL                COMMENT '渠道名称',
    `icon`        VARCHAR(255) NULL                    COMMENT '图标（emoji 或图片地址）',
    `sort_order`  INT          NOT NULL DEFAULT 0      COMMENT '排序',
    `is_system`   TINYINT      NOT NULL DEFAULT 0      COMMENT '是否系统预设：1=是 0=用户自建',
    `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1=启用 0=停用',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_src_channel_user` (`user_id`),
    KEY `idx_src_channel_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '进货渠道';

-- 系统预设（user_id=0，只读）
INSERT INTO `ledger_source_channel` (`id`, `user_id`, `name`, `sort_order`, `is_system`, `status`)
SELECT 9001, 0, '拼多多', 1, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ledger_source_channel` WHERE `user_id` = 0 AND `name` = '拼多多');

INSERT INTO `ledger_source_channel` (`id`, `user_id`, `name`, `sort_order`, `is_system`, `status`)
SELECT 9002, 0, '闲鱼', 2, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ledger_source_channel` WHERE `user_id` = 0 AND `name` = '闲鱼');

INSERT INTO `ledger_source_channel` (`id`, `user_id`, `name`, `sort_order`, `is_system`, `status`)
SELECT 9003, 0, '淘宝', 3, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ledger_source_channel` WHERE `user_id` = 0 AND `name` = '淘宝');

INSERT INTO `ledger_source_channel` (`id`, `user_id`, `name`, `sort_order`, `is_system`, `status`)
SELECT 9004, 0, '批发市场', 4, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ledger_source_channel` WHERE `user_id` = 0 AND `name` = '批发市场');

INSERT INTO `ledger_source_channel` (`id`, `user_id`, `name`, `sort_order`, `is_system`, `status`)
SELECT 9005, 0, '朋友赠', 5, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `ledger_source_channel` WHERE `user_id` = 0 AND `name` = '朋友赠');
