import request from '@/utils/request'
import { parseStrEmpty } from '@/utils/ruoyi'

// 查询付费阅读订单列表
export function listOrder(query) {
  return request({
    url: '/cms/order/list',
    method: 'get',
    params: query
  })
}

// 查询订单详情
export function getOrder(id) {
  return request({
    url: '/cms/order/' + parseStrEmpty(id),
    method: 'get'
  })
}
