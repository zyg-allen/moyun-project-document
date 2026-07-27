package com.moyun.agent.parser;

import com.moyun.agent.dto.ImageAnalysisResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 图片分析结果解析器
 * 
 * <p>解析多模态AI返回的结构化文本，提取各个字段信息</p>
 * 
 * @author laomao
 */
@Slf4j
@Component
public class ImageAnalysisParser {
    
    /**
     * 图片类型匹配模式
     */
    private static final Pattern TYPE_PATTERN = Pattern.compile("【图片类型】\\s*[:：]?\\s*([^\\n]+)");
    
    /**
     * 信息价值匹配模式
     */
    private static final Pattern VALUE_PATTERN = Pattern.compile("【信息价值】\\s*[:：]?\\s*([^\\n]+)");
    
    /**
     * 存储建议匹配模式
     */
    private static final Pattern ADVICE_PATTERN = Pattern.compile("【存储建议】\\s*[:：]?\\s*([^\\n]+)");
    
    /**
     * 判断理由匹配模式
     */
    private static final Pattern REASON_PATTERN = Pattern.compile("【理由】\\s*[:：]?\\s*([^\\n]+)");
    
    /**
     * 主题匹配模式
     */
    private static final Pattern SUBJECT_PATTERN = Pattern.compile("【主题】\\s*[:：]?\\s*([^\\n]+)");
    
    /**
     * 关键词匹配模式
     */
    private static final Pattern KEYWORDS_PATTERN = Pattern.compile("【关键词】\\s*[:：]?\\s*([^\\n]+)");
    
    /**
     * 关系结构匹配模式
     */
    private static final Pattern RELATION_PATTERN = Pattern.compile("【关系】\\s*[:：]?\\s*([^\\n]+)");
    
    /**
     * 补充说明匹配模式
     */
    private static final Pattern SUPPLEMENT_PATTERN = Pattern.compile("【补充】\\s*[:：]?\\s*([^\\n]+)");
    
    /**
     * 组件匹配模式
     */
    private static final Pattern COMPONENTS_PATTERN = Pattern.compile("【组件】\\s*[:：]?\\s*([^\\n]+)");
    
    /**
     * 解析AI返回的分析结果
     * 
     * @param aiResponse AI返回的原始文本
     * @return 解析后的结果对象，解析失败返回null
     */
    public ImageAnalysisResult parse(String aiResponse) {
        if (aiResponse == null || aiResponse.isEmpty()) {
            return null;
        }
        
        ImageAnalysisResult result = new ImageAnalysisResult();
        result.setFullDescription(aiResponse);
        
        // 提取第一步：价值判断字段
        result.setImageType(extractField(aiResponse, TYPE_PATTERN));
        result.setInformationValue(extractField(aiResponse, VALUE_PATTERN));
        result.setStorageAdvice(extractField(aiResponse, ADVICE_PATTERN));
        result.setReason(extractField(aiResponse, REASON_PATTERN));
        
        // 提取第二步：详细分析字段
        result.setSubject(extractField(aiResponse, SUBJECT_PATTERN));
        result.setKeywords(extractField(aiResponse, KEYWORDS_PATTERN));
        result.setRelationship(extractField(aiResponse, RELATION_PATTERN));
        result.setComponents(extractField(aiResponse, COMPONENTS_PATTERN));
        result.setSupplement(extractField(aiResponse, SUPPLEMENT_PATTERN));
        
        // 容错处理：如果没有明确的存储建议，尝试从文本中推断
        if (result.getStorageAdvice() == null) {
            if (aiResponse.contains("丢弃") || aiResponse.contains("跳过") || aiResponse.contains("不存储")) {
                result.setStorageAdvice("丢弃");
            } else if (aiResponse.contains("存储") || aiResponse.contains("保留")) {
                result.setStorageAdvice("存储");
            }
        }
        
        // 容错处理：如果没有明确的价值评估，尝试从文本中推断
        if (result.getInformationValue() == null) {
            if (aiResponse.contains("高价值") || aiResponse.contains("信息丰富")) {
                result.setInformationValue("高");
            } else if (aiResponse.contains("低价值") || aiResponse.contains("无价值")) {
                result.setInformationValue("低");
            }
        }
        
        return result;
    }
    
    /**
     * 从文本中提取指定字段
     * 
     * @param text 原始文本
     * @param pattern 匹配模式
     * @return 提取的字段值，未找到返回null
     */
    private String extractField(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }
    
    /**
     * 验证分析结果是否有效
     * 
     * <p>检查必要字段是否完整，关键词数量是否足够</p>
     * 
     * @param result 分析结果
     * @return true-有效，false-无效
     */
    public boolean isValidAnalysis(ImageAnalysisResult result) {
        if (result == null) {
            return false;
        }
        
        // 必须有存储建议
        if (result.getStorageAdvice() == null) {
            log.warn("AI返回缺少存储建议");
            return false;
        }
        
        // 如果建议存储，检查关键词
        if (result.shouldStore()) {
            String keywords = result.getKeywords();
            
            // 如果没有关键词，尝试从完整描述中提取
            if (keywords == null || keywords.isEmpty()) {
                log.warn("AI建议存储但未提供关键词");
                log.debug("完整响应: {}", result.getFullDescription());
                
                // 如果有主题，也算有效
                if (result.getSubject() != null && !result.getSubject().isEmpty()) {
                    log.info("虽无关键词但有主题，认为有效");
                    return true;
                }
                
                // 如果完整描述足够长，也算有效
                if (result.getFullDescription() != null && result.getFullDescription().length() > 50) {
                    log.info("虽无关键词但完整描述充足，认为有效");
                    return true;
                }
                
                return false;
            }
            
            // 如果关键词是"无文字内容"，说明图片确实没有文字，也算有效
            keywords = keywords.trim();
            if ("无文字内容".equals(keywords) || "无".equals(keywords) || "无文字".equals(keywords)) {
                log.info("图片无文字内容，但分析有效");
                return true;
            }
            
            // 关键词数量至少2个（降低要求，因为有些架构图文字较少）
            if (result.getKeywordCount() < 2) {
                log.warn("关键词数量不足: {}，关键词: {}", result.getKeywordCount(), result.getKeywords());
                // 如果只有1个关键词，但主题存在，也算有效
                if (result.getKeywordCount() == 1 && result.getSubject() != null && !result.getSubject().isEmpty()) {
                    log.info("关键词只有1个但有主题，认为有效");
                    return true;
                }
                return false;
            }
        }
        
        return true;
    }
}
