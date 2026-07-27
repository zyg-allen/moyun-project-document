package com.moyun.agent.service.impl.chat;

import com.moyun.agent.service.chat.ContentScoringService;
import com.moyun.agent.util.TextProcessingUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;

/**
 * 内容评分服务
 *
 * <p>负责计算检索内容与用户查询的相关性分数，包括：
 * <ul>
 *   <li>关键词匹配评分</li>
 *   <li>语义相关性评分</li>
 *   <li>主题一致性检查</li>
 * </ul>
 * </p>
 *
 * @author laomao
 * @since 2025-12-11
 */
@Slf4j
@Service
public class ContentScoringServiceImpl implements ContentScoringService {

    /**
     * 计算内容与查询的相关性分数
     *
     * <p>
     * 综合多个维度计算相关性分数：
     * <ul>
     * <li>文本长度惩罚（太短或太长都会降权）</li>
     * <li>完整查询匹配（+15分）</li>
     * <li>关键词匹配（首次+3分，后续每次+1分）</li>
     * <li>关键词覆盖率加分</li>
     * <li>关键词密度检查</li>
     * </ul>
     * </p>
     *
     * @param text      待评分的文本内容（已转小写）
     * @param keywords  提取的关键词数组
     * @param fullQuery 完整的用户查询（已转小写）
     * @return 相关性分数，分数越高越相关
     */
    @Override
    public double calculateRelevanceScore(String text, String[] keywords, String fullQuery) {
        double score = 0.0;
        int textLength = text.length();

        // 0. 检测图片内容并降权（图片描述通常不如文本内容精准）
        boolean isImageContent = text.startsWith("这张图片") || text.startsWith("图片位置：")
                || text.contains("图片展示了") || text.contains("图片呈现");
        double imageContentPenalty = 1.0;
        if (isImageContent) {
            // 图片内容降权到30%，因为图片描述通常冗长且不够精准
            imageContentPenalty = 0.3;
            log.debug("  🖼️ 检测到图片内容，应用降权系数 0.3");
        }

        // 1. 文本长度预检查（太短的文本直接降低基础权重）
        double lengthPenalty = 1.0;
        if (textLength < 30) {
            // 太短（<30字符），可能只是标题或片段，大幅降权
            lengthPenalty = 0.5;
            log.debug("  ⚠ 文本极短 ({}字符)，基础权重×0.5", textLength);
        } else if (textLength < 80) {
            // 较短（30-80字符），可能信息不完整，中度降权
            lengthPenalty = 0.7;
            log.debug("  ⚠ 文本较短 ({}字符)，基础权重×0.7", textLength);
        } else if (textLength > 2000) {
            // 太长（>2000字符），可能不够精准，轻度降权
            lengthPenalty = 0.9;
            log.debug("  ⚠ 文本过长 ({}字符)，基础权重×0.9", textLength);
        }

        // 1. 完整查询匹配（最高优先级）
        boolean hasFullMatch = text.contains(fullQuery);
        if (hasFullMatch) {
            score += 30.0;  // 完整短语匹配基础分
            log.debug("  ✓ 完整短语匹配：+30.0 (最高优先级)");
        }

        // 2. 关键词匹配（智能策略）
        int matchedKeywordCount = 0;
        // 泛化词黑名单（这些词太通用，需要特殊处理）
        Set<String> genericWords = Set.of("处理", "系统", "管理", "方案", "技术", "平台", "服务", "数据", "信息", "功能", "实现", "支持");
        
        // 🎯 优化策略：检测是否包含核心领域词（非泛化词）
        // 对于多字查询，如果包含领域词，允许拆分词评分；否则要求完整匹配
        boolean isMultiWordConcept = fullQuery.length() >= 4 && !fullQuery.matches(".*[a-zA-Z]+.*");
        boolean hasCoreKeyword = false;
        if (isMultiWordConcept) {
            // 检查是否包含核心领域词（非泛化词的关键词）
            for (String keyword : keywords) {
                if (!genericWords.contains(keyword) && keyword.length() >= 2) {
                    if (text.contains(keyword)) {
                        hasCoreKeyword = true;
                        log.debug("  ✓ 检测到核心领域词 \"{}\"，允许拆分词评分", keyword);
                        break;
                    }
                }
            }
        }
        
        // 决定是否评分拆分词：
        // - 单一概念（<4字）：始终评分
        // - 多字概念：完整匹配 或 包含核心领域词
        boolean shouldScoreKeywords = !isMultiWordConcept || hasFullMatch || hasCoreKeyword;
        
        for (String keyword : keywords) {
            if (keyword.length() < 2) {
                continue; // 跳过单字词
            }
            
            // 🎯 关键策略判断：多字完整概念且无完整匹配时，跳过拆分词评分
            if (!shouldScoreKeywords) {
                log.debug("  ⏭️ 跳过拆分词 \"{}\"（多字完整概念且无完整短语匹配）", keyword);
                continue;
            }
            
            // 计算关键词出现次数
            int count = countOccurrences(text, keyword);
            if (count > 0) {
                matchedKeywordCount++;
                
                // 基础分数：第一次出现3分，后续每次1分
                double keywordScore = 3.0 + (count - 1) * 1.0;
                
                // ⚠️ 智能降权策略：
                // - 领域词（如"污水"）：正常分数 (×1.0)
                // - 泛化词在查询中（如"污水处理方案"的"处理"）：降权到30% (×0.3)
                // - 泛化词独立出现（如拆分出的"系统"）：跳过 (×0)
                if (genericWords.contains(keyword)) {
                    if (fullQuery.equals(keyword)) {
                        // 完整查询就是泛化词（如用户问"系统"），正常分数
                        log.debug("  ✓ 泛化词 \"{}\" (完整查询) 出现 {} 次：+{}", keyword, count, keywordScore);
                    } else if (fullQuery.contains(keyword)) {
                        // 泛化词在查询中（如"污水处理方案"的"处理"），降权到30%
                        keywordScore *= 0.3;
                        log.debug("  ⚠️ 泛化词 \"{}\" (降权30%) 出现 {} 次：+{}", keyword, count, String.format("%.2f", keywordScore));
                    } else {
                        // 泛化词独立出现（拆分出来的），跳过
                        log.debug("  ⏭️ 跳过独立泛化词 \"{}\"（不在查询中）", keyword);
                        continue;
                    }
                } else {
                    // 领域词，正常分数
                    log.debug("  ✓ 关键词 \"{}\" 出现 {} 次：+{}", keyword, count, keywordScore);
                }
                
                score += keywordScore;
            }
        }

        // 3. 关键词覆盖率加分（匹配的关键词越多越好）
        // 注意：如果是多字完整概念且无完整匹配，拆分词已被跳过，这里不计分
        if (shouldScoreKeywords && keywords.length > 0 && matchedKeywordCount > 0) {
            double coverageBonus = (matchedKeywordCount * 1.0 / keywords.length) * 5.0;
            score += coverageBonus;
            log.debug("  ✓ 关键词覆盖率 {}/{}: +{}", matchedKeywordCount, keywords.length,
                    String.format("%.2f", coverageBonus));
        }

        // 4. 关键词密度检查（密度太低说明不够相关）
        if (shouldScoreKeywords && matchedKeywordCount > 0) {
            int totalKeywordChars = 0;
            for (String keyword : keywords) {
                totalKeywordChars += keyword.length() * countOccurrences(text, keyword);
            }
            double density = (double) totalKeywordChars / textLength;

            if (density > 0.05 && density < 0.3) {
                // 5%-30%密度最佳
                score += 2.0;
                log.debug("  ✓ 关键词密度合理 ({}): +2.0", String.format("%.2f%%", density * 100));
            } else if (density < 0.02) {
                // 密度太低（<2%），可能不够相关
                score *= 0.8;
                log.debug("  ⚠ 关键词密度过低 ({}): ×0.8", String.format("%.2f%%", density * 100));
            }
        }

        // 5. 主题一致性检查（检测文本主题是否与查询匹配）
        double topicPenalty = checkTopicConsistency(text, fullQuery, keywords);
        if (topicPenalty < 1.0) {
            score *= topicPenalty;
            log.debug("  ⚠️ 主题不一致惩罚：×{}", String.format("%.2f", topicPenalty));
        }
        
        // 6. 语义相关词匹配（扩展语义理解）
        // 注意：对于多字完整概念，如果没有完整匹配，语义扩展也会导致误匹配，应谨慎使用
        if (shouldScoreKeywords) {
            double semanticScore = calculateSemanticScore(text, keywords, fullQuery);
            score += semanticScore;
        }

        // 7. 标题/开头匹配（前100字符）- 完整短语优先
        String beginning = text.substring(0, Math.min(100, textLength));
        if (beginning.contains(fullQuery)) {
            score += 5.0;  // 完整短语在开头，大幅加分
            log.debug("  ✓ 完整短语在开头：+5.0");
        }

        // 7. 应用长度惩罚（最后统一应用）
        score *= lengthPenalty;
        if (lengthPenalty < 1.0) {
            log.debug("  ⚠ 应用长度惩罚，最终分数: {}", String.format("%.2f", score));
        }

        // 8. 应用图片内容惩罚（图片描述权重低于文本内容）
        score *= imageContentPenalty;
        if (imageContentPenalty < 1.0) {
            log.debug("  🖼️ 应用图片内容惩罚，最终分数: {}", String.format("%.2f", score));
        }

        return score;
    }

