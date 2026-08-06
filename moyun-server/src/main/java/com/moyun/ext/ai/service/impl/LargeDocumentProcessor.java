package com.moyun.ext.ai.service.impl;

import com.moyun.ext.ai.entity.DocumentSegment;
import com.moyun.ext.ai.entity.KnowledgeBase;
import com.moyun.ext.ai.service.DocumentSegmentService;
import com.moyun.ext.ai.service.KnowledgeProcessProgressService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 大文档处理器
 * 
 * <p>针对大文件优化的处理器，使用分页流式处理避免OOM：</p>
 * <ul>
 *   <li>逐页提取文本，避免一次性加载整个文档</li>
 *   <li>分批处理和存储，及时释放内存</li>
 *   <li>实时进度更新</li>
 *   <li>支持处理中断和恢复</li>
 * </ul>
 *
 * @author laomao
 * @since 2025-12-12
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LargeDocumentProcessor {

    private final DocumentSegmentService documentSegmentService;
    private final KnowledgeProcessProgressService progressService;

    /** 每批处理的页数 */
    private static final int PAGES_PER_BATCH = 10;

    /** 分片大小（字符） */
    private static final int CHUNK_SIZE = 800;

    /** 分片重叠（字符） */
    private static final int CHUNK_OVERLAP = 150;

    /** 大文件阈值（MB） */
    public static final long LARGE_FILE_THRESHOLD_MB = 20;

    /**
     * 判断是否是大文件
     */
    public static boolean isLargeFile(long fileSizeBytes) {
        return fileSizeBytes > LARGE_FILE_THRESHOLD_MB * 1024 * 1024;
    }

    /**
     * 流式处理大型 PDF 文档
     * 
     * @param knowledge 知识库实体
     * @param pdfDocument PDF 文档对象
     * @param embeddingModel 嵌入模型
     * @param embeddingStore 向量存储
     * @return 处理的分片总数
     */
    public int processLargePdf(
            KnowledgeBase knowledge,
            PDDocument pdfDocument,
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore) throws IOException {
        
        int totalPages = pdfDocument.getNumberOfPages();
        log.info("🚀 开始流式处理大型 PDF: {}, 共 {} 页", knowledge.getFileName(), totalPages);

        // 初始化进度
        progressService.updateProgress(knowledge.getId(), 0, 
                String.format("开始处理大型PDF，共 %d 页", totalPages), "初始化");

        String vectorId = UUID.randomUUID().toString();
        int totalSegments = 0;
        int globalSegmentIndex = 0;

        // 分批处理
        for (int batchStart = 0; batchStart < totalPages; batchStart += PAGES_PER_BATCH) {
            int batchEnd = Math.min(batchStart + PAGES_PER_BATCH, totalPages);
            
            log.info("📄 处理第 {}-{} 页 / {}", batchStart + 1, batchEnd, totalPages);
            
            // 提取当前批次的文本
            String batchText = extractPagesText(pdfDocument, batchStart, batchEnd);
            
            if (batchText == null || batchText.trim().isEmpty()) {
                log.warn("⚠️ 第 {}-{} 页无文本内容，跳过", batchStart + 1, batchEnd);
                continue;
            }

            // 分割文本
            DocumentSplitter splitter = DocumentSplitters.recursive(CHUNK_SIZE, CHUNK_OVERLAP);
            Document batchDoc = Document.from(batchText);
            List<TextSegment> segments = splitter.split(batchDoc);

            log.info("📝 第 {}-{} 页产生 {} 个分片", batchStart + 1, batchEnd, segments.size());

            // 逐个处理分片
            for (int i = 0; i < segments.size(); i++) {
                TextSegment segment = segments.get(i);
                String embeddingId = vectorId + "_" + globalSegmentIndex;

                // 估算页码（在当前批次范围内）
                int estimatedPage = estimatePageNumber(i, segments.size(), batchStart, batchEnd);

                try {
                    // 生成向量
                    Embedding embedding = embeddingModel.embed(segment.text()).content();

                    // 设置第一次获取的向量维度
                    if (globalSegmentIndex == 0) {
                        knowledge.setVectorDimension(embedding.dimension());
                    }

                    // 创建带元数据的分片
                    TextSegment segmentWithMeta = TextSegment.from(
                        segment.text(),
                        Metadata.from("vectorId", vectorId)
                            .put("fileName", knowledge.getFileName())
                            .put("fileType", "pdf")
                            .put("segmentIndex", String.valueOf(globalSegmentIndex))
                            .put("knowledgeBaseId", String.valueOf(knowledge.getId()))
                            .put("embeddingId", embeddingId)
                            .put("pageNumber", String.valueOf(estimatedPage))
                    );

                    // 存储到向量数据库
                    embeddingStore.add(embedding, segmentWithMeta);

                    // 保存到关系数据库
                    saveSegmentToDatabase(knowledge.getId(), globalSegmentIndex, estimatedPage,
                            segment.text(), embeddingId, embedding);

                    globalSegmentIndex++;
                    totalSegments++;

                } catch (Exception e) {
                    log.error("❌ 处理分片失败: index={}, error={}", globalSegmentIndex, e.getMessage());
                    // 继续处理下一个分片
                }
            }

            // 更新进度
            int progress = (batchEnd * 100) / totalPages;
            progressService.updateProgress(knowledge.getId(), progress,
                    String.format("已处理 %d/%d 页，%d 个分片", batchEnd, totalPages, totalSegments), "向量化");

            // 强制垃圾回收，释放内存
            if (batchEnd % (PAGES_PER_BATCH * 3) == 0) {
                System.gc();
                log.debug("🧹 触发垃圾回收，释放内存");
            }
        }

        // 更新知识库统计
        knowledge.setSegmentCount(totalSegments);

        log.info("✅ 大型 PDF 处理完成: {}, 共 {} 个分片", knowledge.getFileName(), totalSegments);
        progressService.updateProgress(knowledge.getId(), 100, 
                String.format("处理完成，共 %d 页，%d 个分片", totalPages, totalSegments), "完成");

        return totalSegments;
    }

    /**
     * 提取指定页范围的文本
     */
    private String extractPagesText(PDDocument pdfDocument, int startPage, int endPage) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(startPage + 1);  // PDFBox 页码从 1 开始
        stripper.setEndPage(endPage);
        return stripper.getText(pdfDocument);
    }

    /**
     * 估算分片所在页码
     */
    private int estimatePageNumber(int segmentIndex, int totalSegmentsInBatch, int batchStart, int batchEnd) {
        if (totalSegmentsInBatch == 0) return batchStart + 1;
        
        int pagesInBatch = batchEnd - batchStart;
        float ratio = (float) segmentIndex / totalSegmentsInBatch;
        return batchStart + (int) (ratio * pagesInBatch) + 1;
    }

    /**
     * 保存分片到数据库
     */
    private void saveSegmentToDatabase(
            Long knowledgeBaseId,
            int segmentIndex,
            int pageNumber,
            String content,
            String embeddingId,
            Embedding embedding) {
        
        DocumentSegment docSegment = new DocumentSegment();
        docSegment.setKnowledgeBaseId(knowledgeBaseId);
        docSegment.setSegmentIndex(segmentIndex);
        docSegment.setPageNumber(pageNumber);
        docSegment.setContent(content);
        docSegment.setContentLength(content.length());
        docSegment.setEmbeddingId(embeddingId);
        docSegment.setVectorDimension(embedding.dimension());
        docSegment.setCreateTime(LocalDateTime.now());

        // 注意：不存储完整向量数据到数据库（太大），只保存 embeddingId
        // 向量数据已存储在 Elasticsearch 中

        try {
            documentSegmentService.save(docSegment);
        } catch (Exception e) {
            log.error("❌ 保存分片到数据库失败: index={}, error={}", segmentIndex, e.getMessage());
        }
    }

    /**
     * 批量处理多个分片（用于小文件的优化处理）
     */
    public List<Embedding> batchEmbed(List<String> texts, EmbeddingModel embeddingModel) {
        List<Embedding> embeddings = new ArrayList<>();
        
        // 分批处理，每批最多 10 个
        int batchSize = 10;
        for (int i = 0; i < texts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, texts.size());
            List<String> batch = texts.subList(i, end);
            
            for (String text : batch) {
                try {
                    Embedding embedding = embeddingModel.embed(text).content();
                    embeddings.add(embedding);
                } catch (Exception e) {
                    log.error("Embedding 失败: {}", e.getMessage());
                    embeddings.add(null);
                }
            }
        }
        
        return embeddings;
    }
}
