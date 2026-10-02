import request from '@/utils/request'

/**
 * 简历解析配置 API
 *
 * 四类词表：section 章节标题词典 / skill 技能词域 / degree 学历词 / position 岗位词。
 * 规则解析引擎读取本配置；配置为空时引擎使用内置默认词典兜底，不会导致解析失败。
 */

/** 分页列表 */
export function listResumeParseConfig(query) {
  return request({
    url: '/cms/interview/resumeParseConfig/list',
    method: 'get',
    params: query,
  })
}

/** 按类型取全部启用配置（供下拉/规则引擎对照） */
export function listResumeParseConfigByType(configType) {
  return request({
    url: `/cms/interview/resumeParseConfig/byType/${configType}`,
    method: 'get',
  })
}

/** 详情 */
export function getResumeParseConfig(id) {
  return request({
    url: `/cms/interview/resumeParseConfig/${id}`,
    method: 'get',
  })
}

/** 新增 */
export function addResumeParseConfig(data) {
  return request({
    url: '/cms/interview/resumeParseConfig',
    method: 'post',
    data,
  })
}

/** 修改 */
export function updateResumeParseConfig(data) {
  return request({
    url: '/cms/interview/resumeParseConfig',
    method: 'put',
    data,
  })
}

/** 删除 */
export function delResumeParseConfig(id) {
  return request({
    url: `/cms/interview/resumeParseConfig/${id}`,
    method: 'delete',
  })
}
