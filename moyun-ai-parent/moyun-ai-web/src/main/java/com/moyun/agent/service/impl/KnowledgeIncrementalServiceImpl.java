package com.moyun.agent.service.impl;

import com.moyun.agent.entity.DocumentSegment;
import com.moyun.agent.entity.KnowledgeBase;
import com.moyun.agent.service.DocumentSegmentService;
import com.moyun.agent.service.KnowledgeIncrementalService;
import com.moyun.agent.store.ElasticsearchEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识库增量更新服务实现
 *
 * @author laomao
 * @since 2025-12-12
 */
@Slf4j
@Service
public class KnowledgeIncrementalServiceImpl implements KnowledgeIncrementalService {

    @Autowired
    private DocumentSegmentService documentSegmentService;

    @Autowired
    private ElasticsearchEmbeddingStore elasticsearchEmbeddingStore;

    /**
     * 计算文件内容的SHA-256哈希值
     */
    @Override
    public String calculateContentHash(InputStream inputStream) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            
            byte[] hashBytes = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (Exception e) {
            log.error("❌ 计算文件哈希失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 检查文件是否需要重新处理
     */
    @Override
    public boolean needsReprocessing(KnowledgeBase knowledge, String newHash) {
        if (knowledge == null || newHash == null) {
            return true;
        }
        
        String oldHash = knowledge.getContentHash();
        
        // 无旧哈希值，需要处理
        if (oldHash == null || oldHash.isEmpty()) {
            log.info("📝 文档无历史哈希值，需要处理: {}", knowledge.getFileName());
            return true;
        }
        
        // 哈希值不同，文件已变更
        if (!oldHash.equals(newHash)) {
            log.info("🔄 文档内容已变更，需要重新处理: {}", knowledge.getFileName());
            log.debug("   旧哈希: {}", oldHash);
            log.debug("   新哈希: {}", newHash);
            return true;
        }
        
        // 检查是否标记为需要重新处理
        if (Boolean.TRUE.equals(knowledge.getNeedReprocess())) {
            log.info("🔄 文档已标记需要重新处理: {}", knowledge.getFileName());
            return true;
        }
        
        log.info("✅ 文档未变更，跳过处理: {}", knowledge.getFileName());
        return false;
    }

    /**
     * 执行增量更新
     */
    @Override
    public IncrementalUpdateResult performIncrementalUpdate(KnowledgeBase knowledge) {
        if (knowledge == null) {
            return IncrementalUpdateResult.fail("知识库文档为空");
        }

        try {
            log.info("🔄 开始增量更新: {}", knowledge.getFileName());
            
            // 1. 获取现有分片
            List<DocumentSegment> existingSegments = documentSegmentService.getSegmentsByKnowledgeBaseId(knowledge.getId());
            log.info("📊 现有分片数: {}", existingSegments.size());
            
            // 2. 删除旧的向量数据（ES中）
            int deletedCount = cleanupStaleVectors(knowledge.getId());
            log.info("🗑️ 清理旧向量: {} 个", deletedCount);
            
            // 3. 删除数据库中的旧分片
            if (!existingSegments.isEmpty()) {
                documentSegmentService.deleteByKnowledgeBaseId(knowledge.getId());
                log.info("🗑️ 清理旧分片数据库记录");
            }
            
            // 4. 更新知识库状态，标记需要重新处理
            knowledge.setNeedReprocess(true);
            knowledge.setLastProcessedTime(LocalDateTime.now());
            
            // 返回结果，实际的向量化会在主流程中进行
            return IncrementalUpdateResult.success(0, 0, deletedCount);
            
        } catch (Exception e) {
            log.error("❌ 增量更新失败: {}", e.getMessage(), e);
            return IncrementalUpdateResult.fail("增量更新失败: " + e.getMessage());
        }
    }

    /**
     * 清理ES中过期的向量数据
     */
    @Override
    public int cleanupStaleVectors(Long knowledgeId) {
        try {
            // 通过knowledgeBaseId删除ES中的向量
            int deleted = elasticsearchEmbeddingStore.deleteByKnowledgeBaseId(String.valueOf(knowledgeId));
            log.info("✅ 清理ES向量完成: knowledgeId={}, 删除数量={}", knowledgeId, deleted);
            return deleted;
        } catch (Exception e) {
            log.error("❌ 清理ES向量失败: knowledgeId={}, error={}", knowledgeId, e.getMessage());
            return 0;
        }
    }
}
