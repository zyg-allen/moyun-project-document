import request from '@/utils/request';

// ==================== 面试配置管理（v11.x 智能面试） ====================
// 说明：
//   - 人设/提示词模板/评分权重/追问策略/自我介绍环节管理
//   - 权限前缀 cms:interview:config: list/query/create/update/remove
//   - 删除默认配置会被后端拒绝（前端展示错误消息即可）

// 查询面试配置分页列表
export function listInterviewConfig(query) {
  return request({
    url: '/cms/interview/config/list',
    method: 'get',
    params: query
  });
}

// 查询面试配置详情
export function getInterviewConfig(id) {
  return request({
    url: '/cms/interview/config/' + id,
    method: 'get'
  });
}

// 新增面试配置
export function addInterviewConfig(data) {
  return request({
    url: '/cms/interview/config',
    method: 'post',
    data: data
  });
}

// 修改面试配置
export function updateInterviewConfig(data) {
  return request({
    url: '/cms/interview/config',
    method: 'put',
    data: data
  });
}

// 删除面试配置
export function delInterviewConfig(id) {
  return request({
    url: '/cms/interview/config/' + id,
    method: 'delete'
  });
}