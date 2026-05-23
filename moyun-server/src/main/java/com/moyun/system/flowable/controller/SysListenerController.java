package com.moyun.system.flowable.controller;

import com.moyun.common.core.controller.BaseController;
import com.moyun.common.core.domain.AjaxResult;
import com.moyun.common.core.domain.TableDataInfo;
import com.moyun.system.domain.SysListener;
import com.moyun.system.service.ISysListenerService;
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
 * 监听器管理Controller
 *
 * @author Tony
 * @date 2022-12-25
 */
@Tag(name = "监听器管理", description = "流程监听器的增删改查操作接口")
@RestController
@RequestMapping("/flowable/listener")
@RequiredArgsConstructor
@Validated
public class SysListenerController extends BaseController {

    private final ISysListenerService sysListenerService;

    /**
     * 查询监听器列表
     */
    @Operation(summary = "获取监听器列表", description = "根据条件查询监听器列表")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取监听器列表",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TableDataInfo.class)))
    })
    @GetMapping("/list")
    public TableDataInfo list(SysListener sysListener) {
        startPage();
        List<SysListener> list = sysListenerService.selectSysListenerList(sysListener);
        return getDataTable(list);
    }

    /**
     * 获取监听器详细信息
     */
    @Operation(summary = "获取监听器详情", description = "根据监听器ID获取详细信息")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取监听器详情",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AjaxResult.class)))
    })
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(
        @Parameter(description = "监听器ID", required = true) @PathVariable("id") Long id
    ) {
        return AjaxResult.success(sysListenerService.selectSysListenerById(id));
    }

    /**
     * 新增监听器
     */
    @Operation(summary = "新增监听器", description = "创建新的流程监听器")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功新增监听器",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AjaxResult.class)))
    })
    @PostMapping
    public AjaxResult add(
        @Parameter(description = "监听器信息", required = true) @Valid @RequestBody SysListener sysListener
    ) {
        return AjaxResult.success(sysListenerService.insertSysListener(sysListener));
    }

    /**
     * 修改监听器
     */
    @Operation(summary = "修改监听器", description = "更新流程监听器信息")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功修改监听器",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AjaxResult.class)))
    })
    @PutMapping
    public AjaxResult edit(
        @Parameter(description = "监听器信息", required = true) @Valid @RequestBody SysListener sysListener
    ) {
        return AjaxResult.success(sysListenerService.updateSysListener(sysListener));
    }

    /**
     * 删除监听器
     */
    @Operation(summary = "删除监听器", description = "根据监听器ID删除监听器")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功删除监听器",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AjaxResult.class)))
    })
    @DeleteMapping("/{ids}")
    public AjaxResult remove(
        @Parameter(description = "监听器ID，多个用逗号分隔", required = true) @PathVariable Long[] ids
    ) {
        int result = sysListenerService.deleteSysListenerByIds(ids);
        return result > 0 ? AjaxResult.success("删除成功") : AjaxResult.error("删除失败");
    }
}
