import type { Article, ArticleDetailParams, ArticleListParams, CreateArticleParams, UpdateArticleParams } from '@/types/api';
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
export declare const getArticleList: (params?: ArticleListParams) => Promise<import("./client").ApiResponse<import("./client").PaginationResponse<Article>>>;
export declare const getArticleDetail: (params: ArticleDetailParams) => Promise<import("./client").ApiResponse<Article>>;
export declare const createArticle: (params: CreateArticleParams) => Promise<import("./client").ApiResponse<Article>>;
export declare const publishArticle: (params: CreateArticleParams) => Promise<import("./client").ApiResponse<{
    id: number;
    status: string;
    publishedAt: string;
    message: string;
}>>;
export declare const saveDraft: (params: Partial<Article>) => Promise<import("./client").ApiResponse<Article>>;
export declare const getMyArticles: (params: {
    pageNum?: number;
    pageSize?: number;
    status?: string;
    title?: string;
    categoryId?: number;
}) => Promise<import("./client").ApiResponse<any>>;
export declare const updateArticle: (params: UpdateArticleParams) => Promise<import("./client").ApiResponse<Article>>;
export declare const deleteArticle: (id: string) => Promise<import("./client").ApiResponse<unknown>>;
export declare const toggleLikeArticle: (articleId: string) => Promise<import("./client").ApiResponse<LikeResult>>;
export declare const incrementView: (articleId: string) => Promise<import("./client").ApiResponse<ViewResult>>;
export declare const getCategoryRecommendedArticles: (categoryName: string, params?: ArticleListParams, limit?: number) => Promise<import("./client").ApiResponse<import("./client").PaginationResponse<Article>>>;
export declare const getHomeData: () => Promise<import("./client").ApiResponse<{
    carouselArticles: Article[];
    featuredArticles: Article[];
    hotArticles: Article[];
    latestArticles: Article[];
}>>;
/** 收藏结果类型 */
export interface BookmarkResult {
    isBookmarked: boolean;
    articleId: number;
}
export declare const toggleBookmarkArticle: (articleId: string) => Promise<import("./client").ApiResponse<BookmarkResult>>;
