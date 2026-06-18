import { httpGet, httpPost } from './client';
import type {
  UserGrowthVO,
  UserStatsVO,
  UserBadgeVO,
  AchievementVO,
  GrowthRankingItem,
  CheckinResult,
} from '@/types/api';

// 获取当前登录用户的成长信息
export const getMyGrowth = () => {
  return httpGet<UserGrowthVO>('/portal/growth/me');
};

// 获取指定用户的成长信息（公开接口）
export const getUserGrowth = (userId: string | number) => {
  return httpGet<UserGrowthVO>(`/portal/growth/user/${userId}`);
};

// 获取当前登录用户的统计信息
export const getMyStats = () => {
  return httpGet<UserStatsVO>('/portal/growth/stats');
};

// 获取指定用户的统计信息（公开接口）
export const getUserStatsById = (userId: string | number) => {
  return httpGet<UserStatsVO>(`/portal/growth/stats`, { userId });
};

// 获取当前登录用户的徽章列表
export const getMyBadges = () => {
  return httpGet<UserBadgeVO[]>('/portal/growth/badges');
};

// 获取指定用户的徽章列表（公开接口）
export const getUserBadges = (userId: string | number) => {
  return httpGet<UserBadgeVO[]>(`/portal/growth/badges`, { userId });
};

// 获取所有成就列表（含当前用户达成状态）
export const getMyAchievements = () => {
  return httpGet<AchievementVO[]>('/portal/growth/achievements');
};

// 获取指定用户的成就达成状态（公开接口）
export const getUserAchievements = (userId: string | number) => {
  return httpGet<AchievementVO[]>(`/portal/growth/achievements`, { userId });
};

// 获取成长值排行榜（公开接口）
export const getGrowthRanking = (limit = 10) => {
  return httpGet<GrowthRankingItem[]>('/portal/growth/ranking', { limit });
};

// 每日签到
export const checkin = () => {
  return httpPost<CheckinResult>('/portal/growth/checkin');
};