    /**
     * 检查主题一致性（避免跨领域误匹配）
     * 
     * <p>
     * 检测文本的主题领域是否与查询匹配，避免因泛化词导致的错误匹配。
     * 例如："污水处理"属于环保领域，不应该匹配到AI/文档处理领域的内容。
     * </p>
     * 
     * @param text      文本内容
     * @param fullQuery 完整查询
     * @param keywords  关键词数组
     * @return 惩罚系数（0.0-1.0），1.0表示完全匹配，<1.0表示需要降权
     */
    @Override
    public double checkTopicConsistency(String text, String fullQuery, String[] keywords) {
        // 定义主题领域关键词
        Map<String, Set<String>> topicDomains = new HashMap<>();
        
        // 环保/水务领域
        topicDomains.put("环保水务", Set.of("污水", "排水", "积水", "水位", "水质", "涝情", "防汛", "河道", "泵站", "管网", "井盖", "监测", "预警"));
        
        // AI/文档处理领域
        topicDomains.put("AI文档", Set.of("人工智能", "大模型", "文档解析", "向量", "embedding", "知识库", "rag", "检索", "算法", "gpu", "训练", "推理"));
        
        // 检测查询属于哪个领域
        String queryDomain = null;
        int maxQueryMatches = 0;
        for (Map.Entry<String, Set<String>> entry : topicDomains.entrySet()) {
            int matches = 0;
            for (String domainWord : entry.getValue()) {
                if (fullQuery.contains(domainWord)) {
                    matches++;
                }
            }
            if (matches > maxQueryMatches) {
                maxQueryMatches = matches;
                queryDomain = entry.getKey();
            }
        }
        
        // 如果查询没有明确领域，不惩罚
        if (queryDomain == null || maxQueryMatches == 0) {
            return 1.0;
        }
        
        // 检测文本属于哪个领域
        String textDomain = null;
        int maxTextMatches = 0;
        for (Map.Entry<String, Set<String>> entry : topicDomains.entrySet()) {
            int matches = 0;
            for (String domainWord : entry.getValue()) {
                if (text.contains(domainWord)) {
                    matches++;
                }
            }
            if (matches > maxTextMatches) {
                maxTextMatches = matches;
                textDomain = entry.getKey();
            }
        }
        
        // 如果文本没有明确领域，轻度惩罚
        if (textDomain == null || maxTextMatches == 0) {
            return 0.8;
        }
        
        // 如果查询和文本领域不一致，严重惩罚
        if (!queryDomain.equals(textDomain)) {
            log.debug("    ⚠️ 主题不匹配：查询领域[{}] vs 文本领域[{}]", queryDomain, textDomain);
            return 0.2;  // 降至20%，基本排除
        }
        
        // 领域一致，不惩罚
        return 1.0;
    }

