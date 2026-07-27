package com.moyun.agent.util;

import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

/**
 * 文档清洗工具类
 * 
 * @author laomao
 */
@Slf4j
public class DocumentCleanerUtil {
    
    // 特殊字符正则
    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]");
    
    // 表格描述正则（简单匹配）
    private static final Pattern TABLE_DESC_PATTERN = Pattern.compile("(表\\s*\\d+[：:].+?\\n|Table\\s+\\d+[：:].+?\\n)");
    
    // 页眉页脚正则（简单匹配页码等）
    private static final Pattern PAGE_NUMBER_PATTERN = Pattern.compile("(第\\s*\\d+\\s*页|Page\\s+\\d+|\\d+\\s*/\\s*\\d+)");
    
    /**
     * 清洗文档文本
     * 
     * @param text 原始文本
     * @param removeSpecialChars 是否删除特殊字符
     * @param removeTableDesc 是否删除表格描述
     * @param removeHeaderFooter 是否删除页眉页脚
     * @return 清洗后的文本
     */
    public static String cleanText(String text, 
                                   boolean removeSpecialChars, 
                                   boolean removeTableDesc, 
                                   boolean removeHeaderFooter) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        String cleaned = text;
        
        // 删除特殊字符
        if (removeSpecialChars) {
            cleaned = removeSpecialCharacters(cleaned);
        }
        
        // 删除表格描述
        if (removeTableDesc) {
            cleaned = removeTableDescriptions(cleaned);
        }
        
        // 删除页眉页脚
        if (removeHeaderFooter) {
            cleaned = removeHeaderFooter(cleaned);
        }
        
        return cleaned;
    }
    
    /**
     * 删除特殊字符
     */
    private static String removeSpecialCharacters(String text) {
        // 删除控制字符
        String cleaned = SPECIAL_CHARS_PATTERN.matcher(text).replaceAll("");
        
        // 删除零宽字符
        cleaned = cleaned.replaceAll("[\u200B-\u200D\uFEFF]", "");
        
        // 删除其他特殊符号（保留常用标点）
        cleaned = cleaned.replaceAll("[\\p{C}&&[^\\n\\r\\t]]", "");
        
        log.debug("删除特殊字符: 原长度={}, 新长度={}", text.length(), cleaned.length());
        return cleaned;
    }
    
    /**
     * 删除表格描述
     */
    private static String removeTableDescriptions(String text) {
        String cleaned = TABLE_DESC_PATTERN.matcher(text).replaceAll("");
        
        // 删除常见的表格标记
        cleaned = cleaned.replaceAll("(?m)^\\|.*\\|$", ""); // Markdown表格
        cleaned = cleaned.replaceAll("(?m)^[-+|\\s]+$", ""); // 表格分隔线
        
        log.debug("删除表格描述: 原长度={}, 新长度={}", text.length(), cleaned.length());
        return cleaned;
    }
    
    /**
     * 删除页眉页脚
     */
    private static String removeHeaderFooter(String text) {
        // 按行处理
        String[] lines = text.split("\\n");
        StringBuilder result = new StringBuilder();
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            // 跳过可能是页眉页脚的行
            if (isLikelyHeaderFooter(trimmed)) {
                continue;
            }
            
            result.append(line).append("\n");
        }
        
        String cleaned = result.toString();
        log.debug("删除页眉页脚: 原长度={}, 新长度={}", text.length(), cleaned.length());
        return cleaned;
    }
    
    /**
     * 判断是否可能是页眉页脚
     */
    private static boolean isLikelyHeaderFooter(String line) {
        if (line.isEmpty()) {
            return false;
        }
        
        // 页码模式
        if (PAGE_NUMBER_PATTERN.matcher(line).find()) {
            return true;
        }
        
        // 短行且只包含数字、日期等
        if (line.length() < 30) {
            // 纯数字或日期格式
            if (line.matches("^[\\d\\s/\\-.:]+$")) {
                return true;
            }
            
            // 版权信息
            if (line.matches("(?i).*(copyright|©|版权|保留).*")) {
                return true;
            }
        }
        
        return false;
    }
}
