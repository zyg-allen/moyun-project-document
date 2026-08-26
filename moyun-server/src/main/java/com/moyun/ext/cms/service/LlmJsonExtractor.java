package com.moyun.ext.cms.service;

/**
 * LLM 返回 JSON 提取工具（v10.13）
 * <p>剥离 markdown 代码块围栏并截取 JSON 本体，
 * 与 ResumeAiAdviceService / ResumeParseService 中同策略的集中实现。</p>
 *
 * @author moyun
 */
final class LlmJsonExtractor {

    private LlmJsonExtractor() {
    }

    /**
     * 提取 LLM 响应中的 JSON 本体：
     * 1) 剥离 ```json / ``` 围栏；2) 截取首个 { 到最后一个 }（兼容前后说明文字）。
     */
    static String extract(String llmResponse) {
        if (llmResponse == null) {
            return "";
        }
        String s = llmResponse.trim();
        if (s.startsWith("```")) {
            int firstLineEnd = s.indexOf('\n');
            if (firstLineEnd > 0) {
                s = s.substring(firstLineEnd + 1).trim();
            }
            int fenceEnd = s.lastIndexOf("```");
            if (fenceEnd >= 0) {
                s = s.substring(0, fenceEnd).trim();
            }
        }
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return s.substring(start, end + 1);
        }
        return s;
    }
}
