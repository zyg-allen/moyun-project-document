import { httpGet, httpPost, httpPut } from './client';
import type {
  Notification,
  NotificationListParams,
  MarkNotificationReadParams,
  NotificationStats,
  PaginationResponse,
} from '@/types/api';

// 获取通知列表
export const getNotificationList = (params?: NotificationListParams) => {
  return httpGet<PaginationResponse<Notification>>('/notification/list', params);
};

// 标记已读
export const markNotificationRead = (params: MarkNotificationReadParams) => {
  return httpPut('/notification/read', params);
};

// 获取通知统计
export const getNotificationStats = () => {
  return httpGet<NotificationStats>('/notification/stats');
};

// 别名函数
export const markAsRead = (id: string) => {
  return markNotificationRead({ id });
};
