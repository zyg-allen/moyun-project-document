import request from '@/utils/request'

// ===== VIP等级 =====

// 查询VIP等级列表
export function listTier(query) {
  return request({
    url: '/system/vip/tier/list',
    method: 'get',
    params: query
  })
}

// 新增VIP等级
export function addTier(data) {
  return request({
    url: '/system/vip/tier',
    method: 'post',
    data: data
  })
}

// 修改VIP等级
export function updateTier(data) {
  return request({
    url: '/system/vip/tier',
    method: 'put',
    data: data
  })
}

// 删除VIP等级（free 禁删、有会员卡禁删）
export function delTier(ids) {
  return request({
    url: '/system/vip/tier/' + ids,
    method: 'delete'
  })
}

// ===== VIP权益 =====

// 查询VIP权益列表
export function listBenefit(query) {
  return request({
    url: '/system/vip/benefit/list',
    method: 'get',
    params: query
  })
}

// 新增VIP权益
export function addBenefit(data) {
  return request({
    url: '/system/vip/benefit',
    method: 'post',
    data: data
  })
}

// 修改VIP权益
export function updateBenefit(data) {
  return request({
    url: '/system/vip/benefit',
    method: 'put',
    data: data
  })
}

// 删除VIP权益
export function delBenefit(ids) {
  return request({
    url: '/system/vip/benefit/' + ids,
    method: 'delete'
  })
}

// ===== 等级权益配置 =====

// 查询等级权益矩阵（platformCode 必填）
export function getTierBenefit(query) {
  return request({
    url: '/system/vip/tier-benefit/list',
    method: 'get',
    params: query
  })
}

// 保存等级权益（整体覆盖式，benefitValue 空=解除）
export function saveTierBenefit(data) {
  return request({
    url: '/system/vip/tier-benefit/save',
    method: 'post',
    data: data
  })
}

// ===== 接口注册 =====

// 查询接口注册列表
export function listRegistry(query) {
  return request({
    url: '/system/vip/registry/list',
    method: 'get',
    params: query
  })
}

// 修改接口注册（仅 apiDesc/enabled 可改）
export function updateRegistry(data) {
  return request({
    url: '/system/vip/registry',
    method: 'put',
    data: data
  })
}

// 重新扫描接口
export function scanRegistry() {
  return request({
    url: '/system/vip/registry/scan',
    method: 'post'
  })
}

// ===== 会员卡 =====

// 查询会员卡列表
export function listCard(query) {
  return request({
    url: '/system/vip/card/list',
    method: 'get',
    params: query
  })
}

// 作废会员卡
export function delCard(ids) {
  return request({
    url: '/system/vip/card/' + ids,
    method: 'delete'
  })
}

// ===== 使用统计 =====

// 查询权益使用统计列表
export function listUsage(query) {
  return request({
    url: '/system/vip/usage/list',
    method: 'get',
    params: query
  })
}
