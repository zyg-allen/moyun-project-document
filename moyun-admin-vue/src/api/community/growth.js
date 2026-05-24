import request from '@/utils/request'

export function listLevelConfig(query) {
  return request({
    url: '/admin/growth/level/page',
    method: 'get',
    params: query
  })
}

export function addLevelConfig(data) {
  return request({
    url: '/admin/growth/level/add',
    method: 'post',
    data: data
  })
}

export function updateLevelConfig(data) {
  return request({
    url: '/admin/growth/level/edit',
    method: 'put',
    data: data
  })
}

export function deleteLevelConfig(levelId) {
  return request({
    url: `/admin/growth/level/delete/${levelId}`,
    method: 'delete'
  })
}

export function listBadge(query) {
  return request({
    url: '/admin/growth/badge/page',
    method: 'get',
    params: query
  })
}

export function addBadge(data) {
  return request({
    url: '/admin/growth/badge/add',
    method: 'post',
    data: data
  })
}

export function updateBadge(data) {
  return request({
    url: '/admin/growth/badge/edit',
    method: 'put',
    data: data
  })
}

export function deleteBadge(badgeId) {
  return request({
    url: `/admin/growth/badge/delete/${badgeId}`,
    method: 'delete'
  })
}

export function listPointsRule(query) {
  return request({
    url: '/admin/growth/points/page',
    method: 'get',
    params: query
  })
}

export function updatePointsRule(data) {
  return request({
    url: '/admin/growth/points/edit',
    method: 'put',
    data: data
  })
}
