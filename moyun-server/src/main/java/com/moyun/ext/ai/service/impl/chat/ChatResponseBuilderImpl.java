package com.moyun.ext.ai.service.impl.chat;

import com.moyun.ext.ai.engine.tool.ToolResult;
import com.moyun.ext.ai.service.chat.ChatResponseBuilderService;
import com.moyun.ext.ai.util.JsonUtils;
import dev.langchain4j.rag.content.Content;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 对话响应构建器
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
@Slf4j
@Service
public class ChatResponseBuilderImpl implements ChatResponseBuilderService {

    /**
     * 替换回答中的图片占位符为实际图片HTML
     *
     * <p>
     * 将AI回复中的 [[IMAGE_N]] 占位符替换为实际的图片HTML。
     * 注意：每个占位符只替换第一次出现，避免同一张图片重复显示。
     * </p>
     *
     * @param response AI的原始回复内容
     * @param contents 检索到的内容列表（包含图片信息）
     * @return 替换占位符后的回复内容
     */
    @Override
    public String replaceImagePlaceholders(String response, List<Content> contents) {
        String result = response;
        int imageIndex = 1; // 图片序号从1开始

        for (int i = 0; i < contents.size(); i++) {
            Content content = contents.get(i);
            var metadata = content.textSegment().metadata();
            String type = metadata != null ? metadata.getString("type") : "text";

            if ("image".equals(type)) {
                String imagePath = metadata != null ? metadata.getString("imagePath") : null;
                String fileName = metadata != null ? metadata.getString("fileName") : "未知文件";
                String pageNumber = metadata != null ? metadata.getString("pageNumber") : "0";
                String description = content.textSegment().text();

                if (imagePath != null) {
                    String placeholder = "[[IMAGE_" + imageIndex + "]]";

                    // 检查占位符是否存在
                    int firstOccurrence = result.indexOf(placeholder);
                    if (firstOccurrence >= 0) {
                        // 构建图片HTML
                        String imageHtml = buildImageHtml(imageIndex, imagePath, fileName, pageNumber, description);

                        // 只替换第一次出现的占位符，后续出现的替换为简单引用文本
                        result = result.substring(0, firstOccurrence)
                                + imageHtml
                                + result.substring(firstOccurrence + placeholder.length())
                                        .replace(placeholder, "（见上图 " + imageIndex + "）");

                        log.info("✅ 替换图片占位符: {} -> 图片HTML (路径: {})", placeholder, imagePath);
                    } else {
                        log.debug("⚠️ 未找到图片占位符: {}", placeholder);
                    }
                    imageIndex++; // 只有图片才增加序号
                } else {
                    log.warn("⚠️ 图片类型内容缺少路径，跳过序号: {}", imageIndex);
                    imageIndex++;
                }
            }
        }

        log.info("📊 占位符替换完成，共处理 {} 张图片", imageIndex - 1);
        return result;
    }

    /**
     * 预先构建图片索引到HTML的映射
     *
     * <p>
     * 用于流式输出时快速替换图片占位符，避免重复遍历内容列表
     * </p>
     *
     * @param contents 检索到的内容列表
     * @return 图片索引到HTML的映射，key为图片序号（从1开始），value为图片HTML
     */
    @Override
    public Map<Integer, String> buildImageHtmlMap(List<Content> contents) {
        Map<Integer, String> imageHtmlMap = new HashMap<>();
        if (contents == null || contents.isEmpty()) {
            return imageHtmlMap;
        }

        int imageIndex = 1;
        for (Content content : contents) {
            var metadata = content.textSegment().metadata();
            String type = metadata != null ? metadata.getString("type") : "text";

            if ("image".equals(type)) {
                String imagePath = metadata != null ? metadata.getString("imagePath") : null;
                String fileName = metadata != null ? metadata.getString("fileName") : "未知文件";
                String pageNumber = metadata != null ? metadata.getString("pageNumber") : "0";
                String description = content.textSegment().text();

                if (imagePath != null) {
                    String imageHtml = buildImageHtml(imageIndex, imagePath, fileName, pageNumber, description);
                    imageHtmlMap.put(imageIndex, imageHtml);
                }
                imageIndex++;
            }
        }

        log.info("📊 预构建图片HTML映射，共 {} 张图片", imageHtmlMap.size());
        return imageHtmlMap;
    }

