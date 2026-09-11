import { httpGet, httpPost, httpPut, httpDelete } from './client';
import type {
  ResumeJobTarget, ResumeJobMatchReport, ResumeDeepOptimizeVO, ResumeOptimizeHistory,
  ResumeScoreReport, UserResumeVO,
} from '@/types/api';

/**
 * 简历优化工作台 API（v10.13，对应后端 PortalResumeOptimizeController）
 * 链路：岗位目标 CRUD → 匹配分析 → 深度优化建议 → 采纳保存新版本 → 优化历史
 */

/** 岗位目标列表 */
export const getJobTargets = () => {
  return httpGet<ResumeJobTarget[]>('/portal/resume/optimize/job-target');
};

/** 新建岗位目标 */
export const createJobTarget = (data: Partial<ResumeJobTarget>) => {
  return httpPost<number>('/portal/resume/optimize/job-target', data as Record<string, unknown>);
};

/** 更新岗位目标 */
export const updateJobTarget = (id: number | string, data: Partial<ResumeJobTarget>) => {
  return httpPut<null>(`/portal/resume/optimize/job-target/${id}`, data as Record<string, unknown>);
};

/** 删除岗位目标 */
export const deleteJobTarget = (id: number | string) => {
  return httpDelete<null>(`/portal/resume/optimize/job-target/${id}`);
};

/** 执行岗位匹配分析（LLM 四维 + 规则兜底，结果存档） */
export const runJobMatch = (resumeId: number | string, jobTargetId: number | string) => {
  return httpPost<ResumeJobMatchReport>(`/portal/resume/optimize/match/${resumeId}/${jobTargetId}`);
};

/** 最近匹配报告 */
export const getLatestMatch = (resumeId: number | string) => {
  return httpGet<ResumeJobMatchReport | null>(`/portal/resume/optimize/match/${resumeId}/latest`);
};

/** 生成深度优化建议（同步，兼容旧版；长耗时场景建议改用 submitDeepOptimizeTask） */
export const generateDeepOptimize = (resumeId: number | string, jobTargetId: number | string) => {
  return httpPost<ResumeDeepOptimizeVO>(`/portal/resume/optimize/deep/${resumeId}/${jobTargetId}`);
};

// ===== v10.19：异步任务化（解决大模型调用超时） =====

/** 深度优化异步任务状态（前端轮询返回结构） */
export interface ResumeOptimizeTaskVO {
  taskId: number;
  /** pending/running/success/failed */
  status: 'pending' | 'running' | 'success' | 'failed';
  /** 进度百分比 0-100 */
  progress: number;
  /** 优化结果（status=success 时填充，对应 ResumeDeepOptimizeVO） */
  result?: ResumeDeepOptimizeVO | null;
  /** 失败原因（status=failed 时填充） */
  errorMsg?: string | null;
}

/**
 * 提交深度优化异步任务（v10.19 推荐）
 * 立即返回任务ID，后端异步调用 LLM 生成建议。前端通过 getDeepOptimizeTaskStatus 轮询。
 */
export const submitDeepOptimizeTask = (resumeId: number | string, jobTargetId: number | string) => {
  return httpPost<number>(`/portal/resume/optimize/deep/${resumeId}/${jobTargetId}/async`);
};

/** 查询深度优化任务状态（前端轮询，建议 3-5 秒一次） */
export const getDeepOptimizeTaskStatus = (taskId: number | string) => {
  return httpGet<ResumeOptimizeTaskVO>(`/portal/resume/optimize/deep/task/${taskId}`);
};

/** 采纳建议并保存新版本（版本号+1，记录优化历史） */
export const applyDeepOptimize = (params: {
  resumeId: number | string;
  optimize: ResumeDeepOptimizeVO;
  adopted: number[];
}) => {
  return httpPost<number>('/portal/resume/optimize/deep/apply', params as unknown as Record<string, unknown>);
};

/** 优化历史列表（v10.15：返回带评分对比的历史记录） */
export const getOptimizeHistory = (resumeId: number | string) => {
  return httpGet<ResumeOptimizeHistory[]>(`/portal/resume/optimize/history/${resumeId}`);
};

/** AI 实时辅助建议项（v10.14 字段级） */
export interface FieldAssistSuggestion {
  text: string;
  reason?: string;
}

/**
 * 字段级 AI 实时辅助（v10.14 设计文档 P0 需求#2）
 * field: work_description / project_description / self_intro / skills
 * 返回 3 个差异化优化版本，用户采纳后替换字段内容
 */
export const aiFieldAssist = (params: {
  field: 'work_description' | 'project_description' | 'self_intro' | 'skills';
  originalText: string;
  position?: string;
  skillNames?: string[];
}) => {
  return httpPost<FieldAssistSuggestion[]>('/portal/resume/optimize/ai-assist', params as unknown as Record<string, unknown>);
};

// ===== v10.18 阶段五：评分报告存档 =====

/**
 * 保存评分报告（触发评分 + 入库存档）
 * 后端会先调用 scoreResume 写 portal_user_resume 评分字段并自动入库 source=manual，
 * 若 source 传入非 manual 或带 jobTargetId/position，则补全刚插入的报告记录。
 */
export const saveScoreReport = (params: {
  resumeId: number | string;
  source?: 'manual' | 'optimize' | 'template';
  jobTargetId?: number | string;
  position?: string;
}) => {
  return httpPost<ResumeScoreReport>('/portal/resume/optimize/score-report', params as unknown as Record<string, unknown>);
};

/** 评分报告列表（按时间倒序，可追溯历史评分） */
export const getScoreReports = (resumeId: number | string) => {
  return httpGet<ResumeScoreReport[]>(`/portal/resume/optimize/score-report/${resumeId}`);
};

// ===== v10.18 阶段一：模板套用打通 =====

/**
 * 模板套用：拉取模板详情（含 sampleData 结构化示例数据）
 * 后端 GET /portal/interview/resume/{id} 直接返回 sampleData 字段（实体已扩展）。
 * 前端拿到后由 stores/resume.ts 的 fillFromTemplate 解析填充到编辑页表单。
 *
 * 注：此处复用 interview.ts 的 getResumeTemplateDetail，仅为类型对齐与文档命名一致而导出别名。
 * 页面 useTemplate 局部函数请直接 import { getResumeTemplateDetail } from '@/api/interview' 或此处别名。
 */
export { getResumeTemplateDetail as getTemplateDetail } from './interview';

// ===== v10.22 阶段二：AI 填充空字段草稿 =====

/** v10.22：AI 填充空字段（为空的工作/项目/自我介绍生成草稿） */
export const aiDraftEmptyFields = (resumeId: number | string, jobTargetId?: number | string) => {
  return httpPost<{
    works?: UserResumeVO['works'];
    projects?: UserResumeVO['projects'];
    selfIntro?: string;
    message?: string;
  }>(`/portal/resume/optimize/ai-draft/${resumeId}?jobTargetId=${jobTargetId ?? ''}`);
};
