import request from '@/utils/request'

// 查询报名列表
export function getVerify() {
  return request({
    url: '/app/custom/verifies',
    method: 'get',
  })
}