    /**
     * 构建图片展示HTML
     *
     * <p>
     * 生成简洁的图片卡片，包含：
     * <ul>
     * <li>图片序号标签</li>
     * <li>来源文件名和页码</li>
     * <li>可点击放大的图片</li>
     * <li>加载失败的备用提示</li>
     * </ul>
     * </p>
     *
     * @param index       图片序号（从1开始）
     * @param imagePath   图片存储路径
     * @param fileName    来源文件名
     * @param pageNumber  所在页码
     * @param description 图片描述（用于alt属性）
     * @return 图片HTML字符串
     */
    @Override
    public String buildImageHtml(int index, String imagePath, String fileName, String pageNumber, String description) {
        StringBuilder html = new StringBuilder();

        // 简洁的图片容器
        html.append(
                "\n\n<div class='inline-image-card' style='margin: 12px 0; padding: 12px; border: 1px solid #e4e7ed; border-radius: 8px; background: #fafafa; max-width: 500px;'>\n");

        // 图片标题栏 - 简洁
        html.append(
                "<div style='display: flex; align-items: center; gap: 8px; margin-bottom: 8px; font-size: 13px; color: #606266;'>\n");
        html.append(
                "<span style='background: #67c23a; color: white; padding: 2px 8px; border-radius: 4px; font-size: 12px;'>📷 图")
                .append(index).append("</span>\n");
        html.append("<span>").append(fileName).append(" · 第").append(pageNumber).append("页</span>\n");
        html.append("</div>\n");

        // 图片 - 限制最大宽度
        // URL编码图片路径，处理特殊字符
        String encodedPath = URLEncoder.encode(imagePath.replace("\\", "/"), StandardCharsets.UTF_8);

        html.append("<div style='text-align: center;'>\n");
        html.append("<img src='/api/image?path=").append(encodedPath).append("' ");
        html.append("alt='").append(fileName).append("' ");
        html.append(
                "style='max-width: 100%; max-height: 350px; border-radius: 6px; box-shadow: 0 2px 8px rgba(0,0,0,0.08); cursor: zoom-in;' ");
        html.append("onclick=\"window.open('/api/image?path=").append(encodedPath).append("', '_blank')\" ");
        html.append("title='点击查看大图' ");
        html.append("onerror=\"this.style.display='none'; this.nextElementSibling.style.display='block'\" />\n");
        html.append("<div style='display:none; color: #f56c6c; padding: 15px; font-size: 13px;'>⚠️ 图片加载失败</div>\n");
        html.append("</div>\n");

        html.append("</div>\n\n");

        return html.toString();
    }

    /**
     * 构建工具执行结果HTML
     *
     * <p>
     * 将工具执行结果格式化为美观的卡片样式，包含：
     * <ul>
     * <li>工具名称和执行耗时</li>
     * <li>执行结果内容</li>
     * </ul>
     * </p>
     *
     * @param toolName 工具名称
     * @param result   工具执行结果
     * @return 工具结果HTML字符串
     */
    @Override
    public String buildToolResultHtml(String toolName, ToolResult result) {
        StringBuilder html = new StringBuilder();

        html.append(
                "\n\n<div class='tool-result-card' style='margin: 16px 0; padding: 16px; border: 1px solid #e6a23c; border-radius: 8px; background: linear-gradient(135deg, #fdf6ec 0%, #fef9f3 100%);'>\n");

        // 标题栏
        html.append("<div style='display: flex; align-items: center; gap: 8px; margin-bottom: 12px;'>\n");
        html.append(
                "<span style='background: #e6a23c; color: white; padding: 4px 10px; border-radius: 4px; font-size: 12px; font-weight: 500;'>");
        html.append("<i class='fa-solid fa-wrench'></i> 工具执行</span>\n");
        html.append("<span style='color: #606266; font-size: 13px;'>").append(toolName).append("</span>\n");
        html.append("<span style='color: #67c23a; font-size: 12px;'>(").append(result.getDurationMs())
                .append("ms)</span>\n");
        html.append("</div>\n");

        // 结果内容
        html.append(
                "<div style='background: white; padding: 12px; border-radius: 6px; font-size: 14px; line-height: 1.6; color: #303133; white-space: pre-wrap;'>\n");
        html.append(
                result.getContent() != null ? result.getContent().replace("<", "&lt;").replace(">", "&gt;") : "无返回内容");
        html.append("\n</div>\n");

        html.append("</div>\n\n");

        return html.toString();
    }

