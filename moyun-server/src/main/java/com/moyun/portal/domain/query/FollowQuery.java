package com.moyun.portal.domain.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import com.moyun.core.base.page.PageDomain;

/**
 * 关注查询对象
 *
 * @author moyun
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "关注查询对象")
public class FollowQuery extends PageDomain {

    /**
     * 关注者ID
     */
    @Schema(description = "关注者ID", example = "1")
    private Long followerId;

    /**
     * 被关注者ID
     */
    @Schema(description = "被关注者ID", example = "2")
    private Long followingId;

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
}
