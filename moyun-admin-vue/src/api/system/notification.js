import request from '@/utils/request'

// 查询当前管理员的通知列表（个人 + 广播，含已读状态）
export function listMyNotification(query) {
  return request({
    url: '/system/notification/list',
    method: 'get',
    params: query
  })
}

// 获取当前管理员未读通知数
export function getUnreadCount() {
  return request({
    url: '/system/notification/unread-count',
    method: 'get'
  })
}

// 标记单条通知已读
export function markAsRead(id) {
  return request({
    url: '/system/notification/' + id + '/read',
    method: 'post'
  })
}

// 标记所有未读通知为已读
export function markAllAsRead() {
  return request({
    url: '/system/notification/mark-all-read',
    method: 'post'
  })
}

// 获取通知详情
export function getNotification(id) {
  return request({
    url: '/system/notification/' + id,
    method: 'get'
  })
}
