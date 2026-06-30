import { httpGet, httpPost } from './client';
// 获取当前登录用户的成长信息
export const getMyGrowth = () => {
    return httpGet('/portal/growth/me');
};
// 获取指定用户的成长信息（公开接口）
export const getUserGrowth = (userId) => {
    return httpGet(`/portal/growth/user/${userId}`);
};
// 获取当前登录用户的统计信息
export const getMyStats = () => {
    return httpGet('/portal/growth/stats');
};
// 获取指定用户的统计信息（公开接口）
export const getUserStatsById = (userId) => {
    return httpGet(`/portal/growth/stats`, { userId });
};
// 获取当前登录用户的徽章列表
export const getMyBadges = () => {
    return httpGet('/portal/growth/badges');
};
// 获取指定用户的徽章列表（公开接口）
export const getUserBadges = (userId) => {
    return httpGet(`/portal/growth/badges`, { userId });
};
// 获取所有成就列表（含当前用户达成状态）
export const getMyAchievements = () => {
    return httpGet('/portal/growth/achievements');
};
// 获取指定用户的成就达成状态（公开接口）
export const getUserAchievements = (userId) => {
    return httpGet(`/portal/growth/achievements`, { userId });
};
// 获取成长值排行榜（公开接口）
export const getGrowthRanking = (limit = 10) => {
    return httpGet('/portal/growth/ranking', { limit });
};
// 每日签到
export const checkin = () => {
    return httpPost('/portal/growth/checkin');
};
