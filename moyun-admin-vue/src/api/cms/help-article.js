import request from '@/utils/request'

// 查询帮助文章列表
export function listHelpArticle(query) {
  return request({ url: '/cms/help-article/list', method: 'get', params: query })
}

// 查询帮助文章详细
export function getHelpArticle(id) {
  return request({ url: '/cms/help-article/' + id, method: 'get' })
}

// 新增帮助文章
export function addHelpArticle(data) {
  return request({ url: '/cms/help-article', method: 'post', data: data })
}

// 修改帮助文章
export function updateHelpArticle(data) {
  return request({ url: '/cms/help-article', method: 'put', data: data })
}

// 删除帮助文章
export function delHelpArticle(id) {
  return request({ url: '/cms/help-article/' + id, method: 'delete' })
}
