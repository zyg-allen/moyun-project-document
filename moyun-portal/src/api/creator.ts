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
  /** 省份名（已由后端用 IP 解析聚合，如"北京市"/"广东省"），未解析成功为"未知" */
  region: string;
  /** 读者数 */
  value: number;
  /** 占比百分比（0-100，保留 1 位小数） */
  percentage: number;
}

/** 读者时段分布项 */
export interface ReaderHour {
  hour: number;
  value: number;
  /** 占比百分比 */
  percentage: number;
}

/** v1.1 读者性别分布项 */
export interface ReaderGender {
  /** male / female / other / unknown */
  gender: string;
  value: number;
  percentage: number;
}

/** v1.1 读者年龄段分布项 */
export interface ReaderAgeRange {
  /** under_18 / 18_24 / 25_30 / 31_35 / 36_45 / over_45 / unknown */
  range: string;
  value: number;
  percentage: number;
}

/** v1.1 读者画像数据局限说明（前端展示在卡片下方，告知用户数据可能不真实的原因） */
export interface ReaderProfileDataNote {
  genderNote: string;
  ageRangeNote: string;
  regionNote: string;
}

/** 读者画像 */
export interface ReaderProfile {
  regions: ReaderRegion[];
  genders: ReaderGender[];
  ageRanges: ReaderAgeRange[];
  hours: ReaderHour[];
  dataNote: ReaderProfileDataNote;
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
