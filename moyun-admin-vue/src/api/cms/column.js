import request from '@/utils/request'
import { parseStrEmpty } from '@/utils/ruoyi'

// 查询专栏列表
export function listColumn(query) {
  return request({
    url: '/cms/column/list',
    method: 'get',
    params: query
  })
}

// 查询专栏详情
export function getColumn(id) {
  return request({
    url: '/cms/column/' + parseStrEmpty(id),
    method: 'get'
  })
}

// 新增专栏
export function addColumn(data) {
  return request({
    url: '/cms/column',
    method: 'post',
    data: data
  })
}

// 修改专栏
export function updateColumn(data) {
  return request({
    url: '/cms/column',
    method: 'put',
    data: data
  })
}

// 删除专栏（支持批量，ids 逗号分隔）
export function delColumn(ids) {
  return request({
    url: '/cms/column/' + ids,
    method: 'delete'
  })
}

// 审核专栏（状态流转 draft→published→archived）
export function changeColumnStatus(id, status) {
  return request({
    url: '/cms/column/' + id + '/status',
    method: 'put',
    data: { status: status }
  })
}

// 分页查询专栏已绑定的文章列表（维护文章弹窗用）
export function listColumnArticles(id, params) {
  return request({
    url: '/cms/column/' + id + '/articles',
    method: 'get',
    params: params
  })
}

// 批量绑定文章到专栏
export function bindColumnArticles(id, articleIds) {
  return request({
    url: '/cms/column/' + id + '/articles',
    method: 'post',
    data: { articleIds: articleIds }
  })
}

// 将文章移出专栏
export function removeColumnArticle(id, articleId) {
  return request({
    url: '/cms/column/' + id + '/articles/' + articleId,
    method: 'delete'
  })
}
