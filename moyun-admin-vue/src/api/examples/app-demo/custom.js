// 代码生成器演示样例 API：无对应后端实现，仅供代码生成器参考。
// 由 api/app/ 迁移至 api/examples/app-demo/，避免污染主项目业务结构。
import request from '@/utils/request'

// 查询报名列表
export function getVerify() {
  return request({
    url: '/app/custom/verifies',
    method: 'get',
  })
}
