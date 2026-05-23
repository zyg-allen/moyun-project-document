package com.moyun.community.growth.controller.app;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.common.core.controller.BaseController;
import com.moyun.common.core.domain.AjaxResult;
import com.moyun.common.core.domain.TableDataInfo;
import com.moyun.common.utils.SecurityUtils;
import com.moyun.community.growth.entity.CmsPointsLog;
import com.moyun.community.growth.entity.CmsUserBadge;
import com.moyun.community.growth.entity.CmsLevelConfig;
import com.moyun.community.growth.service.ICmsPointsLogService;
import com.moyun.community.growth.service.ICmsUserBadgeService;
import com.moyun.community.growth.service.ICmsLevelConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/app/growth")
public class GrowthAppController extends BaseController {

    @Autowired
    private ICmsPointsLogService pointsLogService;

    @Autowired
    private ICmsUserBadgeService userBadgeService;

    @Autowired
    private ICmsLevelConfigService levelConfigService;

    @GetMapping("/points/log")
    public TableDataInfo pointsLog() {
        Long userId = SecurityUtils.getUserId();
        startPage();
        List<CmsPointsLog> list = pointsLogService.list(
            new LambdaQueryWrapper<CmsPointsLog>()
                .eq(CmsPointsLog::getUserId, userId)
                .orderByDesc(CmsPointsLog::getCreateTime)
        );
        return getDataTable(list);
    }

    @GetMapping("/badges")
    public AjaxResult badges() {
        Long userId = SecurityUtils.getUserId();
        List<CmsUserBadge> list = userBadgeService.list(
            new LambdaQueryWrapper<CmsUserBadge>()
                .eq(CmsUserBadge::getUserId, userId)
                .orderByDesc(CmsUserBadge::getCreateTime)
        );
        return success(list);
    }

    @GetMapping("/level")
    public AjaxResult level() {
        Long userId = SecurityUtils.getUserId();

        Long totalPoints = pointsLogService.getTotalPoints(userId);

        CmsLevelConfig currentLevel = levelConfigService.getLevelByPoints(totalPoints);

        CmsLevelConfig nextLevel = null;
        Long pointsToNextLevel = 0L;
        double progressPercent = 100.0;

        if (currentLevel != null) {
            nextLevel = levelConfigService.getNextLevel(currentLevel.getLevel());
            if (nextLevel != null) {
                pointsToNextLevel = nextLevel.getMinPoints() - totalPoints;
                long levelRange = nextLevel.getMinPoints() - currentLevel.getMinPoints();
                long currentProgress = totalPoints - currentLevel.getMinPoints();
                progressPercent = (double) currentProgress / levelRange * 100;
            }
        } else {
            currentLevel = levelConfigService.getFirstLevel();
            if (currentLevel != null) {
                nextLevel = levelConfigService.getNextLevel(currentLevel.getLevel());
                if (nextLevel != null) {
                    pointsToNextLevel = nextLevel.getMinPoints() - totalPoints;
                    long levelRange = nextLevel.getMinPoints() - currentLevel.getMinPoints();
                    long currentProgress = totalPoints - currentLevel.getMinPoints();
                    progressPercent = (double) currentProgress / levelRange * 100;
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalPoints", totalPoints);
        result.put("currentLevel", currentLevel);
        result.put("nextLevel", nextLevel);
        result.put("pointsToNextLevel", pointsToNextLevel);
        result.put("progressPercent", Math.min(progressPercent, 100.0));

        return success(result);
    }
}
