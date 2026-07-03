package com.moyun.portal.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.entity.PortalStudyPlan;
import com.moyun.portal.mapper.PortalStudyPlanMapper;
import com.moyun.util.bean.PageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学习计划 后台 Controller（任务 3.2 后台，只读查看）
 *
 * <p>供后台管理页面调用，提供分页只读查询能力。</p>
 *
 * <p>路径前缀 /cms/portal/studyPlan，权限标识 portal:studyPlan:list。</p>
 *
 * @author moyun
 */
@Tag(name = "学习计划管理", description = "学习计划后台只读查看接口")
@RestController
@RequestMapping("/cms/portal/studyPlan")
public class CmsStudyPlanController extends BaseController {

    @Autowired
    private PortalStudyPlanMapper studyPlanMapper;

    @Operation(summary = "学习计划分页列表", description = "分页查询全部学习计划，支持按用户ID、状态、类型筛选（只读）")
    @PreAuthorize("@ss.hasPermi('portal:studyPlan:list')")
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(required = false) Long userId,
                            @RequestParam(required = false) String status,
                            @RequestParam(required = false) String planType,
                            @RequestParam(required = false) String title,
                            @RequestParam(defaultValue = "1") Integer pageNum,
                            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<PortalStudyPlan> page = PageUtils.buildPage(pageNum, pageSize);
        LambdaQueryWrapper<PortalStudyPlan> qw = Wrappers.<PortalStudyPlan>lambdaQuery()
                .orderByDesc(PortalStudyPlan::getCreatedTime);
        if (userId != null) {
            qw.eq(PortalStudyPlan::getUserId, userId);
        }
        if (status != null && !status.isEmpty()) {
            qw.eq(PortalStudyPlan::getStatus, status);
        }
        if (planType != null && !planType.isEmpty()) {
            qw.eq(PortalStudyPlan::getPlanType, planType);
        }
        if (title != null && !title.isEmpty()) {
            qw.like(PortalStudyPlan::getTitle, title);
        }
        Page<PortalStudyPlan> result = studyPlanMapper.selectPage(page, qw);
        return success(result);
    }
}
