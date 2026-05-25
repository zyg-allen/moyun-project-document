
// ... existing code ...
package com.moyun.ext.flowable.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 流程下一节点传输对象
 *
 * @author Tony
 * @date 2021-04-03
 */
@Data
@Schema(description = "流程下一节点传输对象")
public class FlowNextDto implements Serializable {

    @Schema(description = "审批人类型")
    private String type;

    @Schema(description = "是否需要动态指定任务审批人")
    private String dataType;

    @Schema(description = "流程变量")
    private String vars;

    @Schema(description = "节点ID")
    private String nodeId;

    @Schema(description = "节点名称")
    private String nodeName;

    @Schema(description = "节点类型")
    private String nodeType;

    @Schema(description = "节点类型名称")
    private String nodeTypeName;

    @Schema(description = "节点变量")
    private Object nodeVariable;

    @Schema(description = "节点用户列表")
    private List<String> nodeUserList;

    @Schema(description = "节点组列表")
    private List<String> nodeGroupList;

    @Schema(description = "节点字段列表")
    private List<FlowFromFieldDTO> formFieldList;

    @Schema(description = "是否多实例")
    private Boolean multiInstance;

    @Schema(description = "会签类型")
    private String multiType;
}
// ... existing code ...
