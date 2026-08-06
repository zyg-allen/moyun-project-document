package com.moyun.ext.ai.service.impl;

import com.moyun.ext.ai.entity.KnowledgeConfig;
import com.moyun.ext.ai.service.AdaptiveChunkingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 自适应分片服务实现
 *
 * <p>根据文档类型和配置，动态选择最佳的分片策略</p>
 *
 * @author laomao
 * @since 2025-01-22
 */
@Slf4j
@Service
public class AdaptiveChunkingServiceImpl implements AdaptiveChunkingService {

    // 句子边界字符
    private static final char[] SENTENCE_BOUNDARIES = {
            '。', '！', '？', '.', '!', '?', '\n', '\r'
    };

    @Override
    public int getAdaptiveChunkSize(KnowledgeConfig config, String documentType, String content) {
        // 1. 如果配置了固定策略，使用配置的分片大小
        if ("fixed".equals(config.getChunkingStrategy())) {
            return config.getSegmentMaxLength() != null ? config.getSegmentMaxLength() : 800;
        }

        // 2. 根据文档类型返回推荐的分片大小
        switch (documentType) {
            case "faq":
                // FAQ: 使用配置的FAQ分片大小，默认400
                int faqSize = config.getFaqChunkSize() != null ? config.getFaqChunkSize() : 400;
                log.debug("FAQ文档，使用分片大小: {}", faqSize);
                return faqSize;

            case "table":
                // 表格: 使用较大的分片，避免切断表格
                log.debug("表格文档，使用分片大小: 1500");
                return 1500;

            case "code":
                // 代码: 使用较大的分片，保持函数/类完整
                log.debug("代码文档，使用分片大小: 2000");
                return 2000;

            case "technical":
                // 技术文档: 使用配置的技术文档分片大小，默认1200
                int techSize = config.getTechnicalChunkSize() != null ? config.getTechnicalChunkSize() : 1200;
                log.debug("技术文档，使用分片大小: {}", techSize);
                return techSize;

            case "general":
            default:
                // 通用文档: 使用配置的默认分片大小
                int generalSize = config.getSegmentMaxLength() != null ? config.getSegmentMaxLength() : 800;
                log.debug("通用文档，使用分片大小: {}", generalSize);
                return generalSize;
        }
    }

    @Override
    public int findSmartBoundary(String content, int targetPos, int searchRadius) {
        if (content == null || content.isEmpty()) {
            return targetPos;
        }

        int contentLength = content.length();
        if (targetPos >= contentLength) {
            return contentLength;
        }

        // 1. 如果目标位置刚好是边界字符，直接返回
        if (isBoundaryChar(content.charAt(targetPos))) {
            return targetPos + 1; // 包含边界字符
        }

        // 2. 向后搜索最近的边界（优先）
        int forwardBoundary = findBoundaryForward(content, targetPos, searchRadius);
        if (forwardBoundary != -1) {
            log.debug("找到向后边界: {} (距离: {})", forwardBoundary, forwardBoundary - targetPos);
            return forwardBoundary;
        }

        // 3. 向前搜索最近的边界（备选）
        int backwardBoundary = findBoundaryBackward(content, targetPos, searchRadius);
        if (backwardBoundary != -1) {
            log.debug("找到向前边界: {} (距离: {})", backwardBoundary, targetPos - backwardBoundary);
            return backwardBoundary;
        }

        // 4. 如果都找不到，返回目标位置
        log.debug("未找到边界，使用目标位置: {}", targetPos);
        return targetPos;
    }

    @Override
    public boolean shouldUseSmartBoundary(KnowledgeConfig config, String documentType) {
        // 1. 检查配置是否启用智能边界
        if (config.getEnableSmartBoundary() != null && !config.getEnableSmartBoundary()) {
            return false;
        }

        // 2. 表格和代码文档不使用智能边界（需要保持结构完整）
        if ("table".equals(documentType) || "code".equals(documentType)) {
            log.debug("表格/代码文档，禁用智能边界");
            return false;
        }

        // 3. 其他文档类型使用智能边界
        return true;
    }

    /**
     * 向后搜索边界字符
     */
    private int findBoundaryForward(String content, int startPos, int searchRadius) {
        int endPos = Math.min(startPos + searchRadius, content.length());

        for (int i = startPos; i < endPos; i++) {
            if (isBoundaryChar(content.charAt(i))) {
                return i + 1; // 包含边界字符
            }
        }

        return -1;
    }

    /**
     * 向前搜索边界字符
     */
    private int findBoundaryBackward(String content, int startPos, int searchRadius) {
        int beginPos = Math.max(startPos - searchRadius, 0);

        for (int i = startPos - 1; i >= beginPos; i--) {
            if (isBoundaryChar(content.charAt(i))) {
                return i + 1; // 包含边界字符
            }
        }

        return -1;
    }

    /**
     * 判断是否为边界字符
     */
    private boolean isBoundaryChar(char c) {
        for (char boundary : SENTENCE_BOUNDARIES) {
            if (c == boundary) {
                return true;
            }
        }
        return false;
    }
}
