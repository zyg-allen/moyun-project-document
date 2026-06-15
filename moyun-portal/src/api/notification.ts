import { httpGet, httpPost, httpDelete, httpGetList } from './client'
import type { ApiResponse } from '@/types/api'
import type { Notification, NotificationType } from '@/types'

export interface GetNotificationListParams {
  page?: number
  pageSize?: number
  type?: NotificationType
  isRead?: boolean
}

export interface MarkAsReadParams {
  id?: string
  all?: boolean
}

export async function getNotificationList(
  params?: GetNotificationListParams
): Promise<ApiResponse<{ list: Notification[]; total: number; page: number; pageSize: number }>> {
  return httpGetList<Notification>('/portal/notification/list', params)
}

export async function getUnreadCount(): Promise<ApiResponse<{ count: number }>> {
  return httpGet('/portal/notification/unread-count')
}

export async function markAsRead(
  params: MarkAsReadParams
): Promise<ApiResponse<void>> {
  if (params.all) {
    return httpPost('/portal/notification/mark-all-read')
  }
  return httpPost(`/portal/notification/${params.id}/read`)
}

export async function deleteNotification(
  id: string
): Promise<ApiResponse<void>> {
  return httpDelete(`/portal/notification/${id}`)
}
