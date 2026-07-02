import request from '@/utils/request'

// 查询分类列表（扁平，用于下拉筛选）
// 可传 params: { status, parentId, name }
export function listCategories(params) {
  return request({
    url: '/portal/admin/categories/list',
    method: 'get',
    params: params
  })
}

// 查询所有分类（含停用）
export function listAllCategories() {
  return request({
    url: '/portal/admin/categories/all',
    method: 'get'
  })
}
