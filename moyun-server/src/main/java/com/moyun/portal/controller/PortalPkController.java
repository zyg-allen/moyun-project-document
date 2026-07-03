package com.moyun.portal.controller;

import com.moyun.common.annotation.Anonymous;
import com.moyun.common.constant.HttpStatus;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.cms.service.IPkService;
import com.moyun.portal.util.PortalSecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * PK 对战 Controller（阶段三 3.7 排行榜 / PK）
 * <p>
 * 接口前缀：/portal/learn/pk
 * <ul>
 *   <li>发起挑战：POST /portal/learn/pk/challenge （需登录）</li>
 *   <li>接受挑战：POST /portal/learn/pk/{id}/accept （需登录）</li>
 *   <li>拒绝挑战：POST /portal/learn/pk/{id}/decline （需登录）</li>
 *   <li>提交答案：POST /portal/learn/pk/{id}/answer （需登录）</li>
 *   <li>对战详情：GET /portal/learn/pk/{id} （需登录）</li>
 *   <li>我的对战：GET /portal/learn/pk/my/list （需登录）</li>
 *   <li>公司挑战榜：GET /portal/learn/pk/leaderboard/company （公开）</li>
 * </ul>
 *
 * @author moyun
 */
@Tag(name = "PK 对战", description = "好友 PK / 公司题目挑战榜（异步对战）")
@RestController
@RequestMapping("/portal/learn/pk")
public class PortalPkController extends BaseController {

    @Autowired
    private IPkService pkService;

    private Long currentUserId() {
        return PortalSecurityUtils.getUserId();
    }

    // ==================== 发起 / 接受 / 拒绝 ====================

    @Operation(summary = "发起挑战", description = "从题库随机抽 5 题发起一场异步对战")
    @PostMapping("/challenge")
    public AjaxResult challenge(@RequestBody Map<String, Object> body) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        Long opponentId = toLong(body.get("opponentId"));
        String scene = body.get("scene") != null ? body.get("scene").toString() : "1v1";
        Long companyId = toLong(body.get("companyId"));
        return AjaxResult.success(pkService.createChallenge(userId, opponentId, scene, companyId));
    }

    @Operation(summary = "接受挑战", description = "应战方接受，状态 pending -> ongoing")
    @PostMapping("/{id}/accept")
    public AjaxResult accept(@PathVariable("id") Long id) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(pkService.acceptChallenge(id, userId));
    }

    @Operation(summary = "拒绝挑战", description = "应战方拒绝，状态 pending -> declined")
    @PostMapping("/{id}/decline")
    public AjaxResult decline(@PathVariable("id") Long id) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(pkService.declineChallenge(id, userId));
    }

    // ==================== 答题 ====================

    @Operation(summary = "提交答案", description = "提交某题答案并计分；双方全部答完时自动结算")
    @PostMapping("/{id}/answer")
    public AjaxResult answer(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        Long questionId = toLong(body.get("questionId"));
        String answer = body.get("answer") != null ? body.get("answer").toString() : null;
        return AjaxResult.success(pkService.submitAnswer(id, userId, questionId, answer));
    }

    // ==================== 查询 ====================

    @Operation(summary = "对战详情", description = "对战详情，含双方得分与题目简要")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable("id") Long id) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(pkService.getChallengeDetail(id, userId));
    }

    @Operation(summary = "我的对战列表", description = "作为发起方或应战方的对战，可按 status 筛选")
    @GetMapping("/my/list")
    public AjaxResult myList(
            @Parameter(description = "状态筛选：pending/ongoing/finished")
            @RequestParam(value = "status", required = false) String status) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(pkService.getMyChallenges(userId, status));
    }

    // ==================== 公司题目挑战榜（公开） ====================

    @Operation(summary = "公司题目挑战榜", description = "按 company_id 聚合用户通过题数，未传 companyId 时聚合所有公司")
    @GetMapping("/leaderboard/company")
    @Anonymous
    public AjaxResult companyLeaderboard(
            @Parameter(description = "公司ID（可选）")
            @RequestParam(value = "companyId", required = false) Long companyId,
            @Parameter(description = "取前 N 名，默认 100，上限 100")
            @RequestParam(value = "limit", required = false) Integer limit) {
        return AjaxResult.success(pkService.getCompanyLeaderboard(companyId, limit));
    }

    private static Long toLong(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        try {
            return Long.parseLong(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