    /**
     * 构建参考资料按钮HTML
     *
     * <p>
     * 为每个参考来源生成可点击的按钮，支持：
     * <ul>
     * <li>显示来源文件名和页码</li>
     * <li>点击查看原文内容</li>
     * <li>点击定位到PDF原文位置</li>
     * <li>显示相关性分数（调试用）</li>
     * </ul>
     * </p>
     *
     * @param contents           已过滤的有效参考来源列表
     * @param contentRerankScores 重排分数映射
     * @return 参考资料按钮HTML字符串
     */
    @Override
    public String buildReferencesButtons(List<Content> contents, Map<Content, Double> contentRerankScores) {
        if (contents == null || contents.isEmpty()) {
            return "";
        }

        StringBuilder html = new StringBuilder();
        html.append(
                "\n\n<div class='references-section' style='margin-top: 16px; padding-top: 12px; border-top: 1px solid #e4e7ed;'>\n");
        html.append("<span style='color: #909399; font-size: 13px; margin-right: 8px;'>📚 参考来源：</span>\n");

        for (int i = 0; i < contents.size(); i++) {
            Content content = contents.get(i);
            var metadata = content.textSegment().metadata();

            String type = metadata.getString("type") != null ? metadata.getString("type") : "text";
            String fileName = metadata.getString("fileName") != null ? metadata.getString("fileName") : "未知文件";
            String fileType = metadata.getString("fileType") != null ? metadata.getString("fileType") : "";
            String segmentIndex = metadata.getString("segmentIndex") != null ? metadata.getString("segmentIndex") : "0";
            String knowledgeBaseId = metadata.getString("knowledgeBaseId");

            // 获取定位信息
            // 页码已经是从1开始的（calculatePdfPageNumber和ImageExtractionService都是1-indexed）
            String pageNumber = metadata.getString("pageNumber");
            String lineStart = metadata.getString("lineStart");
            String lineEnd = metadata.getString("lineEnd");
            String imagePath = metadata.getString("imagePath");

            String text = content.textSegment().text();

            // 获取重排分数和向量分数
            Double rerankScore = contentRerankScores != null ? contentRerankScores.get(content) : null;
            Double vectorScore = null;
            if (metadata != null) {
                Object scoreObj = metadata.toMap().get("score");
                if (scoreObj != null) {
                    vectorScore = Double.parseDouble(scoreObj.toString());
                }
            }

            // 转义特殊字符
            String escapedText = text.replace("\"", "&quot;")
                    .replace("'", "&#39;")
                    .replace("\n", "\\n")
                    .replace("\r", "");

            // 参考来源按钮组容器
            html.append("<span style='display: inline-block; margin: 0 6px 6px 0;'>");

            // 来源按钮
            html.append("<button class='reference-source-btn' ");
            html.append("style='padding: 4px 12px; ");
            html.append("font-size: 13px; color: #409eff; background: #ecf5ff; border: 1px solid #b3d8ff; ");
            html.append("border-radius: 4px 0 0 4px; cursor: pointer; transition: all 0.3s;' ");
            html.append("onmouseover=\"this.style.background='#409eff'; this.style.color='white';\" ");
            html.append("onmouseout=\"this.style.background='#ecf5ff'; this.style.color='#409eff';\" ");
            html.append("data-index='").append(i + 1).append("' ");
            html.append("data-type='").append(type).append("' ");
            html.append("data-text='").append(escapedText).append("' ");
            html.append("data-filename='").append(fileName).append("' ");
            html.append("data-filetype='").append(fileType).append("' ");
            html.append("data-segment='").append(segmentIndex).append("' ");
            html.append("data-kb-id='").append(knowledgeBaseId).append("' ");

            // 添加定位信息
            if (pageNumber != null) {
                html.append("data-page='").append(pageNumber).append("' ");
            }
            if (lineStart != null) {
                html.append("data-line-start='").append(lineStart).append("' ");
                html.append("data-line-end='").append(lineEnd).append("' ");
            }
            if (imagePath != null) {
                html.append("data-image-path='").append(imagePath).append("' ");
            }

            // 添加分数信息（用于反馈）
            if (rerankScore != null) {
                html.append("data-rerank-score='").append(rerankScore).append("' ");
            }
            if (vectorScore != null) {
                html.append("data-vector-score='").append(vectorScore).append("' ");
            }

            html.append("onclick='showReferenceSource(this)'>");
            html.append("来源").append(i + 1);
            html.append("</button>");

            // 不准确反馈按钮
            html.append("<button class='feedback-btn' ");
            html.append("style='padding: 4px 8px; margin-left: -1px; ");
            html.append("font-size: 12px; color: #909399; background: #f5f7fa; border: 1px solid #dcdfe6; ");
            html.append("border-radius: 0 4px 4px 0; cursor: pointer; transition: all 0.3s;' ");
            html.append(
                    "onmouseover=\"this.style.background='#f56c6c'; this.style.color='white'; this.style.borderColor='#f56c6c';\" ");
            html.append(
                    "onmouseout=\"this.style.background='#f5f7fa'; this.style.color='#909399'; this.style.borderColor='#dcdfe6';\" ");
            html.append("data-kb-id='").append(knowledgeBaseId).append("' ");
            html.append("data-filename='").append(fileName).append("' ");
            html.append("data-page='").append(pageNumber != null ? pageNumber : "").append("' ");
            html.append("data-segment='").append(segmentIndex).append("' ");
            if (rerankScore != null) {
                html.append("data-rerank-score='").append(rerankScore).append("' ");
            }
            if (vectorScore != null) {
                html.append("data-vector-score='").append(vectorScore).append("' ");
            }
            html.append("onclick='submitFeedback(this, \"inaccurate\")' ");
            html.append("title='标记此来源不准确'>");
            html.append("<i class='fa-regular fa-thumbs-down'></i>");
            html.append("</button>");

            html.append("</span>\n");
        }

        html.append("</div>\n");
        return html.toString();
    }

