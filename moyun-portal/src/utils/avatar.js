/**
 * 头像工具类 - 提供头像处理和默认值
 */
// 默认头像 - 使用更稳定的服务
const DEFAULT_AVATARS = [
    'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIyMDAiIGhlaWdodD0iMjAwIj48ZGVmcz48cGF0dGVybiBpZD0iYSIgcGF0dGVyblVuaXRzPSJ1c2VyU3BhY2VPblVzZSIgd2lkdGg9IjEwIiBoZWlnaHQ9IjEwIj48Y2lyY2xlIGN4PSI1IiBjeT0iNSIgcj0iMiIgZmlsbD0iI2Y1ZjVmNSIvPjwvcGF0dGVybj48L2RlZnM+PHJlY3Qgd2lkdGg9IjIwMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiMwMDdCRkYiLz48Y2lyY2xlIGN4PSIxMDAiIGN5PSI3NSIgcj0iNDAiIGZpbGw9IiMwMERBNzYiLz48cGF0aCBkPSJNMzUgMjAwIGMxMi01NSA0Ni04NSA2NS04NSBoMzQgYzE5IDAgNTMgMzAgNjUgODV6IiBmaWxsPSIjMDREQzc2Ii8+PC9zdmc+',
    'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIyMDAiIGhlaWdodD0iMjAwIj48cmVjdCB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgZmlsbD0iI2Y0N2NjYSIvPjxjaXJjbGUgY3g9IjEwMCIgY3k9Ijc1IiByPSI0MCIgZmlsbD0iI2ZmZTc1ZiIvPjxwYXRoIGQ9Ik0zNSAyMDAgYzEyLTU1IDQ2LTg1IDY1LTg1IGgzNCBjMTkgMCA1MyAzMCA2NSA4NXoiIGZpbGw9IiNmZmU3NWYiLz48L3N2Zz4=',
    'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIyMDAiIGhlaWdodD0iMjAwIj48cmVjdCB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgZmlsbD0iIzNkOGNmOCIvPjxjaXJjbGUgY3g9IjEwMCIgY3k9Ijc1IiByPSI0MCIgZmlsbD0iI2FmZTVmZiIvPjxwYXRoIGQ9Ik0zNSAyMDAgYzEyLTU1IDQ2LTg1IDY1LTg1IGgzNCBjMTkgMCA1MyAzMCA2NSA4NXoiIGZpbGw9IiNhZmU1ZmYiLz48L3N2Zz4='
];
/**
 * 获取头像，如果为空则返回默认头像
 * @param avatar 用户头像URL
 * @param seed 种子（用于区分不同用户的默认头像）
 */
export function getSafeAvatar(avatar, seed) {
    if (avatar && avatar.trim()) {
        return avatar;
    }
    // 如果没有提供seed，返回第一个默认头像
    if (!seed) {
        return DEFAULT_AVATARS[0];
    }
    // 根据seed选择对应的默认头像
    let hash = 0;
    for (let i = 0; i < seed.length; i++) {
        const char = seed.charCodeAt(i);
        hash = ((hash << 5) - hash) + char;
        hash = hash & hash; // 转换为32位整数
    }
    return DEFAULT_AVATARS[Math.abs(hash) % DEFAULT_AVATARS.length];
}
/**
 * 检查头像URL是否有效，如果无效则返回默认头像
 * @param avatar 用户头像URL
 * @param seed 种子
 */
export function validateAvatar(avatar, seed) {
    if (!avatar) {
        return getSafeAvatar(null, seed);
    }
    // 检查是否是有效的URL或data URL
    if (avatar.startsWith('http://') || avatar.startsWith('https://') || avatar.startsWith('data:')) {
        return avatar;
    }
    return getSafeAvatar(null, seed);
}
export default {
    getSafeAvatar,
    validateAvatar
};
