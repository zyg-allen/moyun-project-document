package com.moyun.ext.ai.service;

import com.moyun.ext.ai.entity.KnowledgeBase;

import java.io.InputStream;

/**
 * 知识库增量更新服务接口
 *
 * <p>支持知识库文档的增量更新，避免全量重建：
 * <ul>
 *   <li>文件变更检测 - 通过哈希值判断文件是否变化</li>
 *   <li>增量向量化 - 只处理变更的分片</li>
 *   <li>向量清理 - 删除过期的向量数据</li>
 * </ul>
 * </p>
 *
 * @author laomao
 * @since 2025-12-12
 */
public interface KnowledgeIncrementalService {

    /**
     * 计算文件内容的哈希值
     *
     * @param inputStream 文件输入流
     * @return SHA-256哈希值（十六进制字符串）
     */
    String calculateContentHash(InputStream inputStream);

    /**
     * 检查文件是否需要重新处理
     *
     * @param knowledge 知识库文档
     * @param newHash   新文件的哈希值
     * @return true如果需要重新处理
     */
    boolean needsReprocessing(KnowledgeBase knowledge, String newHash);

    /**
     * 执行增量更新
     *
     * <p>只更新变更的部分，而不是全量重建</p>
     *
     * @param knowledge 知识库文档
     * @return 更新结果
     */
    IncrementalUpdateResult performIncrementalUpdate(KnowledgeBase knowledge);

    /**
     * 清理过期的向量数据
     *
     * @param knowledgeId 知识库ID
     * @return 清理的向量数量
     */
    int cleanupStaleVectors(Long knowledgeId);

    /**
     * 增量更新结果
     */
    class IncrementalUpdateResult {
        private final boolean success;
        private final int addedSegments;
        private final int updatedSegments;
        private final int deletedSegments;
        private final String message;

        public IncrementalUpdateResult(boolean success, int added, int updated, int deleted, String message) {
            this.success = success;
            this.addedSegments = added;
            this.updatedSegments = updated;
            this.deletedSegments = deleted;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public int getAddedSegments() { return addedSegments; }
        public int getUpdatedSegments() { return updatedSegments; }
        public int getDeletedSegments() { return deletedSegments; }
        public String getMessage() { return message; }

        public static IncrementalUpdateResult success(int added, int updated, int deleted) {
            return new IncrementalUpdateResult(true, added, updated, deleted, 
                String.format("增量更新完成: 新增%d, 更新%d, 删除%d", added, updated, deleted));
        }

        public static IncrementalUpdateResult fail(String message) {
            return new IncrementalUpdateResult(false, 0, 0, 0, message);
        }
    }
}