    /**
     * 构建参考来源JSON字符串
     *
     * <p>
     * 将参考来源列表序列化为JSON格式，包含完整的元数据信息，
     * 用于前端展示和页面刷新后的状态恢复。
     * </p>
     *
     * @param contents 参考来源内容列表
     * @return JSON格式的参考来源字符串
     */
    @Override
    public String buildReferenceSourcesJson(List<Content> contents) {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        for (Content content : contents) {
            var metadata = content.textSegment().metadata();
            if (metadata != null) {
                String fileName = metadata.getString("fileName");
                Integer pageNumber = metadata.getInteger("pageNumber");
                String type = metadata.getString("type");
                String imagePath = metadata.getString("imagePath");
                String knowledgeBaseId = metadata.getString("knowledgeBaseId");
                String segmentIndex = metadata.getString("segmentIndex");
                String fileType = metadata.getString("fileType");
                String text = content.textSegment().text();

                // 如果有imagePath但没有type，自动推断为image类型
                if (imagePath != null && (type == null || type.isEmpty())) {
                    type = "image";
                    log.info("🔧 自动推断类型为image（因为有imagePath）");
                }

                log.info("构建参考来源JSON - fileName: {}, pageNumber: {}, type: {}, imagePath: {}",
                        fileName, pageNumber, type, imagePath != null ? "有" : "无");

                if (fileName != null) {
                    if (!first) {
                        json.append(",");
                    }
                    first = false;

                    json.append("{");
                    json.append("\"fileName\":\"").append(escapeJson(fileName)).append("\"");
                    if (pageNumber != null) {
                        json.append(",\"pageNumber\":").append(pageNumber);
                    }
                    if (type != null && !type.isEmpty()) {
                        json.append(",\"type\":\"").append(escapeJson(type)).append("\"");
                    }
                    if (imagePath != null) {
                        json.append(",\"imagePath\":\"").append(escapeJson(imagePath)).append("\"");
                    }
                    if (knowledgeBaseId != null) {
                        json.append(",\"knowledgeBaseId\":\"").append(escapeJson(knowledgeBaseId)).append("\"");
                    }
                    if (segmentIndex != null) {
                        json.append(",\"segmentIndex\":\"").append(escapeJson(segmentIndex)).append("\"");
                    }
                    if (fileType != null) {
                        json.append(",\"fileType\":\"").append(escapeJson(fileType)).append("\"");
                    }
                    if (text != null && !text.isEmpty()) {
                        json.append(",\"text\":\"").append(escapeJson(text)).append("\"");
                    }
                    json.append("}");
                }
            }
        }

        json.append("]");
        return json.toString();
    }

    /**
     * JSON字符串转义
     *
     * @param str 原始字符串
     * @return 转义后的JSON安全字符串
     */
    private String escapeJson(String str) {
        return JsonUtils.escapeJson(str);
    }
}
