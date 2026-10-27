-- 存钱流水业务标记（可重复执行检查）
ALTER TABLE ledger_transaction ADD COLUMN biz_type varchar(32) DEFAULT NULL COMMENT '业务来源：saving=存钱转入' AFTER client_uuid;
