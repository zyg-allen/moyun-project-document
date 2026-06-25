package com.moyun.ext.cms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.TableDataInfo;
import com.moyun.portal.domain.entity.PortalFeedback;
import com.moyun.portal.mapper.PortalFeedbackMapper;
import com.moyun.util.bean.PageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * CMS 意见反馈管理 Controller
 * 后台管理用户提交的意见反馈，支持查询、处理、回复
 *
 * @author moyun
 */
@Tag(name = "CMS意见反馈管理", description = "用户意见反馈的查询与处理")
@RestController
@RequestMapping("/cms/feedback")
public class CmsFeedbackController extends BaseController {

    @Autowired
    private PortalFeedbackMapper feedbackMapper;

    /**
     * 查询反馈列表（分页）
     */
    @Operation(summary = "查询反馈列表", description = "分页查询意见反馈，支持按类型/状态/用户名筛选")
    @PreAuthorize("@ss.hasPermi('cms:feedback:list')")
    @GetMapping("/list")
    public TableDataInfo list(PortalFeedback query) {
        Page<PortalFeedback> page = PageUtils.startPage();
        LambdaQueryWrapper<PortalFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.getFeedbackType() != null && !query.getFeedbackType().isEmpty(),
                        PortalFeedback::getFeedbackType, query.getFeedbackType())
                .eq(query.getStatus() != null && !query.getStatus().isEmpty(),
                        PortalFeedback::getStatus, query.getStatus())
                .like(query.getUsername() != null && !query.getUsername().isEmpty(),
                        PortalFeedback::getUsername, query.getUsername())
                .like(query.getSubject() != null && !query.getSubject().isEmpty(),
                        PortalFeedback::getSubject, query.getSubject())
                .orderByDesc(PortalFeedback::getCreateTime);
        Page<PortalFeedback> result = feedbackMapper.selectPage(page, wrapper);
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(200);
        rspData.setMsg("查询成功");
        rspData.setRows(result.getRecords());
        rspData.setTotal(result.getTotal());
        return rspData;
    }

    /**
     * 获取反馈详情
     */
    @Operation(summary = "获取反馈详情", description = "根据ID获取反馈记录详情")
    @PreAuthorize("@ss.hasPermi('cms:feedback:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(feedbackMapper.selectById(id));
    }

    /**
     * 处理反馈（标记状态并记录处理结果）
     * 仅允许更新 status/handleResult 两个字段，防止前端篡改 userId/username/description 等
     */
    @Operation(summary = "处理反馈", description = "处理反馈记录，标记状态并记录处理结果")
    @PreAuthorize("@ss.hasPermi('cms:feedback:handle')")
    @Log(title = "意见反馈", businessType = BusinessType.UPDATE)
    @PutMapping("/handle")
    public AjaxResult handle(@RequestBody PortalFeedback feedback) {
        if (feedback.getId() == null) {
            return error("反馈ID不能为空");
        }
        // status 白名单校验
        String status = feedback.getStatus();
        if (status == null || status.isEmpty()) {
            return error("处理状态不能为空");
        }
        if (!status.equals("processing") && !status.equals("resolved") && !status.equals("rejected")) {
            return error("非法的处理状态：" + status);
        }
        LambdaUpdateWrapper<PortalFeedback> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(PortalFeedback::getId, feedback.getId())
                .set(PortalFeedback::getStatus, status)
                .set(PortalFeedback::getHandleResult, feedback.getHandleResult())
                .set(PortalFeedback::getHandler, getUsername())
                .set(PortalFeedback::getHandleTime, LocalDateTime.now())
                .set(PortalFeedback::getUpdateTime, LocalDateTime.now());
        return toAjax(feedbackMapper.update(null, updateWrapper));
    }

    /**
     * 批量删除反馈记录
     */
    @Operation(summary = "删除反馈记录", description = "批量删除反馈记录")
    @PreAuthorize("@ss.hasPermi('cms:feedback:remove')")
    @Log(title = "意见反馈", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(feedbackMapper.deleteByIds(java.util.Arrays.asList(ids)));
    }
}
