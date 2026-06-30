import { httpDelete, httpGet, httpGetList, httpPost, httpPut } from './client';
// 获取文章列表
export const getArticleList = (params) => {
    return httpGetList('/portal/article/list', params);
};
// 获取文章详情（支持ID或slug）
export const getArticleDetail = (params) => {
    return httpGet(`/portal/article/${params.id}`);
};
// 创建文章
export const createArticle = (params) => {
    return httpPost('/portal/article', params);
};
// 发布文章（发布后进入待审核状态 pending）
export const publishArticle = (params) => {
    return httpPost('/portal/article/publish', params);
};
// 保存草稿（真实入库，返回草稿详情含 id）
export const saveDraft = (params) => {
    return httpPost('/portal/article/draft', params);
};
// 我的文章列表（前台，按 authorId + status 查询，authorId 后端自动取当前登录用户）
export const getMyArticles = (params) => {
    return httpGet('/portal/article/my', params);
};
// 更新文章
export const updateArticle = (params) => {
    return httpPut('/portal/article', params);
};
// 删除文章
export const deleteArticle = (id) => {
    return httpDelete(`/portal/article/${id}`);
};
// 文章点赞/取消点赞 - 统一接口
export const toggleLikeArticle = (articleId) => {
    return httpPost(`/portal/article/${articleId}/like`);
};
// 增加文章浏览量 - 行业标准防刷逻辑
export const incrementView = (articleId) => {
    return httpPost(`/portal/article/${articleId}/view`);
};
// 获取分类推荐文章
export const getCategoryRecommendedArticles = (categoryName, params, limit = 8) => {
    return httpGetList('/portal/article/categoryRecommended', { categoryName, limit, ...params });
};
// 获取首页数据汇总（保留旧接口，向后兼容）
export const getHomeData = () => {
    return httpGet('/portal/article/home');
};
// 文章收藏/取消收藏 - 统一接口
export const toggleBookmarkArticle = (articleId) => {
    return httpPost(`/portal/bookmark/${articleId}/toggle`);
};
