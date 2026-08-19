import request from '@/utils/request'

// 查询书架列表（后台按用户/书籍筛选）
export function listBookshelf(query) {
  return request({
    url: '/portal/admin/bookshelves/list',
    method: 'get',
    params: query
  })
}

// 书架详情
export function getBookshelf(id) {
  return request({
    url: '/portal/admin/bookshelves/' + id,
    method: 'get'
  })
}

// 批量移出书架（后台管理操作）
export function delBookshelf(ids) {
  return request({
    url: '/portal/admin/bookshelves/' + ids,
    method: 'delete'
  })
}
