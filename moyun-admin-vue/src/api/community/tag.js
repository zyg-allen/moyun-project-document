import request from '@/utils/request'

export function listTag(query) {
  return request({
    url: '/admin/tag/page',
    method: 'get',
    params: query
  })
}

export function getTag(tagId) {
  return request({
    url: `/admin/tag/${tagId}`,
    method: 'get'
  })
}

export function addTag(data) {
  return request({
    url: '/admin/tag/add',
    method: 'post',
    data: data
  })
}

export function updateTag(data) {
  return request({
    url: '/admin/tag/edit',
    method: 'put',
    data: data
  })
}

export function deleteTag(tagId) {
  return request({
    url: `/admin/tag/delete/${tagId}`,
    method: 'delete'
  })
}

export function listAllTag() {
  return request({
    url: '/admin/tag/list',
    method: 'get'
  })
}
