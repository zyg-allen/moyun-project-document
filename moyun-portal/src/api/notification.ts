import { httpGet, httpPost, httpGetList } from './client'
import type { ApiResponse } from '@/types/api'
import type { Notification, NotificationType } from '@/types'

export interface GetNotificationListParams {
  pageNum?: number
  pageSize?: number
  type?: NotificationType
}

export interface MarkAsReadParams {
  id?: string | number
}

export async function getNotificationList(
  params?: GetNotificationListParams
): Promise<ApiResponse<{ list: Notification[]; total: number; page: number; pageSize: number }>> {
  return httpGetList<Notification>('/portal/notification/list', params)
}

export async function getUnreadCount(): Promise<ApiResponse<number>> {
  return httpGet<number>('/portal/notification/unread-count')
}

export async function markAsRead(
  params: MarkAsReadParams
): Promise<ApiResponse<void>> {
  return httpPost(`/portal/notification/${params.id}/read`)
}

/**
 * 全部通知标记已读（批量操作）
 * 后端一次性将当前用户所有未读通知标记已读，避免前端逐条标记遗漏分页数据
 */
export async function markAllAsRead(): Promise<ApiResponse<number>> {
  return httpPost<number>('/portal/notification/read-all')
}

/**
 * 获取公开广播通知（未登录用户也可调用）
 * 用于公告列表、版本发布等场景，只返回 scope=all 的通知
 */
export async function getBroadcastList(
  params?: GetNotificationListParams
): Promise<ApiResponse<{ list: Notification[]; total: number; page: number; pageSize: number }>> {
  return httpGetList<Notification>('/portal/notification/broadcast', params)
}
