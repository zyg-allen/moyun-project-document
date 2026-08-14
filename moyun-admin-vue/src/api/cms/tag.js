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

// 标签状态修改（后端 PortalTag 主键为 id，此处映射 tagId → id）
export function changeTagStatus(tagId, status) {
  const data = { id: tagId, status: status }
  return request({
    url: '/cms/tag/changeStatus',
    method: 'put',
    data: data
  })
}

// ==================== 通用标签系统（后台版） ====================
// 注意：后台管理页统一走 /cms/tag/* 接口，使用后台 token 体系（SysLoginController 签发）。
// 切勿调用 /portal/tag/*：那是前台门户接口，使用门户 token 体系，后台 token 调用会被认证拦截返回 401
// （表现为"登录状态已过期"）。后端 CmsTagController 已提供 hot/bind 后台同源接口。

// 拉取标签列表（后台通用），支持关键字与 module 过滤
export function listPortalTag(query) {
  return request({
    url: '/cms/tag/list',
    method: 'get',
    params: query
  })
}

// 热门标签（按 module 分类）— 后台版，走 /cms/tag/hot
export function getHotTags(module, limit) {
  return request({
    url: '/cms/tag/hot',
    method: 'get',
    params: { module, limit }
  })
}

// 为实体绑定标签（entityType, entityId, tagIds / tagNames, module）— 后台版，走 /cms/tag/bind
export function bindTagsToEntity(data) {
  return request({
    url: '/cms/tag/bind',
    method: 'post',
    data: data
  })
}
