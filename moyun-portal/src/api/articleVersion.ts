import { httpGet, httpPost } from './client';

/** 版本列表项（不含大字段） */
export interface ArticleVersionItem {
  id: number;
  articleId: number;
  versionNo: number;
  title: string;
  excerpt?: string;
  operatorId?: number;
  createdTime?: string;
}

/** 版本详情（含完整内容） */
export interface ArticleVersionDetail extends ArticleVersionItem {
  content?: string;
  contentMarkdown?: string;
}

/** 版本对比单个版本 */
export interface VersionDiffSide {
  found: boolean;
  id?: number;
  versionNo?: number;
  title?: string;
  content?: string;
  contentMarkdown?: string;
  excerpt?: string;
  createdTime?: string;
}

/** 版本对比结果 */
export interface VersionDiffResult {
  v1: VersionDiffSide;
  v2: VersionDiffSide;
}

/**
 * 版本列表（需登录）
 * GET /portal/article/{id}/versions
 */
export const getArticleVersions = (articleId: string | number) => {
  return httpGet<ArticleVersionItem[]>(`/portal/article/${articleId}/versions`);
};

/**
 * 版本详情（需登录）
 * GET /portal/article/version/{versionId}
 */
export const getArticleVersionDetail = (versionId: string | number) => {
  return httpGet<ArticleVersionDetail>(`/portal/article/version/${versionId}`);
};

/**
 * 回滚版本（需登录）
 * POST /portal/article/{id}/rollback/{versionId}
 * 返回回滚后生成的新版本快照
 */
export const rollbackArticleVersion = (articleId: string | number, versionId: string | number) => {
  return httpPost<ArticleVersionDetail | null>(`/portal/article/${articleId}/rollback/${versionId}`);
};

/**
 * 版本对比（需登录）
 * GET /portal/article/{id}/diff/{v1}/{v2}
 * 仅返回两个版本的 title + content 文本，前端做展示（不实现真正的 diff 算法）
 */
export const diffArticleVersions = (
  articleId: string | number,
  v1: number,
  v2: number,
) => {
  return httpGet<VersionDiffResult>(`/portal/article/${articleId}/diff/${v1}/${v2}`);
};
