import { httpPost, httpGetList } from './client';

// 举报类型
export type ReportType = 'spam' | 'inappropriate' | 'infringement' | 'fraud' | 'other';
// 反馈类型
export type FeedbackType = 'suggestion' | 'bug' | 'experience' | 'other';
// 举报目标类型：comment=评论 / article=文章 / user=用户
export type ReportTargetType = 'comment' | 'article' | 'user';
// 处理状态：pending=待处理/processing=处理中/resolved=已解决/rejected=已驳回
export type HandleStatus = 'pending' | 'processing' | 'resolved' | 'rejected';

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

/** 我的举报/反馈记录（与后端 PortalReport/PortalFeedback 对齐） */
export interface MyReportRecord {
  id: number;
  reportType: ReportType;
  targetUrl?: string;
  targetType?: ReportTargetType;
  targetId?: number;
  description: string;
  contact?: string;
  /** 图片证据（JSON 数组字符串，需自行 parse） */
  images?: string;
  userId?: number;
  username?: string;
  status: HandleStatus;
  handler?: string;
  handleResult?: string;
  handleTime?: string;
  createTime: string;
  updateTime?: string;
}

export interface MyFeedbackRecord {
  id: number;
  feedbackType: FeedbackType;
  subject?: string;
  description: string;
  contact?: string;
  userId?: number;
  username?: string;
  status: HandleStatus;
  handler?: string;
  handleResult?: string;
  handleTime?: string;
  createTime: string;
  updateTime?: string;
}

export interface MyListParams {
  pageNum?: number;
  pageSize?: number;
  status?: HandleStatus;
  reportType?: ReportType;
  feedbackType?: FeedbackType;
}

/** 解析后端 images 字段（JSON 数组字符串）为 URL 数组 */
export const parseImages = (images?: string): string[] => {
  if (!images) return [];
  try {
    const parsed = JSON.parse(images);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
};

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
 * 提交反馈
 * POST /portal/feedback/submit
 * 需要登录（PortalJwtAuthenticationTokenFilter 鉴权）
 */
export const submitFeedback = (params: SubmitFeedbackParams) => {
  return httpPost<string>('/portal/feedback/submit', params);
};

/**
 * 查询我的举报列表（分页，含处理进度）
 * GET /portal/report/my-list
 */
export const getMyReports = (params: MyListParams) => {
  return httpGetList<MyReportRecord>('/portal/report/my-list', params);
};

/**
 * 查询我的反馈列表（分页，含处理进度）
 * GET /portal/feedback/my-list
 */
export const getMyFeedbacks = (params: MyListParams) => {
  return httpGetList<MyFeedbackRecord>('/portal/feedback/my-list', params);
};
