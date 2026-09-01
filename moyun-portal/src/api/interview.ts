import { httpGet, httpPost, httpDelete, httpPut, httpGetList } from './client';
import type {
  InterviewCategoryVO,
  InterviewPositionVO,
  InterviewQuestionVO,
  InterviewQuestionDetailVO,
  InterviewQuestionNeighborVO,
  InterviewQuestionQuery,
  InterviewSubmissionVO,
  InterviewExperienceVO,
  InterviewExperienceQuery,
  InterviewCommentVO,
  InterviewResumeTemplateVO,
  InterviewResumeTemplateQuery,
  InterviewHomeDataVO,
  UserResumeVO,
  UserResumeQuery,
  ResumeAiAdviceVO,
  ResumeParseVO,
  TagVO,
  PageResult,
  UserProfileSnapshotVO,
} from '@/types/api';

// ==================== 首页数据 ====================

export const getInterviewHome = () => {
  return httpGet<InterviewHomeDataVO>('/portal/interview/home');
};

// ==================== 分类 ====================

export const getInterviewCategoryList = () => {
  return httpGet<InterviewCategoryVO[]>('/portal/interview/category/list');
};

// ==================== 岗位字典（v5.9 阶段1：驱动模拟面试岗位选择与画像抽题） ====================

/**
 * 获取启用的岗位字典列表（公开接口）
 * GET /portal/interview/position/list
 * 返回所有 status=active 的岗位，含必备技能与热门公司 JSON 字符串。
 * 用于模拟面试岗位选择、用户档案目标岗位选择等场景。
 */
export const getInterviewPositions = () => {
  return httpGet<InterviewPositionVO[]>('/portal/interview/position/list');
};

// ==================== 题目 ====================

export const getQuestionList = (params?: InterviewQuestionQuery) => {
  return httpGetList<InterviewQuestionVO>('/portal/interview/question/list', params);
};

/**
 * 画像推荐题目（v5.9 阶段1：题库页"为你推荐"）
 * GET /portal/interview/question/recommend?limit=
 * 基于用户画像（薄弱点 + 岗位必备技能 + 热门兜底）三路召回，需登录。
 * 未登录或无画像时返回空列表，前端按需隐藏"为你推荐"模块。
 * 返回的 VO 中 recommendReason / recommendTag 标识推荐来源。
 */
export const getRecommendedQuestions = (limit = 6) => {
  return httpGet<InterviewQuestionVO[]>('/portal/interview/question/recommend', { limit });
};

/**
 * 我的画像快照（迁移自 mockInterview.ts，AI 面试官统一入口）
 * GET /portal/interview/profile?position=&scene=
 * 返回薄弱知识点 + 岗位必备技能，用于题库页/知识图谱页画像展示与语音面试画像抽题。
 */
export const getMyProfile = (params?: { position?: string; scene?: string }) => {
  return httpGet<UserProfileSnapshotVO>('/portal/interview/profile', params);
};

export const getQuestionDetail = (questionId: string | number) => {
  return httpGet<InterviewQuestionDetailVO>(`/portal/interview/question/${questionId}`);
};

/**
 * 相邻题目导航（v12.0 做题页/阅读页连续浏览）
 * GET /portal/interview/question/{id}/neighbor?practiceMode=&difficulty=&keyword=&questionType=&categoryId=
 * 按来源列表页的筛选条件返回上一题/下一题（排序与列表一致：sort 升序 + createTime 降序），
 * 同时返回当前序号与总数，用于展示"第 x / 共 n 题"进度。
 * - 做题页（choice/coding）传 practiceMode + difficulty/keyword
 * - 阅读详情页传 categoryId/questionType/difficulty/keyword（与题库列表页筛选同源）
 */
export const getQuestionNeighbor = (
  questionId: string | number,
  params?: Pick<InterviewQuestionQuery, 'practiceMode' | 'difficulty' | 'keyword' | 'questionType' | 'categoryId'>
) => {
  return httpGet<InterviewQuestionNeighborVO>(
    `/portal/interview/question/${questionId}/neighbor`,
    params
  );
};

export const submitAnswer = (
  questionId: string | number,
  body: {
    code?: string;
    content?: string;
    language?: string;
    answerType?: 'code' | 'text' | 'design' | 'choice' | 'reading';
    note?: string;
    /** 选择题作答：单选如 "A"，多选如 "A,B,C"（服务端权威判分） */
    answer?: string;
  }
) => {
  return httpPost<InterviewSubmissionVO>(
    `/portal/interview/question/${questionId}/submit`,
    body
  );
};

