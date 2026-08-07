// 代码生成器演示样例 API：无对应后端实现，仅供代码生成器参考。
// 由 api/app/ 迁移至 api/examples/app-demo/，避免污染主项目业务结构。
import request from '@/utils/request'

// 查询报名列表
export function listRegister(query) {
  return request({
    url: '/app/register/list',
    method: 'get',
    params: query
  })
}

// 查询报名详细
export function getRegister(id) {
  return request({
    url: '/app/register/' + id,
    method: 'get'
  })
}

// 新增报名
export function addRegister(data) {
  return request({
    url: '/app/register',
    method: 'post',
    data: data
  })
}

// 修改报名
export function updateRegister(data) {
  return request({
    url: '/app/register',
    method: 'put',
    data: data
  })
}

// 删除报名
export function delRegister(id) {
  return request({
    url: '/app/register/' + id,
    method: 'delete'
  })
}
