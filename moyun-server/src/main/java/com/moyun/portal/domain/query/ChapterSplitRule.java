package com.moyun.portal.domain.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 章节分章规则
 *
 * @author moyun
 */
@Data
@Schema(description = "章节分章规则")
public class ChapterSplitRule {

    /**
     * 分章模式：
     * auto = 自动识别（默认，依次尝试中文章节正则→英文章节→Markdown标题）
     * regex = 自定义正则
     * heading = 按标题级别（h1/h2/h3）
     * fixed = 按固定字数切段（每 N 字一章）
     */
    @Schema(description = "分章模式：auto/regex/heading/fixed", example = "auto")
    private String mode = "auto";

    /** 自定义正则（mode=regex 时生效） */
    @Schema(description = "自定义正则")
    private String regex;

    /** 标题级别（mode=heading 时生效，如 "h2"） */
    @Schema(description = "标题级别")
    private String headingLevel;

    /** 固定字数（mode=fixed 时生效，默认 3000） */
    @Schema(description = "固定字数")
    private Integer fixedWordCount = 3000;

    /** 是否识别分卷（如"第一部"） */
    @Schema(description = "是否识别分卷")
    private Boolean detectVolume = true;

    /** 最小章节字数（低于此数合并到上一章，默认 100） */
    @Schema(description = "最小章节字数")
    private Integer minChapterWords = 100;
}
