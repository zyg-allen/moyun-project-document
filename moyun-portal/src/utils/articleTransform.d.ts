import type { Article } from '@/types/api';
/**
 * 统一的 API 文章数据 → 前台 Article 格式转换
 *
 * 解决 HomePage / ListPage 等页面各自实现 transformArticle 导致字段不一致的问题。
 * 合并了 HomePage（字段更全）与 ListPage（嵌套访问）两份实现的回退策略。
 */
export declare function transformArticle(apiArticle: any): Article;
/**
 * 批量转换文章列表
 */
export declare function transformArticleList(apiArticles?: any[]): Article[];
declare const _default: {
    transformArticle: typeof transformArticle;
    transformArticleList: typeof transformArticleList;
};
export default _default;
