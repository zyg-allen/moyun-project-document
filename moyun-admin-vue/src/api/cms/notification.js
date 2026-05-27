import request from '@/utils/request'
import { parseStrEmpty } from "@/utils/ruoyi";

// 查询通知列表
export function listNotification(query) {
  return request({
    url: '/cms/notification/list',
    method: 'get',
    params: query
  })
}

// 查询通知详细
export function getNotification(id) {
  return request({
    url: '/cms/notification/' + parseStrEmpty(id),
    method: 'get'
  })
}

// 新增通知
export function addNotification(data) {
  return request({
    url: '/cms/notification',
    method: 'post',
    data: data
  })
}

// 发送系统通知
export function sendSystemNotification(data) {
  return request({
    url: '/cms/notification/send-all',
    method: 'post',
    data: data
  })
}

// 修改通知
export function updateNotification(data) {
  return request({
    url: '/cms/notification',
    method: 'put',
    data: data
  })
}

// 删除通知
export function delNotification(id) {
  return request({
    url: '/cms/notification/' + id,
    method: 'delete'
  })
}
