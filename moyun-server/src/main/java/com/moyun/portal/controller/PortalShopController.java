package com.moyun.portal.controller;

import com.moyun.common.annotation.Anonymous;
import com.moyun.common.constant.HttpStatus;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.page.PageDomain;
import com.moyun.ext.cms.service.IShopService;
import com.moyun.portal.util.PortalSecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 积分商城 Controller（门户端，阶段四 4.4）
 *
 * 公开接口：商品列表；兑换/我的记录需登录。
 *
 * @author moyun
 */
@Tag(name = "积分商城", description = "商品列表、积分兑换、兑换记录")
@RestController
@RequestMapping("/portal/shop")
public class PortalShopController extends BaseController {

    @Autowired
    private IShopService shopService;

    private Long currentUserId() {
        return PortalSecurityUtils.getUserId();
    }

    @Operation(summary = "商品列表", description = "返回所有上架的积分商品（公开）")
    @GetMapping("/list")
    @Anonymous
    public AjaxResult list() {
        return AjaxResult.success(shopService.listItems());
    }

    @Operation(summary = "我的积分", description = "返回当前用户积分余额")
    @GetMapping("/my-points")
    public AjaxResult myPoints() {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(shopService.myPoints(userId));
    }

    @Operation(summary = "兑换商品", description = "扣除积分、扣减库存、写入兑换记录（实物需传 address）")
    @PostMapping("/exchange/{itemId}")
    public AjaxResult exchange(@PathVariable("itemId") Long itemId,
                               @RequestParam(value = "address", required = false) String address) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        Long exchangeId = shopService.exchange(userId, itemId, address);
        return AjaxResult.success(exchangeId);
    }

    @Operation(summary = "我的兑换记录", description = "分页查询当前用户兑换记录")
    @GetMapping("/my-exchanges")
    public AjaxResult myExchanges(PageDomain query) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(shopService.myExchanges(userId, query));
    }
}
