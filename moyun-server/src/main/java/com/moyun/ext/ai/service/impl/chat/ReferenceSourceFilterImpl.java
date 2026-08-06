package com.moyun.ext.ai.service.impl.chat;

import com.moyun.ext.ai.entity.Agent;
import com.moyun.ext.ai.service.chat.ReferenceSourceFilterService;
import dev.langchain4j.rag.content.Content;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 参考来源过滤器
 *
 * <p>负责过滤和筛选有效的参考来源，包括：
 * <ul>
 *   <li>根据重排分数过滤低质量来源</li>
 *   <li>检测目录内容并过滤</li>
 *   <li>检测纯题目段落并过滤</li>
 *   <li>提取AI回复中提到的页码</li>
 * </ul>
 * </p>
 *
 * @author laomao
 * @since 2025-12-11
 */
@Slf4j
@Service
public class ReferenceSourceFilterImpl implements ReferenceSourceFilterService {

    /**
     * 过滤有效的参考来源
     *
     * <p>
     * 返回前几个高相关性的来源（去重），同时保留AI回复中引用的所有图片。
     * 过滤规则：
     * <ul>
     * <li>保留AI回复中引用的图片（必须保留）</li>
     * <li>按页码去重，每页只保留一个来源</li>
     * <li>过滤掉纯问题片段和目录内容</li>
     * <li>最多返回智能体配置的数量</li>
     * </ul>
     * </p>
     *
     * @param agent        智能体配置（用于获取maxResults、minScore等参数）
     * @param contents     原始内容列表
     * @param aiResponse   AI的回复文本
     * @param rerankScores 重排分数映射
     * @return 过滤后的有效参考来源列表
     */
    @Override
    public List<Content> filterValidReferenceSources(Agent agent, List<Content> contents, String aiResponse,
                                                      Map<Content, Double> rerankScores) {
        if (contents == null || contents.isEmpty()) {
            return new ArrayList<>();
        }

        List<Content> validContents = new ArrayList<>();
        Set<String> seenPages = new HashSet<>();
        
        // 🎯 使用智能体配置的参数
        // 精度优先：只保留最相关的3-5个来源，确保高质量
        int maxSourceCount = Math.min(5, agent.getRagMaxResults() != null ? agent.getRagMaxResults() : 3);
        double minScoreThreshold = agent.getRagMinScore() != null ? agent.getRagMinScore() : 0.5;

        // 1. 首先收集AI回复中引用的所有图片（必须保留）
        List<Content> referencedImages = new ArrayList<>();
        int imageIndex = 1;
        for (Content content : contents) {
            var metadata = content.textSegment().metadata();
            if (metadata == null) {
                continue;
            }

            String type = metadata.getString("type");
            if ("image".equals(type)) {
                String placeholder = "[[IMAGE_" + imageIndex + "]]";
                if (aiResponse != null && aiResponse.contains(placeholder)) {
                    referencedImages.add(content);
                    String pageNumber = metadata.getString("pageNumber");
                    String knowledgeBaseId = metadata.getString("knowledgeBaseId");
                    String pageKey = knowledgeBaseId + "_" + (pageNumber != null ? pageNumber : "0");
                    seenPages.add(pageKey);
                    log.info("✅ 保留被引用的图片 - 页码: {}, 占位符: {}", pageNumber, placeholder);
                }
                imageIndex++;
            }
        }

        // 2. 添加被引用的图片到结果
        validContents.addAll(referencedImages);

        // 3. 根据重排分数智能选择文本来源（而不是固定取前N个）
        // 使用传入的重排分数映射（避免ThreadLocal跨线程丢失）
        Map<Content, Double> contentRerankScores = rerankScores;

        // 收集所有文本内容及其分数
        List<Content> textContents = new ArrayList<>();
        for (Content content : contents) {
            var metadata = content.textSegment().metadata();
            if (metadata == null) {
                continue;
            }

            String type = metadata.getString("type");
            if ("image".equals(type)) {
                continue; // 图片已处理
            }

            textContents.add(content);
        }

        int textCount = 0; // 文本来源计数器

        // 如果没有重排分数，降级为旧逻辑（取前3个）
        if (contentRerankScores == null || contentRerankScores.isEmpty()) {
            log.warn("⚠️ 未找到重排分数，使用默认逻辑取前3个文本来源");
            for (Content content : textContents) {
                if (textCount >= 3)
                    break;

                var metadata = content.textSegment().metadata();
                String knowledgeBaseId = metadata.getString("knowledgeBaseId");
                String pageNumber = metadata.getString("pageNumber");
                if (knowledgeBaseId == null)
                    continue;

                String pageKey = knowledgeBaseId + "_" + (pageNumber != null ? pageNumber : "0");
                if (seenPages.contains(pageKey)) {
                    continue;
                }
                seenPages.add(pageKey);
                validContents.add(content);
                textCount++;
                log.info("✅ 保留来源 - 页码: {}", pageNumber);
            }
        } else {
            // 新逻辑：基于重排分数智能过滤
            // 1. 计算最高分和平均分
            double maxScore = 0;
            double totalScore = 0;
            int scoreCount = 0;
            for (Content content : textContents) {
                Double score = contentRerankScores.get(content);
                if (score != null && score > 0) {
                    maxScore = Math.max(maxScore, score);
                    totalScore += score;
                    scoreCount++;
                }
            }

            // 2. 动态阈值：精度优先 - 最高分的75% 或 平均分的85%，取较大值
            // 高阈值确保只保留最相关的来源，牺牲数量换取精度
            double avgScore = scoreCount > 0 ? totalScore / scoreCount : 0;
            double dynamicThreshold = Math.max(maxScore * 0.75, avgScore * 0.85);
            double threshold = Math.max(minScoreThreshold, dynamicThreshold);
            
            log.info("📊 重排分数统计 - 最高: {}, 平均: {}, 阈值: {} (最高分75% 或 平均分85%, 最低{})",
                    String.format("%.2f", maxScore), 
                    String.format("%.2f", avgScore),
                    String.format("%.2f", threshold),
                    String.format("%.2f", minScoreThreshold));

            // 3. 根据分数和质量过滤（最多保留智能体配置的数量）
            for (Content content : textContents) {
                if (textCount >= maxSourceCount)
                    break;

                var metadata = content.textSegment().metadata();
                String knowledgeBaseId = metadata.getString("knowledgeBaseId");
                String pageNumber = metadata.getString("pageNumber");
                if (knowledgeBaseId == null)
                    continue;

                // 按页码去重
                String pageKey = knowledgeBaseId + "_" + (pageNumber != null ? pageNumber : "0");
                if (seenPages.contains(pageKey)) {
                    continue;
                }

                // 分数检查
                Double score = contentRerankScores.get(content);
                if (score == null || score < threshold) {
                    log.debug("⏭️ 跳过低分来源 - 页码: {}, 分数: {} (阈值: {})",
                            pageNumber,
                            score != null ? String.format("%.2f", score) : "0.00",
                            String.format("%.2f", threshold));
                    continue;
                }
                
                // 质量检查：过滤目录内容
                String text = content.textSegment().text();
                if (isTableOfContents(text)) {
                    log.info("📋 跳过目录内容 - 页码: {}, 分数: {}", pageNumber, String.format("%.2f", score));
                    continue;
                }
                
                // 质量检查：过滤过短或无意义的内容（提高到80字符，确保内容充实）
                if (text.length() < 80) {
                    log.debug("⏭️ 跳过过短内容 - 页码: {}, 长度: {}", pageNumber, text.length());
                    continue;
                }
                
                // 质量检查：过滤纯图片描述（这类内容对理解帮助不大）
                if (text.startsWith("这张图片") || text.startsWith("图片位置：") || 
                    text.contains("图片展示了") || text.contains("图片呈现")) {
                    log.debug("⏭️ 跳过图片描述内容 - 页码: {}", pageNumber);
                    continue;
                }
                
                // 通过所有检查，保留
                seenPages.add(pageKey);
                validContents.add(content);
                textCount++;
                log.info("✅ 保留高质量来源 - 页码: {}, 分数: {}, 长度: {}", 
                        pageNumber, String.format("%.2f", score), text.length());
            }
        }

        log.info("✅ 过滤后共 {} 个有效参考来源（图片: {}, 文本: {}）",
                validContents.size(), referencedImages.size(), textCount);
        return validContents;
    }

