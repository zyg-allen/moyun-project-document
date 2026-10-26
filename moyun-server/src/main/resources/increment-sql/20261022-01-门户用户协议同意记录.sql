-- ==============================================================================
-- 20261022-01 门户用户协议同意记录（与门户账号体系共用）
-- 幂等：表存在则跳过；本脚本不涉及 sys_menu
-- ==============================================================================

SET @tbl_exists := (
  SELECT COUNT(*) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_user_agreement'
);

SET @ddl := IF(@tbl_exists = 0,
  'CREATE TABLE `portal_user_agreement` (
     `id` bigint NOT NULL AUTO_INCREMENT COMMENT ''主键'',
     `user_id` bigint NOT NULL COMMENT ''门户用户ID'',
     `doc_type` varchar(32) NOT NULL COMMENT ''文档类型：terms/privacy'',
     `version` varchar(32) NOT NULL COMMENT ''协议版本'',
     `agreed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ''同意时间'',
     `ip` varchar(64) DEFAULT NULL COMMENT ''客户端IP'',
     `user_agent` varchar(255) DEFAULT NULL COMMENT ''客户端UA'',
     PRIMARY KEY (`id`),
     UNIQUE KEY `uk_user_doc_version` (`user_id`,`doc_type`,`version`),
     KEY `idx_user` (`user_id`)
   ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''门户用户协议同意记录（注册/换版重签留痕）''',
  'SELECT ''portal_user_agreement already exists'' AS msg');

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
