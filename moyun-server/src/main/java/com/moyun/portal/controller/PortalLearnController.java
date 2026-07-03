package com.moyun.portal.controller;

import com.moyun.common.annotation.Anonymous;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.cms.service.ILearnCenterService;
import com.moyun.portal.util.PortalSecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学习中心聚合 Controller（任务 3.1，门户端）
 * <p>
 * 公开接口：聚合数据；未登录返回概览骨架，登录返回含个人计划、错题入口。
 *
 * @author moyun
 */
@Tag(name = "学习中心", description = "学习中心聚合数据（今日计划、错题本、统计）")
@RestController
@RequestMapping("/portal/learn")
public class PortalLearnController extends BaseController {

    @Autowired
    private ILearnCenterService learnCenterService;

    @Operation(summary = "学习中心聚合数据", description = "聚合今日计划、连续打卡、错题本入口、学习曲线、推荐题目")
    @GetMapping("/dashboard")
    @Anonymous
    public AjaxResult dashboard() {
        Long userId = PortalSecurityUtils.getUserId();
        return AjaxResult.success(learnCenterService.getDashboard(userId));
    }
}
