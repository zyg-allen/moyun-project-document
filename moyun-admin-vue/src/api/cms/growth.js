import request from '@/utils/request'

// ==================== 成长规则管理 ====================

// 查询成长规则分页列表
export function listGrowthRule(query) {
  return request({
    url: '/cms/growth/rule/list',
    method: 'get',
    params: query
  })
}

// 查询成长规则详情
export function getGrowthRule(id) {
  return request({
    url: '/cms/growth/rule/' + id,
    method: 'get'
  })
}

// 新增成长规则
export function addGrowthRule(data) {
  return request({
    url: '/cms/growth/rule',
    method: 'post',
    data: data
  })
}

// 修改成长规则
export function updateGrowthRule(data) {
  return request({
    url: '/cms/growth/rule',
    method: 'put',
    data: data
  })
}

// 删除成长规则
export function delGrowthRule(ids) {
  return request({
    url: '/cms/growth/rule/' + ids,
    method: 'delete'
  })
}

// ==================== 成就定义管理 ====================

// 查询成就分页列表
export function listGrowthAchievement(query) {
  return request({
    url: '/cms/growth/achievement/list',
    method: 'get',
    params: query
  })
}

// 查询成就详情
export function getGrowthAchievement(id) {
  return request({
    url: '/cms/growth/achievement/' + id,
    method: 'get'
  })
}

// 新增成就
export function addGrowthAchievement(data) {
  return request({
    url: '/cms/growth/achievement',
    method: 'post',
    data: data
  })
}

// 修改成就
export function updateGrowthAchievement(data) {
  return request({
    url: '/cms/growth/achievement',
    method: 'put',
    data: data
  })
}

// 删除成就
export function delGrowthAchievement(ids) {
  return request({
    url: '/cms/growth/achievement/' + ids,
    method: 'delete'
  })
}

// ==================== 用户成长查询 ====================

// 查询用户成长分页列表
export function listUserGrowth(query) {
  return request({
    url: '/cms/growth/user-growth/list',
    method: 'get',
    params: query
  })
}

// 查询用户成长详情（含统计信息）
export function getUserGrowthDetail(userId) {
  return request({
    url: '/cms/growth/user-growth/' + userId,
    method: 'get'
  })
}

// ==================== 用户徽章管理 ====================

// 查询用户徽章分页列表
export function listUserBadge(query) {
  return request({
    url: '/cms/growth/badge/list',
    method: 'get',
    params: query
  })
}

// 授予用户徽章
export function grantUserBadge(data) {
  return request({
    url: '/cms/growth/badge',
    method: 'post',
    data: data
  })
}

// 撤销用户徽章
export function revokeUserBadge(data) {
  return request({
    url: '/cms/growth/badge',
    method: 'delete',
    data: data
  })
}

// ==================== 成长流水查询 ====================

// 查询成长流水分页列表
export function listGrowthLog(query) {
  return request({
    url: '/cms/growth/log/list',
    method: 'get',
    params: query
  })
}
