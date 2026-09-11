-- ============================================================
-- V11.0.2 AI 提供商注册表（配置驱动改造）
--
-- 目标：消灭代码中所有硬编码的提供商分支（openai/dashscope/ollama switch），
-- 提供商能力元数据（API 风格 / 流式能力 / 默认地址 / 是否需要 Key）全部落库，
-- 后台「AI 模块 → 提供商管理」动态增删；新增任何 OpenAI 兼容提供商
-- （DeepSeek/Moonshot/智谱/Groq 等）仅需插入一条记录，零代码改动。
--
-- 核心设计：差异维度是 api_style（协议风格）而非 provider 名：
--   openai_compatible — OpenAI 兼容端点（openai/dashscope/deepseek/moonshot...）
--   ollama_native     — Ollama 原生 API
-- ============================================================

CREATE TABLE IF NOT EXISTS `ai_provider` (
    `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code`              varchar(32)  NOT NULL COMMENT '提供商编码（model_config.provider 关联值，小写唯一）',
    `name`              varchar(64)  NOT NULL COMMENT '显示名称',
    `api_style`         varchar(32)  NOT NULL DEFAULT 'openai_compatible' COMMENT 'API 风格：openai_compatible=OpenAI兼容 / ollama_native=Ollama原生',
    `default_base_url`  varchar(255) NULL COMMENT '默认 Base URL（模型配置留空时兜底）',
    `supports_streaming` tinyint(1)  NOT NULL DEFAULT 1 COMMENT '该提供商 chat 模型是否支持流式输出',
    `requires_api_key`  tinyint(1)   NOT NULL DEFAULT 1 COMMENT '是否必须配置 API Key',
    `enabled`           tinyint(1)   NOT NULL DEFAULT 1 COMMENT '是否启用（停用后不可选/不可用）',
    `sort_order`        int          NOT NULL DEFAULT 0 COMMENT '排序（小在前）',
    `remark`            varchar(255) NULL COMMENT '备注',
    `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       datetime     NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`           tinyint(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_enabled` (`enabled`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 提供商注册表';

-- 种子数据：迁移现有三个代码内置提供商（code 与存量 model_config.provider 对齐，无缝衔接）
INSERT INTO `ai_provider` (`code`, `name`, `api_style`, `default_base_url`, `supports_streaming`, `requires_api_key`, `enabled`, `sort_order`, `remark`) VALUES
('openai',   'OpenAI',         'openai_compatible', 'https://api.openai.com/v1',                        1, 1, 1, 1, 'OpenAI 官方及兼容端点'),
('dashscope','通义千问(百炼)',  'openai_compatible', 'https://dashscope.aliyuncs.com/compatible-mode/v1', 1, 1, 1, 2, '阿里百炼，运行时走 OpenAI 兼容模式'),
('ollama',   'Ollama',         'ollama_native',     'http://localhost:11434',                           1, 0, 1, 3, '本地 Ollama 服务，无需 API Key'),
('deepseek', 'DeepSeek',       'openai_compatible', 'https://api.deepseek.com/v1',                      1, 1, 0, 4, '示例：OpenAI 兼容，后台一键启用'),
('moonshot', 'Moonshot Kimi',  'openai_compatible', 'https://api.moonshot.cn/v1',                       1, 1, 0, 5, '示例：OpenAI 兼容，后台一键启用')
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `api_style` = VALUES(`api_style`);

-- ------------------------------------------------------------
-- 后台菜单：AI 能力 → AI基础配置 → 提供商管理（复用 model-config 权限标识，
-- Controller 的 CRUD 接口即以 cms:ai:model-config:* 鉴权）
-- order_num=4：排在 模型配置(1)/工具管理(2)/数据源管理(3) 之后，避免与存量菜单排序冲突
-- ------------------------------------------------------------
INSERT INTO `moyun-db`.sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, del_flag) VALUES (5440, '提供商管理', 5238, 4, 'provider', 'ai/provider/index', null, '', 1, 0, 'C', '0', '0', 'cms:ai:model-config:list', 'server', 'admin', NOW(), '', null, 'AI提供商注册表（V11.0.2 配置驱动）', '0');
