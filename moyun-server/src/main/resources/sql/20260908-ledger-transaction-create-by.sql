-- 流水表补审计字段：创建人（后台管理/代客修正时数据隔离溯源用）
-- create_time/update_time 已存在（DEFAULT CURRENT_TIMESTAMP / ON UPDATE），无需变更
ALTER TABLE `ledger_transaction`
    ADD COLUMN `create_by` varchar(64) NULL DEFAULT NULL COMMENT '创建人（门户用户名/admin 代改标识）' AFTER `client_uuid`;
