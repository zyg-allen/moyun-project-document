package com.moyun.portal.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 特殊日期提供者（写作提示模块）
 *
 * <p>为每日写作 prompt 提供"今天是什么日子"上下文，供 AI 生成时结合：
 * <ul>
 *   <li>公历固定日期节日/纪念日（元旦、读书日、程序员节等）</li>
 *   <li>周序节日（母亲节=5月第2个周日、父亲节=6月第3个周日、感恩节=11月第4个周四）</li>
 *   <li>二十四节气（21 世纪通用近似公式，误差 ±1 天，对写作主题场景足够）</li>
 * </ul>
 *
 * <p>说明：农历传统节日（春节/端午/中秋）日期每年变化且需农历换算，
 * 不在此处硬编码，由 AI 模型依据其自身知识自行判断"是否临近农历节日"。
 *
 * @author moyun
 */
public final class SpecialDateProvider {

    private SpecialDateProvider() {
    }

    /** 公历固定节日/纪念日：MonthDay → 名称（精选与写作、生活、职业相关） */
    private static final Map<MonthDay, String> FIXED_DATES = new HashMap<>();

    static {
        FIXED_DATES.put(MonthDay.of(1, 1), "元旦");
        FIXED_DATES.put(MonthDay.of(2, 14), "情人节");
        FIXED_DATES.put(MonthDay.of(3, 8), "国际妇女节");
        FIXED_DATES.put(MonthDay.of(3, 12), "植树节");
        FIXED_DATES.put(MonthDay.of(4, 23), "世界读书日");
        FIXED_DATES.put(MonthDay.of(5, 1), "国际劳动节");
        FIXED_DATES.put(MonthDay.of(5, 4), "中国青年节");
        FIXED_DATES.put(MonthDay.of(6, 1), "国际儿童节");
        FIXED_DATES.put(MonthDay.of(8, 1), "建军节");
        FIXED_DATES.put(MonthDay.of(9, 10), "教师节");
        FIXED_DATES.put(MonthDay.of(10, 1), "国庆节");
        FIXED_DATES.put(MonthDay.of(10, 24), "程序员节");
        FIXED_DATES.put(MonthDay.of(11, 8), "记者节");
        FIXED_DATES.put(MonthDay.of(12, 25), "圣诞节");
    }

    /**
     * 获取指定日期的全部特殊日期名称（节日/纪念日/节气），无则返回空列表。
     */
    public static List<String> getSpecialDates(LocalDate date) {
        List<String> result = new ArrayList<>();
        if (date == null) {
            return result;
        }
        // 1. 公历固定节日
        String fixed = FIXED_DATES.get(MonthDay.from(date));
        if (fixed != null) {
            result.add(fixed);
        }
        // 2. 周序节日
        String weekly = getWeeklyHoliday(date);
        if (weekly != null) {
            result.add(weekly);
        }
        // 3. 二十四节气（近似公式）
        String term = getSolarTerm(date);
        if (term != null) {
            result.add(term);
        }
        return result;
    }

    /**
     * 周序节日判断：母亲节（5月第2个周日）、父亲节（6月第3个周日）、感恩节（11月第4个周四）。
     */
    private static String getWeeklyHoliday(LocalDate date) {
        switch (date.getMonth()) {
            case MAY:
                if (isNthDayOfWeek(date, DayOfWeek.SUNDAY, 2)) {
                    return "母亲节";
                }
                break;
            case JUNE:
                if (isNthDayOfWeek(date, DayOfWeek.SUNDAY, 3)) {
                    return "父亲节";
                }
                break;
            case NOVEMBER:
                if (isNthDayOfWeek(date, DayOfWeek.THURSDAY, 4)) {
                    return "感恩节";
                }
                break;
            default:
                break;
        }
        return null;
    }

    /** 判断 date 是否为当月第 n 个指定星期几 */
    private static boolean isNthDayOfWeek(LocalDate date, DayOfWeek target, int nth) {
        if (date.getDayOfWeek() != target) {
            return false;
        }
        LocalDate nthDay = date.with(TemporalAdjusters.dayOfWeekInMonth(nth, target));
        return date.isEqual(nthDay);
    }

    // ==================== 二十四节气（21 世纪近似公式） ====================

    /** 24 节气：月份 → [节气名, 世纪常数 C]；小寒/大寒等 1 月节气归入上年计算，此处按当月近似 */
    private static final double[][] TERM_CONSTANTS = {
            {5.4055, 20.12},   // 1 月：小寒、大寒
            {3.87, 18.73},     // 2 月：立春、雨水
            {5.63, 20.646},    // 3 月：惊蛰、春分
            {4.81, 20.1},      // 4 月：清明、谷雨
            {5.52, 21.04},     // 5 月：立夏、小满
            {5.678, 21.37},    // 6 月：芒种、夏至
            {7.108, 22.83},    // 7 月：小暑、大暑
            {7.5, 22.96},      // 8 月：立秋、处暑
            {7.646, 23.032},   // 9 月：白露、秋分
            {8.318, 23.438},   // 10 月：寒露、霜降
            {7.438, 22.36},    // 11 月：立冬、小雪
            {7.18, 21.94}      // 12 月：大雪、冬至
    };

    private static final String[][] TERM_NAMES = {
            {"小寒", "大寒"}, {"立春", "雨水"}, {"惊蛰", "春分"}, {"清明", "谷雨"},
            {"立夏", "小满"}, {"芒种", "夏至"}, {"小暑", "大暑"}, {"立秋", "处暑"},
            {"白露", "秋分"}, {"寒露", "霜降"}, {"立冬", "小雪"}, {"大雪", "冬至"}
    };

    /**
     * 计算指定日期对应的节气（非节气日返回 null）。
     * 通用公式：[Y×D + C] - L，Y=年份后两位，L=(Y-1)/4 的整数部分；误差 ±1 天。
     */
    static String getSolarTerm(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();
        int y = date.getYear() % 100;
        int leap = (y - 1) / 4;
        for (int i = 0; i < 2; i++) {
            double c = TERM_CONSTANTS[month - 1][i];
            int termDay = (int) (y * 0.2422 + c) - leap;
            // 1 月的闰年修正（2000 年等特殊年份，简化处理）
            if (month == 1 && (date.getYear() % 4 == 0)) {
                termDay = termDay - 1;
            }
            if (termDay == day) {
                return TERM_NAMES[month - 1][i];
            }
        }
        return null;
    }
}
