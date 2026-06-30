import { httpGet, httpPost, httpPut, httpUpload } from './client';
// 用户登录
export const login = (params) => {
    return httpPost('/portal/login', params);
};
// 用户注册
export const register = (params) => {
    return httpPost('/portal/register', params);
};
// 退出登录
export const logout = () => {
    return httpPost('/portal/logout');
};
// 获取当前用户信息
export const getCurrentUser = () => {
    return httpGet('/portal/user/me');
};
// 更新用户信息
export const updateUserProfile = (params) => {
    return httpPut('/portal/user/profile', params);
};
// 更新密码
export const updatePassword = (params) => {
    return httpPut('/portal/user/password', params);
};
// 发送短信验证码
export const sendSmsCode = (params) => {
    return httpPost('/portal/user/send-sms', params);
};
// 上传头像
export const uploadAvatar = (file) => {
    return httpUpload('/portal/user/avatar', file);
};
// 获取用户统计信息
export const getUserStats = (userId) => {
    return httpGet(userId ? `/portal/user/${userId}/stats` : '/portal/user/stats');
};
// 获取用户详情
export const getUserById = (userId) => {
    return httpGet(`/portal/user/${userId}`);
};
// 获取名家列表
export const getAuthors = (limit = 10) => {
    return httpGet('/portal/user/authors', { limit });
};
