import { httpGet, httpPost } from './client';

// ==================== 3.4 刷题日历热力图 ====================

/** 刷题日历单元 */
export interface LearnCalendarCell {
  /** 日期，格式 yyyy-MM-dd */
  date: string;
  /** 当日提交数 */
  count: number;
  /** 当日通过数 */
  successCount: number;
}

/**
 * 刷题日历热力图（需登录）
 * GET /portal/learn/calendar?year=2026
 */
export const getLearnCalendar = (year?: number) => {
  return httpGet<LearnCalendarCell[]>('/portal/learn/calendar', year ? { year } : undefined);
};

// ==================== 3.5 知识图谱 / 标签云 ====================

/** 知识图谱节点 */
export interface KnowledgeNode {
  tagId: number;
  name: string;
  /** 关联题目数 */
  questionCount: number;
  /** 该维度下题目总数（用户视角） */
  total: number;
  /** 已通过题目数 */
  solved: number;
  /** 掌握度 0-100 */
  mastery: number;
}

/** 知识图谱边（标签共现） */
export interface KnowledgeEdge {
  source: number;
  target: number;
  weight: number;
}

/** 知识图谱聚合数据 */
export interface KnowledgeGraph {
  nodes: KnowledgeNode[];
  edges: KnowledgeEdge[];
  userId: number | null;
}

/**
 * 知识图谱 / 标签云（公开）
 * GET /portal/learn/knowledge-graph?userId=123
 * 不传 userId 时，后端在已登录情况下回退到当前用户
 */
export const getKnowledgeGraph = (userId?: number) => {
  return httpGet<KnowledgeGraph>('/portal/learn/knowledge-graph', userId ? { userId } : undefined);
};

// ==================== 3.7 排行榜 / PK ====================

export type LeaderboardType = 'question' | 'score';

/** 排行榜条目 */
export interface LeaderboardItem {
  rank: number;
  userId: number;
  nickname: string;
  avatar: string | null;
  /** 排行主指标值：type=question 时为通过题目数；type=score 时为刷题积分 */
  value: number;
  submitCount: number;
  passedCount: number;
  score: number;
}

/** 排行榜聚合数据 */
export interface Leaderboard {
  type: LeaderboardType;
  list: LeaderboardItem[];
  myRank: number | null;
  myValue: number | null;
  mySubmitCount: number | null;
  myPassedCount: number | null;
  myScore: number | null;
}

/**
 * 排行榜（公开）
 * GET /portal/learn/leaderboard?type=question|score&limit=100
 */
export const getLeaderboard = (type: LeaderboardType = 'question', limit = 100) => {
  return httpGet<Leaderboard>('/portal/learn/leaderboard', { type, limit });
};

// ==================== 3.7 PK 对战（异步对战） ====================

/** PK 场景 */
export type PkScene = '1v1' | 'company';

/** PK 状态 */
export type PkStatus = 'pending' | 'accepted' | 'declined' | 'ongoing' | 'finished';

/** PK 题目简要 */
export interface PkQuestion {
  id: number;
  title: string;
  difficulty: string | null;
}

/** PK 对战 */
export interface PkChallenge {
  id: number;
  challengerId: number;
  opponentId: number;
  status: PkStatus;
  winnerId: number | null;
  challengerScore: number;
  opponentScore: number;
  /** 题目ID列表，逗号分隔 */
  questionIds: string;
  scene: PkScene;
  companyId: number | null;
  createdTime: string | null;
  finishedTime: string | null;
  // 详情/列表扩展字段
  challengerNickname?: string | null;
  challengerAvatar?: string | null;
  opponentNickname?: string | null;
  opponentAvatar?: string | null;
  questions?: PkQuestion[];
}

/** 提交答案结果 */
export interface PkAnswerResult {
  isSuccess: boolean;
  score: number;
  finished: boolean;
  winnerId: number | null;
}

/** 公司挑战榜条目 */
export interface CompanyPkLeaderboardItem {
  rank: number;
  userId: number;
  nickname: string;
  avatar: string | null;
  companyId: number | null;
  companyName: string | null;
  passedCount: number;
}

/** 发起挑战请求体 */
export interface CreatePkChallengeBody {
  opponentId: number;
  scene?: PkScene;
  companyId?: number | null;
}

/** 发起挑战（需登录） POST /portal/learn/pk/challenge */
export const createPkChallenge = (body: CreatePkChallengeBody) => {
  return httpPost<PkChallenge>('/portal/learn/pk/challenge', body as unknown as Record<string, unknown>);
};

/** 接受挑战（需登录） POST /portal/learn/pk/{id}/accept */
export const acceptPkChallenge = (id: number) => {
  return httpPost<boolean>(`/portal/learn/pk/${id}/accept`);
};

/** 拒绝挑战（需登录） POST /portal/learn/pk/{id}/decline */
export const declinePkChallenge = (id: number) => {
  return httpPost<boolean>(`/portal/learn/pk/${id}/decline`);
};

/** 提交答案（需登录） POST /portal/learn/pk/{id}/answer */
export const submitPkAnswer = (id: number, questionId: number, answer: string) => {
  return httpPost<PkAnswerResult>(`/portal/learn/pk/${id}/answer`, { questionId, answer });
};

/** 对战详情（需登录） GET /portal/learn/pk/{id} */
export const getPkDetail = (id: number) => {
  return httpGet<PkChallenge>(`/portal/learn/pk/${id}`);
};

/** 我的对战列表（需登录） GET /portal/learn/pk/my/list?status= */
export const getMyPkChallenges = (status?: PkStatus) => {
  return httpGet<PkChallenge[]>('/portal/learn/pk/my/list', status ? { status } : undefined);
};

/** 公司题目挑战榜（公开） GET /portal/learn/pk/leaderboard/company?companyId=&limit= */
export const getCompanyPkLeaderboard = (companyId?: number, limit = 100) => {
  return httpGet<CompanyPkLeaderboardItem[]>('/portal/learn/pk/leaderboard/company', { companyId, limit });
};
