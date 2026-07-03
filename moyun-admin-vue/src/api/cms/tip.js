import request from '@/utils/request'
import { parseStrEmpty } from '@/utils/ruoyi'

// 查询打赏流水列表
export function listTip(query) {
  return request({
    url: '/cms/tip/list',
    method: 'get',
    params: query
  })
}

// 查询打赏订单详情
export function getTip(id) {
  return request({
    url: '/cms/tip/' + parseStrEmpty(id),
    method: 'get'
  })
}
