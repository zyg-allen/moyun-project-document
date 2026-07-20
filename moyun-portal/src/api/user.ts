import { httpGet, httpPost, httpPut, httpUpload } from './client';
import type {
  User,
  LoginParams,
  LoginResponse,
  RegisterParams,
  RegisterResponse,
  UpdateUserProfileParams,
  UpdatePasswordParams,
  UserStats,
  UserDashboard,
} from '@/types/api';

// 用户登录
export const login = (params: LoginParams) => {
  return httpPost<LoginResponse>('/portal/login', params);
};

// 用户注册
export const register = (params: RegisterParams) => {
  return httpPost<RegisterResponse>('/portal/register', params);
};

// 退出登录
export const logout = () => {
  return httpPost('/portal/logout');
};

// 获取当前用户信息
export const getCurrentUser = () => {
  return httpGet<User>('/portal/user/me');
};

// 更新用户信息
export const updateUserProfile = (params: UpdateUserProfileParams) => {
  return httpPut<User>('/portal/user/profile', params);
};

// 更新密码
export const updatePassword = (params: UpdatePasswordParams) => {
  return httpPut('/portal/user/password', params);
};

// 上传头像
export const uploadAvatar = (file: File) => {
  return httpUpload<User>('/portal/user/avatar', file);
};

// 获取当前登录用户统计信息
export const getUserStats = () => {
  return httpGet<UserStats>('/portal/user/stats');
};

// 获取个人中心 Dashboard 聚合数据（文章/收藏/书架/答题/面经/简历/关注/粉丝/专栏/未读消息/成长等级）
export const getMyDashboard = () => {
  return httpGet<UserDashboard>('/portal/user/me/dashboard');
};

// 获取用户详情
export const getUserById = (userId: string) => {
  return httpGet<User>(`/portal/user/${userId}`);
};

// 获取名家列表
export const getAuthors = (limit = 10) => {
  return httpGet<any[]>('/portal/user/authors', { limit });
};
