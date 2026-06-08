import {httpDelete, httpGet, httpGetList, httpPost, httpPut} from './client';
import type {
    Article,
    ArticleDetailParams,
    ArticleListParams,
    CreateArticleParams,
    UpdateArticleParams,
} from '@/types/api';

// ==================== 首页模块化接口（方案A：核心聚合 + 非核心独立） ====================

/** 核心文章数据类型 */
export interface HomeArticlesData {
    carouselArticles: Article[];
    featuredArticles: Article[];
    hotArticles: Article[];
    latestArticles: Article[];
}

/** 分类简化类型 */
export interface HomeCategoryItem {
    id: number;
    name: string;
    slug: string;
    icon: string;
}

/** 标签简化类型 */
export interface HomeTagItem {
    id: number;
    name: string;
    slug: string;
}

/** 友情链接简化类型 */
export interface HomeFriendLinkItem {
    id: number;
    name: string;
    url: string;
    logo: string;
}

/** 作者简化类型 */
export interface HomeAuthorItem {
    id: number;
    username: string;
    nickname: string;
    avatar: string;
    bio: string;
    articleCount: number;
    followerCount: number;
}

/** 分类推荐文章类型 */
export interface HomeCategoryArticleItem {
    categoryId: number;
    categoryName: string;
    articles: Article[];
}

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

// 核心模块：获取首页文章数据（轮播+精选+热门+最新）
export const getHomeArticles = () => {
    return httpGet<HomeArticlesData>('/portal/home/articles');
};

// 非核心模块：获取首页分类列表
export const getHomeCategories = () => {
    return httpGet<HomeCategoryItem[]>('/portal/home/categories');
};

// 非核心模块：获取首页热门标签
export const getHomeTags = () => {
    return httpGet<HomeTagItem[]>('/portal/home/tags');
};

// 非核心模块：获取首页友情链接
export const getHomeFriendLinks = () => {
    return httpGet<HomeFriendLinkItem[]>('/portal/home/friendLinks');
};

// 非核心模块：获取首页名家列表
export const getHomeAuthors = () => {
    return httpGet<HomeAuthorItem[]>('/portal/home/authors');
};

// 非核心模块：获取首页分类推荐文章
export const getHomeCategoryArticles = () => {
    return httpGet<HomeCategoryArticleItem[]>('/portal/home/categoryArticles');
};

// 缓存管理：刷新首页所有缓存
export const refreshHomeCache = () => {
    return httpPost('/portal/home/refresh');
};

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

// 文章点赞/取消点赞 - 统一接口
export const toggleLikeArticle = (articleId: string) => {
    return httpPost<LikeResult>(`/portal/article/${articleId}/like`);
};

// 检查点赞状态
export const checkLikeStatus = (articleId: string) => {
    return httpGet<LikeResult>(`/portal/article/${articleId}/like/status`);
};

// 文章点赞（保留旧接口兼容）
export const likeArticle = (articleId: string) => {
    return toggleLikeArticle(articleId);
};

// 取消点赞（保留旧接口兼容）
export const unlikeArticle = (articleId: string) => {
    return toggleLikeArticle(articleId);
};

// 增加文章浏览量 - 行业标准防刷逻辑
export const incrementView = (articleId: string) => {
    return httpPost<ViewResult>(`/portal/article/${articleId}/view`);
};

// 获取推荐文章
export const getRelatedArticles = (articleId: string, limit = 5) => {
    return httpGetList<Article>(`/portal/article/${articleId}/related`, {limit});
};

// 获取热门文章
export const getHotArticles = (limit = 10) => {
    return httpGetList<Article>('/portal/article/hot', {limit});
};

// 获取精选文章
export const getFeaturedArticles = (limit = 10) => {
    return httpGetList<Article>('/portal/article/featured', {limit});
};

// 获取轮播文章
export const getCarouselArticles = () => {
    return httpGet<Article[]>('/portal/article/carousel');
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

// 按分类获取文章列表
export const getArticlesByCategory = (categoryId?: string, categoryName?: string, pageSize = 10) => {
    return httpGetList<Article>('/portal/article/byCategory', {categoryId, categoryName, pageSize});
};

// 获取分类推荐文章
export const getCategoryRecommendedArticles = (categoryName?: string, categoryId?: string, limit = 8) => {
    return httpGetList<Article>('/portal/article/categoryRecommended', {categoryName, categoryId, limit});
};
