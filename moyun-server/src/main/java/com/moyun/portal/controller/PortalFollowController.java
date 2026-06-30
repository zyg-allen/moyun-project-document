package com.moyun.portal.controller;

import java.util.HashMap;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.moyun.common.annotation.Log;
import com.moyun.common.constant.HttpStatus;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.service.IPortalFollowService;
import com.moyun.portal.util.PortalSecurityUtils;

/**
 * 门户关注 接口
 *
 * 语义化 RESTful 契约（与前端 follow.ts 对齐）：
 *   POST   /portal/follow/{userId}              关注用户（幂等）
 *   DELETE /portal/follow/{userId}              取消关注（幂等）
 *   GET    /portal/follow/check/{userId}        检查当前用户是否已关注目标用户
 *
 * 死接口清理说明（仅删除 Controller 方法，Service/Mapper/XML 保留不动，便于后续按需恢复）：
 *   - GET  /portal/follow/{userId}/followers    粉丝列表（前端已不调用）
 *   - GET  /portal/follow/{userId}/following    关注列表（前端已不调用）
 *   - POST /portal/follow/toggle/{userId}       切换关注（已被 POST/DELETE {userId} 取代）
 *   - GET  /portal/follow/isFollowing/{userId}  旧版检查关注（已被 check/{userId} 取代）
 *
 * @author moyun
 */
@Tag(name = "门户关注", description = "门户关注/取消关注/检查关注接口")
@RestController
@RequestMapping("/portal/follow")
public class PortalFollowController extends BaseController {

    @Autowired
    private IPortalFollowService portalFollowService;

    /**
     * 关注用户（幂等：若已关注则保持已关注状态）
     */
    @Operation(summary = "关注用户", description = "当前登录用户关注目标用户（幂等，已关注不会重复插入）")
    @Log(title = "门户关注", businessType = BusinessType.INSERT)
    @PostMapping("/{userId}")
    public AjaxResult follow(@Parameter(description = "被关注用户ID") @PathVariable Long userId) {
        Long currentUserId = PortalSecurityUtils.getUserId();
        if (currentUserId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return success(portalFollowService.follow(currentUserId, userId));
    }

    /**
     * 取消关注用户（幂等：若未关注则保持未关注状态）
     */
    @Operation(summary = "取消关注", description = "当前登录用户取消关注目标用户（幂等，未关注不会报错）")
    @Log(title = "门户关注", businessType = BusinessType.DELETE)
    @DeleteMapping("/{userId}")
    public AjaxResult unfollow(@Parameter(description = "被取消关注用户ID") @PathVariable Long userId) {
        Long currentUserId = PortalSecurityUtils.getUserId();
        if (currentUserId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return success(portalFollowService.unfollow(currentUserId, userId));
    }

    /**
     * 检查当前登录用户是否已关注目标用户
     */
    @Operation(summary = "检查是否已关注", description = "检查当前登录用户是否已关注目标用户")
    @GetMapping("/check/{userId}")
    public AjaxResult checkFollow(@Parameter(description = "目标用户ID") @PathVariable Long userId) {
        Long currentUserId = PortalSecurityUtils.getUserId();
        Map<String, Object> result = new HashMap<>();
        if (currentUserId == null) {
            // 未登录返回 false，不报错（允许游客浏览作者主页）
            result.put("following", false);
            return success(result);
        }
        result.put("following", portalFollowService.isFollowing(currentUserId, userId));
        return success(result);
    }
}
