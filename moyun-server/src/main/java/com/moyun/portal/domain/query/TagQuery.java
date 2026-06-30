package com.moyun.portal.domain.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import com.moyun.core.base.page.PageDomain;

/**
 * 标签查询对象
 *
 * @author moyun
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "标签查询对象")
public class TagQuery extends PageDomain {

    /**
     * 标签名称（模糊查询）
     */
    @Schema(description = "标签名称", example = "Java")
    private String name;

    /**
     * 标签状态
     */
    @Schema(description = "标签状态", example = "0")
    private String status;

    /**
     * 开始时间
     */
    @Schema(description = "开始时间", example = "2024-01-01")
    private String startTime;

    /**
     * 结束时间
     */
    @Schema(description = "结束时间", example = "2024-12-31")
    private String endTime;

    /**
     * 所属模块（article/book/book_list/book_quote/interview_question/interview_experience/interview_resume_template/comment/common）
     */
    @Schema(description = "所属模块", example = "article")
    private String module;

    /**
     * 分类ID（用于标签推荐：筛选该分类下文章使用过的标签）
     */
    @Schema(description = "分类ID", example = "1")
    private Long categoryId;
}
