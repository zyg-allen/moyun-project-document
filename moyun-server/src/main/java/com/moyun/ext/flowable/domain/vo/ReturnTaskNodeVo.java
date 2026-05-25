package com.moyun.ext.flowable.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 流程可退回节点视图对象
 *
 * @author Tony
 * @date 2021-04-03
 */
@Data
@Schema(description = "流程可退回节点视图对象")
public class ReturnTaskNodeVo {

    @Schema(description = "节点ID")
    private String nodeId;

    @Schema(description = "节点名称")
    private String nodeName;

    @Schema(description = "节点Key")
    private String nodeKey;
}