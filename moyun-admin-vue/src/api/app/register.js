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
