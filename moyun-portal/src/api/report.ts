import { httpPost } from './client';

// 举报类型
export type ReportType = 'spam' | 'inappropriate' | 'infringement' | 'fraud' | 'other';
// 反馈类型
export type FeedbackType = 'suggestion' | 'bug' | 'experience' | 'other';
// 举报目标类型：comment=评论 / article=文章 / user=用户
export type ReportTargetType = 'comment' | 'article' | 'user';

export interface SubmitReportParams {
  reportType: ReportType;
  targetUrl?: string;
  /** 举报目标类型：comment/article/user，定向举报具体内容时填写（与 targetId 配合） */
  targetType?: ReportTargetType;
  /** 举报目标ID（评论/文章/用户ID） */
  targetId?: string | number;
  description: string;
  contact?: string;
  images?: string[];
}

export interface SubmitFeedbackParams {
  feedbackType: FeedbackType;
  subject?: string;
  description: string;
  contact?: string;
}

/**
 * 提交举报
 * POST /portal/report/submit
 * 需要登录（PortalJwtAuthenticationTokenFilter 鉴权）
 */
export const submitReport = (params: SubmitReportParams) => {
  const payload = { ...params };
  // 后端 images 字段为 String（JSON 数组序列化）
  if (Array.isArray(params.images) && params.images.length > 0) {
    payload.images = JSON.stringify(params.images) as any;
  } else {
    delete (payload as any).images;
  }
  return httpPost<string>('/portal/report/submit', payload);
};

/**
 * 提交内容举报（评论/文章等具体内容定向举报，阶段四 4.3）
 * POST /portal/report/submit
 * @param targetType 目标类型 comment/article/user
 * @param targetId   目标ID
 * @param reportType 举报理由类型
 * @param description 问题描述
 */
export const submitContentReport = (
  targetType: ReportTargetType,
  targetId: string | number,
  reportType: ReportType,
  description: string,
) => {
  return submitReport({ reportType, targetType, targetId, description });
};

/**
 * 提交反馈
 * POST /portal/feedback/submit
 * 需要登录（PortalJwtAuthenticationTokenFilter 鉴权）
 */
export const submitFeedback = (params: SubmitFeedbackParams) => {
  return httpPost<string>('/portal/feedback/submit', params);
};
