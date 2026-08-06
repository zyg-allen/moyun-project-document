package com.moyun.ext.ai.service.chat;

import java.util.Map;
import java.util.Set;

/**
 * 流式Token处理器接口
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
public interface StreamingTokenProcessor {

    /**
     * 处理流式Token
     *
     * @param context 处理上下文
     * @param token   新收到的Token
     * @return 处理后应该发送的内容（可能为空）
     */
    String processToken(ProcessContext context, String token);

    /**
     * 获取缓冲区中剩余的内容
     *
     * @param context 处理上下文
     * @return 剩余内容
     */
    String flushBuffer(ProcessContext context);

    /**
     * 处理上下文
     */
    class ProcessContext {
        /** 流式缓冲区 */
        private final StringBuilder buffer = new StringBuilder();
        
        /** 已替换的图片索引 */
        private final Set<Integer> replacedImageIndexes = new java.util.HashSet<>();
        
        /** 图片HTML映射 */
        private Map<Integer, String> imageHtmlMap;

        public StringBuilder getBuffer() {
            return buffer;
        }

        public Set<Integer> getReplacedImageIndexes() {
            return replacedImageIndexes;
        }

        public Map<Integer, String> getImageHtmlMap() {
            return imageHtmlMap;
        }

        public void setImageHtmlMap(Map<Integer, String> imageHtmlMap) {
            this.imageHtmlMap = imageHtmlMap;
        }
    }
}
