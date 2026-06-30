import {httpDelete, httpGet, httpGetList, httpPost, httpPut} from './client';
import type {
    Article,
    ArticleDetailParams,
    ArticleListParams,
    CreateArticleParams,
    UpdateArticleParams,
} from '@/types/api';

// ==================== 首页模块化接口（方案A：核心聚合 + 非核心独立） ====================

/** 点赞结果类型 */
export interface LikeResult {
    isLiked: boolean;
    likeCount: number;
}

/** 阅读量结果类型 */
export interface ViewResult {
    increased: boolean;
    views: number;
    message: string;
}

// 获取文章列表
export const getArticleList = (params?: ArticleListParams) => {
    return httpGetList<Article>('/portal/article/list', params);
};

// 获取文章详情（支持ID或slug）
export const getArticleDetail = (params: ArticleDetailParams) => {
    return httpGet<Article>(`/portal/article/${params.id}`);
};

// 创建文章
export const createArticle = (params: CreateArticleParams) => {
    return httpPost<Article>('/portal/article', params);
};

// 发布文章（发布后进入待审核状态 pending）
export const publishArticle = (params: CreateArticleParams) => {
    return httpPost<{ id: number; status: string; publishedAt: string; message: string }>('/portal/article/publish', params);
};

// 保存草稿（真实入库，返回草稿详情含 id）
export const saveDraft = (params: Partial<Article>) => {
    return httpPost<Article>('/portal/article/draft', params);
};

// 我的文章列表（前台，按 authorId + status 查询，authorId 后端自动取当前登录用户）
export const getMyArticles = (params: {
    pageNum?: number;
    pageSize?: number;
    status?: string; // draft / pending / published / rejected / archived
    title?: string;
    categoryId?: number;
}) => {
    return httpGet<any>('/portal/article/my', params);
};

// 更新文章
export const updateArticle = (params: UpdateArticleParams) => {
    return httpPut<Article>('/portal/article', params);
};

// 删除文章
export const deleteArticle = (id: string) => {
    return httpDelete(`/portal/article/${id}`);
};

// 文章点赞/取消点赞 - 统一接口
export const toggleLikeArticle = (articleId: string) => {
    return httpPost<LikeResult>(`/portal/article/${articleId}/like`);
};

// 增加文章浏览量 - 行业标准防刷逻辑
export const incrementView = (articleId: string) => {
    return httpPost<ViewResult>(`/portal/article/${articleId}/view`);
};

// 获取分类推荐文章
export const getCategoryRecommendedArticles = (categoryName: string, params?: ArticleListParams, limit = 8) => {
    return httpGetList<Article>('/portal/article/categoryRecommended', { categoryName, limit, ...params });
};

// 获取首页数据汇总（保留旧接口，向后兼容）
export const getHomeData = () => {
    return httpGet<{
        carouselArticles: Article[];
        featuredArticles: Article[];
        hotArticles: Article[];
        latestArticles: Article[];
    }>('/portal/article/home');
};

/** 收藏结果类型 */
export interface BookmarkResult {
    isBookmarked: boolean;
    articleId: number;
}

// 文章收藏/取消收藏 - 统一接口
export const toggleBookmarkArticle = (articleId: string) => {
    return httpPost<BookmarkResult>(`/portal/bookmark/${articleId}/toggle`);
};
