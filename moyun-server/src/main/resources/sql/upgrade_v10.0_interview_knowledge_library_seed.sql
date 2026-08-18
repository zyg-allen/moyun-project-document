-- ============================================================
-- upgrade_v10.0_interview_knowledge_library_seed.sql
-- 墨韵·智库 v10.0 Phase 0-3 行业知识库初始化脚本
-- 内容: 为 AI 语音面试官 RAG 出题(方案 §3.1 Phase 3 / V10.3)初始化 3 个面试行业知识库
--       库1: Java后端面试热题库  库2: 前端面试热题库  库3: 通用软技能与HR题库
-- 特性: 全部幂等(可重复执行),基于 name 去重
-- 执行顺序: 在 init_v7.8.sql 之后执行(依赖 ai_knowledge_library 表存在)
-- 关联文档: docs/14_AI语音面试官评估与落地计划.md §Phase 0 / V10.3
--
-- 说明:
--   本脚本仅初始化知识库元数据(library + config),不包含文档内容上传。
--   实际文档(md格式面试热题)需通过后台 /ai/knowledge-base 上传后由系统自动向量化。
--   V10.3 行业RAG出题功能开发时,需保证这3个库中至少有 5 篇已向量化的文档。
--
-- 后续 TODO(运营跟进):
--   1. 上传 Java后端面试热题文档(至少5篇md,覆盖:JVM/并发/Spring/MySQL/Redis)
--   2. 上传 前端面试热题文档(至少5篇md,覆盖:JS基础/Vue/React/浏览器/性能)
--   3. 上传 通用软技能文档(至少3篇md,覆盖:STAR法则/项目复盘/职业规划)
--   4. 文档上传后,在后台触发向量化,确认 ai_knowledge_base.status=2(处理成功)
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 第一节: 初始化 3 个面试行业知识库
-- ============================================================

SELECT 'P0-3 行业知识库初始化开始' AS phase,
       (SELECT COUNT(*) FROM ai_knowledge_library WHERE status = 'active') AS before_count,
       '目标: 新增3个面试行业知识库' AS target;

-- 库1: Java后端面试热题库
INSERT INTO ai_knowledge_library
  (name, description, icon, category, tags, document_count, total_segments, total_size,
   usage_count, hit_count, last_used_time, status, is_public, created_at, updated_at)
SELECT
  'Java后端面试热题库',
  'AI语音面试官行业RAG出题用知识库-V10.3启用。覆盖Java基础/JVM/并发编程/Spring全家桶/MyBatis/MySQL/Redis/MQ/分布式微服务/设计模式等后端高频面试考点。文档来源:官方文档摘要+高频面经+内部题库解析。',
  '☕',
  '面试行业知识库',
  '["Java","后端","面试","RAG","V10.3"]',
  0, 0, 0, 0, 0, NULL, 'active', 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM ai_knowledge_library WHERE name = 'Java后端面试热题库' AND status = 'active'
);

-- 库2: 前端面试热题库
INSERT INTO ai_knowledge_library
  (name, description, icon, category, tags, document_count, total_segments, total_size,
   usage_count, hit_count, last_used_time, status, is_public, created_at, updated_at)
SELECT
  '前端面试热题库',
  'AI语音面试官行业RAG出题用知识库-V10.3启用。覆盖JavaScript/TypeScript/HTML/CSS/Vue/React/Node.js/Webpack/Vite/浏览器原理/HTTP/性能优化等前端高频面试考点。',
  '🎨',
  '面试行业知识库',
  '["前端","JavaScript","Vue","React","面试","RAG","V10.3"]',
  0, 0, 0, 0, 0, NULL, 'active', 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM ai_knowledge_library WHERE name = '前端面试热题库' AND status = 'active'
);

-- 库3: 通用软技能与HR题库
INSERT INTO ai_knowledge_library
  (name, description, icon, category, tags, document_count, total_segments, total_size,
   usage_count, hit_count, last_used_time, status, is_public, created_at, updated_at)
SELECT
  '通用软技能与HR题库',
  'AI语音面试官行业RAG出题用知识库-V10.3启用。覆盖STAR法则/项目经验表达/团队协作/沟通能力/职业规划/抗压能力/学习能力等HR与软技能高频面试考点。',
  '💬',
  '面试行业知识库',
  '["HR","软技能","STAR","面试","RAG","V10.3"]',
  0, 0, 0, 0, 0, NULL, 'active', 1, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM ai_knowledge_library WHERE name = '通用软技能与HR题库' AND status = 'active'
);

-- ============================================================
-- 第二节: 为每个知识库创建默认配置(若不存在)
-- 知识库配置决定文档分片与检索策略
-- ============================================================

-- Java后端面试热题库配置
INSERT INTO ai_knowledge_library_config
  (library_id, segment_mode, segment_separator, segment_max_length, segment_overlap_length,
   preprocess_replace_spaces, preprocess_remove_urls, preprocess_remove_extra_newlines,
   index_mode, embedding_model, retrieval_mode, retrieval_top_k,
   rerank_enabled, rerank_model, created_at, updated_at)
