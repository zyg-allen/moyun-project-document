package com.moyun.portal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.vo.AchievementVO;
import com.moyun.portal.domain.vo.UserBadgeVO;
import com.moyun.portal.domain.vo.UserGrowthVO;
import com.moyun.portal.domain.vo.UserStatsVO;
import com.moyun.portal.service.IPortalGrowthService;
import com.moyun.portal.util.PortalSecurityUtils;

import java.util.List;

/**
 * 门户用户成长体系 接口
 *
 * 提供成长信息查询、统计、徽章、排行榜、签到等接口。
 *
 * @author moyun
 */
@Tag(name = "用户成长", description = "用户成长体系相关接口")
@RestController
@RequestMapping("/portal/growth")
@Slf4j
public class PortalGrowthController extends BaseController {

    @Autowired
    private IPortalGrowthService portalGrowthService;

    @Operation(summary = "获取当前用户成长信息", description = "获取成长值、等级、头衔、赛季排名等")
    @GetMapping("/me")
    public AjaxResult getMyGrowth() {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return error("用户未登录");
        }
        UserGrowthVO vo = portalGrowthService.getUserGrowth(userId);
        return success(vo);
    }

    @Operation(summary = "获取指定用户成长信息")
    @GetMapping("/user/{userId}")
    public AjaxResult getUserGrowth(@PathVariable Long userId) {
        UserGrowthVO vo = portalGrowthService.getUserGrowth(userId);
        return success(vo);
    }

    @Operation(summary = "获取当前用户统计信息", description = "文章数、浏览量、获赞数、解题数等")
    @GetMapping("/stats")
    public AjaxResult getMyStats(
            @Parameter(description = "用户ID（可选，不传则获取当前登录用户）")
            @RequestParam(value = "userId", required = false) Long userId) {
        Long targetUserId = userId;
        if (targetUserId == null) {
            targetUserId = PortalSecurityUtils.getUserId();
            if (targetUserId == null) {
                return error("用户未登录");
            }
        }
        UserStatsVO vo = portalGrowthService.getUserStats(targetUserId);
        return success(vo);
    }

    @Operation(summary = "获取用户徽章列表")
    @GetMapping("/badges")
    public AjaxResult getMyBadges(
            @Parameter(description = "用户ID（可选，不传则获取当前登录用户）")
            @RequestParam(value = "userId", required = false) Long userId) {
        Long targetUserId = userId;
        if (targetUserId == null) {
            targetUserId = PortalSecurityUtils.getUserId();
            if (targetUserId == null) {
                return error("用户未登录");
            }
        }
        List<UserBadgeVO> list = portalGrowthService.getUserBadges(targetUserId);
        return success(list);
    }

    @Operation(summary = "获取所有成就列表", description = "返回所有启用的成就，含当前用户达成状态（未登录时 earned 恒为 false）")
    @GetMapping("/achievements")
    public AjaxResult getAllAchievements(
            @Parameter(description = "用户ID（可选，不传则使用当前登录用户）")
            @RequestParam(value = "userId", required = false) Long userId) {
        Long targetUserId = userId;
        if (targetUserId == null) {
            targetUserId = PortalSecurityUtils.getUserId();
        }
        List<AchievementVO> list = portalGrowthService.getAllAchievements(targetUserId);
        return success(list);
    }

    @Operation(summary = "获取赛季排行榜", description = "按本季成长值排名")
    @GetMapping("/ranking")
    public AjaxResult getRanking(
            @Parameter(description = "前N名，默认50")
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        if (limit > 100) limit = 100;
        List<UserGrowthVO> list = portalGrowthService.getRanking(limit);
        return success(list);
    }

    @Operation(summary = "每日签到", description = "签到获得成长值，连续签到有额外奖励")
    @PostMapping("/checkin")
    public AjaxResult checkin() {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return error("用户未登录");
        }
        return success(portalGrowthService.checkin(userId));
    }
}
