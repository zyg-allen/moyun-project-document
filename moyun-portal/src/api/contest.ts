import { httpGet, httpGetList, httpPost } from './client';

// ==================== 类型 ====================

/** 创作挑战/征文活动 */
export interface WritingContestVO {
  id: string | number;
  title: string;
  description?: string;
  theme?: string;
  cover?: string;
  startTime?: string;
  endTime?: string;
  voteEndTime?: string;
  prize?: string;
  /** draft/collecting/voting/ended */
  status?: string;
  createdTime?: string;
  updatedTime?: string;
}

/** 活动投稿 */
export interface ContestSubmissionVO {
  id: string | number;
  contestId: string | number;
  userId: string | number;
  articleId: string | number;
  /** pending/shortlisted/eliminated/winner */
  status?: string;
  voteCount?: number;
  rank?: number;
  remark?: string;
  createdTime?: string;
}

/** 活动详情（含投稿列表 + 当前用户投票标记） */
export interface ContestDetailVO {
  contest: WritingContestVO;
  submissions: ContestSubmissionVO[];
  /** 当前用户已投票的投稿ID集合 */
  votedSubmissionIds: (string | number)[];
  /** 当前用户是否已投稿该活动 */
  hasSubmitted: boolean;
}

/** 投票切换返回 */
export interface VoteToggleResult {
  voted: boolean;
  voteCount: number;
}

/** 活动列表查询参数 */
export interface ContestListQuery {
  pageNum?: number;
  pageSize?: number;
  status?: string;
}

// ==================== API ====================

/**
 * 活动列表（公开，分页）
 * GET /portal/contest/list
 */
export const getContestList = (params?: ContestListQuery) => {
  return httpGetList<WritingContestVO>('/portal/contest/list', params);
};

/**
 * 活动详情（公开，含投稿列表）
 * GET /portal/contest/{id}
 */
export const getContestDetail = (id: string | number) => {
  return httpGet<ContestDetailVO>(`/portal/contest/${id}`);
};

/**
 * 投稿（需登录）
 * POST /portal/contest/{id}/submit
 * body: { articleId }
 */
export const submitContest = (id: string | number, articleId: string | number) => {
  return httpPost<string | number>(`/portal/contest/${id}/submit`, { articleId });
};

/**
 * 投票（需登录，toggle）
 * POST /portal/contest/submission/{id}/vote
 */
export const voteSubmission = (id: string | number) => {
  return httpPost<VoteToggleResult>(`/portal/contest/submission/${id}/vote`);
};

/**
 * 我的投稿（需登录，分页）
 * GET /portal/contest/my/submissions
 */
export const getMySubmissions = (params?: ContestListQuery) => {
  return httpGetList<ContestSubmissionVO>('/portal/contest/my/submissions', params);
};
