package com.moyun.portal.domain.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import com.moyun.core.base.page.PageDomain;

/**
 * 友情链接查询对象
 *
 * @author moyun
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "友情链接查询对象")
public class FriendLinkQuery extends PageDomain {

    /**
     * 链接名称（模糊查询）
     */
    @Schema(description = "链接名称", example = "百度")
    private String name;

    /**
     * 链接状态
     */
    @Schema(description = "链接状态", example = "0")
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
}