    /**
     * 计算语义相关分数
     *
     * <p>
     * 基于领域知识的语义扩展，识别与查询关键词语义相关的内容。
     * 包含多个领域的语义映射：
     * <ul>
     * <li>服务器相关：CPU、GPU、内存、存储等</li>
     * <li>架构相关：系统架构、模块、组件等</li>
     * <li>RAG相关：知识库、检索、向量、Embedding等</li>
     * <li>技术规格模式：参数格式、列表枚举等</li>
     * </ul>
     * </p>
     *
     * @param text      待评分的文本内容（已转小写）
     * @param keywords  提取的关键词数组
     * @param fullQuery 完整的用户查询
     * @return 语义相关分数
     */
    @Override
    public double calculateSemanticScore(String text, String[] keywords, String fullQuery) {
        double score = 0.0;
        String lowerText = text.toLowerCase();

        // 定义语义相关词映射（扩展版）
        java.util.Map<String, String[]> semanticMap = new java.util.HashMap<>();
        semanticMap.put("服务器", new String[] { "cpu", "gpu", "npu", "内存", "存储", "硬盘", "系统盘", "数据盘", "鲲鹏", "昇腾", "算力",
                "主机", "机器", "节点", "core", "处理器" });
        semanticMap.put("架构", new String[] { "系统架构", "技术架构", "平台架构", "设计", "模块", "组件", "层次", "结构", "框架", "拓扑" });
        semanticMap.put("模型", new String[] { "大模型", "embedding", "向量", "llm", "ai模型", "算法", "神经网络", "深度学习", "机器学习" });
        semanticMap.put("知识库", new String[] { "文档", "向量库", "rag", "检索", "知识管理", "知识图谱", "语料", "数据库" });
        semanticMap.put("部署", new String[] { "安装", "配置", "环境", "运维", "上线", "发布", "迁移", "升级" });
        semanticMap.put("性能", new String[] { "速度", "效率", "吞吐量", "延迟", "响应时间", "优化", "加速", "并发" });
        semanticMap.put("安全", new String[] { "权限", "认证", "授权", "加密", "防护", "隔离", "审计", "合规" });
        // RAG相关的语义扩展（核心）
        semanticMap.put("rag", new String[] { "知识库", "检索", "向量", "embedding", "文档解析", "知识引擎", "召回", "重排", "rerank",
                "语义检索", "知识存储", "文档分片", "幻觉" });
        semanticMap.put("检索", new String[] { "rag", "向量检索", "语义检索", "召回", "匹配", "查询", "搜索", "bm25" });
        semanticMap.put("向量", new String[] { "embedding", "向量化", "向量库", "语义", "相似度", "rag" });
        semanticMap.put("文档", new String[] { "pdf", "word", "文档解析", "分片", "切片", "知识库", "文本" });

        // 检查查询中的关键词是否有语义扩展
        int semanticMatches = 0;
        for (String keyword : keywords) {
            if (semanticMap.containsKey(keyword)) {
                String[] relatedWords = semanticMap.get(keyword);
                int matchCount = 0;
                for (String word : relatedWords) {
                    if (lowerText.contains(word.toLowerCase())) {
                        matchCount++;
                        semanticMatches++;
                    }
                }

                if (matchCount > 0) {
                    // 匹配越多，分数越高（但有上限）
                    double matchScore = Math.min(matchCount * 1.5, 5.0);
                    score += matchScore;
                    log.debug("  ✓ 语义相关词匹配 {} 个 (来自 \"{}\"): +{}", matchCount, keyword,
                            String.format("%.1f", matchScore));
                }
            }
        }

        // 语义匹配丰富度加分
        if (semanticMatches >= 3) {
            score += 3.0;
            log.debug("  ✓ 语义匹配丰富 ({} 个相关词): +3.0", semanticMatches);
        }

        // 特殊模式匹配：技术规格模式（增强版）
        int patternMatches = 0;

        // 1. 硬件规格模式
        if (fullQuery.contains("服务器") || fullQuery.contains("配置") || fullQuery.contains("硬件")) {
            // CPU/GPU规格：48Core*4, 8核, 16线程
            if (lowerText.matches(".*\\d+\\s*core.*") || lowerText.matches(".*\\d+核.*")) {
                patternMatches++;
                log.debug("  ✓ CPU规格模式");
            }

            // 内存规格：64GB, 32G
            if (lowerText.matches(".*\\d+\\s*gb.*") || lowerText.matches(".*\\d+g[^a-z].*")) {
                patternMatches++;
                log.debug("  ✓ 内存规格模式");
            }

            // 存储规格：3.2T SSD, 960GB
            if (lowerText.matches(".*\\d+\\.?\\d*\\s*t\\s*(ssd|hdd).*") ||
                    lowerText.matches(".*\\d+\\s*tb.*")) {
                patternMatches++;
                log.debug("  ✓ 存储规格模式");
            }

            // 网络规格：25G*4, 200G网口
            if (lowerText.matches(".*\\d+g\\s*\\*\\s*\\d+.*") || lowerText.matches(".*\\d+g.*网.*")) {
                patternMatches++;
                log.debug("  ✓ 网络规格模式");
            }

            // 品牌/型号：鲲鹏、昇腾、NVIDIA等
            if (lowerText.contains("鲲鹏") || lowerText.contains("昇腾") ||
                    lowerText.contains("nvidia") || lowerText.contains("intel")) {
                patternMatches++;
                log.debug("  ✓ 品牌型号模式");
            }
        }

        // 2. 列表/枚举模式（1. 2. 3. 或 一、二、三）
        if (lowerText.matches(".*[1-9]\\s*[.、].*") || lowerText.matches(".*[一二三四五六七八九十]、.*")) {
            score += 2.0;
            log.debug("  ✓ 列表枚举模式: +2.0");
        }

        // 3. 标题模式（包含"（"、"【"等）
        if (lowerText.matches(".*[（\\(].*[）\\)].*") || lowerText.matches(".*【.*】.*")) {
            score += 1.5;
            log.debug("  ✓ 标题格式模式: +1.5");
        }

        // 技术规格模式总分
        if (patternMatches > 0) {
            double patternScore = Math.min(patternMatches * 2.5, 8.0);
            score += patternScore;
            log.debug("  ✓ 技术规格模式匹配 {} 项: +{}", patternMatches, String.format("%.1f", patternScore));
        }

        return score;
    }

