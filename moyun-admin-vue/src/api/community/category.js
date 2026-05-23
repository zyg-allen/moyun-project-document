import request from '@/utils/request'

export function listCategory(query) {
  return request({
    url: '/admin/category/list',
    method: 'get',
    params: query
  })
}

export function getCategoryTree() {
  return request({
    url: '/admin/category/tree',
    method: 'get'
  })
}

export function saveCategory(data) {
  return request({
    url: '/admin/category/save',
    method: 'post',
    data
  })
}

export function updateCategory(data) {
  return request({
    url: '/admin/category/update',
    method: 'put',
    data
  })
}

export function deleteCategory(categoryId) {
  return request({
    url: `/admin/category/delete/${categoryId}`,
    method: 'delete'
  })
}

export function listTag() {
  return request({
    url: '/admin/category/tag/list',
    method: 'get'
  })
}

export function saveTag(data) {
  return request({
    url: '/admin/category/tag/save',
    method: 'post',
    data
  })
}

export function updateTag(data) {
  return request({
    url: '/admin/category/tag/update',
    method: 'put',
    data
  })
}
