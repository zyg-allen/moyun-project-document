package com.moyun.ext.flowable.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.TableDataInfo;
import com.moyun.ext.flowable.service.ISysDeployFormService;
import com.moyun.ext.flowable.service.ISysFormService;
import com.moyun.system.domain.entity.SysDeployForm;
import com.moyun.system.domain.entity.SysForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 表单管理Controller
 *
 * @author Tony
 * @date 2021-04-03
 */
@Tag(name = "表单管理", description = "流程表单的增删改查操作接口")
@RestController
@RequestMapping("/flowable/form")
public class SysFormController extends BaseController {

    @Autowired
    private ISysFormService sysFormService;
    @Autowired
    private ISysDeployFormService sysDeployFormService;


    /**
     * 查询流程表单列表
     */
    @PreAuthorize("@ss.hasPermi('flowable:form:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysForm sysForm) {
        startPage();
        List<SysForm> list = sysFormService.selectSysFormList(sysForm);
        return getDataTable(list);
    }


    /**
     * 表单列表
     */
    @Operation(summary = "获取表单列表", description = "根据条件分页查询表单列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取表单列表",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AjaxResult.class)))
    })
    @GetMapping("/formList")
    public AjaxResult list(
            @Parameter(description = "查询参数") @RequestParam Map<String, Object> params
    ) {
        SysForm sysForm = new SysForm();
        // 可以根据params设置查询条件
        List<SysForm> list = sysFormService.selectSysFormList(sysForm);
        return AjaxResult.success(list);
    }

    /**
     * 表单详情
     */
    @Operation(summary = "获取表单详情", description = "根据表单ID获取详细信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取表单详情",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AjaxResult.class)))
    })
    @GetMapping("/{formId}")
    public AjaxResult detail(
            @Parameter(description = "表单ID", required = true) @PathVariable Long formId
    ) {
        return AjaxResult.success(sysFormService.selectSysFormById(formId));
    }

    /**
     * 新增表单
     */
    @Operation(summary = "新增表单", description = "创建新的流程表单")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功新增表单",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AjaxResult.class)))
    })
    @PostMapping
    public AjaxResult add(
            @Parameter(description = "表单信息", required = true) @RequestBody SysForm sysForm
    ) {
        int result = sysFormService.insertSysForm(sysForm);
        return result > 0 ? AjaxResult.success() : AjaxResult.error();
    }

    /**
     * 修改表单
     */
    @Operation(summary = "修改表单", description = "更新流程表单信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功修改表单",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AjaxResult.class)))
    })
    @PutMapping
    public AjaxResult edit(
            @Parameter(description = "表单信息", required = true) @RequestBody SysForm sysForm
    ) {
        int result = sysFormService.updateSysForm(sysForm);
        return result > 0 ? AjaxResult.success() : AjaxResult.error();
    }

    /**
     * 删除表单
     */
    @Operation(summary = "删除表单", description = "根据表单ID删除表单")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功删除表单",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AjaxResult.class)))
    })
    @DeleteMapping("/{formId}")
    public AjaxResult delete(
            @Parameter(description = "表单ID", required = true) @PathVariable Long formId
    ) {
        int result = sysFormService.deleteSysFormById(formId);
        return result > 0 ? AjaxResult.success() : AjaxResult.error();
    }

    /**
     * 挂载流程表单
     */
    @Operation(summary = "挂载流程表单", description = "根据表单ID挂载流程表单")
    @PostMapping("/addDeployForm")
    public AjaxResult addDeployForm(@RequestBody SysDeployForm sysDeployForm) {
        return toAjax(sysDeployFormService.insertSysDeployForm(sysDeployForm));
    }
}