/**
 * 记录题目阅读行为（v12.0 阅读闭环）
 * POST /portal/interview/question/{id}/read
 * 详情页加载/停留时上报：同用户同题目同一天仅记一次成长事件（read_question）。
 * 若携带 note，则同时落一条阅读笔记提交并记 write_note 成长事件。
 */
export const recordQuestionRead = (
  questionId: string | number,
  body?: { note?: string }
) => {
  return httpPost<{ readRecorded: boolean; noteRecorded: boolean }>(
    `/portal/interview/question/${questionId}/read`,
    body ?? {}
  );
};

// ==================== 题目点赞 ====================

export const toggleQuestionLike = (questionId: string | number) => {
  return httpPost<{ liked: boolean; likeCount: number }>(
    `/portal/interview/question/${questionId}/like`
  );
};

// ==================== 题目收藏 ====================

export const toggleQuestionBookmark = (
  questionId: string | number,
  note?: string
) => {
  return httpPost<{ bookmarked: boolean }>(
    `/portal/interview/question/${questionId}/bookmark`,
    note !== undefined ? { note } : {}
  );
};

// ==================== 精选笔记 ====================

// 查询某题目的精选笔记列表（公开接口）
export const getFeaturedNotes = (questionId: string | number) => {
  return httpGet<InterviewSubmissionVO[]>(
    `/portal/interview/question/${questionId}/featured-notes`
  );
};

// ==================== 我的（个人中心） ====================

// 我的收藏题目列表
export const getMyBookmarkList = (params?: { pageNum?: number; pageSize?: number }) => {
  return httpGetList<InterviewQuestionVO>('/portal/interview/bookmark/my', params);
};

// 我的答题历史
export const getMySubmissionList = (params?: { pageNum?: number; pageSize?: number }) => {
  return httpGetList<InterviewSubmissionVO>('/portal/interview/submission/my', params);
};

// 我的面经列表（含草稿/待审核）
export const getMyExperienceList = (params?: InterviewExperienceQuery) => {
  return httpGetList<InterviewExperienceVO>('/portal/interview/experience/my', params);
};

// ==================== 面经 ====================

export const getExperienceList = (params?: InterviewExperienceQuery) => {
  return httpGetList<InterviewExperienceVO>('/portal/interview/experience/list', params);
};

export const getExperienceDetail = (experienceId: string | number) => {
  return httpGet<InterviewExperienceVO>(
    `/portal/interview/experience/${experienceId}`
  );
};

export const toggleExperienceLike = (experienceId: string | number) => {
  return httpPost<{ liked: boolean; likeCount: number }>(
    `/portal/interview/experience/${experienceId}/like`
  );
};

// 发布面经
export const publishExperience = (body: {
  title: string;
  company: string;
  position?: string;
  year?: number;
  month?: number;
  content: string;
  summary?: string;
  coverImage?: string;
  tags?: string;
  status?: 'draft' | 'pending';
}) => {
  return httpPost<InterviewExperienceVO>('/portal/interview/experience', body);
};

// 更新面经
export const updateExperience = (
  id: string | number,
  body: Partial<{
    title: string;
    company: string;
    position: string;
    year: number;
    month: number;
    content: string;
    summary: string;
    coverImage: string;
    tags: string;
  }>
) => {
  return httpPut<InterviewExperienceVO>(`/portal/interview/experience`, { id, ...body });
};

// 删除面经
export const deleteExperience = (experienceId: string | number) => {
  return httpDelete<{ success: boolean }>(`/portal/interview/experience/${experienceId}`);
};

// ==================== 面经评论 ====================

export const getCommentList = (params: {
  experienceId: string | number;
  pageNum?: number;
  pageSize?: number;
}) => {
  return httpGetList<InterviewCommentVO>('/portal/interview/comment/list', params);
};

export const publishComment = (body: {
  experienceId: string | number;
  content: string;
  parentId?: string | number;
  replyToUserId?: string | number;
}) => {
  return httpPost<InterviewCommentVO>('/portal/interview/comment', body);
};

export const toggleCommentLike = (commentId: string | number) => {
  return httpPost<{ liked: boolean; likeCount: number }>(
    `/portal/interview/comment/${commentId}/like`
  );
};

// 删除评论
export const deleteComment = (commentId: string | number) => {
  return httpDelete<{ success: boolean }>(`/portal/interview/comment/${commentId}`);
};

// ==================== 简历模板 ====================

