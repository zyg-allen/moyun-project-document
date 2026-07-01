import request from '@/utils/request'

// 查询推荐位列表（后台分页）
export function listBookRecommend(query) {
  return request({
    url: '/portal/admin/bookRecommend/list',
    method: 'get',
    params: query
  })
}

// 推荐位详情
export function getBookRecommend(id) {
  return request({
    url: '/portal/admin/bookRecommend/' + id,
    method: 'get'
  })
}

// 新增推荐位
export function addBookRecommend(data) {
  return request({
    url: '/portal/admin/bookRecommend',
    method: 'post',
    data: data
  })
}

// 修改推荐位
export function updateBookRecommend(data) {
  return request({
    url: '/portal/admin/bookRecommend',
    method: 'put',
    data: data
  })
}

// 删除推荐位
export function delBookRecommend(ids) {
  return request({
    url: '/portal/admin/bookRecommend/' + ids,
    method: 'delete'
  })
}

// 上下架切换
export function toggleBookRecommendActive(id) {
  return request({
    url: '/portal/admin/bookRecommend/' + id + '/toggle',
    method: 'put'
  })
}

// 批量排序
export function batchUpdateSort(list) {
  return request({
    url: '/portal/admin/bookRecommend/sort',
    method: 'put',
    data: list
  })
}
