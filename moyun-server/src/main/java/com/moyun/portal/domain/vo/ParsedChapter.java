package com.moyun.portal.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 解析出的章节
 *
 * @author moyun
 */
@Data
@Schema(description = "解析出的章节")
public class ParsedChapter {

    /** 章节序号（从1开始） */
    @Schema(description = "章节序号")
    private Integer chapterNo;

    /** 章节标题 */
    @Schema(description = "章节标题")
    private String title;

    /** 章节正文（Markdown 格式） */
    @Schema(description = "章节正文")
    private String content;

    /** 章节字数 */
    @Schema(description = "字数")
    private Integer wordCount;

    /** 是否疑似卷名（如"第一部"） */
    @Schema(description = "是否分卷")
    private Boolean isVolume;

    public ParsedChapter() {
    }

    public ParsedChapter(Integer chapterNo, String title, String content) {
        this.chapterNo = chapterNo;
        this.title = title;
        this.content = content;
        this.wordCount = countWords(content);
        this.isVolume = false;
    }

    /**
     * 字数统计（中文按字符计，去 HTML 标签与空白）
     */
    public static Integer countWords(String text) {
        if (text == null || text.isEmpty()) return 0;
        // 去除 HTML 标签
        text = text.replaceAll("<[^>]+>", "");
        // 去除 Markdown 标题前缀
        text = text.replaceAll("^#+\\s*", "");
        // 去除空白
        text = text.replaceAll("\\s+", "");
        return text.length();
    }
}
