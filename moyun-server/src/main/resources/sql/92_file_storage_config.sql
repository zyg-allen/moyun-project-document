-- ===================================================================
-- 92_file_storage_config.sql
-- 文件存储模式系统配置项初始化
--
-- 背景（v1.1.2）：
--   原文件存储模式完全由 application-*.yaml 的 minio.* 字段控制，
--   切换需要重启或调 /system/file/storage/switch 接口。
--   为满足"后台界面配置 + 不重启切换 + 持久化"诉求，新增 sys_config 项。
--
-- 说明：
--   - file.storage.mode：minio=对象存储 / local=本地文件系统 / auto=按 MinIO 可达性自动决定（默认）
--   - file.storage.local.path：本地存储根路径，留空则用 moyun.profile 配置
--   - file.storage.access.url：对外访问 URL 前缀（如 CDN 域名），留空则用后端服务地址
--
-- 与 yaml 配置的关系：
--   sys_config 优先级 > yaml。SysFileServiceImpl 在 useMinio 判断时优先读取 sys_config。
--   若 sys_config 不存在或值为空，回退到 yaml 的 minio.enabled / fallbackToLocal。
-- ===================================================================

-- 1. 存储模式（minio / local / auto）
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '文件存储-存储模式', 'file.storage.mode', 'auto', 'Y', 'admin', NOW(),
       '文件存储模式：minio=MinIO对象存储 / local=本地文件系统 / auto=按 MinIO 可达性自动决定（默认 auto）'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'file.storage.mode');

-- 2. 本地存储根路径（留空则用 moyun.profile 配置）
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '文件存储-本地路径', 'file.storage.local.path', '', 'Y', 'admin', NOW(),
       '本地文件存储根路径，留空则使用 moyun.profile 配置（如 /data/moyun/profile）。注意：若改为非默认路径，需同步在 Nginx/CDN 配置该目录的静态映射，否则通过 /profile/** 访问会 404'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'file.storage.local.path');

-- 3. 对外访问 URL 前缀（留空则用后端服务地址，如 CDN 域名 https://cdn.example.com）
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '文件存储-访问URL前缀', 'file.storage.access.url', '', 'Y', 'admin', NOW(),
       '本地文件对外访问 URL 前缀，留空则用后端服务地址。生产环境建议配置 CDN 域名（仅对本地存储生效，MinIO 访问 URL 见 minio.access-url 配置）'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'file.storage.access.url');