    /**
     * 检测是否是目录内容
     *
     * <p>
     * 目录内容通常不适合作为参考来源，需要过滤。
     * 检测特征：
     * <ul>
     * <li>包含大量连续点号（省略号）</li>
     * <li>包含章节标记 + 点号</li>
     * <li>短文本但包含多个页码</li>
     * </ul>
     * </p>
     *
     * @param text 文本内容
     * @return 是否为目录内容
     */
    @Override
    public boolean isTableOfContents(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        // 目录特征1：包含大量连续的点号（省略号）
        int dotCount = 0;
        for (int i = 0; i < text.length() - 1; i++) {
            if (text.charAt(i) == '.' && text.charAt(i + 1) == '.') {
                dotCount++;
            }
        }
        if (dotCount > 5) { // 超过5组连续点号，很可能是目录
            return true;
        }

        // 目录特征2：包含很多中文数字章节标记 + 点号
        boolean hasChapterMarks = text.matches(".*[一二三四五六七八九十]+、.*\\.{3,}.*") ||
                text.matches(".*（[一二三四五六七八九十]+）.*\\.{3,}.*") ||
                text.matches(".*[\\d]+\\..*\\.{3,}.*");

        if (hasChapterMarks) {
            return true;
        }

        // 目录特征3：短文本但包含多个数字（页码）
        String[] lines = text.split("\n");
        int shortLinesWithNumbers = 0;
        for (String line : lines) {
            if (line.trim().length() < 100 && line.matches(".*\\d+.*") && line.contains("...")) {
                shortLinesWithNumbers++;
            }
        }
        if (shortLinesWithNumbers > 3) {
            return true;
        }

        return false;
    }

