import request from '@/utils/request'
import { parseStrEmpty } from "@/utils/ruoyi";

// ==================== CMS 标签管理（原有） ====================

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
  const data = { tagId, status }
  return request({
    url: '/cms/tag/changeStatus',
    method: 'put',
    data: data
  })
}

// ==================== Portal 通用标签系统（新增） ====================

// 拉取标签列表（前端门户/通用），支持关键字与 module 过滤
export function listPortalTag(query) {
  return request({
    url: '/portal/tag/list',
    method: 'get',
    params: query
  })
}

// 热门标签（按 module 分类）
export function getHotTags(module, limit) {
  return request({
    url: '/portal/tag/hot',
    method: 'get',
    params: { module, limit }
  })
}

// 为实体绑定标签（entityType, entityId, tagIds / tagNames, module）
export function bindTagsToEntity(data) {
  return request({
    url: '/portal/tag/bind',
    method: 'post',
    data: data
  })
}
