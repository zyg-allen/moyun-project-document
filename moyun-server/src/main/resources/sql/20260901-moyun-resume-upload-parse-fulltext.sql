-- ============================================================
-- 简历模块重构：上传闭环 + 全文存储（v10.22）
-- 功能：
--   1. 附件简历：上传源文件保存为附件简历类型，可转在线简历
--   2. 全文存储：保存简历时自动拼接全文纯文本，供 AI 分析使用
-- 日期：2026-09-01
-- 注意：增量 ALTER，不修改原 CREATE TABLE 语句
-- ============================================================

-- ---------- 阶段1：附件简历字段 ----------
ALTER TABLE `portal_user_resume`
  ADD COLUMN `source_type` varchar(20) NOT NULL DEFAULT 'online'
    COMMENT '来源类型：online（在线创建）/ attachment（附件解析）' AFTER `status`,
  ADD COLUMN `source_file_url` varchar(500) DEFAULT NULL
    COMMENT '附件源文件URL（source_type=attachment 时有值，支持下载）' AFTER `source_type`,
  ADD COLUMN `source_file_name` varchar(255) DEFAULT NULL
    COMMENT '附件原始文件名（上传时的文件名，用于下载时还原文件名）' AFTER `source_file_url`;

-- 索引：按用户+来源类型查询附件简历列表
ALTER TABLE `portal_user_resume`
  ADD KEY `idx_user_source` (`user_id`, `source_type`);

-- ---------- 阶段3：全文存储字段 ----------
ALTER TABLE `portal_user_resume`
  ADD COLUMN `full_text` mediumtext
    COMMENT '简历全文纯文本（保存时自动拼接结构化字段，供 AI 分析使用）' AFTER `self_intro`;

-- 数据迁移：已有简历 source_type 默认 online（ALTER DEFAULT 已覆盖）
-- full_text 在下次 saveResume 时自动拼接，不强制回填避免锁表
