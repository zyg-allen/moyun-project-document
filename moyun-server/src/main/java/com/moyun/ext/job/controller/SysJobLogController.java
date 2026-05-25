package com.moyun.ext.job.controller;

import com.moyun.common.annotation.Log;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.TableDataInfo;
import com.moyun.common.enums.BusinessType;
import com.moyun.util.file.ExcelUtil;
import com.moyun.ext.job.domain.entity.SysJobLog;
import com.moyun.ext.job.service.ISysJobLogService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 调度任务日志操作处理
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/monitor/jobLog")
public class SysJobLogController extends BaseController {

    @Autowired
    private ISysJobLogService jobLogService;

    @PreAuthorize("@ss.hasPermi('monitor:job:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysJobLog sysJobLog) {
        startPage();
        List<SysJobLog> list = jobLogService.selectJobLogList(sysJobLog);
        return getDataTable(list);
    }

    @Log(title = "调度日志", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysJobLog sysJobLog) {
        List<SysJobLog> list = jobLogService.selectJobLogList(sysJobLog);
        ExcelUtil<SysJobLog> util = new ExcelUtil<SysJobLog>(SysJobLog.class);
        util.exportExcel(response, list, "调度日志");
    }

    @GetMapping(value = "/{jobLogId}")
    public AjaxResult getInfo(@PathVariable("jobLogId") Long jobLogId) {
        return success(jobLogService.selectJobLogById(jobLogId));
    }

    @DeleteMapping("/{logIds}")
    public AjaxResult remove(@PathVariable Long[] logIds) {
        return toAjax(jobLogService.deleteJobLogByIds(logIds));
    }

    @DeleteMapping("/clean")
    public AjaxResult clean() {
        jobLogService.cleanJobLog();
        return success();
    }
}
