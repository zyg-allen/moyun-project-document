import { httpPost } from './client';
/**
 * 提交举报
 * POST /portal/report/submit
 * 需要登录（PortalJwtAuthenticationTokenFilter 鉴权）
 */
export const submitReport = (params) => {
    const payload = { ...params };
    // 后端 images 字段为 String（JSON 数组序列化）
    if (Array.isArray(params.images) && params.images.length > 0) {
        payload.images = JSON.stringify(params.images);
    }
    else {
        delete payload.images;
    }
    return httpPost('/portal/report/submit', payload);
};
/**
 * 提交反馈
 * POST /portal/feedback/submit
 * 需要登录（PortalJwtAuthenticationTokenFilter 鉴权）
 */
export const submitFeedback = (params) => {
    return httpPost('/portal/feedback/submit', params);
};
