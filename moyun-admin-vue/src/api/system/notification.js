import request from '@/utils/request'
import { parseStrEmpty } from "@/utils/ruoyi";

// 查询通知列表
export function listNotification(query) {
  return request({
    url: '/system/notification/list',
    method: 'get',
    params: query
  })
}

// 查询通知详细
export function getNotification(id) {
  return request({
    url: '/system/notification/' + parseStrEmpty(id),
    method: 'get'
  })
}

// 新增通知
export function addNotification(data) {
  return request({
    url: '/system/notification',
    method: 'post',
    data: data
  })
}

// 发送系统通知
export function sendSystemNotification(data) {
  return request({
    url: '/system/notification/send-all',
    method: 'post',
    data: data
  })
}

// 修改通知
export function updateNotification(data) {
  return request({
    url: '/system/notification',
    method: 'put',
    data: data
  })
}

// 删除通知
export function delNotification(id) {
  return request({
    url: '/system/notification/' + id,
    method: 'delete'
  })
}

// ============ 通知收件箱 API ============

// 查询我的通知收件箱列表
export function listInboxNotification(query) {
  return request({
    url: '/system/notification/inbox/list',
    method: 'get',
    params: query
  })
}

// 查询收件箱未读数量
export function getInboxUnreadCount() {
  return request({
    url: '/system/notification/inbox/unread-count',
    method: 'get'
  })
}

// 标记单条通知为已读
export function markInboxAsRead(id) {
  return request({
    url: '/system/notification/inbox/' + id + '/read',
    method: 'post'
  })
}

// 标记全部通知为已读
export function markInboxAllAsRead() {
  return request({
    url: '/system/notification/inbox/read-all',
    method: 'post'
  })
}
