import request from '@/utils/request'
import { parseStrEmpty } from '@/utils/ruoyi'

// 查询职位列表
export function listJob(query) {
  return request({
    url: '/cms/job/list',
    method: 'get',
    params: query
  })
}

// 查询职位详细
export function getJob(id) {
  return request({
    url: '/cms/job/' + parseStrEmpty(id),
    method: 'get'
  })
}

// 新增职位
export function addJob(data) {
  return request({
    url: '/cms/job',
    method: 'post',
    data: data
  })
}

// 修改职位
export function updateJob(data) {
  return request({
    url: '/cms/job',
    method: 'put',
    data: data
  })
}

// 删除职位（支持批量，ids 逗号分隔）
export function delJob(ids) {
  return request({
    url: '/cms/job/' + ids,
    method: 'delete'
  })
}

// 查询面试公司列表（用于职位表单公司下拉，复用面试空间公司接口）
export function listInterviewCompany(query) {
  return request({
    url: '/cms/interview/company/list',
    method: 'get',
    params: query
  })
}
