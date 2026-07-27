package com.moyun.agent.service.impl;

import com.moyun.agent.service.QueryTypeClassifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * 查询类型分类器实现
 *
 * <p>基于规则的查询类型识别，用于动态调整混合检索权重</p>
 *
 * @author laomao
 * @since 2025-01-22
 */
@Slf4j
@Service
public class QueryTypeClassifierImpl implements QueryTypeClassifier {

    // 错误代码模式（E1001、ERR-500、ERROR_CODE_123等）
    private static final Pattern ERROR_CODE_PATTERN = Pattern.compile(
            "(?i)(E\\d{3,}|ERR[-_]\\d+|ERROR[-_]CODE[-_]\\d+|\\d{3}错误|错误码\\d+)",
            Pattern.CASE_INSENSITIVE
    );

    // 版本号模式（v1.0.0、Java 17、Python 3.9等）
    private static final Pattern VERSION_PATTERN = Pattern.compile(
            "(?i)(v\\d+\\.\\d+(\\.\\d+)?|版本\\d+|\\d+\\.\\d+(\\.\\d+)?版本|Java\\s*\\d+|Python\\s*\\d+)",
            Pattern.CASE_INSENSITIVE
    );

    // 标识符模式（ID、编号、序列号等）
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile(
            "(?i)(ID[:：]?\\s*\\d+|编号[:：]?\\s*\\d+|序列号[:：]?\\s*[A-Z0-9]+)",
            Pattern.CASE_INSENSITIVE
    );

    // 专有名词模式（大写字母开头的连续单词，如 Qwen3-Rerank、Redis、MySQL）
    private static final Pattern PROPER_NOUN_PATTERN = Pattern.compile(
            "([A-Z][a-z]+[-]?[A-Z0-9][a-zA-Z0-9]*|[A-Z]{2,})",
            Pattern.MULTILINE
    );

    // 语义查询关键词
    private static final String[] SEMANTIC_KEYWORDS = {
            "如何", "怎么", "怎样", "为什么", "什么", "哪些", "哪个",
            "原理", "机制", "架构", "流程", "过程", "方法", "步骤",
            "区别", "对比", "比较", "优势", "劣势", "优缺点",
            "解释", "说明", "介绍", "描述", "概念", "定义"
    };

    // 精确查询关键词
    private static final String[] EXACT_KEYWORDS = {
            "错误", "报错", "异常", "失败", "问题",
            "配置", "参数", "设置", "选项",
            "命令", "语法", "格式", "规范"
    };

    @Override
    public String classifyQueryType(String query) {
        if (query == null || query.trim().isEmpty()) {
            return "semantic";
        }

        log.debug("开始分类查询类型: {}", query);

        // 1. 检查是否为精确查询
        if (isExactQuery(query)) {
            log.info("识别为精确查询: {}", query);
            return "exact";
        }

        // 2. 检查是否为语义查询
        if (isSemanticQuery(query)) {
            log.info("识别为语义查询: {}", query);
            return "semantic";
        }

        // 3. 默认为混合查询
        log.info("识别为混合查询: {}", query);
        return "hybrid";
    }

    @Override
    public boolean isExactQuery(String query) {
        if (query == null || query.isEmpty()) {
            return false;
        }

        int exactScore = 0;

        // 1. 检查错误代码
        if (ERROR_CODE_PATTERN.matcher(query).find()) {
            exactScore += 3;
            log.debug("包含错误代码，精确分数 +3");
        }

        // 2. 检查版本号
        if (VERSION_PATTERN.matcher(query).find()) {
            exactScore += 2;
            log.debug("包含版本号，精确分数 +2");
        }

        // 3. 检查标识符
        if (IDENTIFIER_PATTERN.matcher(query).find()) {
            exactScore += 2;
            log.debug("包含标识符，精确分数 +2");
        }

        // 4. 检查专有名词（至少2个）
        var matcher = PROPER_NOUN_PATTERN.matcher(query);
        int properNounCount = 0;
        while (matcher.find()) {
            properNounCount++;
        }
        if (properNounCount >= 2) {
            exactScore += 2;
            log.debug("包含{}个专有名词，精确分数 +2", properNounCount);
        }

        // 5. 检查精确查询关键词
        for (String keyword : EXACT_KEYWORDS) {
            if (query.contains(keyword)) {
                exactScore += 1;
                log.debug("包含精确关键词'{}'，精确分数 +1", keyword);
                break; // 只计算一次
            }
        }

        // 6. 查询长度较短（<15字符）且包含特殊字符，可能是精确查询
        if (query.length() < 15 && query.matches(".*[A-Z0-9-_]+.*")) {
            exactScore += 1;
            log.debug("短查询且包含特殊字符，精确分数 +1");
        }

        boolean isExact = exactScore >= 3;
        log.debug("精确查询评分: {}, 判定结果: {}", exactScore, isExact);
        return isExact;
    }

    @Override
    public boolean isSemanticQuery(String query) {
        if (query == null || query.isEmpty()) {
            return false;
        }

        int semanticScore = 0;

        // 1. 检查语义查询关键词
        for (String keyword : SEMANTIC_KEYWORDS) {
            if (query.contains(keyword)) {
                semanticScore += 2;
                log.debug("包含语义关键词'{}'，语义分数 +2", keyword);
                break; // 只计算一次
            }
        }

        // 2. 查询长度较长（>20字符），可能是语义查询
        if (query.length() > 20) {
            semanticScore += 1;
            log.debug("查询较长({}字符)，语义分数 +1", query.length());
        }

        // 3. 包含问号，可能是开放性问题
        if (query.contains("？") || query.contains("?")) {
            semanticScore += 1;
            log.debug("包含问号，语义分数 +1");
        }

        // 4. 不包含专有名词或特殊标识符
        if (!PROPER_NOUN_PATTERN.matcher(query).find() &&
                !ERROR_CODE_PATTERN.matcher(query).find() &&
                !VERSION_PATTERN.matcher(query).find()) {
            semanticScore += 1;
            log.debug("不包含专有名词/错误码/版本号，语义分数 +1");
        }

        boolean isSemantic = semanticScore >= 3;
        log.debug("语义查询评分: {}, 判定结果: {}", semanticScore, isSemantic);
        return isSemantic;
    }

    @Override
    public double[] getRecommendedWeights(String queryType) {
        switch (queryType) {
            case "exact":
                // 精确查询：BM25权重提高到70%
                log.debug("精确查询，推荐权重: 向量30% + BM25 70%");
                return new double[]{0.3, 0.7};

            case "semantic":
                // 语义查询：向量权重保持70%
                log.debug("语义查询，推荐权重: 向量70% + BM25 30%");
                return new double[]{0.7, 0.3};

            case "hybrid":
            default:
                // 混合查询：平衡权重
                log.debug("混合查询，推荐权重: 向量50% + BM25 50%");
                return new double[]{0.5, 0.5};
        }
    }
}
