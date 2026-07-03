package com.moyun.portal.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.moyun.common.constant.HttpStatus;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.mapper.PortalCreatorMapper;
import com.moyun.portal.util.PortalSecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 创作者中心 Controller（所有接口均需登录）
 * <p>
 * 不新建任何表，直接聚合 portal_article_view / portal_like / portal_bookmark /
 * portal_follow / portal_article 等现有表。
 *
 * @author moyun
 */
@Tag(name = "创作者中心", description = "数据看板、创作日历热力图、读者画像")
@RestController
@RequestMapping("/portal/creator")
public class PortalCreatorController extends BaseController {

    @Autowired
    private PortalCreatorMapper portalCreatorMapper;

    private Long currentUserId() {
        return PortalSecurityUtils.getUserId();
    }

    // ==================== 数据看板（近 30 天） ====================

    @Operation(summary = "数据看板", description = "近 30 天每日阅读/点赞/收藏/新增粉丝趋势")
    @GetMapping("/dashboard")
    public AjaxResult dashboard() {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        LocalDateTime startTime = LocalDate.now().minusDays(29).atStartOfDay();

        // 近 30 天日期序列（含今天），保证无数据日也填充 0
        List<String> dates = new ArrayList<>();
        Map<String, Long> viewMap = toLongMap(portalCreatorMapper.dailyViewTrend(userId, startTime));
        Map<String, Long> likeMap = toLongMap(portalCreatorMapper.dailyLikeTrend(userId, startTime));
        Map<String, Long> bookmarkMap = toLongMap(portalCreatorMapper.dailyBookmarkTrend(userId, startTime));
        Map<String, Long> followerMap = toLongMap(portalCreatorMapper.dailyFollowerTrend(userId, startTime));

        List<Long> views = new ArrayList<>();
        List<Long> likes = new ArrayList<>();
        List<Long> bookmarks = new ArrayList<>();
        List<Long> followers = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            String date = LocalDate.now().minusDays(29 - i).toString();
            dates.add(date);
            views.add(viewMap.getOrDefault(date, 0L));
            likes.add(likeMap.getOrDefault(date, 0L));
            bookmarks.add(bookmarkMap.getOrDefault(date, 0L));
            followers.add(followerMap.getOrDefault(date, 0L));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("dates", dates);
        result.put("views", views);
        result.put("likes", likes);
        result.put("bookmarks", bookmarks);
        result.put("followers", followers);
        return AjaxResult.success(result);
    }

    // ==================== 创作日历热力图（近 1 年） ====================

    @Operation(summary = "创作日历热力图", description = "近 1 年文章创建/更新按日统计")
    @GetMapping("/calendar")
    public AjaxResult calendar() {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        LocalDateTime startTime = LocalDate.now().minusYears(1).atStartOfDay();
        List<Map<String, Object>> rows = portalCreatorMapper.calendarHeatmap(userId, startTime);

        List<Map<String, Object>> result = new ArrayList<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                Map<String, Object> item = new HashMap<>();
                item.put("date", String.valueOf(row.get("date")));
                item.put("count", toLong(row.get("count")));
                result.add(item);
            }
        }
        return AjaxResult.success(result);
    }

    // ==================== 读者画像（近 30 天） ====================

    @Operation(summary = "读者画像", description = "近 30 天读者地域分布 Top10 与时段分布（0-23 时）")
    @GetMapping("/reader-profile")
    public AjaxResult readerProfile() {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        LocalDateTime startTime = LocalDate.now().minusDays(29).atStartOfDay();

        // 地域分布 Top10（无城市字段时按 IP 聚合，无数据则空数组）
        List<Map<String, Object>> regionRows = portalCreatorMapper.readerRegionTop10(userId, startTime);
        List<Map<String, Object>> regions = new ArrayList<>();
        if (regionRows != null) {
            for (Map<String, Object> row : regionRows) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("region", String.valueOf(row.get("region")));
                item.put("value", toLong(row.get("value")));
                regions.add(item);
            }
        }

        // 时段分布（0-23 时），保证 24 个桶均有值
        Map<Integer, Long> hourMap = new HashMap<>();
        List<Map<String, Object>> hourRows = portalCreatorMapper.readerHourDistribution(userId, startTime);
        if (hourRows != null) {
            for (Map<String, Object> row : hourRows) {
                int hour = toInt(row.get("hour"));
                hourMap.put(hour, toLong(row.get("value")));
            }
        }
        List<Map<String, Object>> hours = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("hour", h);
            item.put("value", hourMap.getOrDefault(h, 0L));
            hours.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("regions", regions);
        result.put("hours", hours);
        return AjaxResult.success(result);
    }

    // ==================== 私有工具方法 ====================

    private Map<String, Long> toLongMap(List<Map<String, Object>> rows) {
        Map<String, Long> map = new HashMap<>();
        if (rows == null) {
            return map;
        }
        for (Map<String, Object> row : rows) {
            String date = String.valueOf(row.get("date"));
            map.put(date, toLong(row.get("value")));
        }
        return map;
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private int toInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
