package com.moyun.ext.flowable.controller;

import com.moyun.core.base.BaseController;
import com.moyun.core.base.AjaxResult;
import com.moyun.ext.flowable.service.IFlowTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 流程任务管理
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/flow/task")
public class FlowTaskController extends BaseController {

    @Autowired
    private IFlowTaskService flowTaskService;

    @PreAuthorize("@ss.hasPermi('flow:task:list')")
    @GetMapping("/todoList")
    public AjaxResult todoList(@RequestParam Map<String, Object> params) {
        return success(flowTaskService.selectTodoTaskList(params));
    }

    @PreAuthorize("@ss.hasPermi('flow:task:list')")
    @GetMapping("/finishedList")
    public AjaxResult finishedList(@RequestParam Map<String, Object> params) {
        return success(flowTaskService.selectFinishedTaskList(params));
    }

    @GetMapping(value = "/{taskId}")
    public AjaxResult getInfo(@PathVariable("taskId") String taskId) {
        return success(flowTaskService.selectTaskById(taskId));
    }

    @PostMapping("/complete/{taskId}")
    public AjaxResult complete(@PathVariable("taskId") String taskId,
                               @RequestParam(value = "variables", required = false) String variables) {
        Map<String, Object> variablesMap = new java.util.HashMap<>();
        flowTaskService.completeTask(taskId, variablesMap);
        return success();
    }

    @PutMapping("/delegate/{taskId}")
    public AjaxResult delegate(@PathVariable("taskId") String taskId,
                               @RequestParam("userId") String userId) {
        flowTaskService.delegateTask(taskId, userId);
        return success();
    }

    @PutMapping("/assign/{taskId}")
    public AjaxResult assign(@PathVariable("taskId") String taskId,
                             @RequestParam("userId") String userId) {
        flowTaskService.assigneeTask(taskId, userId);
        return success();
    }
}
