-- =====================================================================
-- 记账模块增量脚本：流水凭证截图
-- 文件：20260903-02-moyun-ledger-voucher.sql
-- 日期：2026-09-03
-- 说明：
--   1. ledger_transaction 新增 voucher_url 字段，保存记账时上传的凭证截图
--      （复用门户文件上传 /portal/file/upload，返回 SysFile.fileUrl）；
--   2. 按项目规范：不改动原 CREATE TABLE，以增量 ALTER 追加。
-- 关联：
--   - 后端：LedgerTransaction 实体 / TransactionCreateDTO / applyDto
--   - 前端：moyun-ledger-app 记一笔页（pages/record/index.vue）
-- =====================================================================

-- 1. 流水表：凭证截图URL（复用门户文件服务，仅存 URL 不落本模块表）
ALTER TABLE `ledger_transaction`
    ADD COLUMN `voucher_url` VARCHAR(500) NULL COMMENT '凭证截图URL（门户文件服务地址）' AFTER `merchant`;
