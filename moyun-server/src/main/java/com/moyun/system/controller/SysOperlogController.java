package com.moyun.system.controller;

import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.TableDataInfo;
import com.moyun.system.domain.entity.SysOperLog;
import com.moyun.system.domain.query.OperLogQuery;
import com.moyun.system.service.ISysOperLogService;
import com.moyun.util.file.ExcelUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 操作日志记录
 *
 * @author ruoyi
 */
@Tag(name = "操作日志管理", description = "操作日志记录相关接口")
@RestController
@RequestMapping("/monitor/operlog")
public class SysOperlogController extends BaseController {
    @Autowired
    private ISysOperLogService operLogService;

    @Operation(summary = "获取操作日志列表")
    @PreAuthorize("@ss.hasPermi('monitor:operlog:list')")
    @GetMapping("/list")
    public TableDataInfo list(OperLogQuery query) {
        startPage();
        List<SysOperLog> list = operLogService.selectOperLogList(query);
        return getDataTable(list);
    }

    @Operation(summary = "导出操作日志")
    @Log(title = "操作日志", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('monitor:operlog:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, OperLogQuery query) {
        List<SysOperLog> list = operLogService.selectOperLogList(query);
        ExcelUtil<SysOperLog> util = new ExcelUtil<SysOperLog>(SysOperLog.class);
        util.exportExcel(response, list, "操作日志");
    }

    @Operation(summary = "删除操作日志")
    @Log(title = "操作日志", businessType = BusinessType.DELETE)
    @PreAuthorize("@ss.hasPermi('monitor:operlog:remove')")
    @DeleteMapping("/{operIds}")
    public AjaxResult remove(@PathVariable Long[] operIds) {
        return toAjax(operLogService.deleteOperLogByIds(operIds));
    }

    @Operation(summary = "清空操作日志")
    @Log(title = "操作日志", businessType = BusinessType.CLEAN)
    @PreAuthorize("@ss.hasPermi('monitor:operlog:remove')")
    @DeleteMapping("/clean")
    public AjaxResult clean() {
        operLogService.cleanOperLog();
        return success();
    }
}