SELECT
  lib.id,
  'general',           -- 通用分段(面试题文档混合长短,不适合qa模式)
  '\n\n',              -- 段落分隔
  800,                 -- 800字符/段(保证单个面试题完整)
  100,                 -- 100字符重叠(上下文连贯)
  1, 1, 1,
  'high_quality',      -- 高质量索引(面试场景对召回精度要求高)
  'text-embedding-v3', -- 默认DashScope向量化模型
  'hybrid',            -- 混合检索(向量+关键词,面试术语命中关键)
  5,                   -- Top5(出题场景需精选,不需要太多)
  1,                   -- 启用Rerank(提升出题质量)
  'qwen3-rerank',      -- Rerank模型
  NOW(), NOW()
FROM ai_knowledge_library lib
WHERE lib.name = 'Java后端面试热题库' AND lib.status = 'active'
  AND NOT EXISTS (
    SELECT 1 FROM ai_knowledge_library_config c WHERE c.library_id = lib.id
  );

-- 前端面试热题库配置(同上策略)
INSERT INTO ai_knowledge_library_config
  (library_id, segment_mode, segment_separator, segment_max_length, segment_overlap_length,
   preprocess_replace_spaces, preprocess_remove_urls, preprocess_remove_extra_newlines,
   index_mode, embedding_model, retrieval_mode, retrieval_top_k,
   rerank_enabled, rerank_model, created_at, updated_at)
SELECT
  lib.id,
  'general', '\n\n', 800, 100, 1, 1, 1,
  'high_quality', 'text-embedding-v3', 'hybrid', 5, 1, 'qwen3-rerank',
  NOW(), NOW()
FROM ai_knowledge_library lib
WHERE lib.name = '前端面试热题库' AND lib.status = 'active'
  AND NOT EXISTS (
    SELECT 1 FROM ai_knowledge_library_config c WHERE c.library_id = lib.id
  );

-- 通用软技能与HR题库配置(段落稍大,STAR故事较长)
INSERT INTO ai_knowledge_library_config
  (library_id, segment_mode, segment_separator, segment_max_length, segment_overlap_length,
   preprocess_replace_spaces, preprocess_remove_urls, preprocess_remove_extra_newlines,
   index_mode, embedding_model, retrieval_mode, retrieval_top_k,
   rerank_enabled, rerank_model, created_at, updated_at)
SELECT
  lib.id,
  'general', '\n\n', 1200, 150, 1, 1, 1,
  'high_quality', 'text-embedding-v3', 'hybrid', 5, 1, 'qwen3-rerank',
  NOW(), NOW()
FROM ai_knowledge_library lib
WHERE lib.name = '通用软技能与HR题库' AND lib.status = 'active'
  AND NOT EXISTS (
    SELECT 1 FROM ai_knowledge_library_config c WHERE c.library_id = lib.id
  );

-- ============================================================
-- 验证段: 知识库初始化结果检查
-- ============================================================
SELECT 'P0-3 行业知识库初始化完成' AS phase,
       (SELECT COUNT(*) FROM ai_knowledge_library WHERE category = '面试行业知识库' AND status = 'active') AS library_count,
       '目标: 3' AS target;

-- 知识库详情
SELECT
  id, name, icon, category, status, is_public,
  document_count, total_segments, created_at
FROM ai_knowledge_library
WHERE category = '面试行业知识库' AND status = 'active'
ORDER BY created_at;

-- 配置详情(验证3个库均有配置)
SELECT
  lib.name AS library_name,
  cfg.segment_mode, cfg.segment_max_length, cfg.retrieval_mode, cfg.retrieval_top_k,
  cfg.rerank_enabled, cfg.embedding_model
FROM ai_knowledge_library lib
LEFT JOIN ai_knowledge_library_config cfg ON cfg.library_id = lib.id
WHERE lib.category = '面试行业知识库' AND lib.status = 'active'
ORDER BY lib.created_at;

-- 文档上传进度检查(V10.3 开发前的就绪度)
SELECT
  lib.name AS library_name,
  COUNT(kb.id) AS uploaded_docs,
  SUM(CASE WHEN kb.processing_status = 'completed' THEN 1 ELSE 0 END) AS vectorized_docs,
  'V10.3启用前需: vectorized_docs >= 5' AS readiness_note
FROM ai_knowledge_library lib
LEFT JOIN ai_knowledge_base kb ON kb.library_id = lib.id
WHERE lib.category = '面试行业知识库' AND lib.status = 'active'
GROUP BY lib.id, lib.name
ORDER BY lib.created_at;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 脚本执行完成
-- 预期结果:
--   library_count = 3
--   每个库均有对应 ai_knowledge_library_config 记录
--   uploaded_docs = 0 (待运营上传)
--   vectorized_docs = 0 (待运营上传后向量化)
--
-- V10.3 启用前的就绪检查(运营完成后):
--   3个库均 vectorized_docs >= 5
--   可通过本脚本最后的"文档上传进度检查"查询验证
-- ============================================================
