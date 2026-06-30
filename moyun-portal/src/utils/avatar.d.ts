/**
 * 头像工具类 - 提供头像处理和默认值
 */
/**
 * 获取头像，如果为空则返回默认头像
 * @param avatar 用户头像URL
 * @param seed 种子（用于区分不同用户的默认头像）
 */
export declare function getSafeAvatar(avatar?: string | null, seed?: string): string;
/**
 * 检查头像URL是否有效，如果无效则返回默认头像
 * @param avatar 用户头像URL
 * @param seed 种子
 */
export declare function validateAvatar(avatar?: string | null, seed?: string): string;
declare const _default: {
    getSafeAvatar: typeof getSafeAvatar;
    validateAvatar: typeof validateAvatar;
};
export default _default;
