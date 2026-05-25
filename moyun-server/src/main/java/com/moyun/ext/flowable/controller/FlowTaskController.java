package com.moyun.ext.flowable.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.ext.flowable.domain.vo.FlowQueryVo;
import com.moyun.ext.flowable.domain.vo.FlowTaskVo;
import com.moyun.ext.flowable.service.IFlowTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 流程任务管理Controller
 *
 * @author Tony
 * @date 2021-04-03
 */
@Slf4j
@Tag(name = "流程任务管理", description = "流程任务的查询、审批、转办、委派等操作接口")
@RestController
@RequestMapping("/flowable/task")
public class FlowTaskController {

    @Autowired
    private IFlowTaskService flowTaskService;

    /**
     * 我发起的流程
     */
    @Operation(summary = "我发起的流程", description = "获取当前用户发起的所有流程实例")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取我发起的流程列表",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AjaxResult.class)))
    })
    @GetMapping(value = "/myProcess")
    public AjaxResult myProcess(
        @Parameter(description = "查询参数") FlowQueryVo queryVo
    ) {
        return flowTaskService.myProcess(queryVo);
    }

    /**
     * 取消申请
     */
    @Operation(summary = "取消申请", description = "取消流程申请")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功取消申请",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AjaxResult.class)))
    })
    @PostMapping(value = "/stopProcess")
    public AjaxResult stopProcess(
        @Parameter(description = "流程任务信息", required = true) @RequestBody FlowTaskVo flowTaskVo
    ) {
        return flowTaskService.stopProcess(flowTaskVo);
    }

    /**
     * 撤回流程
     */
    @Operation(summary = "撤回流程", description = "撤回已提交的流程")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功撤回流程",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AjaxResult.class)))
    })
    @PostMapping(value = "/revokeProcess")
    public AjaxResult revokeProcess(
        @Parameter(description = "流程任务信息", required = true) @RequestBody FlowTaskVo flowTaskVo
    ) {
        return flowTaskService.revokeProcess(flowTaskVo);
    }

    /**
     * 获取待办列表
     */
    @Operation(summary = "获取待办列表", description = "获取当前用户的待办任务列表")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取待办列表",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AjaxResult.class)))
    })
    @GetMapping(value = "/todoList")
    public AjaxResult todoList(
        @Parameter(description = "查询参数") FlowQueryVo queryVo
    ) {
        return flowTaskService.todoList(queryVo);
    }

    /**
     * 获取已办任务
     */
    @Operation(summary = "获取已办任务", description = "获取当前用户的已办任务列表")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取已办任务列表",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AjaxResult.class)))
    })
    @GetMapping(value = "/finishedList")
    public AjaxResult finishedList(
        @Parameter(description = "查询参数") FlowQueryVo queryVo
    ) {
        return flowTaskService.finishedList(queryVo);
    }

    /**
     * 流程历史流转记录
     */
    @Operation(summary = "流程历史流转记录", description = "获取流程实例的历史流转记录")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取流转记录",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AjaxResult.class)))
    })
    @GetMapping(value = "/flowRecord")
    public AjaxResult flowRecord(
        @Parameter(description = "流程实例ID") @RequestParam String procInsId,
        @Parameter(description = "部署ID") @RequestParam String deployId
    ) {
        return flowTaskService.flowRecord(procInsId, deployId);
    }

    /**
     * 根据任务ID查询挂载的表单信息
     */
    @Operation(summary = "查询任务表单", description = "根据任务ID查询挂载的表单信息")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取任务表单",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AjaxResult.class)))
    })
    @GetMapping(value = "/getTaskForm")
    public AjaxResult getTaskForm(
        @Parameter(description = "任务ID") @RequestParam String taskId
    ) {
        return flowTaskService.getTaskForm(taskId);
    }

    /**
     * 流程初始化表单
     */
    @Operation(summary = "流程初始化表单", description = "获取流程初始化表单数据")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取初始化表单",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AjaxResult.class)))
    })
    @GetMapping(value = "/flowFormData")
    public AjaxResult flowFormData(
        @Parameter(description = "部署ID") @RequestParam String deployId
    ) {
        return flowTaskService.flowFormData(deployId);
    }

    /**
     * 获取流程变量
     */
    @Operation(summary = "获取流程变量", description = "根据任务ID获取流程变量")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取流程变量",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AjaxResult.class)))
    })
    @GetMapping(value = "/processVariables/{taskId}")
    public AjaxResult processVariables(
        @Parameter(description = "任务ID", required = true) @PathVariable(value = "taskId") String taskId
    ) {
        return flowTaskService.processVariables(taskId);
    }

    /**
     * 审批任务
     */
    @Operation(summary = "审批任务", description = "审批通过任务")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功审批任务",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AjaxResult.class)))
    })
    @PostMapping(value = "/complete")
    public AjaxResult complete(
        @Parameter(description = "流程任务信息", required = true) @RequestBody FlowTaskVo flowTaskVo
    ) {
        return flowTaskService.complete(flowTaskVo);
    }

    /**
     * 驳回任务
     */
    @Operation(summary = "驳回任务", description = "驳回任务到上一节点")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功驳回任务",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AjaxResult.class)))
    })
    @PostMapping(value = "/reject")
    public AjaxResult taskReject(
        @Parameter(description = "流程任务信息", required = true) @RequestBody FlowTaskVo flowTaskVo
    ) {
        flowTaskService.taskReject(flowTaskVo);
        return AjaxResult.success();
    }

    /**
     * 退回任务
     */
    @Operation(summary = "退回任务", description = "退回任务到指定节点")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功退回任务",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AjaxResult.class)))
    })
    @PostMapping(value = "/return")
    public AjaxResult taskReturn(
        @Parameter(description = "流程任务信息", required = true) @RequestBody FlowTaskVo flowTaskVo
    ) {
        flowTaskService.taskReturn(flowTaskVo);
        return AjaxResult.success();
    }

    /**
     * 获取所有可回退的节点
     */
    @Operation(summary = "获取可回退节点", description = "获取所有可回退的节点列表")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取可回退节点",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AjaxResult.class)))
    })
    @PostMapping(value = "/returnList")
    public AjaxResult findReturnTaskList(
        @Parameter(description = "流程任务信息", required = true) @RequestBody FlowTaskVo flowTaskVo
    ) {
        return flowTaskService.findReturnTaskList(flowTaskVo);
    }

    /**
     * 删除任务
     */
    @Operation(summary = "删除任务", description = "删除指定的流程任务")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功删除任务",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AjaxResult.class)))
    })
    @DeleteMapping(value = "/delete")
    public AjaxResult delete(
        @Parameter(description = "流程任务信息", required = true) @RequestBody FlowTaskVo flowTaskVo
    ) {
        flowTaskService.deleteTask(flowTaskVo);
        return AjaxResult.success();
    }

    /**
     * 认领/签收任务
     */
    @Operation(summary = "认领任务", description = "认领/签收任务")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功认领任务",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AjaxResult.class)))
    })
    @PostMapping(value = "/claim")
    public AjaxResult claim(
        @Parameter(description = "流程任务信息", required = true) @RequestBody FlowTaskVo flowTaskVo
    ) {
        flowTaskService.claim(flowTaskVo);
        return AjaxResult.success();
    }

    /**
     * 取消认领/签收任务
     */
    @Operation(summary = "取消认领任务", description = "取消认领/签收任务")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功取消认领任务",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AjaxResult.class)))
    })
    @PostMapping(value = "/unClaim")
    public AjaxResult unClaim(
        @Parameter(description = "流程任务信息", required = true) @RequestBody FlowTaskVo flowTaskVo
    ) {
        flowTaskService.unClaim(flowTaskVo);
        return AjaxResult.success();
    }

    /**
     * 委派任务
     */
    @Operation(summary = "委派任务", description = "委派任务给其他人")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功委派任务",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AjaxResult.class)))
    })
    @PostMapping(value = "/delegateTask")
    public AjaxResult delegate(
        @Parameter(description = "流程任务信息", required = true) @RequestBody FlowTaskVo flowTaskVo
    ) {
        flowTaskService.delegateTask(flowTaskVo);
        return AjaxResult.success();
    }

    /**
     * 任务归还
     */
    @Operation(summary = "任务归还", description = "归还委派的任务")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功归还任务",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AjaxResult.class)))
    })
    @PostMapping(value = "/resolveTask")
    public AjaxResult resolveTask(
        @Parameter(description = "流程任务信息", required = true) @RequestBody FlowTaskVo flowTaskVo
    ) {
        flowTaskService.resolveTask(flowTaskVo);
        return AjaxResult.success();
    }

    /**
     * 转办任务
     */
    @Operation(summary = "转办任务", description = "转办任务给其他人")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功转办任务",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AjaxResult.class)))
    })
    @PostMapping(value = "/assignTask")
    public AjaxResult assign(
        @Parameter(description = "流程任务信息", required = true) @RequestBody FlowTaskVo flowTaskVo
    ) {
        flowTaskService.assignTask(flowTaskVo);
        return AjaxResult.success();
    }

    /**
     * 多实例加签
     */
    @Operation(summary = "多实例加签", description = "为多实例任务添加执行人")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功加签",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AjaxResult.class)))
    })
    @PostMapping(value = "/addMultiInstanceExecution")
    public AjaxResult addMultiInstanceExecution(
        @Parameter(description = "流程任务信息", required = true) @RequestBody FlowTaskVo flowTaskVo
    ) {
        flowTaskService.addMultiInstanceExecution(flowTaskVo);
        return AjaxResult.success("加签成功");
    }

    /**
     * 多实例减签
     */
    @Operation(summary = "多实例减签", description = "为多实例任务减少执行人")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功减签",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AjaxResult.class)))
    })
    @PostMapping(value = "/deleteMultiInstanceExecution")
    public AjaxResult deleteMultiInstanceExecution(
        @Parameter(description = "流程任务信息", required = true) @RequestBody FlowTaskVo flowTaskVo
    ) {
        flowTaskService.deleteMultiInstanceExecution(flowTaskVo);
        return AjaxResult.success("减签成功");
    }

    /**
     * 获取下一节点
     */
    @Operation(summary = "获取下一节点", description = "获取流程的下一节点信息")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取下一节点",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AjaxResult.class)))
    })
    @PostMapping(value = "/nextFlowNode")
    public AjaxResult getNextFlowNode(
        @Parameter(description = "流程任务信息", required = true) @RequestBody FlowTaskVo flowTaskVo
    ) {
        return flowTaskService.getNextFlowNode(flowTaskVo);
    }

    /**
     * 流程发起时获取下一节点
     */
    @Operation(summary = "流程发起时获取下一节点", description = "流程发起时获取下一节点信息")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取下一节点",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AjaxResult.class)))
    })
    @PostMapping(value = "/nextFlowNodeByStart")
    public AjaxResult getNextFlowNodeByStart(
        @Parameter(description = "流程任务信息", required = true) @RequestBody FlowTaskVo flowTaskVo
    ) {
        return flowTaskService.getNextFlowNodeByStart(flowTaskVo);
    }

    /**
     * 生成流程图
     */
    @Operation(summary = "生成流程图", description = "根据流程实例ID生成流程图")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功生成流程图",
            content = @Content(mediaType = "image/png"))
    })
    @GetMapping("/diagram/{processId}")
    public void genProcessDiagram(
        HttpServletResponse response,
        @Parameter(description = "流程实例ID", required = true) @PathVariable("processId") String processId
    ) {
        InputStream inputStream = flowTaskService.diagram(processId);
        OutputStream os = null;
        BufferedImage image = null;
        try {
            image = ImageIO.read(inputStream);
            response.setContentType("image/png");
            os = response.getOutputStream();
            if (image != null) {
                ImageIO.write(image, "png", os);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (os != null) {
                    os.flush();
                    os.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取流程执行节点
     */
    @Operation(summary = "获取流程执行节点", description = "获取流程实例的执行节点信息")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取执行节点",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AjaxResult.class)))
    })
    @GetMapping("/flowViewer/{procInsId}/{executionId}")
    public AjaxResult getFlowViewer(
        @Parameter(description = "流程实例ID", required = true) @PathVariable("procInsId") String procInsId,
        @Parameter(description = "执行ID", required = true) @PathVariable("executionId") String executionId
    ) {
        return flowTaskService.getFlowViewer(procInsId, executionId);
    }

    /**
     * 流程节点信息
     */
    @Operation(summary = "流程节点信息", description = "获取流程节点信息和XML")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取节点信息",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AjaxResult.class)))
    })
    @GetMapping("/flowXmlAndNode")
    public AjaxResult flowXmlAndNode(
        @Parameter(description = "流程实例ID") @RequestParam(value = "procInsId", required = false) String procInsId,
        @Parameter(description = "部署ID") @RequestParam(value = "deployId", required = false) String deployId
    ) {
        return flowTaskService.flowXmlAndNode(procInsId, deployId);
    }

    /**
     * 流程节点表单
     */
    @Operation(summary = "流程节点表单", description = "获取流程节点表单信息")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取节点表单",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AjaxResult.class)))
    })
    @GetMapping("/flowTaskForm")
    public AjaxResult flowTaskForm(
        @Parameter(description = "任务ID") @RequestParam(value = "taskId", required = false) String taskId
    ) throws Exception {
        return flowTaskService.flowTaskForm(taskId);
    }

    /**
     * 流程节点信息
     */
    @Operation(summary = "流程节点详细信息", description = "获取流程节点的详细信息")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取节点详细信息",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AjaxResult.class)))
    })
    @GetMapping("/flowTaskInfo")
    public AjaxResult flowTaskInfo(
        @Parameter(description = "流程实例ID", required = true) @RequestParam(value = "procInsId") String procInsId,
        @Parameter(description = "节点ID", required = true) @RequestParam(value = "elementId") String elementId
    ) {
        return flowTaskService.flowTaskInfo(procInsId, elementId);
    }
}