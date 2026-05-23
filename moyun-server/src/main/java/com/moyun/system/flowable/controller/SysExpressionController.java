package com.moyun.system.flowable.controller;

import com.moyun.common.core.controller.BaseController;
import com.moyun.common.core.domain.AjaxResult;
import com.moyun.common.core.domain.TableDataInfo;
import com.moyun.system.domain.SysExpression;
import com.moyun.system.flowable.service.ISysExpressionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 流程达式Controller
 * 
 * @author ruoyi
 * @date 2022-12-12
 */
@Tag(name = "流程表达式管理", description = "流程表达式的增删改查操作接口")
@RestController
@RequestMapping("/flowable/expression")
@RequiredArgsConstructor
@Validated
public class SysExpressionController extends BaseController {

    private final ISysExpressionService sysExpressionService;

    /**
     * 查询流程达式列表
     */
    @Operation(summary = "获取流程表达式列表", description = "根据条件查询流程表达式列表")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取流程表达式列表",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TableDataInfo.class)))
    })
    @GetMapping("/list")
    public TableDataInfo list(SysExpression sysExpression) {
        startPage();
        List<SysExpression> list = sysExpressionService.selectSysExpressionList(sysExpression);
        return getDataTable(list);
    }

    /**
     * 获取流程达式详细信息
     */
    @Operation(summary = "获取流程表达式详情", description = "根据流程表达式ID获取详细信息")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取流程表达式详情",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AjaxResult.class)))
    })
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(
        @Parameter(description = "流程表达式ID", required = true) @PathVariable("id") Long id
    ) {
        return AjaxResult.success(sysExpressionService.selectSysExpressionById(id));
    }

    /**
     * 新增流程达式
     */
    @Operation(summary = "新增流程表达式", description = "创建新的流程表达式")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功新增流程表达式",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AjaxResult.class)))
    })
    @PostMapping
    public AjaxResult add(
        @Parameter(description = "流程表达式信息", required = true) @Valid @RequestBody SysExpression sysExpression
    ) {
        return AjaxResult.success(sysExpressionService.insertSysExpression(sysExpression));
    }

    /**
     * 修改流程达式
     */
    @Operation(summary = "修改流程表达式", description = "更新流程表达式信息")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功修改流程表达式",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AjaxResult.class)))
    })
    @PutMapping
    public AjaxResult edit(
        @Parameter(description = "流程表达式信息", required = true) @Valid @RequestBody SysExpression sysExpression
    ) {
        return AjaxResult.success(sysExpressionService.updateSysExpression(sysExpression));
    }

    /**
     * 删除流程达式
     */
    @Operation(summary = "删除流程表达式", description = "根据流程表达式ID删除流程表达式")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功删除流程表达式",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AjaxResult.class)))
    })
    @DeleteMapping("/{ids}")
    public AjaxResult remove(
        @Parameter(description = "流程表达式ID，多个用逗号分隔", required = true) @PathVariable Long[] ids
    ) {
        int result = sysExpressionService.deleteSysExpressionByIds(ids);
        return result > 0 ? AjaxResult.success("删除成功") : AjaxResult.error("删除失败");
    }
}