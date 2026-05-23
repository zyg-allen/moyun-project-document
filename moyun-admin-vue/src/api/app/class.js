import request from '@/utils/request'

// 查询班级列表
export function listClass(query) {
  return request({
    url: '/app/class/list',
    method: 'get',
    params: query
  })
}

// 查询班级详细
export function getClass(id) {
  return request({
    url: '/app/class/' + id,
    method: 'get'
  })
}

// 新增班级
export function addClass(data) {
  return request({
    url: '/app/class',
    method: 'post',
    data: data
  })
}

// 修改班级
export function updateClass(data) {
  return request({
    url: '/app/class',
    method: 'put',
    data: data
  })
}

// 删除班级
export function delClass(id) {
  return request({
    url: '/app/class/' + id,
    method: 'delete'
  })
}
