package com.moyun.portal.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.entity.PortalFriendLink;
import com.moyun.portal.domain.query.FriendLinkQuery;
import com.moyun.portal.service.IPortalFriendLinkService;
import com.moyun.util.bean.PageUtils;

/**
 * 门户友情链接 Controller
 *
 * 说明：本期清理死接口，仅保留前端在用的 list（首页 HomePage 调用 getFriendLinks）。
 * 已删除以下死方法：export / getInfo / add / edit / remove。
 * 对应的 Service / Mapper / XML 实现保持不变，未做任何修改。
 */
@Tag(name = "门户友情链接", description = "门户友情链接的增删改查操作接口")
@RestController
@RequestMapping("/portal/friend-link")
public class PortalFriendLinkController extends BaseController {

    @Autowired
    private IPortalFriendLinkService portalFriendLinkService;

    @Operation(summary = "获取友情链接列表", description = "根据条件分页查询友情链接列表")
    @GetMapping("/list")
    public AjaxResult list(FriendLinkQuery query) {
        Page<PortalFriendLink> page = PageUtils.buildPage(query);
        Page<PortalFriendLink> resultPage = portalFriendLinkService.selectPortalFriendLinkPage(page, query);
        return success(resultPage);
    }
}
