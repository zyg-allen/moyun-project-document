
// ... existing code ...
package com.moyun.ext.flowable.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 流程查询视图对象
 *
 * @author Tony
 * @date 2021-04-03
 */
@Data
@Schema(description = "工作流任务相关--请求参数")
public class FlowQueryVo {

    @Schema(description = "流程名称")
    private String name;

    @Schema(description = "开始时间")
    private String startTime;

    @Schema(description = "结束时间")
    private String endTime;

    @Schema(description = "当前页码")
    private Integer pageNum;

    @Schema(description = "每页条数")
    private Integer pageSize;

}
// ... existing code ...
