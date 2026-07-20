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
import com.moyun.util.ip.AddressUtils;

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

    @Operation(summary = "读者画像", description = "近 30 天读者地域分布 Top10 + 性别分布 + 年龄段分布 + 时段分布（0-23 时）")
    @GetMapping("/reader-profile")
    public AjaxResult readerProfile() {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        LocalDateTime startTime = LocalDate.now().minusDays(29).atStartOfDay();

        // ===== 1. 地域分布 Top10 =====
        // 数据源：portal_article_view.ip，用 AddressUtils 解析为"省 市"，再按省份二次聚合
        List<Map<String, Object>> regionRows = portalCreatorMapper.readerRegionTop10(userId, startTime);
        // 第一阶段：IP → 省份（用 Map 聚合去重）
        Map<String, Long> provinceAgg = new LinkedHashMap<>();
        if (regionRows != null) {
            for (Map<String, Object> row : regionRows) {
                String ip = String.valueOf(row.get("region"));
                long count = toLong(row.get("value"));
                String province = resolveProvinceFromIp(ip);
                provinceAgg.merge(province, count, Long::sum);
            }
        }
        // 第二阶段：按聚合后人数倒序排 Top10，计算占比
        long regionTotal = provinceAgg.values().stream().mapToLong(Long::longValue).sum();
        List<Map<String, Object>> regions = new ArrayList<>();
        provinceAgg.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .forEach(e -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("region", e.getKey());
                    item.put("value", e.getValue());
                    // 占比百分比（保留 1 位小数）
                    double pct = regionTotal > 0 ? (e.getValue() * 100.0 / regionTotal) : 0.0;
                    item.put("percentage", Math.round(pct * 10) / 10.0);
                    regions.add(item);
                });

        // ===== 2. 性别分布 =====
        // 后端返回的桶 key：male / female / other / unknown（unknown = 未填写或游客无法统计）
        Map<String, Long> genderMap = new LinkedHashMap<>();
        // 默认桶顺序，保证前端展示稳定
        genderMap.put("male", 0L);
        genderMap.put("female", 0L);
        genderMap.put("other", 0L);
        genderMap.put("unknown", 0L);
        List<Map<String, Object>> genderRows = portalCreatorMapper.readerGenderDistribution(userId, startTime);
        if (genderRows != null) {
            for (Map<String, Object> row : genderRows) {
                String g = String.valueOf(row.get("gender"));
                if (g == null || g.isEmpty() || "null".equals(g)) {
                    g = "unknown";
                }
                genderMap.put(g, toLong(row.get("value")));
            }
        }
        long genderTotal = genderMap.values().stream().mapToLong(Long::longValue).sum();
        List<Map<String, Object>> genders = new ArrayList<>();
        for (Map.Entry<String, Long> e : genderMap.entrySet()) {
            if (e.getValue() == 0L) continue; // 跳过 0 桶，减少前端渲染负担
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("gender", e.getKey());
            item.put("value", e.getValue());
            double pct = genderTotal > 0 ? (e.getValue() * 100.0 / genderTotal) : 0.0;
            item.put("percentage", Math.round(pct * 10) / 10.0);
            genders.add(item);
        }

        // ===== 3. 年龄段分布 =====
        // 桶顺序：under_18 / 18_24 / 25_30 / 31_35 / 36_45 / over_45 / unknown
        Map<String, Long> ageMap = new LinkedHashMap<>();
        ageMap.put("under_18", 0L);
        ageMap.put("18_24", 0L);
        ageMap.put("25_30", 0L);
        ageMap.put("31_35", 0L);
        ageMap.put("36_45", 0L);
        ageMap.put("over_45", 0L);
        ageMap.put("unknown", 0L);
        List<Map<String, Object>> ageRows = portalCreatorMapper.readerAgeRangeDistribution(userId, startTime);
        if (ageRows != null) {
            for (Map<String, Object> row : ageRows) {
                String ar = String.valueOf(row.get("age_range"));
                if (ar == null || ar.isEmpty() || "null".equals(ar)) {
                    ar = "unknown";
                }
                ageMap.put(ar, toLong(row.get("value")));
            }
        }
        long ageTotal = ageMap.values().stream().mapToLong(Long::longValue).sum();
        List<Map<String, Object>> ageRanges = new ArrayList<>();
        for (Map.Entry<String, Long> e : ageMap.entrySet()) {
            if (e.getValue() == 0L) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("range", e.getKey());
            item.put("value", e.getValue());
            double pct = ageTotal > 0 ? (e.getValue() * 100.0 / ageTotal) : 0.0;
            item.put("percentage", Math.round(pct * 10) / 10.0);
            ageRanges.add(item);
        }

        // ===== 4. 时段分布（0-23 时），保证 24 个桶均有值 =====
        Map<Integer, Long> hourMap = new HashMap<>();
        List<Map<String, Object>> hourRows = portalCreatorMapper.readerHourDistribution(userId, startTime);
        if (hourRows != null) {
            for (Map<String, Object> row : hourRows) {
                int hour = toInt(row.get("hour"));
                hourMap.put(hour, toLong(row.get("value")));
            }
        }
        long hourTotal = hourMap.values().stream().mapToLong(Long::longValue).sum();
        List<Map<String, Object>> hours = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("hour", h);
            long v = hourMap.getOrDefault(h, 0L);
            item.put("value", v);
            double pct = hourTotal > 0 ? (v * 100.0 / hourTotal) : 0.0;
            item.put("percentage", Math.round(pct * 10) / 10.0);
            hours.add(item);
        }

        // ===== 数据局限提示（前端可展示在卡片下方） =====
        // 说明为何性别/年龄段可能不真实：游客无法统计 + birthday 自填可能不准
        Map<String, Object> dataNote = new LinkedHashMap<>();
        dataNote.put("genderNote", "仅统计登录读者，游客不参与；读者未填写性别时归入 unknown");
        dataNote.put("ageRangeNote", "基于读者自填生日计算，非实名用户可能不准；游客不参与统计");
        dataNote.put("regionNote", "按读者 IP 解析省份聚合，内网 IP 归入'未知'");

        Map<String, Object> result = new HashMap<>();
        result.put("regions", regions);
        result.put("genders", genders);
        result.put("ageRanges", ageRanges);
        result.put("hours", hours);
        result.put("dataNote", dataNote);
        return AjaxResult.success(result);
    }

    /**
     * v1.1 读者画像：把 IP 解析为省份名（用于地域分布地图组件）
     * <p>策略：
     * <ul>
     *   <li>调用 AddressUtils.getRealAddressByIP 解析，返回 "省 市" 或 "内网IP" / "XX XX"</li>
     *   <li>提取省份部分（首个空格前），城市部分忽略</li>
     *   <li>解析失败或内网 IP 时返回 "未知"</li>
     *   <li>AddressUtils 受 RuoYiConfig.isAddressEnabled() 开关控制，关闭时返回 "XX XX"</li>
     * </ul>
     * </p>
     */
    private String resolveProvinceFromIp(String ip) {
        if (ip == null || ip.isEmpty() || "未知".equals(ip) || "null".equals(ip)) {
            return "未知";
        }
        try {
            String full = AddressUtils.getRealAddressByIP(ip);
            if (full == null || full.isEmpty() || "XX XX".equals(full) || "内网IP".equals(full)) {
                return "未知";
            }
            // full 格式："省 市"（空格分隔），取省份部分
            int spaceIdx = full.indexOf(' ');
            if (spaceIdx > 0) {
                return full.substring(0, spaceIdx);
            }
            return full;
        } catch (Exception e) {
            return "未知";
        }
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
