import request from '@/utils/request'

// ==================== 学习计划（任务 3.2 后台，只读） ====================

// 查询学习计划分页列表（只读）
export function listStudyPlan(query) {
  return request({
    url: '/cms/portal/studyPlan/list',
    method: 'get',
    params: query
  })
}

// ==================== 错题本（任务 3.3 后台，只读） ====================

// 查询错题本分页列表（只读）
export function listWrongQuestion(query) {
  return request({
    url: '/cms/portal/wrongQuestion/list',
    method: 'get',
    params: query
  })
}
