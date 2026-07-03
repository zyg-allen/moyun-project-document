import { httpGet, httpPost, httpGetList } from './client';
import type { MockInterviewDetailVO, MockInterviewQaVO, MockInterviewVO } from '@/types/api';

/**
 * AI 模拟面试官（任务 3.10 学习者成长闭环）
 * 后端：PortalMockInterviewController，路径 /portal/interview/mock
 * 简化实现：规则化评分，不依赖外部 LLM。
 */

/** 开始面试请求体 */
export interface MockInterviewStartBody {
  /** 面试岗位（如 后端开发） */
  position?: string;
  /** 面试场景（如 算法/系统设计，对应题目 tags） */
  scene?: string;
}

/** 提交答案请求体 */
export interface MockInterviewAnswerBody {
  /** 题目序号（从 0 开始） */
  questionIdx: number;
  /** 用户回答 */
  answer: string;
}

/**
 * 开始模拟面试（需登录）
 * POST /portal/interview/mock/start
 * 按岗位/场景从题库抽取 5 道题，返回面试详情（含初始题目列表）
 */
export const startMockInterview = (data: MockInterviewStartBody) => {
  return httpPost<MockInterviewDetailVO>(
    '/portal/interview/mock/start',
    data as unknown as Record<string, unknown>
  );
};

/**
 * 面试详情（需登录，含问答列表与已答数）
 * GET /portal/interview/mock/{id}
 */
export const getMockInterviewDetail = (id: string | number) => {
  return httpGet<MockInterviewDetailVO>(`/portal/interview/mock/${id}`);
};

/**
 * 提交答案（需登录），返回 AI 规则评分结果（含 score 与 aiFeedback）
 * POST /portal/interview/mock/{id}/answer
 */
export const submitMockAnswer = (id: string | number, body: MockInterviewAnswerBody) => {
  return httpPost<MockInterviewQaVO>(`/portal/interview/mock/${id}/answer`, body as unknown as Record<string, unknown>);
};

/**
 * 结束面试（需登录），生成总结与总分
 * POST /portal/interview/mock/{id}/finish
 */
export const finishMockInterview = (id: string | number) => {
  return httpPost<MockInterviewDetailVO>(`/portal/interview/mock/${id}/finish`);
};

/**
 * 我的模拟面试列表（需登录，分页）
 * GET /portal/interview/mock/my/list
 */
export const getMyMockInterviews = (params?: { pageNum?: number; pageSize?: number }) => {
  return httpGetList<MockInterviewVO>('/portal/interview/mock/my/list', params);
};
