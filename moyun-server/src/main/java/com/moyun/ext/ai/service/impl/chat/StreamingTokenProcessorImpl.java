package com.moyun.ext.ai.service.impl.chat;

import com.moyun.ext.ai.service.chat.StreamingTokenProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 流式Token处理器实现类
 *
 * <p>负责处理流式对话中的Token，包括：
 * <ul>
 *   <li>过滤工具调用标记</li>
 *   <li>替换图片占位符</li>
 *   <li>缓冲区管理</li>
 * </ul>
 * </p>
 *
 * @author laomao
 * @since 2025-12-11
 */
@Slf4j
@Service
public class StreamingTokenProcessorImpl implements StreamingTokenProcessor {

    /** 图片占位符正则 */
    private static final Pattern IMAGE_PLACEHOLDER_PATTERN = Pattern.compile("\\[\\[IMAGE_(\\d+)\\]\\]");

    /** 工具调用开始标记 */
    private static final String TOOL_CALL_START = "[TOOL_CALL]";

    /** 工具调用结束标记 */
    private static final String TOOL_CALL_END = "[/TOOL_CALL]";

    /**
     * 处理流式Token
     *
     * @param context 处理上下文
     * @param token   新收到的Token
     * @return 处理后应该发送的内容（可能为空）
     */
    @Override
    public String processToken(ProcessContext context, String token) {
        StringBuilder buffer = context.getBuffer();
        buffer.append(token);
        String bufferContent = buffer.toString();

        // 1. 处理工具调用标记
        String afterToolFilter = filterToolCallMarkers(context, bufferContent);
        if (afterToolFilter == null) {
            return ""; // 等待更多内容
        }

        // 2. 处理图片占位符
        return processImagePlaceholders(context, afterToolFilter);
    }

    /**
     * 过滤工具调用标记
     *
     * @param context       处理上下文
     * @param bufferContent 缓冲区内容
     * @return 过滤后的内容，如果需要等待更多内容则返回null
     */
    private String filterToolCallMarkers(ProcessContext context, String bufferContent) {
        StringBuilder buffer = context.getBuffer();

        // 检测工具调用开始
        int possibleStart = bufferContent.indexOf("[");
        if (possibleStart >= 0 && bufferContent.substring(possibleStart).startsWith(TOOL_CALL_START)) {
            int toolCallStart = possibleStart;
            int toolCallEnd = bufferContent.indexOf(TOOL_CALL_END, toolCallStart);

            if (toolCallEnd < 0) {
                // 工具调用标记未完成，只输出标记之前的内容
                String beforeToolCall = "";
                if (toolCallStart > 0) {
                    beforeToolCall = bufferContent.substring(0, toolCallStart);
                }
                // 保留工具调用部分在缓冲区
                buffer.setLength(0);
                buffer.append(bufferContent.substring(toolCallStart));

                return beforeToolCall.isEmpty() ? null : beforeToolCall;
            } else {
                // 工具调用标记完整，移除整个工具调用标记
                String afterToolCall = bufferContent.substring(toolCallEnd + TOOL_CALL_END.length());
                String beforeToolCall = toolCallStart > 0 ? bufferContent.substring(0, toolCallStart) : "";

                // 清空缓冲区，保留工具调用之后的内容
                buffer.setLength(0);
                if (!afterToolCall.trim().isEmpty()) {
                    buffer.append(afterToolCall);
                }

                return beforeToolCall.isEmpty() ? null : beforeToolCall;
            }
        }

        // 检查是否可能是工具调用开始的一部分（排除图片占位符）
        if (bufferContent.contains("[") && !bufferContent.contains(TOOL_CALL_START)) {
            boolean isImagePlaceholder = bufferContent.contains("[[IMAGE_")
                    || Pattern.compile("\\[\\[IMAGE_\\d*\\]?\\]?$").matcher(bufferContent).find();

            if (!isImagePlaceholder) {
                // 检查是否可能是工具调用开始的一部分
                for (int i = 1; i < TOOL_CALL_START.length(); i++) {
                    if (bufferContent.endsWith(TOOL_CALL_START.substring(0, i))) {
                        int safeEnd = bufferContent.length() - i;
                        if (safeEnd > 0) {
                            String safeContent = bufferContent.substring(0, safeEnd);
                            buffer.setLength(0);
                            buffer.append(bufferContent.substring(safeEnd));
                            return safeContent;
                        } else {
                            return null; // 等待更多内容
                        }
                    }
                }
            }
        }

        // 清空缓冲区，返回全部内容
        buffer.setLength(0);
        return bufferContent;
    }

    /**
     * 处理图片占位符
     *
     * @param context       处理上下文
     * @param bufferContent 缓冲区内容
     * @return 处理后的内容
     */
    private String processImagePlaceholders(ProcessContext context, String bufferContent) {
        if (bufferContent == null || bufferContent.isEmpty()) {
            return "";
        }

        StringBuilder buffer = context.getBuffer();
        Matcher matcher = IMAGE_PLACEHOLDER_PATTERN.matcher(bufferContent);
        StringBuilder outputBuffer = new StringBuilder();

        int lastEnd = 0;

        while (matcher.find()) {
            int imageIndex = Integer.parseInt(matcher.group(1));

            // 输出占位符之前的内容
            if (matcher.start() > lastEnd) {
                outputBuffer.append(bufferContent.substring(lastEnd, matcher.start()));
            }

            // 替换占位符
            if (!context.getReplacedImageIndexes().contains(imageIndex)
                    && context.getImageHtmlMap() != null
                    && context.getImageHtmlMap().containsKey(imageIndex)) {
                // 第一次出现，替换为图片HTML
                outputBuffer.append(context.getImageHtmlMap().get(imageIndex));
                context.getReplacedImageIndexes().add(imageIndex);
                log.info("✅ 流式替换图片占位符: [[IMAGE_{}]] -> 图片HTML", imageIndex);
            } else if (context.getReplacedImageIndexes().contains(imageIndex)) {
                // 重复出现，替换为文字引用
                outputBuffer.append("（见上图 ").append(imageIndex).append("）");
            } else {
                // 没有对应的图片，保留原占位符
                outputBuffer.append(matcher.group());
            }

            lastEnd = matcher.end();
        }

        // 检查是否有未完成的占位符
        int incompleteStart = bufferContent.lastIndexOf("[[IMAGE_");
        if (incompleteStart >= lastEnd && !bufferContent.substring(incompleteStart).contains("]]")) {
            // 有未完成的占位符，保留在缓冲区
            if (incompleteStart > lastEnd) {
                outputBuffer.append(bufferContent.substring(lastEnd, incompleteStart));
            }
            buffer.append(bufferContent.substring(incompleteStart));
        } else {
            // 没有未完成的占位符，输出剩余内容
            if (lastEnd < bufferContent.length()) {
                outputBuffer.append(bufferContent.substring(lastEnd));
            }
        }

        return outputBuffer.toString();
    }

    /**
     * 获取缓冲区中剩余的内容
     *
     * @param context 处理上下文
     * @return 剩余内容（已过滤工具调用标记）
     */
    @Override
    public String flushBuffer(ProcessContext context) {
        StringBuilder buffer = context.getBuffer();
        if (buffer.length() == 0) {
            return "";
        }

        String remaining = buffer.toString();
        buffer.setLength(0);

        // 过滤掉工具调用标记
        remaining = Pattern.compile("\\[TOOL_CALL\\].*?\\[/TOOL_CALL\\]", Pattern.DOTALL)
                .matcher(remaining).replaceAll("");

        return remaining;
    }
}
