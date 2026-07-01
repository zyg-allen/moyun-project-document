package com.moyun.portal.domain.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import com.moyun.core.base.page.PageDomain;

/**
 * 书架查询对象
 *
 * @author moyun
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "书架查询对象")
public class BookshelfQuery extends PageDomain {

    /** 用户ID */
    @Schema(description = "用户ID", example = "1")
    private Long userId;

    /** 书籍ID */
    @Schema(description = "书籍ID", example = "1")
    private Long bookId;

    /** 书名（模糊查询） */
    @Schema(description = "书名（模糊）", example = "三体")
    private String bookTitle;

    /** 排序方式：latest=按收藏时间 / recent=按最近阅读 / sort=自定义排序 */
    @Schema(description = "排序方式", example = "latest")
    private String orderBy;
}
