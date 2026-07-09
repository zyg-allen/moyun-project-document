-- =====================================================================
-- sys_file 表增加存储降级标识字段
-- ---------------------------------------------------------------------
-- 背景：MinIO 出现问题时需自动/手动降级到本地存储。
-- 字段说明：
--   storage_type   存储引擎：minio / local（已有）
--   fallback       是否为降级存储：0=正常存储到配置的目标 1=因目标不可用降级到本地
--   local_path     本地备份路径（无论 minio/local 都记录本地绝对路径，
--                  便于 MinIO 不可用时仍能访问文件，或后续同步回 MinIO）
-- =====================================================================

ALTER TABLE `sys_file`
    ADD COLUMN `fallback` TINYINT(1) DEFAULT 0 COMMENT '是否降级存储（0=正常 1=因MinIO不可用降级到本地）' AFTER `object_name`,
    ADD COLUMN `local_path` VARCHAR(500) DEFAULT NULL COMMENT '本地备份绝对路径（MinIO可用时也记录，便于降级访问）' AFTER `fallback`;

-- 为降级查询加索引
ALTER TABLE `sys_file` ADD KEY `idx_fallback` (`fallback`);
