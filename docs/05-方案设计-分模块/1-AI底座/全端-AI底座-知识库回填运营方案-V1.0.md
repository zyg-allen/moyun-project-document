# 知识库数据回填运营指引 —— Java 后端 / 前端面试题库

> 《AI底座企业级评估》P1-7：基础设施已就绪（上传/分片/向量化/检索/引用溯源），只差库内内容。内容自行生产，本文档仅作操作指引。

## 一、建库（管理后台）

| 库名称 | category | 内容范围 |
|---|---|---|
| Java 后端面试题库 | 面试题库 | JVM / 并发 / MySQL / Redis / Spring / MyBatis / 分布式 / 网络 / 场景设计 |
| 前端面试题库 | 面试题库 | JS / ES6+ / CSS / Vue3 / 浏览器 / 网络 / 性能优化 / 工程化 / uni-app / 安全 |

「AI 基础配置 → 知识库管理」新建，记录两库 library_id。

## 二、内容规范（RAG 友好，直接影响检索命中）

一篇一题，`.md` 文件，文件名即问题。模板：

```markdown
# <问题标题>

## 考察要点
<!-- 2-3 行 -->

## 标准答案
<!-- 分点作答，每要点独立成段，关键词前置（首句含核心术语） -->

## 代码示例
<!-- 如适用；≤40 行；纯理论题省略 -->

## 常见追问
<!-- 2-3 个一问一答 -->

## 一句话总结
<!-- 30 字内 -->
```

要点：
1. 二级标题即语义边界，每节自包含，不跨节指代（分片后上下文会丢）
2. 关键词前置：答案首句直接包含核心术语（向量+BM25 双路召回都吃这个）
3. 单篇 800~2000 字；核心答案不用表格（表格分片语义断裂），用列表替代

## 三、上传与向量化

1. 「知识库管理 → 文档管理」上传 `.md`，选目标库
2. 分片参数用库默认；若抽检发现答案被切碎，调大分段长度重新处理
3. 链路：上传 → 解析 → 分片 → Embedding → JVector 写入；完成后核对 `processing_status=completed`、`segment_count>0`
4. failed 的看详情（常见：Embedding 模型未配置/超时），修复后重触发

## 四、回填完成后绑定场景（SQL）

```sql
-- 场景直绑两库（按实际 library_id）
UPDATE `moyun-db`.ai_scene_config
SET knowledge_library_ids = '[<java库id>, <前端库id>]', update_time = NOW()
WHERE scene_code = 'knowledge_qa' AND deleted = 0;

-- 或推荐：管理页建 Agent（绑两库 + RAG 参数 + 人设）后
UPDATE `moyun-db`.ai_scene_config
SET agent_id = <agentId>, update_time = NOW()
WHERE scene_code = 'knowledge_qa' AND deleted = 0;
```

验证：`POST /api/ai/execute {"sceneCode":"knowledge_qa","userInput":"HashMap 扩容机制"}` → references 应含 Java 库文档引用。

## 五、验收

- 全部文档 processing_status=completed、segment_count>0
- 每库抽 10 个 query，≥8 个命中对应文档，无跨题污染（问 HashMap 不召回线程池题）
- 无敏感词、代码无乱码
