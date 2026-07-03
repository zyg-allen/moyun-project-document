import request from '@/utils/request'

// 查询创作挑战/征文活动列表
export function listContest(query) {
  return request({ url: '/cms/contest/list', method: 'get', params: query })
}

// 查询创作挑战详情
export function getContest(id) {
  return request({ url: '/cms/contest/' + id, method: 'get' })
}

// 新增创作挑战
export function addContest(data) {
  return request({ url: '/cms/contest', method: 'post', data: data })
}

// 修改创作挑战
export function updateContest(data) {
  return request({ url: '/cms/contest', method: 'put', data: data })
}

// 删除创作挑战
export function delContest(id) {
  return request({ url: '/cms/contest/' + id, method: 'delete' })
}
