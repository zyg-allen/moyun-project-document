package com.moyun.ext.ai.util;


import com.moyun.ext.ai.engine.tool.ToolResult;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * HTML构建工具类
 *
 * <p>提供各种HTML片段的构建方法，用于前端展示</p>
 *
 * @author laomao
 */
public final class HtmlBuilderUtils {

    private HtmlBuilderUtils() {
        // 工具类禁止实例化
    }

    /**
     * 构建图片展示HTML
     *
     * <p>生成可点击放大的图片HTML，包含图片标题和来源信息</p>
     *
     * @param index 图片序号
     * @param imagePath 图片路径
     * @param fileName 来源文件名
     * @param pageNumber 页码
     * @param description 图片描述
     * @return 图片HTML字符串
     */
    public static String buildImageHtml(int index, String imagePath, String fileName, String pageNumber, String description) {
        StringBuilder html = new StringBuilder();
        html.append("\n\n<div class='image-container' style='margin: 15px 0; text-align: center;'>");

        // 图片标题
        html.append("<div style='font-size: 12px; color: #666; margin-bottom: 5px;'>");
        html.append("📷 图片 ").append(index);
        if (fileName != null) {
            html.append(" - 来源: ").append(fileName);
        }
        if (pageNumber != null) {
            html.append(" 第").append(pageNumber).append("页");
        }
        html.append("</div>");

        // 图片（可点击放大）
        String encodedPath = imagePath;
        try {
            encodedPath = URLEncoder.encode(imagePath, StandardCharsets.UTF_8).replace("+", "%20");
        } catch (Exception e) {
            // 编码失败使用原路径
        }

        html.append("<img src='/api/image/view?path=").append(encodedPath).append("' ");
        html.append("alt='").append(description != null ? escapeHtml(description.substring(0, Math.min(50, description.length()))) : "图片").append("' ");
        html.append("style='max-width: 100%; max-height: 400px; border-radius: 8px; cursor: pointer; box-shadow: 0 2px 8px rgba(0,0,0,0.1);' ");
        html.append("onclick='window.open(this.src, \"_blank\")' ");
        html.append("title='点击查看大图' />");

        html.append("</div>\n\n");
        return html.toString();
    }

    /**
     * 构建工具执行结果HTML
     *
     * <p>将工具执行结果格式化为美观的HTML展示</p>
     *
     * @param toolName 工具名称
     * @param result 工具执行结果
     * @return 工具结果HTML字符串
     */
    public static String buildToolResultHtml(String toolName, ToolResult result) {
        if (result == null) {
            return "";
        }

        StringBuilder html = new StringBuilder();
        html.append("\n\n<div class='tool-result' style='margin: 15px 0; padding: 15px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius: 12px; color: white;'>");

        // 工具标题
        html.append("<div style='font-weight: bold; margin-bottom: 10px;'>");
        html.append("🔧 ").append(getToolDisplayName(toolName));
        html.append("</div>");

        // 工具结果
        html.append("<div style='background: rgba(255,255,255,0.1); padding: 10px; border-radius: 8px;'>");
        html.append(formatToolResult(result));
        html.append("</div>");

        html.append("</div>\n\n");
        return html.toString();
    }

    /**
     * 获取工具的显示名称
     *
     * @param toolName 工具标识名
     * @return 显示名称
     */
    private static String getToolDisplayName(String toolName) {
        if (toolName == null) {
            return "工具";
        }
        switch (toolName.toLowerCase()) {
            case "weather":
            case "get_weather":
                return "天气查询";
            case "time":
            case "get_time":
                return "时间查询";
            case "calculator":
                return "计算器";
            case "search":
            case "web_search":
                return "网络搜索";
            default:
                return toolName;
        }
    }

    /**
     * 格式化工具结果
     *
     * @param result 工具结果
     * @return 格式化后的字符串
     */
    private static String formatToolResult(ToolResult result) {
        if (result == null || result.getData() == null) {
            return "无结果";
        }
        // 简单返回结果的字符串表示
        return escapeHtml(result.getData().toString());
    }

    /**
     * HTML特殊字符转义
     *
     * @param str 原始字符串
     * @return 转义后的字符串
     */
    public static String escapeHtml(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * 构建错误提示HTML
     *
     * @param message 错误信息
     * @return 错误HTML字符串
     */
    public static String buildErrorHtml(String message) {
        return "<div style='color: #f56c6c; margin-top: 10px;'>" +
                "<i class='fa-solid fa-exclamation-triangle'></i> " +
                escapeHtml(message) + "</div>";
    }

    /**
     * 构建成功提示HTML
     *
     * @param message 成功信息
     * @return 成功HTML字符串
     */
    public static String buildSuccessHtml(String message) {
        return "<div style='color: #67c23a; margin-top: 10px;'>" +
                "<i class='fa-solid fa-check-circle'></i> " +
                escapeHtml(message) + "</div>";
    }
}
