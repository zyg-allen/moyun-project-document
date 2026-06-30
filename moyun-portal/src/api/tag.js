import { httpGet, httpPost } from './client';
// 获取热门标签
export const getHotTags = (limit = 20) => {
    return httpGet('/portal/tag/hot', { limit });
};
// 搜索标签
export const searchTagList = (keyword) => {
    return httpGet('/portal/tag/search', { keyword });
};
// 创建新标签
export const createNewTag = (name) => {
    return httpPost('/portal/tag/create', { name });
};
// 获取标签建议
export const getRecommendTags = (title, category) => {
    return httpGet('/portal/tag/recommend', { title, category });
};
