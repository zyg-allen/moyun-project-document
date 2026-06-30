import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';
/**
 * 格式化日期
 * @param date 日期字符串或 Date 对象
 * @param format 格式化模板，默认为 'YYYY-MM-DD HH:mm:ss'
 * @returns 格式化后的日期字符串
 */
export declare function formatDate(date: string | Date, format?: string): string;
/**
 * 相对时间格式化（比如：3分钟前，1小时前）
 * @param date 日期字符串或 Date 对象
 * @returns 相对时间字符串
 */
export declare function formatRelativeTime(date: string | Date): string;
/**
 * 格式化为简短日期
 * @param date 日期字符串或 Date 对象
 * @returns 日期字符串，格式为 YYYY-MM-DD HH:mm
 */
export declare function formatShortDate(date: string | Date): string;
/**
 * 获取两个日期之间的天数差
 * @param date1 日期1
 * @param date2 日期2，默认为今天
 * @returns 天数差
 */
export declare function getDaysDiff(date1: string | Date, date2?: string | Date): number;
/**
 * 检查日期是否是今天
 * @param date 日期
 * @returns 是否是今天
 */
export declare function isToday(date: string | Date): boolean;
/**
 * 检查日期是否是本周
 * @param date 日期
 * @returns 是否是本周
 */
export declare function isThisWeek(date: string | Date): boolean;
/**
 * 检查日期是否是本月
 * @param date 日期
 * @returns 是否是本月
 */
export declare function isThisMonth(date: string | Date): boolean;
export default dayjs;
