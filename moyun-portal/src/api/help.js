import { httpGet } from './client';
// ==================== API ====================
/** 帮助中心首页数据（分类 + 精选问题） */
export const getHelpHome = () => {
    return httpGet('/portal/help/home');
};
/** 搜索帮助文章 */
export const searchHelpArticles = (keyword) => {
    return httpGet(`/portal/help/search?keyword=${encodeURIComponent(keyword)}`);
};
