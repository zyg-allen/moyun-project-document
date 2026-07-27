package com.moyun.agent.util;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * 文本处理工具类
 *
 * <p>提供文本分析、关键词提取、停用词过滤等通用功能</p>
 *
 * @author laomao
 */
public final class TextProcessingUtils {

    private TextProcessingUtils() {
        // 工具类禁止实例化
    }

    /**
     * 中文停用词集合
     *
     * <p>包含常见的中文虚词、代词、疑问词等，用于关键词提取时过滤</p>
     */
    public static final Set<String> STOP_WORDS = Set.of(
            "的", "了", "是", "在", "有", "和", "就", "不", "人", "都", "一", "个", "上", "也", "很", "到",
            "说", "要", "去", "你", "会", "着", "没有", "看", "好", "自己", "这", "那", "什么", "哪些",
            "怎么", "如何", "为什么", "吗", "呢", "啊", "吧", "相关", "内容", "关于", "介绍", "说明",
            "详细", "具体", "请问", "告诉", "帮我"
    );

    /**
     * 无意义词集合
     *
     * <p>包含一些分词错误产生的无意义片段</p>
     */
    public static final Set<String> MEANINGLESS_WORDS = Set.of(
            "ra", "ag", "g相", "关内", "相关内", "关内容"
    );

    /**
     * 英文字符匹配正则（预编译）
     */
    public static final Pattern ENGLISH_PATTERN = Pattern.compile("[a-zA-Z]+");

    /**
     * 中英文标点符号匹配正则（预编译）
     */
    public static final Pattern PUNCTUATION_PATTERN = Pattern.compile("[？?！!，,。.；;：:、]");

    /**
     * 图片占位符匹配正则（预编译）
     *
     * <p>匹配格式：[[IMAGE_1]]、[[IMAGE_2]] 等</p>
     */
    public static final Pattern IMAGE_PLACEHOLDER_PATTERN = Pattern.compile("\\[\\[IMAGE_(\\d+)\\]\\]");

    /**
     * 工具调用标记匹配正则（预编译）
     *
     * <p>匹配格式：[TOOL_CALL]...[/TOOL_CALL]</p>
     */
    public static final Pattern TOOL_CALL_PATTERN = Pattern.compile("\\[TOOL_CALL\\].*?\\[/TOOL_CALL\\]", Pattern.DOTALL);

    /**
     * 判断词语是否为停用词
     *
     * @param word 待检查的词语
     * @return 是否为停用词
     */
    public static boolean isStopWord(String word) {
        return word == null || word.length() < 2 || STOP_WORDS.contains(word);
    }

    /**
     * 判断词语是否为无意义词
     *
     * @param word 待检查的词语
     * @return 是否为无意义词
     */
    public static boolean isMeaninglessWord(String word) {
        return word == null || MEANINGLESS_WORDS.contains(word);
    }

    /**
     * 判断词语是否有效（非停用词且非无意义词）
     *
     * @param word 待检查的词语
     * @return 是否为有效词语
     */
    public static boolean isValidKeyword(String word) {
        return word != null && word.length() >= 2
                && !STOP_WORDS.contains(word)
                && !MEANINGLESS_WORDS.contains(word);
    }

    /**
     * 移除文本中的标点符号
     *
     * @param text 原始文本
     * @return 移除标点后的文本
     */
    public static String removePunctuation(String text) {
        if (text == null) {
            return "";
        }
        return PUNCTUATION_PATTERN.matcher(text).replaceAll(" ").trim();
    }

    /**
     * 统计子字符串在文本中出现的次数
     *
     * @param text 文本
     * @param substring 子字符串
     * @return 出现次数
     */
    public static int countOccurrences(String text, String substring) {
        if (text == null || substring == null || substring.isEmpty()) {
            return 0;
        }
        int count = 0;
        int index = 0;
        String lowerText = text.toLowerCase();
        String lowerSub = substring.toLowerCase();
        while ((index = lowerText.indexOf(lowerSub, index)) != -1) {
            count++;
            index += lowerSub.length();
        }
        return count;
    }

    /**
     * 判断文本是否为纯问题片段（不包含实质内容）
     *
     * @param text 文本内容
     * @return 是否为纯问题片段
     */
    public static boolean isPureQuestionSegment(String text) {
        if (text == null || text.length() < 20) {
            return true;
        }
        // 问题特征词
        String[] questionIndicators = {"什么", "如何", "怎么", "为什么", "哪些", "是否", "能否", "？", "?"};
        int questionCount = 0;
        for (String indicator : questionIndicators) {
            if (text.contains(indicator)) {
                questionCount++;
            }
        }
        // 如果问题特征词过多，且文本较短，认为是纯问题
        return questionCount >= 2 && text.length() < 100;
    }

    /**
     * 判断文本是否为目录内容
     *
     * @param text 文本内容
     * @return 是否为目录
     */
    public static boolean isTableOfContents(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        // 目录特征1：包含大量页码引用（如 "....... 12"）
        int pageRefCount = 0;
        String[] lines = text.split("\n");
        for (String line : lines) {
            if (line.matches(".*\\.{3,}\\s*\\d+\\s*$") || line.matches(".*\\s+\\d+\\s*$")) {
                pageRefCount++;
            }
        }
        if (pageRefCount >= 3) {
            return true;
        }

        // 目录特征2：包含"目录"关键词且有多个章节编号
        if (text.contains("目录") || text.contains("CONTENTS") || text.contains("Contents")) {
            int chapterCount = 0;
            for (String line : lines) {
                if (line.matches("^\\s*(第[一二三四五六七八九十]+章|\\d+\\.\\d*|[一二三四五六七八九十]+、).*")) {
                    chapterCount++;
                }
            }
            if (chapterCount >= 3) {
                return true;
            }
        }

        return false;
    }
}
