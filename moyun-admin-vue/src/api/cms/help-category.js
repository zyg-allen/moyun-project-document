import request from '@/utils/request'

// 查询帮助分类列表
export function listHelpCategory(query) {
  return request({ url: '/cms/help-category/list', method: 'get', params: query })
}

// 查询全部分类（下拉选择用）
export function allHelpCategory() {
  return request({ url: '/cms/help-category/all', method: 'get' })
}

// 查询帮助分类详细
export function getHelpCategory(id) {
  return request({ url: '/cms/help-category/' + id, method: 'get' })
}

// 新增帮助分类
export function addHelpCategory(data) {
  return request({ url: '/cms/help-category', method: 'post', data: data })
}

// 修改帮助分类
export function updateHelpCategory(data) {
  return request({ url: '/cms/help-category', method: 'put', data: data })
}

// 删除帮助分类
export function delHelpCategory(id) {
  return request({ url: '/cms/help-category/' + id, method: 'delete' })
}
