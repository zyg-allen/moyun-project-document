-- =============================================================================
-- 岗位配置统一 · 数据补齐（存量库最终态对齐 init-sql）
-- -----------------------------------------------------------------------------
-- 背景：`20260928-12` 按 **name 精确匹配**把旧字典并入模板，但两边命名不同：
--   模板行名：初级 Java 开发工程师 / 中级 Java 开发工程师（2-5 年经验） / 高级 / 资深 Java 开发工程师（5 年以上经验）
--   字典行名：Java后端工程师（code=java_backend）
--   → 未匹配，导致「Java 后端」出现重复行，且 3 条 Java 模板 **缺 required_skills**
--     （直接后果：ResumeScoringService 的「岗位匹配度」评分为 0、画像必备技能为空）。
--
-- 本脚本按**业务同一岗位**语义合并，并把存量库补齐到与 `init-sql` 一致：
--   ① 3 条 Java 模板：补 code/industry/level/required_skills/hot_companies/sort + 统一命名
--      （保留各自 difficulty/question_count —— 它们本就按初级/中级/高级分层）
--   ② 前端 / 算法 两条（由旧字典迁入）：补 jd_text / keywords
--   ③ 删除由旧字典迁入的重复行「Java后端工程师」（其 6 列已并入模板行）
--
-- 幂等性：每步都带"仅当为空/存在时"守卫，重跑 0 行受影响。
-- =============================================================================

-- ---------------------------------------------------------------------------
-- ① 3 条 Java 模板：补字典字段 + 统一命名
-- ---------------------------------------------------------------------------
UPDATE portal_job_template SET
    name            = '初级 Java 开发工程师',
    code            = 'java_backend',
    industry        = '互联网',
    `level`         = 'junior',
    sort            = 1,
    required_skills = '["Java","Spring","SpringBoot","MyBatis","MySQL","Redis","MQ","JVM","并发编程","分布式","微服务","设计模式"]',
    hot_companies   = '["阿里","腾讯","字节跳动","美团","京东","百度","拼多多","网易","滴滴","快手"]',
    keywords        = 'Java,Spring,SpringBoot,MyBatis,MySQL,Redis,并发编程'
WHERE code IS NULL AND name LIKE '初级 Java 开发工程师%';

UPDATE portal_job_template SET
    name            = '中级 Java 开发工程师（2-5 年经验）',
    code            = 'java_backend_mid',
    industry        = '互联网',
    `level`         = 'mid',
    sort            = 2,
    required_skills = '["Java","Spring","SpringBoot","MyBatis","MySQL","Redis","MQ","JVM","并发编程","分布式","微服务","设计模式"]',
    hot_companies   = '["阿里","腾讯","字节跳动","美团","京东","百度","拼多多","网易","滴滴","快手"]',
    keywords        = 'Java,JVM,并发编程,MySQL,Redis,消息队列,分布式,微服务'
WHERE code IS NULL AND name LIKE '中级 Java 开发工程师%';

UPDATE portal_job_template SET
    name            = '高级 / 资深 Java 开发工程师（5 年以上经验）',
    code            = 'java_backend_senior',
    industry        = '互联网',
    `level`         = 'senior',
    sort            = 3,
    required_skills = '["Java","Spring","SpringBoot","MyBatis","MySQL","Redis","MQ","JVM","并发编程","分布式","微服务","设计模式"]',
    hot_companies   = '["阿里","腾讯","字节跳动","美团","京东","百度","拼多多","网易","滴滴","快手"]',
    keywords        = 'Java,架构设计,高并发,JVM调优,分布式,微服务,分库分表,稳定性'
WHERE code IS NULL AND name LIKE '高级 / 资深 Java 开发工程师%';

-- ①-b 修正存量库脏数据：中级 Java 的 JD 被错写成「初级 Java」原文（复制粘贴所致）
--      守卫：仅当内容确实以「初级」开头时才覆盖，避免误伤已人工修正的文案
UPDATE portal_job_template SET
    jd_text = '中级 Java 开发工程师（2-5 年经验）\n薪资范围：18K - 30K · 14-16薪\n工作地点：深圳市南山区\n\n岗位职责：\n1. 独立负责业务模块的设计、开发与上线，对交付质量负责\n2. 参与系统性能优化、慢查询治理与线上故障定位\n3. 参与技术方案评审，输出设计文档\n\n任职要求：\n1. 本科及以上学历，2-5 年 Java 后端开发经验\n2. 熟悉 JVM 内存模型与 GC 调优，具备并发编程实战经验\n3. 熟悉 MySQL 索引优化与事务隔离级别；熟悉 Redis 缓存设计与穿透/雪崩防护\n4. 熟悉消息队列（Kafka/RocketMQ）使用场景与可靠投递\n5. 了解分布式与微服务（Spring Cloud/Dubbo）相关组件'
