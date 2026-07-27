package com.moyun.agent.service.chat;

import com.moyun.agent.engine.tool.ToolResult;
import dev.langchain4j.rag.content.Content;

import java.util.List;
import java.util.Map;

/**
 * 对话响应构建器接口
 *
 * <p>负责构建对话过程中的各种HTML响应，包括：
 * <ul>
 *   <li>图片展示HTML</li>
 *   <li>工具执行结果HTML</li>
 *   <li>参考来源按钮HTML</li>
 *   <li>参考来源JSON序列化</li>
 * </ul>
 * </p>
 *
 * @author laomao
 * @since 2025-12-11
 */
public interface ChatResponseBuilderService {

    /**
     * 替换回答中的图片占位符为实际图片HTML
     *
     * @param response AI的原始回复内容
     * @param contents 检索到的内容列表（包含图片信息）
     * @return 替换占位符后的回复内容
     */
    String replaceImagePlaceholders(String response, List<Content> contents);

    /**
     * 预先构建图片索引到HTML的映射
     *
     * @param contents 检索到的内容列表
     * @return 图片索引到HTML的映射
     */
    Map<Integer, String> buildImageHtmlMap(List<Content> contents);

    /**
     * 构建图片展示HTML
     *
     * @param index       图片序号
     * @param imagePath   图片存储路径
     * @param fileName    来源文件名
     * @param pageNumber  所在页码
     * @param description 图片描述
     * @return 图片HTML字符串
     */
    String buildImageHtml(int index, String imagePath, String fileName, String pageNumber, String description);

    /**
     * 构建工具执行结果HTML
     *
     * @param toolName 工具名称
     * @param result   工具执行结果
     * @return 工具结果HTML字符串
     */
    String buildToolResultHtml(String toolName, ToolResult result);

    /**
     * 构建参考资料按钮HTML
     *
     * @param contents           已过滤的有效参考来源列表
     * @param contentRerankScores 重排分数映射
     * @return 参考资料按钮HTML字符串
     */
    String buildReferencesButtons(List<Content> contents, Map<Content, Double> contentRerankScores);

    /**
     * 构建参考来源JSON字符串
     *
     * @param contents 参考来源内容列表
     * @return JSON格式的参考来源字符串
     */
    String buildReferenceSourcesJson(List<Content> contents);
}
