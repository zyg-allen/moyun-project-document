package com.moyun.portal.controller;

import com.moyun.common.annotation.Anonymous;
import com.moyun.common.constant.HttpStatus;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.page.PageDomain;
import com.moyun.portal.domain.entity.PortalTipOrder;
import com.moyun.portal.service.IPortalTipService;
import com.moyun.portal.util.PortalSecurityUtils;
import com.moyun.util.bean.PageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 打赏 Controller（门户端）
 * 复用为付费阅读购买记录（target_type='article_paid'）
 *
 * @author moyun
 */
@Tag(name = "打赏", description = "对文章/专栏打赏，复用为付费阅读购买记录")
@RestController
@RequestMapping("/portal/tip")
public class PortalTipController extends BaseController {

    @Autowired
    private IPortalTipService portalTipService;

    private Long currentUserId() {
        return PortalSecurityUtils.getUserId();
    }

    /**
     * 发起打赏（需登录）
     * 简化实现：不接入真实支付，直接置 status='paid'
     */
    @Operation(summary = "发起打赏", description = "对文章/专栏发起打赏，简化版直接置 status='paid'")
    @PostMapping("/{targetType}/{targetId}")
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult toggleTipOrList(@PathVariable("targetType") String targetType,
                                      @PathVariable("targetId") Long targetId,
                                      @RequestBody PortalTipOrder body) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }

        PortalTipOrder order = new PortalTipOrder();
        order.setUserId(userId);
        order.setTargetType(targetType);
        order.setTargetId(targetId);
        order.setAmount(body.getAmount());
        order.setMessage(body.getMessage());

        PortalTipOrder created = portalTipService.toggleTipOrList(order);
        return AjaxResult.success(created);
    }

    /**
     * 我打赏的（需登录）
     */
    @Operation(summary = "我打赏的", description = "分页查询当前用户发起的打赏记录")
    @GetMapping("/my/given")
    public AjaxResult myGiven(PageDomain query) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        Page<PortalTipOrder> page = PageUtils.buildPage(query);
        Page<PortalTipOrder> result = portalTipService.queryMyGiven(userId, page);
        return AjaxResult.success(result);
    }

    /**
     * 我收到的（需登录）
     */
    @Operation(summary = "我收到的", description = "分页查询当前用户收到的打赏记录")
    @GetMapping("/my/received")
    public AjaxResult myReceived(PageDomain query) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        Page<PortalTipOrder> page = PageUtils.buildPage(query);
        Page<PortalTipOrder> result = portalTipService.queryMyReceived(userId, page);
        return AjaxResult.success(result);
    }

    /**
     * 目标的打赏列表（公开）
     */
    @Operation(summary = "目标的打赏列表", description = "公开查询某文章/专栏的打赏列表（仅已支付）")
    @GetMapping("/target/{targetType}/{targetId}")
    @Anonymous
    public AjaxResult targetTips(@PathVariable("targetType") String targetType,
                                 @PathVariable("targetId") Long targetId,
                                 PageDomain query) {
        Page<PortalTipOrder> page = PageUtils.buildPage(query);
        Page<PortalTipOrder> result = portalTipService.queryTargetTips(targetType, targetId, page);
        return AjaxResult.success(result);
    }
}
