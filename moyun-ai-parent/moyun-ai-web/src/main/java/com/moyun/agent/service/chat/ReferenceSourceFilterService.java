package com.moyun.agent.service.chat;

import com.moyun.agent.entity.Agent;
import dev.langchain4j.rag.content.Content;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 参考来源过滤器接口
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
public interface ReferenceSourceFilterService {

    /**
     * 过滤有效的参考来源
     *
     * @param agent        智能体配置
     * @param contents     原始内容列表
     * @param aiResponse   AI的回复文本
     * @param rerankScores 重排分数映射
     * @return 过滤后的有效参考来源列表
     */
    List<Content> filterValidReferenceSources(Agent agent, List<Content> contents, String aiResponse,
                                               Map<Content, Double> rerankScores);

    /**
     * 检测是否是目录内容
     *
     * @param text 文本内容
     * @return 是否为目录内容
     */
    boolean isTableOfContents(String text);

    /**
     * 检测是否是纯题目段落
     *
     * @param text 文本内容
     * @return 是否为纯题目段落
     */
    boolean isPureQuestionSegment(String text);

    /**
     * 从AI回复中提取提到的页码
     *
     * @param aiResponse AI的回复文本
     * @return 提取到的页码集合
     */
    Set<String> extractMentionedPages(String aiResponse);
}
