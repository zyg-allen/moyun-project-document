import request from '@/utils/request'

export function listFriendLink(query) {
  return request({
    url: '/admin/friendlink/page',
    method: 'get',
    params: query
  })
}

export function getFriendLink(linkId) {
  return request({
    url: `/admin/friendlink/${linkId}`,
    method: 'get'
  })
}

export function addFriendLink(data) {
  return request({
    url: '/admin/friendlink/add',
    method: 'post',
    data: data
  })
}

export function updateFriendLink(data) {
  return request({
    url: '/admin/friendlink/edit',
    method: 'put',
    data: data
  })
}

export function deleteFriendLink(linkId) {
  return request({
    url: `/admin/friendlink/delete/${linkId}`,
    method: 'delete'
  })
}

export function updateLinkStatus(linkId, status) {
  return request({
    url: `/admin/friendlink/status/${linkId}/${status}`,
    method: 'put'
  })
}
