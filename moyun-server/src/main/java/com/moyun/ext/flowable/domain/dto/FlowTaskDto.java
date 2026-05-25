
// ... existing code ...
package com.moyun.ext.flowable.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 流程任务传输对象
 *
 * @author Tony
 * @date 2021-04-03
 */
@Data
@Schema(description = "工作流任务相关-返回参数")
public class FlowTaskDto implements Serializable {

    @Schema(description = "任务编号")
    private String taskId;

    @Schema(description = "任务执行编号")
    private String executionId;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "任务Key")
    private String taskDefKey;

    @Schema(description = "任务执行人Id")
    private Long assigneeId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "流程发起人部门名称")
    private String startDeptName;

    @Schema(description = "任务执行人名称")
    private String assigneeName;

    @Schema(description = "任务执行人部门")
    private String assigneeDeptName;

    @Schema(description = "流程发起人Id")
    private String startUserId;

    @Schema(description = "流程发起人名称")
    private String startUserName;

    @Schema(description = "流程类型")
    private String category;

    @Schema(description = "流程变量信息")
    private Object variables;

    @Schema(description = "局部变量信息")
    private Object taskLocalVars;

    @Schema(description = "流程部署编号")
    private String deployId;

    @Schema(description = "流程ID")
    private String procDefId;

    @Schema(description = "流程key")
    private String procDefKey;

    @Schema(description = "流程定义名称")
    private String procDefName;

    @Schema(description = "流程定义内置使用版本")
    private int procDefVersion;

    @Schema(description = "流程实例ID")
    private String procInsId;

    @Schema(description = "历史流程实例ID")
    private String hisProcInsId;

    @Schema(description = "任务耗时")
    private String duration;

    @Schema(description = "任务意见")
    private FlowCommentDto comment;

    @Schema(description = "候选执行人")
    private String candidate;

    @Schema(description = "任务创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @Schema(description = "任务完成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date finishTime;
}
// ... existing code ...

