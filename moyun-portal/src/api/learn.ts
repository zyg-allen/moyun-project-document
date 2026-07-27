import { httpGet, httpPost, httpPut, httpDelete, httpGetList } from './client';

// ==================== 类型定义 ====================

/** 学习中心聚合数据 */
export interface LearnDashboard {
  userId: number | null;
  nickname: string | null;
  loggedIn: boolean;
  /** 累计答题数 */
  totalQuestionCount: number;
  /** 累计通过数 */
  successCount: number;
  /** 通过率 0-100 */
  passRate: number;
  /** 连续打卡天数 */
  streakDays: number;
  /** 错题数（未掌握） */
  wrongCount: number;
  /** 今日待复习错题数 */
  todayReviewCount: number;
  /** 进行中的学习计划数 */
  activePlanCount: number;
  /** 今日完成题数 */
  todayDoneCount: number;
  /** 进行中的学习计划列表 */
  activePlans: StudyPlanVO[];
  /** 最近错题预览 */
  recentWrongQuestions: WrongQuestionVO[];
}

/** 学习计划 VO */
export interface StudyPlanVO {
  id: number;
  userId: number;
  title: string;
  /** 计划类型 daily_question/weekly_reading/custom */
  planType: string | null;
  targetCount: number | null;
  targetCategory: string | null;
  startDate: string | null;
  endDate: string | null;
  /** 状态 active/completed/abandoned */
  status: string;
  createdTime: string | null;
  /** 已完成总数 */
  doneCount: number;
  /** 今日完成数 */
  todayDoneCount: number;
  /** 进度百分比 0-100 */
  progressPercent: number;
  /** 连续打卡天数 */
  streakDays: number;
}

/** 错题本 VO */
export interface WrongQuestionVO {
  id: number;
  userId: number;
  questionId: number;
  attemptId: number | null;
  /** 状态 wrong/reviewing/mastered */
  status: string;
  wrongCount: number;
  lastWrongTime: string | null;
  nextReviewTime: string | null;
  createdTime: string | null;
  /** 题目标题 */
  questionTitle: string | null;
  /** 题目难度 easy/medium/hard */
  questionDifficulty: string | null;
  /** 题目标签（逗号分隔） */
  questionTags: string | null;
  questionCategoryId: number | null;
}

/** 错题本统计 */
export interface WrongQuestionCount {
  wrongCount: number;
  unMasteredCount: number;
  reviewingCount: number;
  masteredCount: number;
  todayReviewCount: number;
}

/** 错题查询参数 */
export interface WrongQuestionQuery {
  pageNum?: number;
  pageSize?: number;
  status?: string;
  tag?: string;
  keyword?: string;
}

/** 学习计划保存请求体 */
export interface StudyPlanSaveBody {
  id?: number;
  title: string;
  planType?: string;
  targetCount?: number;
  targetCategory?: string;
  startDate?: string;
  endDate?: string;
  status?: string;
}

// ==================== 3.1 学习中心 ====================

/**
 * 学习中心聚合数据
 * GET /portal/learn/dashboard
 */
export const getLearnDashboard = () => {
  return httpGet<LearnDashboard>('/portal/learn/dashboard');
};

// ==================== 3.2 学习计划 ====================

/**
 * 创建/修改学习计划（需登录）
 * POST /portal/learn/plan/save
 * 返回值为计划ID
 */
export const saveStudyPlan = (data: StudyPlanSaveBody) => {
  return httpPost<number>('/portal/learn/plan/save', data as unknown as Record<string, unknown>);
};

/**
 * 我的学习计划（需登录，分页）
 * GET /portal/learn/plan/my
 */
export const getMyStudyPlans = (params?: { status?: string; pageNum?: number; pageSize?: number }) => {
  return httpGetList<StudyPlanVO>('/portal/learn/plan/my', params);
};

/**
 * 计划进度（需登录）
 * GET /portal/learn/plan/{id}/progress
 */
export const getStudyPlanProgress = (id: number | string) => {
  return httpGet<StudyPlanVO>(`/portal/learn/plan/${id}/progress`);
};

/**
 * 记录今日完成数（需登录）
 * POST /portal/learn/plan/{id}/progress?delta=N
 */
export const recordPlanProgress = (id: number | string, delta = 1) => {
  return httpPost<number>(`/portal/learn/plan/${id}/progress?delta=${delta}`);
};

/**
 * 切换计划状态（需登录）
 * PUT /portal/learn/plan/{id}/status?status=xxx
 */
export const changePlanStatus = (id: number | string, status: string) => {
  return httpPut<number>(`/portal/learn/plan/${id}/status?status=${encodeURIComponent(status)}`);
};

/**
 * 删除计划（需登录）
 * DELETE /portal/learn/plan/{id}
 */
export const deleteStudyPlan = (id: number | string) => {
  return httpDelete<number>(`/portal/learn/plan/${id}`);
};

/**
 * 基于画像自动生成学习计划（v5.9 阶段3，需登录）
 * POST /portal/learn/plan/auto-generate
 * 根据用户画像快照（薄弱点 + 岗位必备技能）自动生成针对性学习计划。
 * 自动去重，无画像时返回空列表。
 * @returns 生成的计划列表
 */
export const autoGeneratePlans = () => {
  return httpPost<StudyPlanVO[]>('/portal/learn/plan/auto-generate');
};

// ==================== 3.3 错题本 ====================

/**
 * 错题列表（需登录，分页）
 * GET /portal/learn/wrong/list
 */
export const getWrongQuestions = (params?: WrongQuestionQuery) => {
  return httpGetList<WrongQuestionVO>('/portal/learn/wrong/list', params);
};

/**
 * 标记题目已掌握（需登录）
 * POST /portal/learn/wrong/{questionId}/master
 */
export const markWrongQuestionMastered = (questionId: number | string) => {
  return httpPost<number>(`/portal/learn/wrong/${questionId}/master`);
};

/**
 * 今日待复习错题（需登录）
 * GET /portal/learn/wrong/review
 */
export const getTodayReviewWrongQuestions = () => {
  return httpGet<WrongQuestionVO[]>('/portal/learn/wrong/review');
};

/**
 * 错题统计（需登录）
 * GET /portal/learn/wrong/count
 */
export const getWrongQuestionCount = () => {
  return httpGet<WrongQuestionCount>('/portal/learn/wrong/count');
};
