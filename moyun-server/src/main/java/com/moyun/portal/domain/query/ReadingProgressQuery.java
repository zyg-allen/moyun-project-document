package com.moyun.portal.domain.query;

import com.moyun.core.base.page.PageDomain;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 阅读进度查询对象
 *
 * @author moyun
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "阅读进度查询对象")
public class ReadingProgressQuery extends PageDomain
{
    /** 用户ID */
    @Schema(description = "用户ID", example = "1")
    private Long userId;

    /** 书籍ID */
    @Schema(description = "书籍ID", example = "1")
    private Long bookId;

    /** 阅读状态: want_to_read, reading, finished */
    @Schema(description = "阅读状态", example = "reading")
    private String status;
}