    /**
     * 检测是否是纯题目段落
     *
     * <p>
     * 用于参考来源显示过滤，只标记明确的纯题目。
     * 判断标准：文本长度<50字符且以问号结尾
     * </p>
     *
     * @param text 文本内容
     * @return 是否为纯题目段落
     */
    @Override
    public boolean isPureQuestionSegment(String text) {
        String trimmed = text.trim();

        // 只有非常短（<50字符）且以问号结尾才认为是纯题目
        // 这样可以避免误判包含题目+答案的长段落
        if (trimmed.length() < 50 && (trimmed.endsWith("?") || trimmed.endsWith("？"))) {
            return true;
        }

        return false;
    }

    /**
     * 从AI回复中提取提到的页码
     *
     * <p>
     * 匹配格式：第X页、(第X页)、（第X页）
     * </p>
     *
     * @param aiResponse AI的回复文本
     * @return 提取到的页码集合
     */
    @Override
    public Set<String> extractMentionedPages(String aiResponse) {
        Set<String> pages = new HashSet<>();

        if (aiResponse == null || aiResponse.isEmpty()) {
            return pages;
        }

        // 匹配页码格式：第X页、(第X页)、（第X页）
        Pattern pagePattern = Pattern.compile("[（(]?第\\s*(\\d+)\\s*页[）)]?");
        Matcher pageMatcher = pagePattern.matcher(aiResponse);
        while (pageMatcher.find()) {
            pages.add(pageMatcher.group(1));
        }

        if (!pages.isEmpty()) {
            log.info("🔍 AI回复中提到的页码: {}", pages);
        }

        return pages;
    }
}
