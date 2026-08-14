import request from '@/utils/request';

// ==================== 导入模板字段配置（动态模板运营维护） ====================
// 说明：
//   - listImportTemplateConfig：按 businessKey 查询全部字段配置（含停用）
//   - saveImportTemplateConfig：整批保存某业务字段配置（先删后插）
//   - 通用 businessKey：interview_question / interview_experience / article / tag / note 等
// ----------------------------------------------------------------------

// 查询某业务的字段配置
export function listImportTemplateConfig(businessKey) {
  return request({
    url: '/cms/import-template/list',
    method: 'get',
    params: { businessKey }
  });
}

// 整批保存某业务的字段配置（先删后插）
// data: { businessKey: string, configs: PortalImportTemplateConfig[] }
export function saveImportTemplateConfig(data) {
  return request({
    url: '/cms/import-template/save',
    method: 'post',
    data: data
  });
}

// 业务标识常量（统一维护）
export const BUSINESS_KEYS = [
  { value: 'interview_question', label: '题库', remark: '面试题目导入模板' },
  { value: 'interview_experience', label: '面经', remark: '面试经验导入模板' },
  { value: 'article', label: '文章', remark: '文章批量导入模板' },
  { value: 'tag', label: '标签', remark: '标签导入模板' },
  { value: 'note', label: '精选笔记', remark: '精选笔记导入模板' }
];
