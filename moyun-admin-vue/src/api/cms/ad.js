import request from '@/utils/request'
import { parseStrEmpty } from "@/utils/ruoyi";

// 查询广告位列表
export function listAdSlot(query) {
  return request({
    url: '/cms/ad/list',
    method: 'get',
    params: query
  })
}

// 查询广告位详情
export function getAdSlot(id) {
  return request({
    url: '/cms/ad/' + parseStrEmpty(id),
    method: 'get'
  })
}

// 新增广告位
export function addAdSlot(data) {
  return request({
    url: '/cms/ad',
    method: 'post',
    data: data
  })
}

// 修改广告位
export function updateAdSlot(data) {
  return request({
    url: '/cms/ad',
    method: 'put',
    data: data
  })
}

// 删除广告位
export function delAdSlot(id) {
  return request({
    url: '/cms/ad/' + id,
    method: 'delete'
  })
}
