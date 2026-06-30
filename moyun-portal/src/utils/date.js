import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';
import relativeTime from 'dayjs/plugin/relativeTime';
// 配置 dayjs
dayjs.locale('zh-cn');
dayjs.extend(relativeTime);
/**
 * 格式化日期
 * @param date 日期字符串或 Date 对象
 * @param format 格式化模板，默认为 'YYYY-MM-DD HH:mm:ss'
 * @returns 格式化后的日期字符串
 */
export function formatDate(date, format = 'YYYY-MM-DD HH:mm:ss') {
    if (!date)
        return '';
    return dayjs(date).format(format);
}
/**
 * 相对时间格式化（比如：3分钟前，1小时前）
 * @param date 日期字符串或 Date 对象
 * @returns 相对时间字符串
 */
export function formatRelativeTime(date) {
    if (!date)
        return '';
    return dayjs(date).fromNow();
}
/**
 * 格式化为简短日期
 * @param date 日期字符串或 Date 对象
 * @returns 日期字符串，格式为 YYYY-MM-DD HH:mm
 */
export function formatShortDate(date) {
    if (!date)
        return '';
    return dayjs(date).format('YYYY-MM-DD HH:mm');
}
/**
 * 获取两个日期之间的天数差
 * @param date1 日期1
 * @param date2 日期2，默认为今天
 * @returns 天数差
 */
export function getDaysDiff(date1, date2 = new Date()) {
    return dayjs(date2).diff(dayjs(date1), 'day');
}
/**
 * 检查日期是否是今天
 * @param date 日期
 * @returns 是否是今天
 */
export function isToday(date) {
    return dayjs(date).isSame(dayjs(), 'day');
}
/**
 * 检查日期是否是本周
 * @param date 日期
 * @returns 是否是本周
 */
export function isThisWeek(date) {
    return dayjs(date).isSame(dayjs(), 'week');
}
/**
 * 检查日期是否是本月
 * @param date 日期
 * @returns 是否是本月
 */
export function isThisMonth(date) {
    return dayjs(date).isSame(dayjs(), 'month');
}
export default dayjs;
