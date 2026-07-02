package com.moyun.portal.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moyun.common.annotation.Anonymous;
import com.moyun.common.constant.HttpStatus;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.page.PageDomain;
import com.moyun.ext.cms.domain.vo.FeedEventVO;
import com.moyun.ext.cms.service.IFeedService;
import com.moyun.portal.util.PortalSecurityUtils;

/**
 * 动态/Feed 流 前台接口
 *
 * <p>接口契约：</p>
 * <ul>
 *   <li>GET /portal/feed/following — 我的关注动态（需登录，分页，读时拉模式）</li>
 *   <li>GET /portal/feed/hot       — 全站热门动态（@Anonymous 公开，分页，最近 7 天）</li>
 * </ul>
 *
 * @author moyun
 */
@Tag(name = "动态/Feed流", description = "动态流前台接口")
@RestController
@RequestMapping("/portal/feed")
public class PortalFeedController extends BaseController {

    @Autowired
    private IFeedService feedService;

    /**
     * 我的关注动态（需登录，分页）
     */
    @Operation(summary = "我的关注动态", description = "查询我关注的人的最近动态（分页，读时拉模式）")
    @GetMapping("/following")
    public AjaxResult getFollowingFeed(PageDomain query) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        Page<FeedEventVO> page = feedService.getFollowingFeed(userId, query);
        return AjaxResult.success(page);
    }

    /**
     * 全站热门动态（公开，分页）
     */
    @Operation(summary = "全站热门动态", description = "返回最近 7 天的全站热门动态（按时间倒序，分页）")
    @GetMapping("/hot")
    @Anonymous
    public AjaxResult getHotFeed(PageDomain query) {
        Page<FeedEventVO> page = feedService.getHotFeed(query);
        return AjaxResult.success(page);
    }
}
