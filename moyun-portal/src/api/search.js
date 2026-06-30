import { httpGet } from './client';
// 搜索
export const search = (params) => {
    return httpGet('/portal/search', params);
};
