import { httpGet, httpPost, httpPut, httpDelete } from './client';
import type {
  Article,
  ArticleListParams,
  ArticleDetailParams,
  CreateArticleParams,
  UpdateArticleParams,
  PaginationResponse,
} from '@/types/api';

// 获取文章列表
export const getArticleList = (params?: ArticleListParams) => {
  return httpGet<PaginationResponse<Article>>('/article/list', params);
};

// 获取文章详情
export const getArticleDetail = (params: ArticleDetailParams) => {
  return httpGet<Article>(`/article/${params.id}`);
};

// 创建文章
export const createArticle = (params: CreateArticleParams) => {
  return httpPost<Article>('/article', params);
};

// 更新文章
export const updateArticle = (params: UpdateArticleParams) => {
  return httpPut<Article>(`/article/${params.id}`, params);
};

// 删除文章
export const deleteArticle = (id: string) => {
  return httpDelete(`/article/${id}`);
};

// 文章点赞
export const likeArticle = (articleId: string) => {
  return httpPost(`/article/${articleId}/like`);
};

// 取消点赞
export const unlikeArticle = (articleId: string) => {
  return httpDelete(`/article/${articleId}/like`);
};

// 增加文章浏览量
export const incrementView = (articleId: string) => {
  return httpPost(`/article/${articleId}/view`);
};

// 获取推荐文章
export const getRelatedArticles = (articleId: string, limit = 5) => {
  return httpGet<PaginationResponse<Article>>(`/article/${articleId}/related`, { limit });
};

// 获取热门文章
export const getHotArticles = (limit = 10) => {
  return httpGet<PaginationResponse<Article>>('/article/hot', { limit });
};

// 获取精选文章
export const getFeaturedArticles = (limit = 10) => {
  return httpGet<PaginationResponse<Article>>('/article/featured', { limit });
};

// 获取轮播文章
export const getCarouselArticles = () => {
  return httpGet<Article[]>('/article/carousel');
};
