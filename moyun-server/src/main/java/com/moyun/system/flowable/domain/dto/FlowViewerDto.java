
// ... existing code ...
package com.moyun.system.flowable.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 流程查看传输对象
 *
 * @author Tony
 * @date 2021-04-03
 */
@Data
@Schema(description = "流程查看传输对象")
public class FlowViewerDto implements Serializable {

    @Schema(description = "流程key")
    private String key;

    @Schema(description = "是否完成(已经审批)")
    private boolean completed;
}
// ... existing code ...
