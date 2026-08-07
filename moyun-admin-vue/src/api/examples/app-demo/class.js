// 代码生成器演示样例 API：无对应后端实现，仅供代码生成器参考。
// 由 api/app/ 迁移至 api/examples/app-demo/，避免污染主项目业务结构。
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