WHERE code = 'java_backend_mid' AND jd_text LIKE '初级 Java 开发工程师%';

-- ①-c 修正存量库 sort 冲突：中/高级 Java 占用了 2/3，迁入行 sort 与之重复
UPDATE portal_job_template SET sort = 4 WHERE code = 'frontend'  AND sort = 2;
UPDATE portal_job_template SET sort = 5 WHERE code = 'algorithm' AND sort = 3;

-- ---------------------------------------------------------------------------
-- ② 迁入行补 JD 与关键词（仅当为空时才写，保留人工后续维护）
--    ⚠ 提示：以下 JD 为占位文案，请按真实岗位在后台【岗位模板】中完善
-- ---------------------------------------------------------------------------
UPDATE portal_job_template SET
    jd_text = '前端工程师\n薪资范围：18K - 32K · 14-16薪\n工作地点：深圳市南山区\n\n岗位职责：\n1. 负责公司 Web 端产品的开发与迭代，保障交互体验与性能\n2. 参与前端工程化建设（构建、组件库、规范与自动化）\n3. 与后端协作完成接口联调与线上问题排查\n\n任职要求：\n1. 本科及以上学历，2 年以上前端开发经验\n2. 扎实的 JavaScript / TypeScript 基础，熟悉 ES6+ 与异步编程\n3. 熟练掌握 Vue 或 React 其一，理解其响应式与渲染机制\n4. 熟悉 Webpack / Vite 构建原理与常用优化手段\n5. 熟悉浏览器渲染原理、HTTP 缓存与前端性能优化（LCP/CLS 等指标）',
    keywords = 'JavaScript,TypeScript,Vue,React,工程化,浏览器原理,性能优化,HTTP',
    remark   = '原 portal_interview_position 迁入'
WHERE code = 'frontend' AND (jd_text IS NULL OR jd_text = '');

UPDATE portal_job_template SET
    jd_text = '算法工程师\n薪资范围：25K - 45K · 15-16薪\n工作地点：深圳市南山区\n\n岗位职责：\n1. 负责推荐/搜索/NLP 等方向算法模型的设计、训练与上线\n2. 结合业务指标持续迭代模型效果，完成 A/B 实验与归因分析\n3. 参与特征工程与数据链路建设\n\n任职要求：\n1. 硕士及以上学历，计算机/数学/统计相关专业\n2. 扎实的数据结构与算法基础，熟悉动态规划、图论与字符串算法\n3. 熟悉机器学习常用模型与评估指标，了解深度学习基本原理\n4. 熟练使用 Python 及主流框架（PyTorch/TensorFlow）\n5. 有推荐、搜索或 NLP 相关项目经验者优先',
    keywords = '算法,数据结构,动态规划,图论,机器学习,深度学习,数学',
    remark   = '原 portal_interview_position 迁入'
WHERE code = 'algorithm' AND (jd_text IS NULL OR jd_text = '');

-- ---------------------------------------------------------------------------
-- ③ 删除由旧字典迁入的重复行（其字段已并入 Java 模板行）
--     守卫：仅当同名模板行已具备 required_skills 时才删，避免误删唯一数据
-- ---------------------------------------------------------------------------
DELETE dup FROM portal_job_template dup
 WHERE dup.code = 'java_backend'
   AND dup.name = 'Java后端工程师'
   AND EXISTS (SELECT 1 FROM (SELECT 1 FROM portal_job_template
                               WHERE name LIKE '初级 Java 开发工程师%' AND required_skills IS NOT NULL) x);

-- ---------------------------------------------------------------------------
-- ④ 复核 SQL（期望：5 行；Java 三条各有 code/required_skills；无行缺 skills）
-- ---------------------------------------------------------------------------
-- SELECT id, name, code, industry, `level`, sort, difficulty, question_count,
--        LEFT(IFNULL(required_skills,''), 22) AS skills, LEFT(IFNULL(jd_text,''), 22) AS jd
--   FROM portal_job_template ORDER BY sort, id;
--
-- SELECT COUNT(*) AS active_missing_skills FROM portal_job_template
--  WHERE status='active' AND (required_skills IS NULL OR required_skills='');   -- 期望 0
