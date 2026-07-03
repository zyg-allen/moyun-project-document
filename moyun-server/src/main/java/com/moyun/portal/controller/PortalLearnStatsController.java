package com.moyun.portal.controller;

import com.moyun.common.annotation.Anonymous;
import com.moyun.common.constant.HttpStatus;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.cms.domain.vo.KnowledgeGraphVO;
import com.moyun.ext.cms.domain.vo.LearnCalendarCellVO;
import com.moyun.ext.cms.domain.vo.LeaderboardVO;
import com.moyun.ext.cms.service.ILearnStatsService;
import com.moyun.portal.util.PortalSecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学习统计 Controller（阶段三 3.4 / 3.5 / 3.7）
 * <p>
 * 接口前缀：/portal/learn
 * <ul>
 *   <li>3.4 刷题日历热力图：GET /portal/learn/calendar?year=2026 （需登录）</li>
 *   <li>3.5 知识图谱 / 标签云：GET /portal/learn/knowledge-graph?userId=123 （公开，userId 可选）</li>
 *   <li>3.7 排行榜：GET /portal/learn/leaderboard?type=question|score （公开）</li>
 * </ul>
 * 仅做查询聚合，不新建任何表。
 *
 * @author moyun
 */
@Tag(name = "学习统计", description = "刷题日历、知识图谱、排行榜")
@RestController
@RequestMapping("/portal/learn")
public class PortalLearnStatsController extends BaseController {

    @Autowired
    private ILearnStatsService learnStatsService;

    private Long currentUserId() {
        return PortalSecurityUtils.getUserId();
    }

    // ==================== 3.4 刷题日历热力图（需登录） ====================

    @Operation(summary = "刷题日历热力图", description = "按日聚合当前用户某年的提交数与通过数")
    @GetMapping("/calendar")
    public AjaxResult calendar(
            @Parameter(description = "年份，不传则取当前年份")
            @RequestParam(value = "year", required = false) Integer year) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        List<LearnCalendarCellVO> list = learnStatsService.getCalendar(userId, year);
        return AjaxResult.success(list);
    }

    // ==================== 3.5 知识图谱 / 标签云（公开） ====================

    @Operation(summary = "知识图谱 / 标签云", description = "公开访问；传入 userId 时计算该用户的标签掌握度，未传且已登录时使用当前用户")
    @GetMapping("/knowledge-graph")
    @Anonymous
    public AjaxResult knowledgeGraph(
            @Parameter(description = "用户ID（可选，传入时计算该用户掌握度）")
            @RequestParam(value = "userId", required = false) Long userId) {
        // 未显式传 userId 时，若已登录则回退到当前用户，便于"我的知识图谱"场景
        Long target = userId;
        if (target == null) {
            target = currentUserId();
        }
        KnowledgeGraphVO vo = learnStatsService.getKnowledgeGraph(target);
        return AjaxResult.success(vo);
    }

    // ==================== 3.7 排行榜 / PK（公开） ====================

    @Operation(summary = "排行榜", description = "type=question 通过题目数榜 / type=score 刷题积分榜，Top100；已登录时附带我的排名")
    @GetMapping("/leaderboard")
    @Anonymous
    public AjaxResult leaderboard(
            @Parameter(description = "排行类型：question=通过题目数 / score=刷题积分")
            @RequestParam(value = "type", required = false) String type,
            @Parameter(description = "取前 N 名，默认 100，上限 100")
            @RequestParam(value = "limit", required = false) Integer limit) {
        LeaderboardVO vo = learnStatsService.getLeaderboard(type, limit, currentUserId());
        return AjaxResult.success(vo);
    }
}
