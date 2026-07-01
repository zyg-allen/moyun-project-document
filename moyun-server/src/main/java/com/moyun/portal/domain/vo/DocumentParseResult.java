package com.moyun.portal.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 文档解析结果
 *
 * @author moyun
 */
@Data
@Schema(description = "文档解析结果")
public class DocumentParseResult {

    /** 自动识别的标题（取首个标题或文件名） */
    @Schema(description = "标题")
    private String title;

    /** 自动识别的作者（从 docx 元信息抽取） */
    @Schema(description = "作者")
    private String author;

    /** 文档总字数 */
    @Schema(description = "总字数")
    private Integer totalWordCount;

    /** 源文件格式：txt/markdown/docx/pdf */
    @Schema(description = "源文件格式")
    private String sourceFormat;

    /** 章节列表 */
    @Schema(description = "章节列表")
    private List<ParsedChapter> chapters;

    /** 解析是否成功 */
    @Schema(description = "是否成功")
    private Boolean success;

    /** 错误信息 */
    @Schema(description = "错误信息")
    private String errorMsg;

    /**
     * 解析成功构造
     */
    public static DocumentParseResult ok(String title, String author, String sourceFormat, List<ParsedChapter> chapters) {
        DocumentParseResult r = new DocumentParseResult();
        r.setTitle(title);
        r.setAuthor(author);
        r.setSourceFormat(sourceFormat);
        r.setChapters(chapters);
        r.setSuccess(true);
        int total = 0;
        if (chapters != null) {
            for (ParsedChapter c : chapters) {
                if (c.getWordCount() != null) total += c.getWordCount();
            }
        }
        r.setTotalWordCount(total);
        return r;
    }

    /**
     * 解析失败构造
     */
    public static DocumentParseResult fail(String sourceFormat, String errorMsg) {
        DocumentParseResult r = new DocumentParseResult();
        r.setSourceFormat(sourceFormat);
        r.setSuccess(false);
        r.setErrorMsg(errorMsg);
        return r;
    }
}
