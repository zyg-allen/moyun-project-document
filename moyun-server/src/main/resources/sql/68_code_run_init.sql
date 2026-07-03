-- =====================================================
-- 在线代码运行 DDL 脚本（任务 3.6 学习者成长闭环）
-- 记录用户在线运行代码的历史，配合后端沙箱执行器（ProcessBuilder）
-- 幂等设计：CREATE TABLE IF NOT EXISTS
-- 执行顺序：本脚本在 57_column_init.sql 之后执行
-- =====================================================

-- 代码运行记录表
CREATE TABLE IF NOT EXISTS `portal_code_run` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT       NOT NULL                COMMENT '运行者用户ID',
    `language`    VARCHAR(16)  NOT NULL                COMMENT '编程语言 java/python/javascript',
    `code`        MEDIUMTEXT   NOT NULL                COMMENT '用户提交的源代码',
    `stdin`      TEXT                                  COMMENT '标准输入内容',
    `output`     MEDIUMTEXT                            COMMENT '标准输出（截断至 1MB）',
    `error_msg`  MEDIUMTEXT                            COMMENT '错误输出 / 编译错误信息',
    `status`     VARCHAR(16)  NOT NULL DEFAULT 'running' COMMENT '运行状态 running/success/failed/timeout',
    `runtime_ms` INT          DEFAULT NULL            COMMENT '运行耗时（毫秒）',
    `mem_kb`     INT          DEFAULT NULL            COMMENT '内存占用（KB，粗略估算）',
    `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码运行记录';
