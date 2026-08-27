import { httpGet, httpPost, httpPut, httpDelete } from './client';
import type {
  ResumeJobTarget, ResumeJobMatchReport, ResumeDeepOptimizeVO, ResumeOptimizeHistory,
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

/** 生成深度优化建议（需 AI 模型） */
export const generateDeepOptimize = (resumeId: number | string, jobTargetId: number | string) => {
  return httpPost<ResumeDeepOptimizeVO>(`/portal/resume/optimize/deep/${resumeId}/${jobTargetId}`);
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
