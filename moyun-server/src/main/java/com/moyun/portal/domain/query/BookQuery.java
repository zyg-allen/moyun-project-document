package com.moyun.portal.domain.query;

import com.moyun.core.base.page.PageDomain;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 书籍查询对象
 *
 * @author moyun
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "书籍查询对象")
public class BookQuery extends PageDomain
{
    /** 书名（模糊查询） */
    @Schema(description = "书名", example = "代码整洁")
    private String title;

    /** 作者（模糊查询） */
    @Schema(description = "作者", example = "Robert")
    private String author;

    /** ISBN */
    @Schema(description = "ISBN", example = "9787115217685")
    private String isbn;

    /** 分类ID */
    @Schema(description = "分类ID", example = "1")
    private Long categoryId;

    /** 状态 */
    @Schema(description = "状态", example = "active")
    private String status;

    /** 访问级别: free, vip, preview */
    @Schema(description = "访问级别", example = "free")
    private String accessLevel;

    /** 是否精选 */
    @Schema(description = "是否精选", example = "true")
    private Boolean isFeatured;

    /** 标签（逗号分隔，模糊匹配） */
    @Schema(description = "标签", example = "编程")
    private String tag;

    /** 开始时间 */
    @Schema(description = "开始时间", example = "2024-01-01")
    private String startTime;

    /** 结束时间 */
    @Schema(description = "结束时间", example = "2024-12-31")
    private String endTime;
}
