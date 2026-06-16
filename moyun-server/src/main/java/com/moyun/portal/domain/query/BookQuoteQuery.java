package com.moyun.portal.domain.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import com.moyun.core.base.page.PageDomain;

/**
 * 金句摘录查询对象
 *
 * @author moyun
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "金句摘录查询对象")
public class BookQuoteQuery extends PageDomain
{
    /** 用户ID */
    @Schema(description = "用户ID", example = "1")
    private Long userId;

    /** 书籍ID */
    @Schema(description = "书籍ID", example = "1")
    private Long bookId;

    /** 金句内容（模糊查询） */
    @Schema(description = "金句内容", example = "代码")
    private String content;

    /** 是否公开 */
    @Schema(description = "是否公开", example = "true")
    private Boolean isPublic;

    /** 是否精选 */
    @Schema(description = "是否精选", example = "true")
    private Boolean isFeatured;
}
