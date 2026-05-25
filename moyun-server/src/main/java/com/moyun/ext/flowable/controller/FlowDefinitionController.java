package com.moyun.ext.flowable.controller;

import com.moyun.core.base.BaseController;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.TableDataInfo;
import com.moyun.ext.flowable.domain.entity.FlowProcDefDto;
import com.moyun.ext.flowable.service.IFlowDefinitionService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程定义管理
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/flow/definition")
public class FlowDefinitionController extends BaseController {

    @Autowired
    private IFlowDefinitionService flowDefinitionService;

    @PreAuthorize("@ss.hasPermi('flow:definition:list')")
    @GetMapping("/list")
    public TableDataInfo list(FlowProcDefDto flowProcDefDto) {
        startPage();
        List<FlowProcDefDto> list = flowDefinitionService.selectProcessDefinitionList(flowProcDefDto);
        return getDataTable(list);
    }

    @GetMapping(value = "/{processDefinitionId}")
    public AjaxResult getInfo(@PathVariable("processDefinitionId") String processDefinitionId) {
        return success(flowDefinitionService.selectProcessDefinitionById(processDefinitionId));
    }

    @PostMapping("/upload")
    public AjaxResult upload(MultipartFile file) throws IOException {
        String message = flowDefinitionService.importProcessDefinition(file.getInputStream(), file.getOriginalFilename());
        return success(message);
    }

    @PutMapping("/changeState")
    public AjaxResult changeState(@RequestBody FlowProcDefDto flowProcDefDto) {
        flowDefinitionService.suspendOrActivateProcessDefinitionById(
                flowProcDefDto.getId(), flowProcDefDto.getIsSuspended() ? "suspend" : "activate");
        return success();
    }

    @DeleteMapping("/{deploymentIds}")
    public AjaxResult remove(@PathVariable String[] deploymentIds) {
        for (String deploymentId : deploymentIds) {
            flowDefinitionService.deleteDeploymentById(deploymentId);
        }
        return success();
    }

    @GetMapping("/resource/{processDefinitionId}/{resourceType}")
    public void viewImage(@PathVariable("processDefinitionId") String processDefinitionId,
                          @PathVariable("resourceType") String resourceType, HttpServletResponse response) {
    }
}
