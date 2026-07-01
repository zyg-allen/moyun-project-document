package com.moyun.portal.domain.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import com.moyun.core.base.page.PageDomain;

/**
 * 书籍章节查询对象
 *
 * @author moyun
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "书籍章节查询对象")
public class BookChapterQuery extends PageDomain {

    /** 所属书籍ID（必填） */
    @Schema(description = "书籍ID", example = "1")
    private Long bookId;

    /** 章节标题（模糊查询） */
    @Schema(description = "章节标题", example = "第一章")
    private String title;

    /** 是否已发布：true=已发布，false=草稿，null=全部 */
    @Schema(description = "是否已发布", example = "true")
    private Boolean isPublished;

    /** 是否免费：true=免费，false=VIP，null=全部 */
    @Schema(description = "是否免费", example = "true")
    private Boolean isFree;

    /** 是否包含正文（前台目录不需要，后台列表可选） */
    @Schema(description = "是否包含正文", example = "false")
    private Boolean withContent;
}
