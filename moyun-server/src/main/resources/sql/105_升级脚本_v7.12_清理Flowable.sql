-- ====================================================================
-- v7.12 升级脚本：清理 Flowable 工作流（运行库）
-- 适配 MySQL 8.x
-- 说明：本脚本幂等（DROP TABLE IF EXISTS / DELETE 无副作用）
-- 背景：Flowable 工作流模块已下线（前端/后端代码、act_* 建表脚本、菜单种子均已移除）。
--       本脚本清理运行库中的残留：
--         - 39 张 act_* 引擎表
--         - 5 张 sys_* 流程辅助表（sys_deploy_form/sys_expression/sys_form/sys_listener/sys_task_form）
--         - 5 类 Flowable 相关字典（exp_data_type/sys_listener_type/sys_listener_value_type/sys_listener_event_type/sys_process_category）
--         - 3 条 sys_menu（4/118/119）+ 角色菜单关联
-- 执行前请确认无在用的流程实例；act_* 表为 Flowable 引擎独占，与业务表无外键关联。
-- ====================================================================

-- 1. 删除 Flowable 引擎表（39 张，按依赖无关顺序，IF EXISTS 幂等）
DROP TABLE IF EXISTS act_evt_log;
DROP TABLE IF EXISTS act_ge_bytearray;  --
DROP TABLE IF EXISTS act_ge_property;
DROP TABLE IF EXISTS act_hi_actinst;
DROP TABLE IF EXISTS act_hi_attachment;
DROP TABLE IF EXISTS act_hi_comment;
DROP TABLE IF EXISTS act_hi_detail;
DROP TABLE IF EXISTS act_hi_entitylink;
DROP TABLE IF EXISTS act_hi_identitylink;
DROP TABLE IF EXISTS act_hi_procinst;
DROP TABLE IF EXISTS act_hi_taskinst;
DROP TABLE IF EXISTS act_hi_tsk_log;
DROP TABLE IF EXISTS act_hi_varinst;
DROP TABLE IF EXISTS act_id_bytearray;
DROP TABLE IF EXISTS act_id_group;  --
DROP TABLE IF EXISTS act_id_info;
DROP TABLE IF EXISTS act_id_membership;
DROP TABLE IF EXISTS act_id_priv;  --
DROP TABLE IF EXISTS act_id_priv_mapping;
DROP TABLE IF EXISTS act_id_property;
DROP TABLE IF EXISTS act_id_token;
DROP TABLE IF EXISTS act_id_user;
DROP TABLE IF EXISTS act_procdef_info;
DROP TABLE IF EXISTS act_re_deployment; --
DROP TABLE IF EXISTS act_re_model;
DROP TABLE IF EXISTS act_re_procdef; --
DROP TABLE IF EXISTS act_ru_actinst;
DROP TABLE IF EXISTS act_ru_deadletter_job;
DROP TABLE IF EXISTS act_ru_entitylink;
DROP TABLE IF EXISTS act_ru_event_subscr;
DROP TABLE IF EXISTS act_ru_execution; --
DROP TABLE IF EXISTS act_ru_external_job;
DROP TABLE IF EXISTS act_ru_history_job;
DROP TABLE IF EXISTS act_ru_identitylink;
DROP TABLE IF EXISTS act_ru_job;
DROP TABLE IF EXISTS act_ru_suspended_job;
DROP TABLE IF EXISTS act_ru_task;
DROP TABLE IF EXISTS act_ru_timer_job;
DROP TABLE IF EXISTS act_ru_variable;

-- 2. 删除 Flowable 流程辅助表（5 张 sys_* 表，曾放在 system 包下，IF EXISTS 幂等）
DROP TABLE IF EXISTS sys_deploy_form;
DROP TABLE IF EXISTS sys_expression;
DROP TABLE IF EXISTS sys_form;
DROP TABLE IF EXISTS sys_listener;
DROP TABLE IF EXISTS sys_task_form;

-- 3. 删除 Flowable 相关字典数据
--    字典类型：exp_data_type / sys_listener_type / sys_listener_value_type / sys_listener_event_type / sys_process_category
DELETE FROM sys_dict_data WHERE dict_type IN ('exp_data_type', 'sys_listener_type', 'sys_listener_value_type', 'sys_listener_event_type', 'sys_process_category');
DELETE FROM sys_dict_type WHERE dict_type IN ('exp_data_type', 'sys_listener_type', 'sys_listener_value_type', 'sys_listener_event_type', 'sys_process_category');

-- 4. 删除 Flowable 菜单（流程管理 4 / 流程定义 118 / 流程任务 119）
DELETE FROM sys_role_menu WHERE menu_id IN (4, 118, 119);
DELETE FROM sys_menu WHERE menu_id IN (4, 118, 119);

-- ====================================================================
-- 升级完成
-- ====================================================================
