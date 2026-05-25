package com.moyun.ext.flowable.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 流程任务视图对象
 *
 * @author Tony
 * @date 2021-04-03
 */
@Data
@Schema(description = "流程任务视图对象")
public class FlowTaskVo {

    @Schema(description = "任务ID")
    private String taskId;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "任务Key")
    private String taskDefKey;

    @Schema(description = "流程实例ID")
    private String instanceId;

    @Schema(description = "流程定义ID")
    private String definitionId;

    @Schema(description = "流程定义Key")
    private String definitionKey;

    @Schema(description = "流程定义名称")
    private String definitionName;

    @Schema(description = "流程定义版本")
    private Integer definitionVersion;

    @Schema(description = "部署ID")
    private String deploymentId;

    @Schema(description = "流程分类")
    private String category;

    @Schema(description = "流程发起人ID")
    private String startUserId;

    @Schema(description = "流程发起人名称")
    private String startUserName;

    @Schema(description = "任务创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @Schema(description = "任务结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    @Schema(description = "任务办理人")
    private String assignee;

    @Schema(description = "任务办理人名称")
    private String assigneeName;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "目标节点Key")
    private String targetKey;

    @Schema(description = "流程环节定义ID")
    private String defId;

    @Schema(description = "子执行流ID")
    private String currentChildExecutionId;

    @Schema(description = "子执行流是否已执行")
    private Boolean flag;

    @Schema(description = "流程变量")
    private Map<String, Object> variables;

    @Schema(description = "流程参数")
    private Map<String, Object> values;

    @Schema(description = "审批意见")
    private String comment;

    @Schema(description = "审批类型")
    private String type;

    @Schema(description = "流程状态")
    private Integer processStatus;

    @Schema(description = "流程状态名称")
    private String processStatusName;

    @Schema(description = "业务Key")
    private String businessKey;

    @Schema(description = "表单数据")
    private Map<String, Object> formData;

    @Schema(description = "候选人列表")
    private List<String> candidateUsers;

    @Schema(description = "审批组列表")
    private List<String> candidateGroups;
}