    /**
     * 计算子串在文本中出现的次数
     *
     * @param text      文本内容
     * @param substring 要查找的子串
     * @return 出现次数
     */
    @Override
    public int countOccurrences(String text, String substring) {
        return TextProcessingUtils.countOccurrences(text, substring);
    }

    /**
     * 提取并过滤关键词
     *
     * <p>
     * 从查询中提取有效关键词，过滤停用词和无意义词
     * </p>
     *
     * @param query 用户查询
     * @return 关键词数组
     */
    @Override
    public String[] extractKeywords(String query) {
        // 去除标点和停用词
        String normalizedQuery = TextProcessingUtils.PUNCTUATION_PATTERN.matcher(query.toLowerCase()).replaceAll(" ")
                .trim();
        String[] allKeywords = normalizedQuery.split("\\s+");

        // 过滤停用词和无意义词
        List<String> filteredKeywords = new ArrayList<>();
        for (String keyword : allKeywords) {
            if (TextProcessingUtils.isValidKeyword(keyword)) {
                filteredKeywords.add(keyword);
            }
        }

        // 智能关键词提取：识别有意义的词汇
        Set<String> expandedKeywords = new LinkedHashSet<>();

        // 首先添加原始关键词
        for (String keyword : filteredKeywords) {
            // 检查是否包含英文（如RAG），英文词保持完整
            Matcher englishMatcher = TextProcessingUtils.ENGLISH_PATTERN.matcher(keyword);
            if (englishMatcher.find()) {
                // 提取所有英文部分
                englishMatcher.reset();
                while (englishMatcher.find()) {
                    String englishWord = englishMatcher.group().toLowerCase();
                    if (englishWord.length() >= 2) {
                        expandedKeywords.add(englishWord);
                    }
                }
                // 提取中文部分
                String chinesePart = TextProcessingUtils.ENGLISH_PATTERN.matcher(keyword).replaceAll("").trim();
                if (TextProcessingUtils.isValidKeyword(chinesePart)) {
                    expandedKeywords.add(chinesePart);
                }
            } else {
                expandedKeywords.add(keyword);
            }
        }

        // 对于纯中文长词，智能拆分（只拆分>=4字的词，且只取有意义的组合）
        for (String keyword : new ArrayList<>(expandedKeywords)) {
            if (keyword.length() >= 4 && !keyword.matches(".*[a-zA-Z]+.*")) {
                // 拆分成2字词，但只保留有意义的
                for (int i = 0; i <= keyword.length() - 2; i += 2) {
                    String subword = keyword.substring(i, Math.min(i + 2, keyword.length()));
                    if (TextProcessingUtils.isValidKeyword(subword)) {
                        expandedKeywords.add(subword);
                    }
                }
            }
        }

        return expandedKeywords.toArray(new String[0]);
    }
}
