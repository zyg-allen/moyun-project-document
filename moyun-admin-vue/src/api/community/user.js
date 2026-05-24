import request from '@/utils/request'

export function listPortalUser(query) {
  return request({
    url: '/admin/portal/user/page',
    method: 'get',
    params: query
  })
}

export function getPortalUser(userId) {
  return request({
    url: `/admin/portal/user/${userId}`,
    method: 'get'
  })
}

export function updatePortalUserStatus(userId, status) {
  return request({
    url: `/admin/portal/user/status/${userId}/${status}`,
    method: 'put'
  })
}

export function resetUserPassword(userId, password) {
  return request({
    url: `/admin/portal/user/reset-password/${userId}`,
    method: 'put',
    data: { password }
  })
}

export function getUserStats(userId) {
  return request({
    url: `/admin/portal/user/stats/${userId}`,
    method: 'get'
  })
}
