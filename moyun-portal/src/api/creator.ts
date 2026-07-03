import { httpGet } from './client';

/** 数据看板（近 30 天趋势） */
export interface CreatorDashboard {
  dates: string[];
  views: number[];
  likes: number[];
  bookmarks: number[];
  followers: number[];
}

/** 创作日历热力图单元 */
export interface CalendarCell {
  date: string;
  count: number;
}

/** 读者地域分布项 */
export interface ReaderRegion {
  region: string;
  value: number;
}

/** 读者时段分布项 */
export interface ReaderHour {
  hour: number;
  value: number;
}

/** 读者画像 */
export interface ReaderProfile {
  regions: ReaderRegion[];
  hours: ReaderHour[];
}

/**
 * 数据看板（需登录）
 * GET /portal/creator/dashboard
 */
export const getCreatorDashboard = () => {
  return httpGet<CreatorDashboard>('/portal/creator/dashboard');
};

/**
 * 创作日历热力图（需登录）
 * GET /portal/creator/calendar
 */
export const getCreatorCalendar = () => {
  return httpGet<CalendarCell[]>('/portal/creator/calendar');
};

/**
 * 读者画像（需登录）
 * GET /portal/creator/reader-profile
 */
export const getReaderProfile = () => {
  return httpGet<ReaderProfile>('/portal/creator/reader-profile');
};
