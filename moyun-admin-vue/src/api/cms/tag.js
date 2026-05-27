import request from '@/utils/request'
import { parseStrEmpty } from "@/utils/ruoyi";

// 查询标签列表
export function listTag(query) {
  return request({
    url: '/cms/tag/list',
    method: 'get',
    params: query
  })
}

// 查询标签详细
export function getTag(tagId) {
  return request({
    url: '/cms/tag/' + parseStrEmpty(tagId),
    method: 'get'
  })
}

// 新增标签
export function addTag(data) {
  return request({
    url: '/cms/tag',
    method: 'post',
    data: data
  })
}

// 修改标签
export function updateTag(data) {
  return request({
    url: '/cms/tag',
    method: 'put',
    data: data
  })
}

// 删除标签
export function delTag(tagId) {
  return request({
    url: '/cms/tag/' + tagId,
    method: 'delete'
  })
}

// 标签状态修改
export function changeTagStatus(tagId, status) {
  const data = {
    tagId,
    status
  }
  return request({
    url: '/cms/tag/changeStatus',
    method: 'put',
    data: data
  })
}
