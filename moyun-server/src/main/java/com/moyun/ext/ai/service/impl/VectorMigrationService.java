package com.moyun.ext.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.ext.ai.entity.DocumentSegment;
import com.moyun.ext.ai.service.DocumentSegmentService;
import com.moyun.ext.ai.store.JVectorEmbeddingStore;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 向量数据迁移服务
 *
 * <p>从 MySQL {@code ai_document_segment} 表读取已持久化的分片向量数据（vector_data 字段，
 * JSON 格式 float 数组），重建 JVector 向量索引并落盘。</p>
 *
 * <p>使用场景：Elasticsearch 下线后，首次切换到 JVector 时执行一次性迁移；
 * 或 JVector 数据文件丢失后从数据库恢复。迁移过程对每个知识库先清空旧向量再写入，
 * 可安全重复执行。</p>
 *
 * @author laomao
 * @since 2026-09-09
 */
@Slf4j
@Service
public class VectorMigrationService {

    @Autowired
    private DocumentSegmentService documentSegmentService;

    @Autowired
    @Qualifier("embeddingStore")
    private JVectorEmbeddingStore embeddingStore;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 迁移结果统计
     */
    public record MigrationResult(int total, int migrated, int skipped) {
    }

    /**
     * 从数据库重建 JVector 向量索引
     *
     * @param knowledgeBaseId 知识库 ID；为 null 时迁移全部知识库
     * @return 迁移统计（总数 / 成功 / 跳过）
     */
    public MigrationResult migrateFromDatabase(Long knowledgeBaseId) {
        List<DocumentSegment> segments = knowledgeBaseId != null
                ? documentSegmentService.getSegmentsByKnowledgeBaseId(knowledgeBaseId)
                : documentSegmentService.list();

        if (segments == null || segments.isEmpty()) {
            log.warn("向量迁移：未找到任何分片记录, knowledgeBaseId={}", knowledgeBaseId);
            return new MigrationResult(0, 0, 0);
        }

        // 迁移前先清除目标知识库的旧向量，保证可重复执行
        Set<Long> baseIds = new LinkedHashSet<>();
        for (DocumentSegment segment : segments) {
            if (segment.getKnowledgeBaseId() != null) {
                baseIds.add(segment.getKnowledgeBaseId());
            }
        }
        for (Long baseId : baseIds) {
            embeddingStore.deleteByKnowledgeBaseId(String.valueOf(baseId));
        }

        int migrated = 0;
        int skipped = 0;
        for (DocumentSegment segment : segments) {
            try {
                float[] vector = parseVector(segment.getVectorData());
                if (vector == null || vector.length == 0) {
                    skipped++;
                    continue;
                }

                Metadata metadata = new Metadata()
                        .put("knowledgeBaseId", String.valueOf(segment.getKnowledgeBaseId()))
                        .put("embeddingId", segment.getEmbeddingId())
                        .put("segmentIndex", segment.getSegmentIndex() != null
                                ? String.valueOf(segment.getSegmentIndex()) : "");
                if (segment.getPageNumber() != null) {
                    metadata.put("pageNumber", String.valueOf(segment.getPageNumber()));
                }

                TextSegment textSegment = TextSegment.from(
                        segment.getContent() != null ? segment.getContent() : "", metadata);
                embeddingStore.add(Embedding.from(vector), textSegment);
                migrated++;
            } catch (Exception e) {
                skipped++;
                log.error("迁移向量失败: segmentId={}, knowledgeBaseId={}, 原因: {}",
                        segment.getId(), segment.getKnowledgeBaseId(), e.getMessage());
            }
        }

        // 立即落盘，避免等待定时任务
        try {
            embeddingStore.saveToDisk();
        } catch (Exception e) {
            log.error("迁移后落盘失败: {}", e.getMessage(), e);
        }

        log.info("向量迁移完成: knowledgeBaseId={}, 总数={}, 成功={}, 跳过={}, 当前索引条目={}",
                knowledgeBaseId, segments.size(), migrated, skipped, embeddingStore.size());
        return new MigrationResult(segments.size(), migrated, skipped);
    }

    /**
     * 解析 vector_data 字段（JSON float 数组）
     *
     * @return 向量数组；空 / 非法时返回 null
     */
    private float[] parseVector(String vectorData) {
        if (vectorData == null || vectorData.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(vectorData, float[].class);
        } catch (Exception e) {
            log.warn("向量数据解析失败: {}", e.getMessage());
            return null;
        }
    }
}
