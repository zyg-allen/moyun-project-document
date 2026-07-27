package com.moyun.agent.service.impl;

import com.moyun.agent.service.DocumentTypeDetector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * 文档类型检测器实现
 *
 * <p>基于规则的文档类型识别，用于选择合适的分片策略</p>
 *
 * @author laomao
 * @since 2025-01-22
 */
@Slf4j
@Service
public class DocumentTypeDetectorImpl implements DocumentTypeDetector {

    // FAQ特征模式
    private static final Pattern FAQ_PATTERN = Pattern.compile(
            "(?i)(问题|问：|Q:|Q&A|常见问题|FAQ|疑问|解答|答：|A:)",
            Pattern.MULTILINE
    );

    // 表格特征模式（Markdown表格或多个竖线）
    private static final Pattern TABLE_PATTERN = Pattern.compile(
            "(\\|[^\\n]+\\|[^\\n]+\\|)|(<table>)|(<tr>)",
            Pattern.MULTILINE
    );

    // 代码文件扩展名
    private static final Pattern CODE_FILE_PATTERN = Pattern.compile(
            "\\.(java|py|js|ts|cpp|c|h|go|rs|php|rb|swift|kt|scala|sh|sql|xml|json|yaml|yml)$",
            Pattern.CASE_INSENSITIVE
    );

    // 代码特征模式
    private static final Pattern CODE_PATTERN = Pattern.compile(
            "(public class|def |function |import |package |#include|const |var |let )",
            Pattern.MULTILINE
    );

    // 技术文档特征关键词
    private static final String[] TECHNICAL_KEYWORDS = {
            "API", "接口", "参数", "返回值", "配置", "架构", "系统", "服务",
            "数据库", "缓存", "消息队列", "微服务", "部署", "运维", "监控",
            "性能", "优化", "安全", "认证", "授权", "加密", "算法"
    };

    @Override
    public String detectDocumentType(String fileName, String content) {
        log.debug("开始检测文档类型: {}", fileName);

        // 使用评分机制，避免简单if-else导致的误判
        int codeScore = calculateCodeScore(fileName, content);
        int faqScore = calculateFaqScore(fileName, content);
        int tableScore = calculateTableScore(fileName, content);
        int technicalScore = calculateTechnicalScore(fileName, content);

        log.debug("文档类型评分 - 代码:{}, FAQ:{}, 表格:{}, 技术:{}", 
                  codeScore, faqScore, tableScore, technicalScore);

        // 找出最高分
        int maxScore = Math.max(Math.max(codeScore, faqScore), 
                                Math.max(tableScore, technicalScore));

        // 如果最高分小于阈值，判定为通用文档
        if (maxScore < 3) {
            log.info("检测为通用文档: {} (最高分: {})", fileName, maxScore);
            return "general";
        }

        // 返回得分最高的类型
        if (maxScore == codeScore) {
            log.info("检测到代码文档: {} (得分: {})", fileName, codeScore);
            return "code";
        } else if (maxScore == faqScore) {
            log.info("检测到FAQ文档: {} (得分: {})", fileName, faqScore);
            return "faq";
        } else if (maxScore == tableScore) {
            log.info("检测到表格文档: {} (得分: {})", fileName, tableScore);
            return "table";
        } else {
            log.info("检测到技术文档: {} (得分: {})", fileName, technicalScore);
            return "technical";
        }
    }

    /**
     * 计算代码文档评分
     */
    private int calculateCodeScore(String fileName, String content) {
        int score = 0;

        // 文件扩展名匹配（权重最高）
        if (fileName != null && CODE_FILE_PATTERN.matcher(fileName).find()) {
            score += 5;
        }

        // 内容特征匹配
        if (content != null && !content.isEmpty()) {
            String sample = content.substring(0, Math.min(500, content.length()));
            var matcher = CODE_PATTERN.matcher(sample);
            while (matcher.find()) {
                score++;
            }
        }

        return score;
    }

