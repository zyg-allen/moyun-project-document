-- 文件管理表
CREATE TABLE `sys_file`
(
    `id`               bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '文件ID',
    `file_name`        varchar(500)  NOT NULL COMMENT '文件名称',
    `file_ext`         varchar(100) DEFAULT NULL COMMENT '文件扩展名',
    `file_type`        varchar(100) DEFAULT NULL COMMENT '文件类型（image/document/video/audio/other）',
    `file_size`        bigint(20)   DEFAULT NULL COMMENT '文件大小（字节）',
    `file_url`         varchar(1000) NOT NULL COMMENT '文件访问URL',
    `file_path`        varchar(500) DEFAULT NULL COMMENT '文件路径',
    `storage_type`     varchar(100) DEFAULT NULL COMMENT '存储类型（minio/local）',
    `bucket_name`      varchar(100) DEFAULT NULL COMMENT '存储桶名称',
    `object_name`      varchar(500) DEFAULT NULL COMMENT '对象名称',
    `file_md5`         varchar(500) DEFAULT NULL COMMENT '文件MD5值',
    `upload_user_id`   bigint(20)   DEFAULT NULL COMMENT '上传用户ID',
    `upload_user_name` varchar(100) DEFAULT NULL COMMENT '上传用户名称',
    `status`           varchar(20)  DEFAULT '0' COMMENT '状态（0正常 1停用）',
    `business_type`    varchar(500) DEFAULT NULL COMMENT '业务类型',
    `business_id`      varchar(100) DEFAULT NULL COMMENT '业务ID',
    `create_by`        varchar(64)  DEFAULT '' COMMENT '创建者',
    `create_time`      datetime     DEFAULT NULL COMMENT '创建时间',
    `update_by`        varchar(64)  DEFAULT '' COMMENT '更新者',
    `update_time`      datetime     DEFAULT NULL COMMENT '更新时间',
    `remark`           varchar(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_file_type` (`file_type`),
    KEY `idx_storage_type` (`storage_type`),
    KEY `idx_business_type` (`business_type`),
    KEY `idx_business_id` (`business_id`),
    KEY `idx_upload_user_id` (`upload_user_id`),
    KEY `idx_create_time` (`create_time`)
)
ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件管理表';
