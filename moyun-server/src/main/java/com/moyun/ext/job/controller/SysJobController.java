package com.moyun.ext.job.controller;

import com.moyun.common.annotation.Log;
import com.moyun.common.constant.Constants;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.TableDataInfo;
import com.moyun.common.enums.BusinessType;
import com.moyun.common.exception.system.job.TaskException;
import com.moyun.util.string.StringUtils;
import com.moyun.util.file.ExcelUtil;
import com.moyun.ext.job.domain.entity.SysJob;
import com.moyun.ext.job.service.ISysJobService;
import com.moyun.ext.job.util.CronUtils;
import com.moyun.ext.job.util.ScheduleUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 调度任务信息操作处理
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/monitor/job")
public class SysJobController extends BaseController {

    @Autowired
    private ISysJobService jobService;

    @PreAuthorize("@ss.hasPermi('monitor:job:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysJob sysJob) {
        startPage();
        List<SysJob> list = jobService.selectJobList(sysJob);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('monitor:job:export')")
    @Log(title = "定时任务", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysJob sysJob) {
        List<SysJob> list = jobService.selectJobList(sysJob);
        ExcelUtil<SysJob> util = new ExcelUtil<SysJob>(SysJob.class);
        util.exportExcel(response, list, "定时任务");
    }

    @PreAuthorize("@ss.hasPermi('monitor:job:query')")
    @GetMapping(value = "/{jobId}")
    public AjaxResult getInfo(@PathVariable("jobId") Long jobId) {
        return success(jobService.selectJobById(jobId));
    }

    @PreAuthorize("@ss.hasPermi('monitor:job:add')")
    @Log(title = "定时任务", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysJob job) throws SchedulerException, TaskException {
        if (!CronUtils.isValid(job.getCronExpression())) {
            return error("新增任务'" + job.getJobName() + "'失败，Cron表达式不正确");
        } else if (StringUtils.containsIgnoreCase(job.getInvokeTarget(), Constants.LOOKUP_RMI)) {
            return error("新增任务'" + job.getJobName() + "'失败，目标字符串不允许'rmi'调用");
        } else if (StringUtils.containsAnyIgnoreCase(job.getInvokeTarget(), new String[]{Constants.LOOKUP_LDAP, Constants.LOOKUP_LDAPS})) {
            return error("新增任务'" + job.getJobName() + "'失败，目标字符串不允许'ldap(s)'调用");
        } else if (StringUtils.containsAnyIgnoreCase(job.getInvokeTarget(), new String[]{Constants.HTTP, Constants.HTTPS})) {
            return error("新增任务'" + job.getJobName() + "'失败，目标字符串不允许'http(s)'调用");
        } else if (StringUtils.containsAnyIgnoreCase(job.getInvokeTarget(), Constants.JOB_ERROR_STR)) {
            return error("新增任务'" + job.getJobName() + "'失败，目标字符串存在违规");
        } else if (!ScheduleUtils.whiteList(job.getInvokeTarget())) {
            return error("新增任务'" + job.getJobName() + "'失败，目标字符串不在白名单内");
        }
        job.setCreateBy(getUsername());
        return toAjax(jobService.insertJob(job));
    }

    @PreAuthorize("@ss.hasPermi('monitor:job:edit')")
    @Log(title = "定时任务", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysJob job) throws SchedulerException, TaskException {
        if (!CronUtils.isValid(job.getCronExpression())) {
            return error("修改任务'" + job.getJobName() + "'失败，Cron表达式不正确");
        } else if (StringUtils.containsIgnoreCase(job.getInvokeTarget(), Constants.LOOKUP_RMI)) {
            return error("修改任务'" + job.getJobName() + "'失败，目标字符串不允许'rmi'调用");
        } else if (StringUtils.containsAnyIgnoreCase(job.getInvokeTarget(), new String[]{Constants.LOOKUP_LDAP, Constants.LOOKUP_LDAPS})) {
            return error("修改任务'" + job.getJobName() + "'失败，目标字符串不允许'ldap(s)'调用");
        } else if (StringUtils.containsAnyIgnoreCase(job.getInvokeTarget(), new String[]{Constants.HTTP, Constants.HTTPS})) {
            return error("修改任务'" + job.getJobName() + "'失败，目标字符串不允许'http(s)'调用");
        } else if (StringUtils.containsAnyIgnoreCase(job.getInvokeTarget(), Constants.JOB_ERROR_STR)) {
            return error("修改任务'" + job.getJobName() + "'失败，目标字符串存在违规");
        } else if (!ScheduleUtils.whiteList(job.getInvokeTarget())) {
            return error("修改任务'" + job.getJobName() + "'失败，目标字符串不在白名单内");
        }
        job.setUpdateBy(getUsername());
        return toAjax(jobService.updateJob(job));
    }

    @PreAuthorize("@ss.hasPermi('monitor:job:changeStatus')")
    @Log(title = "定时任务", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody SysJob job) throws SchedulerException {
        SysJob newJob = jobService.selectJobById(job.getJobId());
        newJob.setStatus(job.getStatus());
        return toAjax(jobService.changeStatus(newJob));
    }

    @PreAuthorize("@ss.hasPermi('monitor:job:changeStatus')")
    @Log(title = "定时任务", businessType = BusinessType.UPDATE)
    @PutMapping("/run")
    public AjaxResult run(@RequestBody SysJob job) throws SchedulerException {
        boolean result = jobService.run(job);
        return result ? success() : error("任务不存在或已过期！");
    }

    @PreAuthorize("@ss.hasPermi('monitor:job:remove')")
    @Log(title = "定时任务", businessType = BusinessType.DELETE)
    @DeleteMapping("/{jobIds}")
    public AjaxResult remove(@PathVariable Long[] jobIds) throws SchedulerException {
        jobService.deleteJobByIds(jobIds);
        return success();
    }
}
