package com.moyun.ext.ai.engine.workflow.node;

import com.moyun.ext.ai.service.KnowledgeBaseService;
import com.moyun.ext.ai.engine.workflow.WorkflowContext;
import com.moyun.ext.ai.engine.workflow.WorkflowNode;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 知识库检索节点执行器
 * 
 * <p>从知识库中检索相关内容</p>
 * <p>线程安全：无状态，继承BaseNodeExecutor</p>
 *
 * @author laomao
 * @since 2025-11-30
 */
@Slf4j
@Component
public class KnowledgeNodeExecutor extends BaseNodeExecutor {

    @Autowired(required = false)
    private EmbeddingStore<TextSegment> embeddingStore;

    @Autowired(required = false)
    private EmbeddingModel embeddingModel;

    @Autowired(required = false)
    private KnowledgeBaseService knowledgeBaseService;

    @Override
    public String getType() {
        return "knowledge";
    }

    @Override
    public NodeResult execute(WorkflowNode node, WorkflowContext context) {
        Map<String, Object> config = node.getConfig();
        if (config == null) {
            return NodeResult.fail("知识库节点配置为空");
        }

        try {
            // 获取配置
            String query = (String) config.get("query");
            Object kbIdObj = config.get("knowledgeBaseId");
            Integer topK = config.containsKey("topK") ? ((Number) config.get("topK")).intValue() : 5;
            Double minScore = config.containsKey("minScore") ? ((Number) config.get("minScore")).doubleValue() : 0.5;
            String outputVariable = (String) config.getOrDefault("outputVariable", "knowledge_result");
            String outputFormat = (String) config.getOrDefault("outputFormat", "text"); // text, json, markdown

            if (query == null || query.isEmpty()) {
                query = "{{input}}";
            }

            // 替换变量
            query = replaceVariables(query, context);

            log.info("📚 知识库检索: query={}, topK={}", query.length() > 50 ? query.substring(0, 50) + "..." : query, topK);

            // 检查服务是否可用
            if (embeddingStore == null || embeddingModel == null) {
                log.warn("📚 知识库服务未配置，返回空结果");
                context.setVariable(outputVariable, "知识库服务未配置");
                context.setVariable(outputVariable + "_count", 0);
                return NodeResult.success("知识库服务未配置");
            }

            // 执行向量检索
            Embedding queryEmbedding = embeddingModel.embed(query).content();
            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(topK)
                    .minScore(minScore)
                    .build();

            EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);

            // 转换结果
            List<Map<String, Object>> results = new ArrayList<>();
            for (EmbeddingMatch<TextSegment> match : searchResult.matches()) {
                Map<String, Object> item = new HashMap<>();
                item.put("content", match.embedded().text());
                item.put("score", match.score());
                if (match.embedded().metadata() != null) {
                    item.put("metadata", match.embedded().metadata().toMap());
                    item.put("source", match.embedded().metadata().getString("source"));
                }
                results.add(item);
            }

            // 格式化输出
            Object output;
            if ("json".equals(outputFormat)) {
                output = results;
            } else if ("markdown".equals(outputFormat)) {
                output = formatAsMarkdown(results);
            } else {
                output = formatAsText(results);
            }

            log.info("📚 知识库检索完成: 找到 {} 条结果", results.size());

            // 更新知识库使用统计
            if (knowledgeBaseService != null && kbIdObj != null) {
                try {
                    Long knowledgeId = kbIdObj instanceof Number ? ((Number) kbIdObj).longValue() : Long.parseLong(kbIdObj.toString());
                    knowledgeBaseService.updateUsageStats(knowledgeId, results.size());
                } catch (Exception e) {
                    log.warn("更新知识库统计失败: {}", e.getMessage());
                }
            }

            context.setVariable(outputVariable, output);
            context.setVariable(outputVariable + "_count", results.size());

            return NodeResult.success(output);

        } catch (Exception e) {
            log.error("知识库检索失败", e);
            return NodeResult.fail("知识库检索失败: " + e.getMessage());
        }
    }

    private String formatAsText(List<Map<String, Object>> results) {
        if (results.isEmpty()) {
            return "未找到相关内容";
        }
        return results.stream()
                .map(r -> r.getOrDefault("content", "").toString())
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    private String formatAsMarkdown(List<Map<String, Object>> results) {
        if (results.isEmpty()) {
            return "未找到相关内容";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            Map<String, Object> r = results.get(i);
            sb.append("### 参考 ").append(i + 1).append("\n\n");
            sb.append(r.getOrDefault("content", "")).append("\n\n");
            if (r.containsKey("source")) {
                sb.append("*来源: ").append(r.get("source")).append("*\n\n");
            }
        }
        return sb.toString();
    }
}
