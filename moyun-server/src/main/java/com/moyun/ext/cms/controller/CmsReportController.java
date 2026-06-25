package com.moyun.ext.cms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.TableDataInfo;
import com.moyun.portal.domain.entity.PortalReport;
import com.moyun.portal.mapper.PortalReportMapper;
import com.moyun.util.bean.PageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * CMS 举报管理 Controller
 * 后台管理用户提交的举报记录，支持查询、处理、驳回
 *
 * @author moyun
 */
@Tag(name = "CMS举报管理", description = "用户举报记录的查询与处理")
@RestController
@RequestMapping("/cms/report")
public class CmsReportController extends BaseController {

    @Autowired
    private PortalReportMapper reportMapper;

    /**
     * 查询举报列表（分页）
     */
    @Operation(summary = "查询举报列表", description = "分页查询举报记录，支持按类型/状态筛选")
    @PreAuthorize("@ss.hasPermi('cms:report:list')")
    @GetMapping("/list")
    public TableDataInfo list(PortalReport query) {
        Page<PortalReport> page = PageUtils.startPage();
        LambdaQueryWrapper<PortalReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.getReportType() != null && !query.getReportType().isEmpty(),
                        PortalReport::getReportType, query.getReportType())
                .eq(query.getStatus() != null && !query.getStatus().isEmpty(),
                        PortalReport::getStatus, query.getStatus())
                .like(query.getUsername() != null && !query.getUsername().isEmpty(),
                        PortalReport::getUsername, query.getUsername())
                .orderByDesc(PortalReport::getCreateTime);
        Page<PortalReport> result = reportMapper.selectPage(page, wrapper);
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(200);
        rspData.setMsg("查询成功");
        rspData.setRows(result.getRecords());
        rspData.setTotal(result.getTotal());
        return rspData;
    }

    /**
     * 获取举报详情
     */
    @Operation(summary = "获取举报详情", description = "根据ID获取举报记录详情")
    @PreAuthorize("@ss.hasPermi('cms:report:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(reportMapper.selectById(id));
    }

    /**
     * 处理举报（标记为已处理并记录处理结果）
     * 仅允许更新 status/handleResult 两个字段，防止前端篡改 userId/username/description 等
     */
    @Operation(summary = "处理举报", description = "处理举报记录，标记状态并记录处理结果")
    @PreAuthorize("@ss.hasPermi('cms:report:handle')")
    @Log(title = "举报管理", businessType = BusinessType.UPDATE)
    @PutMapping("/handle")
    public AjaxResult handle(@RequestBody PortalReport report) {
        if (report.getId() == null) {
            return error("举报ID不能为空");
        }
        // status 白名单校验，防止非法值或回退已处理记录
        String status = report.getStatus();
        if (status == null || status.isEmpty()) {
            return error("处理状态不能为空");
        }
        if (!status.equals("processing") && !status.equals("resolved") && !status.equals("rejected")) {
            return error("非法的处理状态：" + status);
        }
        LambdaUpdateWrapper<PortalReport> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(PortalReport::getId, report.getId())
                .set(PortalReport::getStatus, status)
                .set(PortalReport::getHandleResult, report.getHandleResult())
                .set(PortalReport::getHandler, getUsername())
                .set(PortalReport::getHandleTime, LocalDateTime.now())
                .set(PortalReport::getUpdateTime, LocalDateTime.now());
        return toAjax(reportMapper.update(null, updateWrapper));
    }

    /**
     * 批量删除举报记录
     */
    @Operation(summary = "删除举报记录", description = "批量删除举报记录")
    @PreAuthorize("@ss.hasPermi('cms:report:remove')")
    @Log(title = "举报管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(reportMapper.deleteByIds(java.util.Arrays.asList(ids)));
    }
}
