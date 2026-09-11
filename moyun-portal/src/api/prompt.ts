import { httpGet, httpGetList } from './client';

// ==================== 类型 ====================

/** 每日写作 prompt */
export interface WritingPromptVO {
  id: string | number;
  promptDate: string;
  title: string;
  description?: string;
  /** 分类（生活/职场/情感/虚构/哲思） */
  category?: string;
  /** 关联特殊日期名称（节日/节气/纪念日） */
  festivalName?: string;
  /** 来源：ai=AI生成 / manual=手动创建 */
  source?: string;
  createdTime?: string;
}

/** 历史 prompt 查询参数 */
export interface PromptHistoryQuery {
  pageNum?: number;
  pageSize?: number;
  category?: string;
}

// ==================== API ====================

/**
 * 今日 prompt（公开）
 * GET /portal/prompt/today
 */
export const getTodayPrompt = () => {
  return httpGet<WritingPromptVO | null>('/portal/prompt/today');
};

/**
 * 历史 prompt（公开，分页）
 * GET /portal/prompt/history
 */
export const getPromptHistory = (params?: PromptHistoryQuery) => {
  return httpGetList<WritingPromptVO>('/portal/prompt/history', params);
};
