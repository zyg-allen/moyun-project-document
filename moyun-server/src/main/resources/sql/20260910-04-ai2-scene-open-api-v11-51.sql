-- v11.51 AI 底座闭环补强：ai_scene_config 新增 open_api 列（通用入口白名单）
-- 背景：/api/ai/execute 原先可直调任何 enabled 场景（含 finance_analysis 这类需查用户库的
-- 业务内部场景），可越权绕过业务 Controller 的鉴权/快照/落表编排。
-- 规则：open_api=1 才能经 /api/ai/execute（含 /stream）外部调用；业务 Service 直调
-- AiGatewayService 不经 Controller 层，不受限。存量场景默认 0（关闭），管理页按需开放。
-- 幂等可重复执行。
ALTER TABLE `moyun-db`.ai_scene_config
    ADD COLUMN open_api TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否开放通用入口调用（1=可经 /api/ai/execute 外部调用）' AFTER enabled;