    /**
     * 计算FAQ文档评分
     */
    private int calculateFaqScore(String fileName, String content) {
        int score = 0;

        // 文件名包含FAQ关键词
        if (fileName != null) {
            String lowerFileName = fileName.toLowerCase();
            if (lowerFileName.contains("faq") || lowerFileName.contains("问答") || 
                lowerFileName.contains("常见问题")) {
                score += 2;
            }
        }

        // 内容特征匹配
        if (content != null && !content.isEmpty()) {
            String sample = content.substring(0, Math.min(1000, content.length()));
            var matcher = FAQ_PATTERN.matcher(sample);
            while (matcher.find()) {
                score++;
            }
        }

        return score;
    }

    /**
     * 计算表格文档评分
     */
    private int calculateTableScore(String fileName, String content) {
        int score = 0;

        // 文件名包含表格关键词
        if (fileName != null) {
            String lowerFileName = fileName.toLowerCase();
            if (lowerFileName.contains("表格") || lowerFileName.contains("数据") || 
                lowerFileName.contains("table")) {
                score += 2;
            }
        }

        // 内容特征匹配
        if (content != null && !content.isEmpty()) {
            String sample = content.substring(0, Math.min(2000, content.length()));
            var matcher = TABLE_PATTERN.matcher(sample);
            while (matcher.find()) {
                score++;
            }
        }

        return score;
    }

    /**
     * 计算技术文档评分
     */
    private int calculateTechnicalScore(String fileName, String content) {
        int score = 0;

        // 文件名包含技术关键词
        if (fileName != null) {
            String lowerFileName = fileName.toLowerCase();
            if (lowerFileName.contains("api") || lowerFileName.contains("技术") || 
                lowerFileName.contains("架构") || lowerFileName.contains("设计")) {
                score += 2;
            }
        }

        // 内容特征匹配
        if (content != null && !content.isEmpty()) {
            String sample = content.substring(0, Math.min(1000, content.length()));
            for (String keyword : TECHNICAL_KEYWORDS) {
                if (sample.contains(keyword)) {
                    score++;
                }
            }
        }

        return score;
    }

    @Override
    public boolean isFaqDocument(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }

        // 检查前1000字符中FAQ特征的出现次数
        String sample = content.substring(0, Math.min(1000, content.length()));
        var matcher = FAQ_PATTERN.matcher(sample);

        int count = 0;
        while (matcher.find()) {
            count++;
        }

        // 如果出现3次以上FAQ特征，判定为FAQ文档
        boolean isFaq = count >= 3;
        log.debug("FAQ特征出现次数: {}, 判定结果: {}", count, isFaq);
        return isFaq;
    }

    @Override
    public boolean isTableDocument(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }

        // 检查前2000字符中表格特征的出现次数
        String sample = content.substring(0, Math.min(2000, content.length()));
        var matcher = TABLE_PATTERN.matcher(sample);

        int count = 0;
        while (matcher.find()) {
            count++;
        }

        // 如果出现5次以上表格特征，判定为表格文档
        boolean isTable = count >= 5;
        log.debug("表格特征出现次数: {}, 判定结果: {}", count, isTable);
        return isTable;
    }

    @Override
    public boolean isCodeDocument(String fileName, String content) {
        if (fileName == null) {
            return false;
        }

        // 1. 检查文件扩展名
        if (CODE_FILE_PATTERN.matcher(fileName).find()) {
            log.debug("文件扩展名匹配代码文件: {}", fileName);
            return true;
        }

        // 2. 检查内容特征
        if (content != null && !content.isEmpty()) {
            String sample = content.substring(0, Math.min(500, content.length()));
            if (CODE_PATTERN.matcher(sample).find()) {
                log.debug("内容特征匹配代码文档");
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isTechnicalDocument(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }

        // 检查前1000字符中技术关键词的出现次数
        String sample = content.substring(0, Math.min(1000, content.length()));
        int keywordCount = 0;

        for (String keyword : TECHNICAL_KEYWORDS) {
            if (sample.contains(keyword)) {
                keywordCount++;
            }
        }

        // 如果出现5个以上技术关键词，判定为技术文档
        boolean isTechnical = keywordCount >= 5;
        log.debug("技术关键词出现次数: {}, 判定结果: {}", keywordCount, isTechnical);
        return isTechnical;
    }
}
