import type { ApiResponse } from '@/types/api';
import type { Notification, NotificationType } from '@/types';
export interface GetNotificationListParams {
    pageNum?: number;
    pageSize?: number;
    type?: NotificationType;
}
export interface MarkAsReadParams {
    id?: string | number;
}
export declare function getNotificationList(params?: GetNotificationListParams): Promise<ApiResponse<{
    list: Notification[];
    total: number;
    page: number;
    pageSize: number;
}>>;
export declare function getUnreadCount(): Promise<ApiResponse<number>>;
export declare function markAsRead(params: MarkAsReadParams): Promise<ApiResponse<void>>;
