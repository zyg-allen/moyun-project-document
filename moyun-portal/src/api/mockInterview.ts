import { httpGet, httpPost, httpGetList } from './client';
import type { MockInterviewDetailVO, MockInterviewQaVO, MockInterviewVO, UserProfileSnapshotVO } from '@/types/api';

/**
 * AI 模拟面试官（任务 3.10 学习者成长闭环）
 * 后端：PortalMockInterviewController，路径 /portal/interview/mock
 * v5.9 阶段0：支持画像驱动抽题（薄弱点 + 岗位必备技能驱动三路召回）。
 */

/** 开始面试请求体 */
export interface MockInterviewStartBody {
  /** 面试岗位（如 后端开发） */
  position?: string;
  /** 面试场景（如 算法/系统设计，对应题目 tags） */
  scene?: string;
  /** 是否基于用户画像（薄弱点 + 岗位必备技能）驱动抽题；为 true 但无画像时后端自动降级随机 */
  personalized?: boolean;
}

/** 提交答案请求体 */
export interface MockInterviewAnswerBody {
  /** 题目序号（从 0 开始） */
  questionIdx: number;
  /** 用户回答 */
  answer: string;
}

/** 获取画像请求参数 */
export interface MockInterviewProfileQuery {
  /** 目标岗位（可空） */
  position?: string;
  /** 面试场景（可空） */
  scene?: string;
}

/**
 * 开始模拟面试（需登录）
 * POST /portal/interview/mock/start
 * 按岗位/场景从题库抽取 5 道题，返回面试详情（含初始题目列表）。
 * personalized=true 时启用画像驱动抽题。
 */
export const startMockInterview = (data: MockInterviewStartBody) => {
  return httpPost<MockInterviewDetailVO>(
    '/portal/interview/mock/start',
    data as unknown as Record<string, unknown>
  );
};

/**
 * 面试详情（需登录，含问答列表与已答数、画像快照）
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
 * 结束面试（需登录），生成总结与总分，并异步刷新用户画像
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

/**
 * 我的画像快照（需登录）
 * GET /portal/interview/mock/my/profile?position=&scene=
 * 返回当前用户的薄弱点、岗位必备技能与面试统计。
 */
export const getMyMockProfile = (params?: MockInterviewProfileQuery) => {
  return httpGet<UserProfileSnapshotVO>('/portal/interview/mock/my/profile', params);
};
