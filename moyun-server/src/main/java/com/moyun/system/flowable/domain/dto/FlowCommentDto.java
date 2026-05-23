
package com.moyun.system.flowable.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流程评论dto
 *
 * @author Tony
 * @date 2023-08-22
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlowCommentDto {
    /**
     * 评论类型
     */
    private String type;

    /**
     * 评论内容
     */
    private String comment;
}
