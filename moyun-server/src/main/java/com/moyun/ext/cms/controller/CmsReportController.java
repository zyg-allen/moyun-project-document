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
import com.moyun.system.domain.entity.SysNotification;
import com.moyun.system.service.ISysNotificationService;
import com.moyun.util.bean.PageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(CmsReportController.class);

    @Autowired
    private PortalReportMapper reportMapper;

    @Autowired
    private ISysNotificationService notificationService;

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
     * 仅允许更新 status/handleResult 两个字段，防止前端篡改 userId/username/description 等。
     * 当请求携带 notifyUser=true 时，向举报提交人发送站内通知（默认不通知）。
     */
    @Operation(summary = "处理举报", description = "处理举报记录，标记状态并记录处理结果，可选通知提交人")
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
        int rows = reportMapper.update(null, updateWrapper);
        // 处理成功后，按需向举报提交人发送站内通知
        if (rows > 0 && Boolean.TRUE.equals(report.getNotifyUser())) {
            sendHandleNotification(report.getId(), status, report.getHandleResult());
        }
        return toAjax(rows);
    }

    /**
     * 举报处理结果站内信通知提交人
     * 通知失败不影响处理主流程
     */
    private void sendHandleNotification(Long reportId, String status, String handleResult) {
        try {
            PortalReport report = reportMapper.selectById(reportId);
            if (report == null || report.getUserId() == null) {
                return;
            }
            SysNotification notification = new SysNotification();
            notification.setType("system");
            notification.setScope("user");
            notification.setUserId(report.getUserId());
            notification.setUserType("portal");
            notification.setNoticeType("1");
            notification.setStatus("0");
            String statusLabel = "processing".equals(status) ? "处理中"
                    : "resolved".equals(status) ? "已解决" : "已驳回";
            notification.setTitle("您的举报处理进度更新：" + statusLabel);
            String content = "您提交的举报（编号 #" + reportId + "）处理状态已更新为「" + statusLabel + "」";
            if (handleResult != null && !handleResult.isEmpty()) {
                content += "，处理说明：" + handleResult;
            }
            content += "。可在「我的举报」中查看详情。";
            notification.setContent(content);
            notification.setData("{\"bizType\":\"report\",\"id\":" + reportId + ",\"status\":\"" + status + "\"}");
            notificationService.insertNotification(notification);
            log.info("举报处理通知已发送，reportId={}, userId={}, status={}", reportId, report.getUserId(), status);
        } catch (Exception e) {
            log.error("举报处理通知发送失败（不影响处理主流程），reportId={}, error={}", reportId, e.getMessage());
        }
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
