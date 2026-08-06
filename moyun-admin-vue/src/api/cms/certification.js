import request from '@/utils/request'

// 查询创作者认证申请分页列表
export function listCertification(query) {
  return request({
    url: '/cms/creator/certification/list',
    method: 'get',
    params: query
  })
}

// 查询单条认证申请详情（含申请人昵称）
export function getCertification(id) {
  return request({
    url: '/cms/creator/certification/' + id,
    method: 'get'
  })
}

// 审核创作者认证申请
// body: { id, status: 'approved' | 'rejected', remark? }
export function auditCertification(data) {
  return request({
    url: '/cms/creator/certification/' + data.id + '/audit',
    method: 'put',
    data: {
      status: data.status,
      remark: data.remark
    }
  })
}
