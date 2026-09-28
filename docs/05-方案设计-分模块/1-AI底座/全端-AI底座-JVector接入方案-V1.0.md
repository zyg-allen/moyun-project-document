> # JVector 接入方案 — 结合现有代码
>
> ## 一、当前代码中 ES 的使用情况
>
> 基于你的项目文档和代码结构，ES 主要用在以下场景：
>
> | 模块           | 用途                | 涉及文件                                       |
> | -------------- | ------------------- | ---------------------------------------------- |
> | **知识库 RAG** | 向量检索 + 语义搜索 | `KnowledgeSearchService`、`RagService`         |
> | **面试出题**   | 相似题目检索        | `QuestionPicker`、`LanceDBService`（已替换？） |
> | **简历推荐**   | 相似简历匹配        | `ResumeSearchService`                          |
> | **全文搜索**   | 文章/面经关键词搜索 | `ArticleSearchService`                         |
>
> **注意**：你之前已经在尝试替换 LanceDB，部分代码可能已经改了。下面的方案基于**你现在仍然用 ES 做向量检索**的假设。
>
>
> ## 二、JVector 集成方案（不改现有业务层）
>
> 核心思路：**保持业务层接口不变，只替换底层的向量存储实现。**
>
> ```
> 业务层（不变）
>     │
>     ▼
> VectorStore 接口（统一抽象）    ← 新增
>     │
>     ├── 实现类 1: EsVectorStore（待删除）
>     │
>     └── 实现类 2: JVectorVectorStore（新增）
> ```
>
>
> ## 三、代码改造步骤
>
> ### Step 1: 新增统一向量存储接口
>
> ```java
> // ============================================================
> // 新建: common/store/VectorStore.java
> // 统一向量存储接口，隔离具体实现
> // ============================================================
> public interface VectorStore {
>
>     /**
>      * 添加向量
>      */
>     void add(String id, float[] vector, Map<String, Object> metadata);
>
>     /**
>      * 批量添加
>      */
>     void addBatch(List<VectorEntry> entries);
>
>     /**
>      * 向量检索
>      */
>     List<VectorSearchResult> search(float[] vector, int limit);
>
>     /**
>      * 带过滤条件的向量检索
>      */
>     List<VectorSearchResult> search(float[] vector, int limit, Map<String, Object> filter);
>
>     /**
>      * 删除向量
>      */
>     void delete(String id);
>
>     /**
>      * 获取数量
>      */
>     long count();
>
>     /**
>      * 清空
>      */
>     void clear();
> }
>
> // ============================================================
> // 向量条目
> // ============================================================
> @Data
> @Builder
> public class VectorEntry {
>     private String id;
>     private float[] vector;
>     private Map<String, Object> metadata;
> }
>
> // ============================================================
> // 检索结果
> // ============================================================
> @Data
> @Builder
> public class VectorSearchResult {
>     private String id;
>     private float score;
>     private Map<String, Object> metadata;
> }
> ```
>
> ### Step 2: 实现 JVector 版本
>
> ```java
> // ============================================================
> // 新建: common/store/JVectorVectorStore.java
> // ============================================================
> @Component
> @ConditionalOnProperty(name = "vector-store.type", havingValue = "jvector")
> @Slf4j
> public class JVectorVectorStore implements VectorStore {
>
>     private final EmbeddingStore<TextSegment> embeddingStore;
>
>     public JVectorVectorStore() {
>         this.embeddingStore = JVectorEmbeddingStore.builder()
>             .dimension(1536)
>             .persistencePath("./data/jvector-index")
>             .maxDegree(16)
>             .beamWidth(100)
>             .similarityFunction(VectorSimilarityFunction.DOT_PRODUCT)
>             .build();
>         log.info("JVector 向量存储初始化完成");
>     }
>
>     @Override
>     public void add(String id, float[] vector, Map<String, Object> metadata) {
>         TextSegment segment = TextSegment.from(
>             metadata.get("text") != null ? metadata.get("text").toString() : ""
>         );
>         embeddingStore.add(Embedding.from(vector), segment);
>     }
>
>     @Override
>     public void addBatch(List<VectorEntry> entries) {
>         List<TextSegment> segments = entries.stream()
>             .map(e -> TextSegment.from(
>                 e.getMetadata().get("text") != null ?
>                     e.getMetadata().get("text").toString() : ""
>             ))
>             .collect(Collectors.toList());
>
>         List<Embedding> embeddings = entries.stream()
>             .map(e -> Embedding.from(e.getVector()))
>             .collect(Collectors.toList());
>
>         embeddingStore.addAll(embeddings, segments);
>     }
>
>     @Override
>     public List<VectorSearchResult> search(float[] vector, int limit) {
>         Embedding queryEmbedding = Embedding.from(vector);
>         List<TextSegment> results = embeddingStore.findRelevant(queryEmbedding, limit);
>
>         return results.stream()
>             .map(segment -> VectorSearchResult.builder()
>                 .id(segment.metadata().getString("id"))
>                 .score(segment.metadata().getDouble("score"))
>                 .metadata(segment.metadata())
>                 .build())
>             .collect(Collectors.toList());
>     }
>
>     @Override
>     public List<VectorSearchResult> search(float[] vector, int limit, Map<String, Object> filter) {
>         // JVector 通过 metadata 过滤
>         String filterString = buildFilterString(filter);
>         Embedding queryEmbedding = Embedding.from(vector);
>         List<TextSegment> results = embeddingStore.findRelevant(queryEmbedding, limit, filterString);
>
>         return results.stream()
>             .map(segment -> VectorSearchResult.builder()
>                 .id(segment.metadata().getString("id"))
>                 .score(segment.metadata().getDouble("score"))
>                 .metadata(segment.metadata())
>                 .build())
>             .collect(Collectors.toList());
>     }
>
>     @Override
>     public void delete(String id) {
>         embeddingStore.remove(id);
>     }
>
>     @Override
>     public long count() {
>         return embeddingStore.count();
>     }
>
>     @Override
>     public void clear() {
>         // JVector 不支持直接清空，删除索引文件或重建
>         embeddingStore.removeAll();
>     }
>
>     private String buildFilterString(Map<String, Object> filter) {
>         if (filter == null || filter.isEmpty()) return "";
>         return filter.entrySet().stream()
>             .map(e -> e.getKey() + " = '" + e.getValue() + "'")
>             .collect(Collectors.joining(" AND "));
>     }
> }
> ```
>
> ### Step 3: 适配你现有的业务层
>
> **改造前（直接调用 ES）**：
> ```java
> @Service
> public class KnowledgeSearchService {
>
>     @Autowired
>     private ElasticsearchRestTemplate esTemplate;
>
>     public List<KnowledgeDoc> search(String keyword) {
>         NativeSearchQuery query = new NativeSearchQueryBuilder()
>             .withQuery(QueryBuilders.matchQuery("content", keyword))
>             .build();
>         return esTemplate.search(query, KnowledgeDoc.class)
>             .getSearchHits().stream()
>             .map(SearchHit::getContent)
>             .collect(Collectors.toList());
>     }
> }
> ```
>
> **改造后（通过统一接口）**：
> ```java
> @Service
> public class KnowledgeSearchService {
>
>     @Autowired
>     private VectorStore vectorStore;        // 注入接口，不依赖具体实现
>
>     @Autowired
>     private EmbeddingModel embeddingModel;
>
>     public List<KnowledgeDoc> search(String keyword) {
>         // 1. 向量化
>         float[] vector = embeddingModel.embed(keyword);
>
>         // 2. 检索
>         List<VectorSearchResult> results = vectorStore.search(vector, 10);
>
>         // 3. 转换结果
>         return results.stream()
>             .map(r -> {
>                 KnowledgeDoc doc = new KnowledgeDoc();
>                 doc.setId(r.getId());
>                 doc.setContent((String) r.getMetadata().get("text"));
>                 doc.setScore(r.getScore());
>                 return doc;
>             })
>             .collect(Collectors.toList());
>     }
> }
> ```
>
> ### Step 4: 配置切换
>
> ```yaml
> # application.yml
> vector-store:
>   type: jvector           # jvector / elasticsearch（切换用）
>
> # JVector 配置（type=jvector 时生效）
> jvector:
>   data-path: ./data/jvector-index
>   embedding-dimension: 1536
> ```
>
> ### Step 5: 移除旧 ES 依赖
>
> ```xml
> <!-- 注释或删除 ES 依赖 -->
> <!--
> <dependency>
>     <groupId>org.springframework.boot</groupId>
>     <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
> </dependency>
> -->
> ```
>
> ### Step 6: 数据迁移
>
> ```java
> // ============================================================
> // 迁移脚本：ES → JVector（执行一次后删除）
> // ============================================================
> @Component
> @Profile("migration")
> public class DataMigrationRunner implements CommandLineRunner {
>
>     @Autowired
>     private ElasticsearchRestTemplate esTemplate;
>
>     @Autowired
>     private JVectorVectorStore jVectorStore;
>
>     @Autowired
>     private EmbeddingModel embeddingModel;
>
>     @Override
>     public void run(String... args) {
>         log.info("开始迁移 ES 数据到 JVector...");
>
>         int page = 0;
>         int size = 100;
>         int total = 0;
>
>         while (true) {
>             // 从 ES 分批读取
>             NativeSearchQuery query = new NativeSearchQueryBuilder()
>                 .withPageable(PageRequest.of(page, size))
>                 .build();
>
>             SearchHits<KnowledgeDoc> hits = esTemplate.search(query, KnowledgeDoc.class);
>             if (hits.getSearchHits().isEmpty()) break;
>
>             List<VectorEntry> entries = new ArrayList<>();
>             for (SearchHit<KnowledgeDoc> hit : hits) {
>                 KnowledgeDoc doc = hit.getContent();
>                 float[] vector = embeddingModel.embed(doc.getContent());
>                 entries.add(VectorEntry.builder()
>                     .id(doc.getId())
>                     .vector(vector)
>                     .metadata(Map.of(
>                         "text", doc.getContent(),
>                         "title", doc.getTitle(),
>                         "category", doc.getCategory()
>                     ))
>                     .build());
>             }
>
>             jVectorStore.addBatch(entries);
>             total += entries.size();
>             log.info("已迁移: {} 条", total);
>             page++;
>         }
>
>         log.info("迁移完成！总计: {} 条", total);
>     }
> }
> ```
>
>
> ## 四、文件变更汇总
>
> | 操作     | 文件                                   | 说明                       |
> | -------- | -------------------------------------- | -------------------------- |
> | **新增** | `common/store/VectorStore.java`        | 统一接口                   |
> | **新增** | `common/store/VectorEntry.java`        | 数据载体                   |
> | **新增** | `common/store/VectorSearchResult.java` | 结果载体                   |
> | **新增** | `common/store/JVectorVectorStore.java` | JVector 实现               |
> | **改造** | `*SearchService.java`                  | 注入 `VectorStore` 替代 ES |
> | **删除** | `config/ElasticsearchConfig.java`      | ES 配置                    |
> | **删除** | `repository/*EsRepository.java`        | ES Repository              |
> | **删除** | `domain/*Document.java`                | ES 实体                    |
> | **改造** | `pom.xml`                              | 移除 ES 依赖               |
>
>
> ## 五、优势
>
> | 优势             | 说明                                                         |
> | ---------------- | ------------------------------------------------------------ |
> | **改动极小**     | 只需改 `xxxSearchService` 的注入对象                         |
> | **可回滚**       | 保留 `EsVectorStore` 实现，切换 `vector-store.type=elasticsearch` 可立即回滚 |
> | **业务层无感知** | 接口不变，业务代码不改                                       |
> | **Windows 开发** | 无需启动 ES，直接跑                                          |
> | **内存降低**     | 从 8GB 降到 3GB                                              |
>
>
> ## 六、一句话总结
>
> > **加一层 `VectorStore` 接口，把 ES 和 JVector 统一起来。业务层只认接口，不认实现。通过 `application.yml` 的 `vector-store.type` 切换，改完直接跑，随时可回滚。**