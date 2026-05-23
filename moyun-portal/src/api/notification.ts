import { httpGet, httpPost, httpDelete } from './client'
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
): Promise<ApiResponse<{ list: Notification[]; total: number }>> {
  return httpGet('/notifications', params)
}

export async function getUnreadCount(): Promise<ApiResponse<{ count: number }>> {
  return httpGet('/notifications/unread-count')
}

export async function markAsRead(
  params: MarkAsReadParams
): Promise<ApiResponse<void>> {
  if (params.all) {
    return httpPost('/notifications/mark-all-read')
  }
  return httpPost(`/notifications/${params.id}/read`)
}

export async function deleteNotification(
  id: string
): Promise<ApiResponse<void>> {
  return httpDelete(`/notifications/${id}`)
}
