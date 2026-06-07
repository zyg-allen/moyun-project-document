import { httpGet, httpGetList, httpPost, httpPut, httpDelete } from './client';
import type {
  Article,
  ArticleListParams,
  ArticleDetailParams,
  CreateArticleParams,
  UpdateArticleParams,
} from '@/types/api';

// 获取标签列表
export const getTagList = () => {
  return httpGetList<any>('/portal/tag/list');
};

// 获取文章列表
export const getArticleList = (params?: ArticleListParams) => {
  return httpGetList<Article>('/portal/article/list', params);
};

// 获取文章详情
export const getArticleDetail = (params: ArticleDetailParams) => {
  return httpGet<Article>(`/portal/article/${params.id}`);
};

// 创建文章
export const createArticle = (params: CreateArticleParams) => {
  return httpPost<Article>('/portal/article', params);
};

// 更新文章
export const updateArticle = (params: UpdateArticleParams) => {
  return httpPut<Article>(`/portal/article/${params.id}`, params);
};

// 删除文章
export const deleteArticle = (id: string) => {
  return httpDelete(`/portal/article/${id}`);
};

// 文章点赞
export const likeArticle = (articleId: string) => {
  return httpPost(`/portal/article/${articleId}/like`);
};

// 取消点赞
export const unlikeArticle = (articleId: string) => {
  return httpDelete(`/portal/article/${articleId}/like`);
};

// 增加文章浏览量
export const incrementView = (articleId: string) => {
  return httpPost(`/portal/article/${articleId}/view`);
};

// 获取推荐文章
export const getRelatedArticles = (articleId: string, limit = 5) => {
  return httpGetList<Article>(`/portal/article/${articleId}/related`, { limit });
};

// 获取热门文章
export const getHotArticles = (limit = 10) => {
  return httpGetList<Article>('/portal/article/hot', { limit });
};

// 获取精选文章
export const getFeaturedArticles = (limit = 10) => {
  return httpGetList<Article>('/portal/article/featured', { limit });
};

// 获取轮播文章
export const getCarouselArticles = () => {
  return httpGet<Article[]>('/portal/article/carousel');
};

// 获取首页数据汇总
export const getHomeData = () => {
  return httpGet<{
    carouselArticles: Article[];
    featuredArticles: Article[];
    hotArticles: Article[];
    latestArticles: Article[];
  }>('/portal/article/home');
};

// 按分类获取文章列表
export const getArticlesByCategory = (categoryId?: string, categoryName?: string, pageSize = 10) => {
  return httpGetList<Article>('/portal/article/byCategory', { categoryId, categoryName, pageSize });
};

// 获取分类推荐文章
export const getCategoryRecommendedArticles = (categoryName?: string, categoryId?: string, limit = 8) => {
  return httpGetList<Article>('/portal/article/categoryRecommended', { categoryName, categoryId, limit });
};