export const getResumeTemplateList = (params?: InterviewResumeTemplateQuery) => {
  return httpGetList<InterviewResumeTemplateVO>(
    '/portal/interview/resume/list',
    params
  );
};

export const getResumeTemplateDetail = (templateId: string | number) => {
  return httpGet<InterviewResumeTemplateVO>(`/portal/interview/resume/${templateId}`);
};

export const downloadResumeTemplate = (templateId: string | number) => {
  return httpGet<{ downloadUrl: string }>(
    `/portal/interview/resume/${templateId}/download`
  );
};

export const toggleResumeTemplateLike = (templateId: string | number) => {
  return httpPost<{ liked: boolean; likeCount: number }>(
    `/portal/interview/resume/${templateId}/like`
  );
};

// ==================== 通用标签系统 ====================

export const bindTagsToEntity = (data: {
  entityType: string;
  entityId: string | number;
  tagIds?: (string | number)[];
  tagNames?: string[];
  module?: string;
}) => {
  return httpPost<void>('/portal/tag/bind', data);
};

export const getHotTags = (module?: string, limit = 20) => {
  return httpGet<TagVO[]>(
    `/portal/tag/hot?${[`module=${encodeURIComponent(module ?? '')}`, `limit=${limit}`]
      .filter((x) => x)
      .join('&')}`
  );
};

export const searchTags = (keyword: string) => {
  return httpGet<TagVO[]>(`/portal/tag/search?keyword=${encodeURIComponent(keyword)}`);
};

// 兼容类型别名（向后兼容）
export type { PageResult, TagVO };

// ==================== 用户简历（第2期）====================

export const getMyResumeList = (params?: UserResumeQuery) => {
  return httpGetList<UserResumeVO>('/portal/interview/resume/user/list', params);
};

export const getResumeDetail = (id: string | number) => {
  return httpGet<UserResumeVO>(`/portal/interview/resume/user/${id}`);
};

export const saveResume = (data: UserResumeVO) => {
  return httpPost<string | number>('/portal/interview/resume/user/save', data as unknown as Record<string, any>);
};

export const deleteResume = (id: string | number) => {
  return httpDelete<number>(`/portal/interview/resume/user/${id}`);
};

export const copyResume = (id: string | number) => {
  return httpPost<string | number>(`/portal/interview/resume/user/${id}/copy`);
};

export const getResumeVersions = (id: string | number) => {
  return httpGet<UserResumeVO[]>(`/portal/interview/resume/user/${id}/versions`);
};

export const exportResumePdf = (id: string | number) => {
  return httpPost<UserResumeVO>(`/portal/interview/resume/user/${id}/export`);
};

export const scoreResume = (id: string | number) => {
  return httpPost<UserResumeVO>(`/portal/interview/resume/user/${id}/score`);
};

/**
 * 获取简历 AI 改进建议（v5.9 阶段2）
 * POST /portal/interview/resume/user/{id}/ai-advice
 * 基于评分明细与岗位匹配度生成改进建议，当前规则化，后期接入 AI 模型。
 */
export const getResumeAiAdvice = (id: string | number) => {
  return httpPost<ResumeAiAdviceVO>(`/portal/interview/resume/user/${id}/ai-advice`);
};

/**
 * 解析简历附件（v10.12）
 * POST /portal/interview/resume/user/parse（multipart）
 * 上传 PDF/Word/TXT，后端抽取文本并结构化解析，字段语义对齐在线简历表单
 */
export const parseResumeAttachment = (file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  return httpPost<ResumeParseVO>('/portal/interview/resume/user/parse', formData);
};

export const updateResumeStatus = (id: string | number, status: string) => {
  return httpPut<number>(
    `/portal/interview/resume/user/${id}/status`,
    { status }
  );
};

// ==================== 附件简历（v10.22） ====================

/**
 * 附件简历列表（v10.22）
 * GET /portal/interview/resume/user/attachments
 * 返回当前用户的附件简历记录（sourceType=attachment），含 sourceFileUrl/sourceFileName
 */
export const getAttachmentList = () => {
  return httpGet<UserResumeVO[]>('/portal/interview/resume/user/attachments');
};

/**
 * 附件转在线简历（v10.22）
 * POST /portal/interview/resume/user/{id}/convert-to-online
 * 将附件简历的结构化解析结果写入在线简历表单字段，转为可编辑的在线简历
 * 返回新在线简历 ID
 */
export const convertAttachmentToOnline = (id: number | string) => {
  return httpPost<number>(`/portal/interview/resume/user/${id}/convert-to-online`);
};

