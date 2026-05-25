package com.moyun.ext.flowable.controller;

import com.moyun.core.base.BaseController;
import com.moyun.core.base.AjaxResult;
import com.moyun.ext.flowable.service.IFlowInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 流程实例管理
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/flow/instance")
public class FlowInstanceController extends BaseController {

    @Autowired
    private IFlowInstanceService flowInstanceService;

    @PreAuthorize("@ss.hasPermi('flow:instance:list')")
    @GetMapping("/list")
    public AjaxResult list(@RequestParam Map<String, Object> params) {
        return success();
    }

    @GetMapping(value = "/{processInstanceId}")
    public AjaxResult getInfo(@PathVariable("processInstanceId") String processInstanceId) {
        return success(flowInstanceService.selectProcessInstanceById(processInstanceId));
    }

    @DeleteMapping("/{processInstanceIds}")
    public AjaxResult remove(@PathVariable String[] processInstanceIds) {
        flowInstanceService.deleteProcessInstanceByIds(processInstanceIds);
        return success();
    }

    @PostMapping("/start")
    public AjaxResult start(@RequestParam("processDefinitionKey") String processDefinitionKey,
                            @RequestParam(value = "variables", required = false) String variables) {
        Map<String, Object> variablesMap = new java.util.HashMap<>();
        Map<String, Object> result = flowInstanceService.startProcessInstanceByKey(processDefinitionKey, variablesMap);
        return success(result);
    }
}
