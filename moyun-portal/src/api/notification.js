import { httpGet, httpPost, httpGetList } from './client';
export async function getNotificationList(params) {
    return httpGetList('/portal/notification/list', params);
}
export async function getUnreadCount() {
    return httpGet('/portal/notification/unread-count');
}
export async function markAsRead(params) {
    return httpPost(`/portal/notification/${params.id}/read`);
}
