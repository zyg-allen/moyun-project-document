export interface HelpCategory {
    id: number;
    name: string;
    icon: string;
    description: string;
    sort: number;
    status: string;
}
export interface HelpArticle {
    id: number;
    categoryId: number;
    title: string;
    content: string;
    viewCount: number;
    likeCount: number;
    sort: number;
    isFeatured: number;
    status: string;
    createTime: string;
}
export interface HelpHomeData {
    categories: HelpCategory[];
    featuredArticles: HelpArticle[];
}
/** 帮助中心首页数据（分类 + 精选问题） */
export declare const getHelpHome: () => Promise<import("./client").ApiResponse<HelpHomeData>>;
/** 搜索帮助文章 */
export declare const searchHelpArticles: (keyword: string) => Promise<import("./client").ApiResponse<HelpArticle[]>>;
